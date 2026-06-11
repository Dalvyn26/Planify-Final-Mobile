package com.example.planify.data.remote.model;

public class CalendarEvent {
    public String id;
    public String title;
    public String description;
    public long startTime;  // timestamp ms
    public long endTime;    // timestamp ms
    public String colorId;  // from Google Calendar

    public CalendarEvent() {}

    public CalendarEvent(String id, String title, String description, long startTime, long endTime, String colorId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.colorId = colorId;
    }
}
