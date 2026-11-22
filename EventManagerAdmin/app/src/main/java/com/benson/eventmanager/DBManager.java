package com.benson.eventmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DBManager extends SQLiteOpenHelper {
    private static final int VERSION = 3;
    private final Context context;

    public DBManager(Context context) {
        super(context, "event_manager.db", null, VERSION);
        this.context = context.getApplicationContext();
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, date TEXT NOT NULL, location TEXT NOT NULL, organiser TEXT NOT NULL, type TEXT NOT NULL, status TEXT NOT NULL, description TEXT DEFAULT '', contact_email TEXT DEFAULT '', contact_phone TEXT DEFAULT '')");
        createSessions(db);
        createGuards(db);
    }

    private void createSessions(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, event_id INTEGER NOT NULL REFERENCES events(id) ON DELETE CASCADE, name TEXT NOT NULL, time TEXT NOT NULL, speaker TEXT DEFAULT '', room TEXT DEFAULT '', type TEXT NOT NULL, description TEXT DEFAULT '')");
        db.execSQL("CREATE INDEX sessions_event_id_idx ON sessions(event_id)");
    }

    private void createGuards(SQLiteDatabase db) {
        // Triggers preserve any historical duplicate rows while preventing new duplicates atomically.
        db.execSQL("CREATE TRIGGER events_unique_insert BEFORE INSERT ON events WHEN EXISTS (SELECT 1 FROM events WHERE name=NEW.name COLLATE NOCASE AND date=NEW.date) BEGIN SELECT RAISE(ABORT, 'duplicate event'); END");
        db.execSQL("CREATE TRIGGER events_unique_update BEFORE UPDATE OF name,date ON events WHEN EXISTS (SELECT 1 FROM events WHERE name=NEW.name COLLATE NOCASE AND date=NEW.date AND id<>OLD.id) BEGIN SELECT RAISE(ABORT, 'duplicate event'); END");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE events ADD COLUMN description TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE events ADD COLUMN contact_email TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE events ADD COLUMN contact_phone TEXT DEFAULT ''");
            createSessions(db);
        }
        if (oldVersion < 3) {
            if (oldVersion >= 2) db.execSQL("CREATE INDEX IF NOT EXISTS sessions_event_id_idx ON sessions(event_id)");
            createGuards(db);
        }
    }

    private ContentValues eventValues(Event e) {
        ContentValues v = new ContentValues();
        v.put("name", e.getName()); v.put("date", e.getDate());
        v.put("location", e.getLocation()); v.put("organiser", e.getOrganiser());
        v.put("type", e.getType()); v.put("status", e.getStatus());
        v.put("description", e.getDescription()); v.put("contact_email", e.getContactEmail());
        v.put("contact_phone", e.getContactPhone());
        return v;
    }

    public void addEvent(Event e) {
        long id = getWritableDatabase().insertOrThrow("events", null, eventValues(e));
        e.setId((int) id);
        changed();
    }
    public void updateEvent(Event e) {
        if (getWritableDatabase().update("events", eventValues(e), "id=?", new String[]{String.valueOf(e.getId())}) != 1) throw new IllegalStateException("Event missing");
        changed();
    }
    public void deleteEvent(int id) {
        if (getWritableDatabase().delete("events", "id=?", new String[]{String.valueOf(id)}) > 0) changed();
    }
    public Event findEvent(int id) {
        try (Cursor c = getReadableDatabase().query("events", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? event(c) : null;
        }
    }
    public ArrayList<Event> listEvents() { return queryEvents(null, null); }
    public ArrayList<Event> searchEvents(String keyword) { return searchEventsAdvanced(keyword, "", ""); }
    public ArrayList<Event> searchEventsAdvanced(String date, String organiser) { return searchEventsAdvanced("", date, organiser); }
    public ArrayList<Event> searchEventsAdvanced(String keyword, String date, String organiser) {
        StringBuilder where = new StringBuilder("1=1");
        ArrayList<String> args = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (name LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')");
            String prefix = escapeLike(keyword) + "%"; args.add(prefix); args.add(prefix);
        }
        if (date != null && !date.isEmpty()) { where.append(" AND date=?"); args.add(date); }
        if (organiser != null && !organiser.isEmpty()) { where.append(" AND organiser LIKE ? ESCAPE '\\'"); args.add(escapeLike(organiser) + "%"); }
        return queryEvents(where.toString(), args.toArray(new String[0]));
    }
    private String escapeLike(String s) { return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"); }
    public boolean isDuplicateEvent(String name, String date, int excludeId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM events WHERE name=? COLLATE NOCASE AND date=? AND id<>? LIMIT 1", new String[]{name, date, String.valueOf(excludeId)})) { return c.moveToFirst(); }
    }
    private ArrayList<Event> queryEvents(String where, String[] args) {
        ArrayList<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT events.*, (SELECT COUNT(*) FROM sessions WHERE event_id=events.id) AS session_count FROM events WHERE " + (where == null ? "1=1" : where) + " ORDER BY substr(date,7,4)||substr(date,4,2)||substr(date,1,2), id", args)) {
            while (c.moveToNext()) out.add(event(c));
        }
        return out;
    }
    private Event event(Cursor c) {
        Event e = new Event();
        e.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        int countColumn = c.getColumnIndex("session_count");
        if (countColumn >= 0) e.setSessionCount(c.getInt(countColumn));
        e.setName(c.getString(c.getColumnIndexOrThrow("name")));
        e.setDate(c.getString(c.getColumnIndexOrThrow("date")));
        e.setLocation(c.getString(c.getColumnIndexOrThrow("location")));
        e.setOrganiser(c.getString(c.getColumnIndexOrThrow("organiser")));
        e.setType(c.getString(c.getColumnIndexOrThrow("type")));
        e.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        e.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        e.setContactEmail(c.getString(c.getColumnIndexOrThrow("contact_email")));
        e.setContactPhone(c.getString(c.getColumnIndexOrThrow("contact_phone")));
        return e;
    }
    private ContentValues sessionValues(Session s) {
        ContentValues v = new ContentValues();
        v.put("event_id", s.getEventId()); v.put("name", s.getName()); v.put("time", s.getTime());
        v.put("speaker", s.getSpeaker()); v.put("room", s.getRoom()); v.put("type", s.getType()); v.put("description", s.getDescription());
        return v;
    }
    public void addSession(Session s) {
        long id = getWritableDatabase().insertOrThrow("sessions", null, sessionValues(s));
        s.setId((int) id); changed();
    }
    public void updateSession(Session s) {
        if (getWritableDatabase().update("sessions", sessionValues(s), "id=? AND event_id=?", new String[]{String.valueOf(s.getId()), String.valueOf(s.getEventId())}) != 1) throw new IllegalStateException("Session missing");
        changed();
    }
    public void deleteSession(int id) {
        if (getWritableDatabase().delete("sessions", "id=?", new String[]{String.valueOf(id)}) > 0) changed();
    }
    public Session findSession(int eventId, int sessionId) {
        try (Cursor c = getReadableDatabase().query("sessions", null, "event_id=? AND id=?", new String[]{String.valueOf(eventId), String.valueOf(sessionId)}, null, null, null)) {
            return c.moveToFirst() ? session(c) : null;
        }
    }
    public ArrayList<Session> listSessionsByEvent(int eventId) { return querySessions(eventId, ""); }
    public ArrayList<Session> searchSessions(int eventId, String keyword) { return querySessions(eventId, keyword); }
    private ArrayList<Session> querySessions(int eventId, String keyword) {
        ArrayList<Session> out = new ArrayList<>();
        String where = "event_id=?";
        ArrayList<String> args = new ArrayList<>(); args.add(String.valueOf(eventId));
        if (keyword != null && !keyword.isEmpty()) {
            where += " AND (name LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\')";
            String prefix = escapeLike(keyword) + "%"; args.add(prefix); args.add(prefix);
        }
        try (Cursor c = getReadableDatabase().query("sessions", null, where, args.toArray(new String[0]), null, null, "time, id")) {
            while (c.moveToNext()) out.add(session(c));
        }
        return out;
    }
    public ArrayList<Session> listAllSessions() {
        ArrayList<Session> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("sessions", null, null, null, null, null, "event_id,id")) { while (c.moveToNext()) out.add(session(c)); }
        return out;
    }
    public int getSessionCount(int eventId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sessions WHERE event_id=?", new String[]{String.valueOf(eventId)})) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }
    private Session session(Cursor c) {
        Session s = new Session();
        s.setId(c.getInt(c.getColumnIndexOrThrow("id"))); s.setEventId(c.getInt(c.getColumnIndexOrThrow("event_id")));
        s.setName(c.getString(c.getColumnIndexOrThrow("name"))); s.setTime(c.getString(c.getColumnIndexOrThrow("time")));
        s.setSpeaker(c.getString(c.getColumnIndexOrThrow("speaker"))); s.setRoom(c.getString(c.getColumnIndexOrThrow("room")));
        s.setType(c.getString(c.getColumnIndexOrThrow("type"))); s.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        return s;
    }
    private void changed() { SyncManager.markDirty(context); SyncManager.request(context, null); }
}
