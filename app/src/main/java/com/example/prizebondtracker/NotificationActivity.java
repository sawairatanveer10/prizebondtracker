package com.example.prizebondtracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class NotificationActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    NotificationAdapter adapter;
    ArrayList<NotificationModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        recyclerView = findViewById(R.id.notificationRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        list.add(new NotificationModel("🎉 Congratulations!", "You won Rs 1,500 on your 750 prize bond!", "2 min ago"));
        list.add(new NotificationModel("📢 New Draw Result", "Prize bond 15000 draw result is available now!", "10 min ago"));
        list.add(new NotificationModel("⭐ Probability Updated", "Your winning probability chart has been refreshed.", "1 hour ago"));
        list.add(new NotificationModel("🔔 Upcoming Draw Reminder", "Next draw is on 31 January. Stay ready!", "Yesterday"));
        list.add(new NotificationModel("⭐ Probability Updated", "Your winning probability chart has been refreshed.", "1 hour ago"));
        list.add(new NotificationModel("🔔 Upcoming Draw Reminder", "Next draw is on 31 January. Stay ready!", "Yesterday"));
        list.add(new NotificationModel("⭐ Probability Updated", "Your winning probability chart has been refreshed.", "1 hour ago"));
        list.add(new NotificationModel("🔔 Upcoming Draw Reminder", "Next draw is on 31 January. Stay ready!", "Yesterday"));


        adapter = new NotificationAdapter(list);
        recyclerView.setAdapter(adapter);
    }
}
