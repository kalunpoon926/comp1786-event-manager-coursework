package com.benson.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private DBManager dbManager;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText etName, etDate, etLocation, etOrganiser, etDescription, etEmail, etPhone, etSearch;
    private Spinner spinnerType, spinnerStatus;
    private Button btnSave;
    private String searchDate = "", searchOrganiser = "";

    private final String[] eventTypes = {"Conference", "Workshop", "Seminar", "Social", "Other"};
    private final String[] eventStatuses = {"Upcoming", "Completed", "Cancelled"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Event Manager");
        }

        dbManager = new DBManager(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(null, this, dbManager);
        recyclerView.setAdapter(adapter);

        etSearch = findViewById(R.id.etSearch);
        etName = findViewById(R.id.etName);
        etDate = findViewById(R.id.etDate);
        etLocation = findViewById(R.id.etLocation);
        etOrganiser = findViewById(R.id.etOrganiser);
        etDescription = findViewById(R.id.etDescription);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, eventTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, eventStatuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        btnSave.setOnClickListener(v -> saveEvent());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                loadEvents();
            }
        });

        loadEvents();
        SyncManager.request(this, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_upload) {
            SyncManager.request(this, (message, ok) -> new AlertDialog.Builder(this).setTitle(ok ? "Upload complete" : "Upload failed").setMessage(message).setPositiveButton("OK", null).show());
            return true;
        } else if (item.getItemId() == R.id.action_cloud_settings) {
            showCloudSettingsDialog();
            return true;
        } else if (item.getItemId() == R.id.action_advanced_search) {
            showAdvancedSearchDialog();
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            Toast.makeText(this, "Event Manager v1.0", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCloudSettingsDialog() {
        android.widget.LinearLayout fields = new android.widget.LinearLayout(this);
        fields.setOrientation(android.widget.LinearLayout.VERTICAL);
        fields.setPadding(32, 8, 32, 8);
        EditText url = new EditText(this), token = new EditText(this);
        url.setHint("Cloud HTTPS URL");
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(SyncManager.cloudUrl(this));
        token.setHint(SyncManager.isConfigured(this) ? "Upload token (leave blank to keep current)" : "Upload token");
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        fields.addView(url); fields.addView(token);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Cloud settings")
                .setView(fields).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String address = url.getText().toString().trim();
            String secret = token.getText().toString().trim();
            if (!address.startsWith("https://")) { url.setError("Enter a HTTPS URL"); return; }
            if (secret.isEmpty() && !SyncManager.isConfigured(this)) { token.setError("Enter the upload token"); return; }
            SyncManager.configure(this, address, secret);
            dialog.dismiss();
            SyncManager.request(this, (message, ok) -> new AlertDialog.Builder(this)
                    .setTitle(ok ? "Upload complete" : "Upload failed")
                    .setMessage(message).setPositiveButton("OK", null).show());
        }));
        dialog.show();
    }

    private void showAdvancedSearchDialog() {
        android.widget.LinearLayout fields = new android.widget.LinearLayout(this);
        fields.setOrientation(android.widget.LinearLayout.VERTICAL);
        fields.setPadding(32, 8, 32, 8);
        EditText date = new EditText(this), organiser = new EditText(this);
        date.setHint("Date (DD/MM/YYYY)"); date.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);
        organiser.setHint("Organiser prefix");
        date.setText(searchDate); organiser.setText(searchOrganiser);
        fields.addView(date); fields.addView(organiser);
        new AlertDialog.Builder(this).setTitle("Advanced search").setView(fields)
                .setPositiveButton("Search", (d, w) -> {
                    String value = date.getText().toString().trim();
                    if (!value.isEmpty() && !isValidDate(value)) { date.setError("Use DD/MM/YYYY"); Toast.makeText(this, "Invalid search date", Toast.LENGTH_SHORT).show(); return; }
                    searchDate = value; searchOrganiser = organiser.getText().toString().trim(); loadEvents();
                }).setNeutralButton("Clear filters", (d, w) -> { searchDate = ""; searchOrganiser = ""; loadEvents(); })
                .setNegativeButton("Cancel", null).show();
    }

    private void loadEvents() {
        String keyword = etSearch.getText().toString().trim();
        String date = searchDate, organiser = searchOrganiser;
        executor.execute(() -> {
            List<Event> events = dbManager.searchEventsAdvanced(keyword, date, organiser);
            runOnUiThread(() -> adapter.setEventList(events));
        });
    }

    private void saveEvent() {
        String name = etName.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String organiser = etOrganiser.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (!validateInput(name, date, location, organiser, email, phone, 0)) return;

        Event event = new Event(name, date, location, organiser, type, status, description, email, phone);
        executor.execute(() -> {
            boolean duplicate = dbManager.isDuplicateEvent(name, date, 0);
            runOnUiThread(() -> {
                if (duplicate) { etName.setError("Name and date already exist"); return; }
                startActivityForResult(ConfirmEventActivity.intent(this, event), 200);
            });
        });
    }

    private boolean validateInput(String name, String date, String location, String organiser, String email, String phone, int excludeId) {
        if (name.isEmpty()) {
            etName.setError("Please enter the event name");
            return false;
        }
        if (location.isEmpty()) {
            etLocation.setError("Please enter the location");
            return false;
        }
        if (organiser.isEmpty()) {
            etOrganiser.setError("Please enter the organiser");
            return false;
        }
        if (date.isEmpty() || !isValidDate(date)) {
            etDate.setError("Please enter a valid date (DD/MM/YYYY)");
            return false;
        }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            return false;
        }
        if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s]{8,15}$")) {
            etPhone.setError("Invalid phone format");
            return false;
        }
        return true;
    }

    private boolean isValidDate(String date) {
        if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) return false;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void showSaveError(Exception ex) {
        runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Could not save")
                .setMessage(ex.getMessage()).setPositiveButton("OK", null).show());
    }

    @Override public void onDetailsClick(Event event) {
        new AlertDialog.Builder(this).setTitle(event.getName())
                .setMessage("Date: " + event.getDate() + "\nLocation: " + event.getLocation() +
                        "\nOrganiser: " + event.getOrganiser() + "\nType: " + event.getType() +
                        "\nStatus: " + event.getStatus() + "\nSessions: " + event.getSessionCount() +
                        "\nDescription: " + event.getDescription() + "\nEmail: " + event.getContactEmail() +
                        "\nPhone: " + event.getContactPhone())
                .setPositiveButton("Sessions", (d, w) -> onSessionsClick(event))
                .setNegativeButton("Close", null).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) { clearForm(); loadEvents(); }
    }

    private void clearForm() {
        etName.setText("");
        etDate.setText("");
        etLocation.setText("");
        etOrganiser.setText("");
        etDescription.setText("");
        etEmail.setText("");
        etPhone.setText("");
        spinnerType.setSelection(0);
        spinnerStatus.setSelection(0);
    }

    @Override
    public void onEditClick(Event event) {
        Intent intent = new Intent(this, EditEventActivity.class);
        intent.putExtra("event_id", event.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure? All sessions of this event will also be deleted.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    executor.execute(() -> {
                        dbManager.deleteEvent(event.getId());
                        runOnUiThread(this::loadEvents);
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onSessionsClick(Event event) {
        Intent intent = new Intent(this, SessionListActivity.class);
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_name", event.getName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }
}
