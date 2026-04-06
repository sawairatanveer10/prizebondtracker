package com.example.prizebondtracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BondNotificationManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String appId = "default-app-id";
    private final String userId;
    private final Context context;

    private final SimpleDateFormat format =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public BondNotificationManager(Context context) {
        this.context = context;
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        checkUpcomingDraws();
        checkWinningResults();
    }

    // ✅ UPCOMING DRAWS (CURRENT MONTH ONLY)
    private void checkUpcomingDraws() {

        db.collection("all_draws")
                .get()
                .addOnSuccessListener(snapshots -> {

                    Calendar todayCal = Calendar.getInstance();
                    Date today = todayCal.getTime();

                    int currentMonth = todayCal.get(Calendar.MONTH);
                    int currentYear = todayCal.get(Calendar.YEAR);

                    for (DocumentSnapshot doc : snapshots) {

                        String dateStr = doc.getString("date");
                        String bond = doc.getString("bondValue");

                        if (dateStr == null) continue;

                        try {
                            Date drawDate = format.parse(dateStr);

                            Calendar drawCal = Calendar.getInstance();
                            drawCal.setTime(drawDate);

                            int drawMonth = drawCal.get(Calendar.MONTH);
                            int drawYear = drawCal.get(Calendar.YEAR);

                            // ✅ Must be current month
                            if (drawMonth == currentMonth
                                    && drawYear == currentYear
                                    && !drawDate.before(today)) {

                                String title = "🔔 Upcoming Draw";
                                String message =
                                        "Rs." + bond + " draw on " + dateStr;

                                checkAndSend(title, message, "upcoming");
                            }

                        } catch (Exception ignored) {
                        }
                    }
                });
    }

    // ✅ WINNING CHECK
    private void checkWinningResults() {

        db.collection("draw_results")
                .get()
                .addOnSuccessListener(results -> {

                    for (DocumentSnapshot resultDoc : results) {

                        String category = resultDoc.getString("category");
                        List<String> winningNumbers =
                                (List<String>) resultDoc.get("numbers");

                        if (category == null || winningNumbers == null)
                            continue;

                        checkUserBonds(category, winningNumbers);
                    }
                });
    }

    private void checkUserBonds(String category,
                                List<String> winningNumbers) {

        db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(userId)
                .collection("bonds")
                .get()
                .addOnSuccessListener(snapshot -> {

                    for (DocumentSnapshot bondDoc : snapshot) {

                        String userBond = bondDoc.getString("number");
                        String denomination =
                                bondDoc.getString("denomination");

                        if (userBond == null || denomination == null)
                            continue;

                        String numericPart = userBond.substring(2);
                        String cleanCategory =
                                denomination.replace("Rs.", "")
                                        .replace(" ", "");

                        if (cleanCategory.equals(category)
                                && winningNumbers.contains(numericPart)) {

                            String title = "🎉 Congratulations!";
                            String message =
                                    "Your bond " + userBond +
                                            " won in Rs." + category + " draw!";

                            checkAndSend(title, message, "win");
                        }
                    }
                });
    }

    // ✅ PREVENT DUPLICATE NOTIFICATIONS
    private void checkAndSend(String title,
                              String message,
                              String type) {

        db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("message", message)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {
                        sendLocalNotification(title, message);
                        saveNotification(title, message, type);
                    }
                });
    }

    private void sendLocalNotification(String title,
                                       String message) {

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(
                            "bond_channel",
                            "Prize Bond Alerts",
                            NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "bond_channel")
                        .setSmallIcon(R.drawable.ic_noti)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true);

        manager.notify(new Random().nextInt(), builder.build());
    }

    private void saveNotification(String title,
                                  String message,
                                  String type) {

        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("message", message);
        map.put("type", type);
        map.put("timestamp", new Date());
        map.put("read", false);

        db.collection("artifacts")
                .document(appId)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .add(map);
    }
}