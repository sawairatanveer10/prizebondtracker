package com.example.prizebondtracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class BondNotificationManager {

    private static final String TAG = "BondNotifManager";
    private static final String APP_ID = "default-app-id";
    private static final String CHANNEL_ID = "bond_channel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;
    private final String userId;

    private final SimpleDateFormat format =
            new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    // Listeners — stopListening() mein remove honge
    private ListenerRegistration adminListener;
    private ListenerRegistration drawResultListener;

    public BondNotificationManager(Context context) {
        this.context = context;
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        createNotificationChannel();

        // Pehle preference check karo, phir sab start karo
        checkPreferenceThenStart();
    }

    // =========================================================================
    // STEP 1 — Preference check
    // =========================================================================

    /**
     * receive_draw_notification preference Firestore se read karta hai.
     * Sirf true hone par teeno listeners/checks start karta hai.
     */
    private void checkPreferenceThenStart() {
        db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {

                    Boolean enabled = userDoc.getBoolean("receive_draw_notification");

                    if (Boolean.TRUE.equals(enabled)) {
                        // ✅ Preference ON → teeno start karo
                        startAdminNotificationListener();
                        startDrawResultListener();
                        checkUpcomingDraws();
                    } else {
                        // 🔕 Preference OFF → kuch nahi
                        Log.d(TAG, "Notifications disabled — skipping all checks");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Preference read failed: " + e.getMessage()));
    }

    // =========================================================================
    // TYPE 1 — Admin notifications (real-time listener)
    // =========================================================================

    /**
     * User ki notifications subcollection par real-time listener lagata hai.
     * Admin jab bhi naya notification bhejta hai — foran show ho jata hai.
     *
     * Admin structure:
     *   title, message, createdAt, expiresAt
     *
     * Hum type: "admin" add karte hain apni taraf se.
     */
    private void startAdminNotificationListener() {
        adminListener = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Admin listener error: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {

                        // Sirf naye documents par action lo
                        if (change.getType() != DocumentChange.Type.ADDED) continue;

                        DocumentSnapshot doc = change.getDocument();

                        String title = doc.getString("title");
                        String message = doc.getString("message");
                        String type = doc.getString("type");

                        if (title == null || message == null) continue;

                        // Admin ka document hai agar type field nahi hai
                        // (admin panel sirf title, message, createdAt, expiresAt save karta hai)
                        if (type == null) {
                            // Yeh admin notification hai — system notification dikhao
                            // Firestore mein already save hai (admin ne kar di)
                            // Sirf system bar mein dikhao
                            showSystemNotification(title, message);

                            // type field update karo taake future mein identify ho sake
                            doc.getReference().update("type", "admin")
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Type update failed: " + e.getMessage()));
                        }
                        // draw_result aur upcoming type wali notifications
                        // hum khud save karte hain — unhe yahan skip karo
                    }
                });

        Log.d(TAG, "Admin notification listener started");
    }

    // =========================================================================
    // TYPE 2 — Draw results (real-time listener)
    // =========================================================================

    /**
     * draw_results collection par listener lagata hai.
     * Naya draw result aane par notification save aur show karta hai.
     */
    private void startDrawResultListener() {
        drawResultListener = db.collection("draw_results")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Draw result listener error: " + error.getMessage());
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {

                        // Sirf naye draw results par action lo
                        if (change.getType() != DocumentChange.Type.ADDED) continue;

                        String docId = change.getDocument().getId();
                        String category = change.getDocument().getString("category");
                        String date = change.getDocument().getString("date");

                        if (category == null || date == null) continue;

                        String title = "New Draw Result Available!";
                        String message = "Rs." + category
                                + " prize bond draw results for "
                                + date + " are now available.";

                        // Duplicate check karke save + show karo
                        checkAndSave(title, message, "draw_result",
                                "draw_" + docId);
                    }
                });

        Log.d(TAG, "Draw result listener started");
    }

    // =========================================================================
    // TYPE 3 — Upcoming draws (one-time check)
    // =========================================================================

    /**
     * all_draws collection se current month ke future draws check karta hai.
     * Ek baar check hota hai app open hone par.
     */
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

                        if (dateStr == null || bond == null) continue;

                        try {
                            Date drawDate = format.parse(dateStr);
                            Calendar drawCal = Calendar.getInstance();
                            drawCal.setTime(drawDate);

                            boolean sameMonth =
                                    drawCal.get(Calendar.MONTH) == currentMonth
                                            && drawCal.get(Calendar.YEAR) == currentYear;

                            boolean isFuture = !drawDate.before(today);

                            if (sameMonth && isFuture) {
                                String title = "Upcoming Draw";
                                String message = "Rs." + bond
                                        + " draw is scheduled on " + dateStr;

                                // Duplicate check karke save + show karo
                                checkAndSave(title, message, "upcoming",
                                        "upcoming_" + bond + "_" + dateStr);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Date parse error: " + e.getMessage());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Upcoming draws fetch failed: " + e.getMessage()));
    }

    // =========================================================================
    // SHARED — Duplicate check → Save → Show
    // =========================================================================

    /**
     * Duplicate check karta hai unique key se.
     * Agar notification pehle se exist nahi karti to:
     *   1. Firestore mein save karta hai
     *   2. System notification bar mein dikhata hai
     *
     * @param uniqueKey  har notification ka unique document ID
     *                   draw result: "draw_{docId}"
     *                   upcoming:    "upcoming_{bond}_{date}"
     */
    private void checkAndSave(String title, String message,
                              String type, String uniqueKey) {

        DocumentReference notifRef = db.collection("artifacts")
                .document(APP_ID)
                .collection("users")
                .document(userId)
                .collection("notifications")
                .document(uniqueKey);

        notifRef.get().addOnSuccessListener(existing -> {

            if (existing.exists()) {
                Log.d(TAG, "Already saved — skipping: " + uniqueKey);
                return;
            }

            // Naya notification save karo
            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("type", type);
            data.put("timestamp", new Date());
            data.put("read", false);

            notifRef.set(data)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Saved: " + uniqueKey);
                        // Save hone ke baad system notification dikhao
                        showSystemNotification(title, message);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Save failed: " + e.getMessage()));
        });
    }

    // =========================================================================
    // System notification bar
    // =========================================================================

    private void showSystemNotification(String title, String message) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_noti)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        manager.notify(new Random().nextInt(), builder.build());
    }

    // =========================================================================
    // Notification channel (Android 8+)
    // =========================================================================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Prize Bond Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Prize bond draw result notifications");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // =========================================================================
    // Cleanup — HomeActivity onDestroy() mein call karo
    // =========================================================================

    public void stopListening() {
        if (adminListener != null) {
            adminListener.remove();
            adminListener = null;
            Log.d(TAG, "Admin listener removed");
        }
        if (drawResultListener != null) {
            drawResultListener.remove();
            drawResultListener = null;
            Log.d(TAG, "Draw result listener removed");
        }
    }
}