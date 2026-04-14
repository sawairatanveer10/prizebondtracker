package com.example.prizebondtracker;

public class NotificationModel {

    String id;
    String title;
    String message;
    String time;
    String type;
    boolean isRead;

    public NotificationModel(String id, String title,
                             String message, String time,
                             boolean isRead, String type) {

        this.id = id;
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
        this.type = type;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public boolean isRead() { return isRead; }

    public String getType() { return type; }

    public void setRead(boolean read) {
        this.isRead = read;
    }
}