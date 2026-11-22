CREATE TABLE events (
  device_id TEXT NOT NULL,
  id INTEGER NOT NULL,
  name TEXT NOT NULL,
  date TEXT NOT NULL,
  location TEXT NOT NULL,
  organiser TEXT NOT NULL,
  type TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('Upcoming','Completed','Cancelled')),
  description TEXT NOT NULL DEFAULT '',
  contact_email TEXT NOT NULL DEFAULT '',
  contact_phone TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (device_id, id)
);
CREATE UNIQUE INDEX events_unique_name_date ON events(device_id, name COLLATE NOCASE, date);
CREATE INDEX events_status_date ON events(status, date);
CREATE TABLE sessions (
  device_id TEXT NOT NULL,
  id INTEGER NOT NULL,
  event_id INTEGER NOT NULL,
  name TEXT NOT NULL,
  time TEXT NOT NULL,
  speaker TEXT NOT NULL DEFAULT '',
  room TEXT NOT NULL DEFAULT '',
  type TEXT NOT NULL CHECK (type IN ('Workshop','Presentation','Panel Discussion','Networking','Keynote','Others')),
  description TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (device_id, id),
  FOREIGN KEY (device_id, event_id) REFERENCES events(device_id, id) ON DELETE CASCADE
);
CREATE INDEX sessions_event ON sessions(device_id, event_id);
