package com.example.prizebondtracker;

public class NotificationModel {

    String id;
    String title;
    String message;
    String time;
    boolean isRead;

    public NotificationModel(String id, String title,
                             String message, String time,
                             boolean isRead) {

        this.id = id;
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public boolean isRead() { return isRead; }
}