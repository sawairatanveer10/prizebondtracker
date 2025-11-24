package com.example.prizebondtracker;

public class NotificationModel {
    String title, message, time;

    public NotificationModel() {}

    public NotificationModel(String title, String message, String time) {
        this.title = title;
        this.message = message;
        this.time = time;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
}
