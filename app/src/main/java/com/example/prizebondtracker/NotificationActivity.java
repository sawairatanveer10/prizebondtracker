package com.example.prizebondtracker;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Date;

public class NotificationActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    NotificationAdapter adapter;
    ArrayList<NotificationModel> list;
    FirebaseFirestore db;
    private final String appId = "default-app-id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Back button
        toolbar.setNavigationOnClickListener(v -> finish());

        // Clear All button
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_all) {

                String userId = FirebaseAuth.getInstance()
                        .getCurrentUser().getUid();

                db.collection("artifacts")
                        .document(appId)
                        .collection("users")
                        .document(userId)
                        .collection("notifications")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {

                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                doc.getReference().delete();
                            }

                            list.clear();
                            adapter.notifyDataSetChanged();
                        });

                return true;
            }
            return false;
        });

        recyclerView = findViewById(R.id.notificationRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new NotificationAdapter(list);
        recyclerView.setAdapter(adapter);

        loadNotifications();
        markAllNotificationsAsRead();
    }

    private ListenerRegistration notificationListener;

    private void loadNotifications() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        notificationListener = db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    list.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        list.add(new NotificationModel(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("message"),
                                getTimeAgo(doc.getDate("createdAt")),
                                Boolean.TRUE.equals(doc.getBoolean("read")),
                                doc.getString("type")
                        ));
                    }

                    adapter.notifyDataSetChanged();
                });
    }
    private String getTimeAgo(Date timestamp) {

        if (timestamp == null) return "Just now";

        long time = timestamp.getTime();
        long now = System.currentTimeMillis();
        long diff = now - time;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minutes ago";
        if (hours < 24) return hours + " hours ago";
        return days + " days ago";
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
    private void markAllNotificationsAsRead() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("read", true);
                    }
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        markAllNotificationsAsRead();
    }
}