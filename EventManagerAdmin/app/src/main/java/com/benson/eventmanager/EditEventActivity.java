package com.benson.eventmanager;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditEventActivity extends AppCompatActivity {

    private EditText etName, etDate, etLocation, etOrganiser, etDescription, etEmail, etPhone;
    private Spinner spinnerType, spinnerStatus;
    private Button btnUpdate;

    private DBManager dbManager;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int eventId;
    private Event currentEvent;

    private final String[] eventTypes = {"Conference", "Workshop", "Seminar", "Social", "Other"};
    private final String[] eventStatuses = {"Upcoming", "Completed", "Cancelled"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Event");
        }

        dbManager = new DBManager(this);
        eventId = getIntent().getIntExtra("event_id", -1);

        etName = findViewById(R.id.etName);
        etDate = findViewById(R.id.etDate);
        etLocation = findViewById(R.id.etLocation);
        etOrganiser = findViewById(R.id.etOrganiser);
        etDescription = findViewById(R.id.etDescription);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        spinnerType = findViewById(R.id.spinnerType);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnUpdate = findViewById(R.id.btnUpdate);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, eventTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, eventStatuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        loadEvent();
        btnUpdate.setOnClickListener(v -> updateEvent());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadEvent() {
        executor.execute(() -> {
            currentEvent = dbManager.findEvent(eventId);
            runOnUiThread(() -> {
                if (currentEvent != null) {
                    etName.setText(currentEvent.getName());
                    etDate.setText(currentEvent.getDate());
                    etLocation.setText(currentEvent.getLocation());
                    etOrganiser.setText(currentEvent.getOrganiser());
                    etDescription.setText(currentEvent.getDescription());
                    etEmail.setText(currentEvent.getContactEmail());
                    etPhone.setText(currentEvent.getContactPhone());
                    setSpinnerSelection(spinnerType, eventTypes, currentEvent.getType());
                    setSpinnerSelection(spinnerStatus, eventStatuses, currentEvent.getStatus());
                }
            });
        });
    }

    private void setSpinnerSelection(Spinner spinner, String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updateEvent() {
        String name = etName.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String organiser = etOrganiser.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String status = spinnerStatus.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (!validateInput(name, date, location, organiser, email, phone, eventId)) return;
        if (currentEvent == null) { Toast.makeText(this, "Event no longer exists", Toast.LENGTH_SHORT).show(); return; }

        currentEvent.setName(name);
        currentEvent.setDate(date);
        currentEvent.setLocation(location);
        currentEvent.setOrganiser(organiser);
        currentEvent.setType(type);
        currentEvent.setStatus(status);
        currentEvent.setDescription(description);
        currentEvent.setContactEmail(email);
        currentEvent.setContactPhone(phone);

        executor.execute(() -> {
            boolean duplicate = dbManager.isDuplicateEvent(name, date, eventId);
            runOnUiThread(() -> {
                if (duplicate) { etName.setError("Name and date already exist"); return; }
                startActivityForResult(ConfirmEventActivity.intent(this, currentEvent), 200);
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

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) finish();
    }
}
