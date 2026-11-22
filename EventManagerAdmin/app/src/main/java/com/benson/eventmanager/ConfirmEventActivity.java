package com.benson.eventmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmEventActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Event event;

    public static Intent intent(Context context, Event e) {
        Intent i = new Intent(context, ConfirmEventActivity.class);
        i.putExtra("id", e.getId()); i.putExtra("name", e.getName()); i.putExtra("date", e.getDate());
        i.putExtra("location", e.getLocation()); i.putExtra("organiser", e.getOrganiser());
        i.putExtra("type", e.getType()); i.putExtra("status", e.getStatus());
        i.putExtra("description", e.getDescription()); i.putExtra("email", e.getContactEmail());
        i.putExtra("phone", e.getContactPhone());
        return i;
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_confirm_event);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); getSupportActionBar().setTitle("Confirm event"); }
        Intent i = getIntent();
        event = new Event(i.getStringExtra("name"), i.getStringExtra("date"), i.getStringExtra("location"), i.getStringExtra("organiser"), i.getStringExtra("type"), i.getStringExtra("status"), i.getStringExtra("description"), i.getStringExtra("email"), i.getStringExtra("phone"));
        event.setId(i.getIntExtra("id", 0));
        TextView details = findViewById(R.id.tvConfirmDetails);
        details.setText("Name: " + event.getName() + "\n\nDate: " + event.getDate() + "\n\nLocation: " + event.getLocation() +
                "\n\nOrganiser: " + event.getOrganiser() + "\n\nType: " + event.getType() + "\n\nStatus: " + event.getStatus() +
                "\n\nDescription: " + orDash(event.getDescription()) + "\n\nContact email: " + orDash(event.getContactEmail()) +
                "\n\nContact phone: " + orDash(event.getContactPhone()));
        findViewById(R.id.btnBackToEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirmSave).setOnClickListener(v -> save());
    }
    private String orDash(String value) { return value == null || value.isEmpty() ? "—" : value; }
    private void save() {
        findViewById(R.id.btnConfirmSave).setEnabled(false);
        executor.execute(() -> {
            try {
                DBManager db = new DBManager(this);
                if (event.getId() == 0) db.addEvent(event); else db.updateEvent(event);
                runOnUiThread(() -> { Toast.makeText(this, "Event saved", Toast.LENGTH_SHORT).show(); setResult(RESULT_OK); finish(); });
            } catch (Exception ex) {
                runOnUiThread(() -> { findViewById(R.id.btnConfirmSave).setEnabled(true); new AlertDialog.Builder(this).setTitle("Could not save").setMessage(ex.getMessage()).setPositiveButton("OK", null).show(); });
            }
        });
    }
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
    @Override protected void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
