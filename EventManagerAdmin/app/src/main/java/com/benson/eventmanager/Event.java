package com.benson.eventmanager;

public class Event {
    private int id;
    private int sessionCount;
    private String name;
    private String date;            // DD/MM/YYYY
    private String location;
    private String organiser;
    private String type;
    private String status;
    private String description;
    private String contactEmail;
    private String contactPhone;

    public Event() {}

    public Event(String name, String date, String location, String organiser, String type, String status,
                 String description, String contactEmail, String contactPhone) {
        this.name = name;
        this.date = date;
        this.location = location;
        this.organiser = organiser;
        this.type = type;
        this.status = status;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
    }

    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int count) { sessionCount = count; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getOrganiser() { return organiser; }
    public void setOrganiser(String organiser) { this.organiser = organiser; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description == null ? "" : description; }
    public void setDescription(String description) { this.description = description; }
    public String getContactEmail() { return contactEmail == null ? "" : contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone == null ? "" : contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}