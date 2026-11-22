package com.benson.eventmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionListActivity extends AppCompatActivity implements SessionAdapter.OnSessionClickListener {

    private RecyclerView recyclerView;
    private SessionAdapter adapter;
    private DBManager dbManager;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int eventId;
    private String eventName;

    private EditText etSearch;
    private final androidx.activity.result.ActivityResultLauncher<String> exportFile =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri == null) return;
                executor.execute(() -> {
                    try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) throw new java.io.IOException("Could not open file");
                        StringBuilder csv = new StringBuilder("Event,Date,Session,Time,Type,Speaker,Room,Description\r\n");
                        Event event = dbManager.findEvent(eventId);
                        if (event == null) throw new java.io.IOException("Event no longer exists");
                        for (Session session : dbManager.listSessionsByEvent(eventId)) {
                            for (String value : new String[]{event.getName(), event.getDate(), session.getName(), session.getTime(), session.getType(), session.getSpeaker(), session.getRoom(), session.getDescription()})
                                csv.append((char)34).append(value.replace(String.valueOf((char)34), new String(new char[]{34,34}))).append((char)34).append(',');
                            csv.setCharAt(csv.length() - 1, '\r'); csv.append('\n');
                        }
                        out.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        runOnUiThread(() -> android.widget.Toast.makeText(this, "Schedule exported", android.widget.Toast.LENGTH_SHORT).show());
                    } catch (Exception ex) {
                        runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("Export failed").setMessage(ex.getMessage()).setPositiveButton("OK", null).show());
                    }
                });
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sessions");
        }

        eventId = getIntent().getIntExtra("event_id", -1);
        eventName = getIntent().getStringExtra("event_name");
        if (eventId < 0) { finish(); return; }

        dbManager = new DBManager(this);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Sessions of: " + eventName);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(null, this);
        recyclerView.setAdapter(adapter);

        etSearch = findViewById(R.id.etSearch);
        Button btnAdd = findViewById(R.id.btnAddSession);
        findViewById(R.id.btnExportSchedule).setOnClickListener(v -> exportFile.launch("event-" + eventId + "-schedule.csv"));
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditSessionActivity.class);
            intent.putExtra("event_id", eventId);
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String keyword = s.toString().trim();
                executor.execute(() -> {
                    List<Session> list = keyword.isEmpty()
                            ? dbManager.listSessionsByEvent(eventId)
                            : dbManager.searchSessions(eventId, keyword);
                    runOnUiThread(() -> adapter.setSessionList(list));
                });
            }
        });

        loadSessions();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadSessions() {
        executor.execute(() -> {
            String keyword = etSearch.getText().toString().trim();
            List<Session> list = keyword.isEmpty() ? dbManager.listSessionsByEvent(eventId) : dbManager.searchSessions(eventId, keyword);
            runOnUiThread(() -> adapter.setSessionList(list));
        });
    }

    @Override public void onDetailsClick(Session session) {
        new AlertDialog.Builder(this).setTitle(session.getName())
                .setMessage("Time: " + session.getTime() + "\nType: " + session.getType() +
                        "\nSpeaker: " + session.getSpeaker() + "\nRoom: " + session.getRoom() +
                        "\nDescription: " + session.getDescription())
                .setPositiveButton("Edit", (d, w) -> onEditClick(session))
                .setNegativeButton("Close", null).show();
    }

    @Override
    public void onEditClick(Session session) {
        Intent intent = new Intent(this, AddEditSessionActivity.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("session_id", session.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Session session) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Session")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> {
                    executor.execute(() -> {
                        dbManager.deleteSession(session.getId());
                        runOnUiThread(this::loadSessions);
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }
}
