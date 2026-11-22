package com.benson.eventmanager;

public class Session {
    private int id;
    private int eventId;
    private String name;
    private String time;
    private String speaker;
    private String room;
    private String type;
    private String description;

    public Session() {}

    public Session(int eventId, String name, String time, String speaker, String room, String type, String description) {
        this.eventId = eventId;
        this.name = name;
        this.time = time;
        this.speaker = speaker;
        this.room = room;
        this.type = type;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getSpeaker() { return speaker == null ? "" : speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }
    public String getRoom() { return room == null ? "" : room; }
    public void setRoom(String room) { this.room = room; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }
}