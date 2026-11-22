package com.benson.eventmanager;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditSessionActivity extends AppCompatActivity {

    private EditText etName, etTime, etSpeaker, etRoom, etDescription;
    private Spinner spinnerType;
    private Button btnSave;

    private DBManager dbManager;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private int eventId;
    private int sessionId = -1;
    private Session currentSession;

    private final String[] sessionTypes = {"Workshop", "Presentation", "Panel Discussion", "Networking", "Keynote", "Others"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_session);

        eventId = getIntent().getIntExtra("event_id", -1);
        sessionId = getIntent().getIntExtra("session_id", -1);
        if (eventId < 0) { finish(); return; }
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(sessionId == -1 ? "Add Session" : "Edit Session");
        }

        dbManager = new DBManager(this);

        etName = findViewById(R.id.etName);
        etTime = findViewById(R.id.etTime);
        etSpeaker = findViewById(R.id.etSpeaker);
        etRoom = findViewById(R.id.etRoom);
        etDescription = findViewById(R.id.etDescription);
        spinnerType = findViewById(R.id.spinnerType);
        btnSave = findViewById(R.id.btnSave);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sessionTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        if (sessionId != -1) {
            btnSave.setText("Update Session");
            loadSession();
        } else {
            btnSave.setText("Add Session");
        }

        btnSave.setOnClickListener(v -> saveSession());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadSession() {
        executor.execute(() -> {
            currentSession = dbManager.findSession(eventId, sessionId);
            runOnUiThread(() -> {
                if (currentSession != null) {
                    etName.setText(currentSession.getName());
                    etTime.setText(currentSession.getTime());
                    etSpeaker.setText(currentSession.getSpeaker());
                    etRoom.setText(currentSession.getRoom());
                    etDescription.setText(currentSession.getDescription());
                    for (int i = 0; i < sessionTypes.length; i++) {
                        if (sessionTypes[i].equals(currentSession.getType())) {
                            spinnerType.setSelection(i);
                            break;
                        }
                    }
                }
            });
        });
    }

    private void saveSession() {
        String name = etName.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String speaker = etSpeaker.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Please enter session name");
            return;
        }
        if (time.isEmpty()) {
            etTime.setError("Please enter time");
            return;
        }

        if (sessionId == -1) {
            Session session = new Session(eventId, name, time, speaker, room, type, description);
            executor.execute(() -> {
                dbManager.addSession(session);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Session added", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            if (currentSession == null) { Toast.makeText(this, "Session no longer exists", Toast.LENGTH_SHORT).show(); return; }
            currentSession.setName(name);
            currentSession.setTime(time);
            currentSession.setSpeaker(speaker);
            currentSession.setRoom(room);
            currentSession.setType(type);
            currentSession.setDescription(description);
            executor.execute(() -> {
                dbManager.updateSession(currentSession);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Session updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }
}