package com.example.prizebondtracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class NotificationAdapter
        extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    ArrayList<NotificationModel> list;

    public NotificationAdapter(ArrayList<NotificationModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);


        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        NotificationModel model = list.get(position);

        holder.title.setText(model.getTitle());
        holder.message.setText(model.getMessage());
        holder.time.setText(model.getTime());

                ImageView icon = holder.itemView.findViewById(R.id.icon);

        if ("upcoming".equals(model.getType())) {
            icon.setImageResource(R.drawable.ic_calender);

        } else if ("draw_result".equals(model.getType())) {
            icon.setImageResource(R.drawable.ic_result);

        } else if ("winning".equals(model.getType())) {
            icon.setImageResource(R.drawable.ic_trophy);

        } else if ("admin".equals(model.getType())) {
            icon.setImageResource(R.drawable.ic_admin);

        } else {
            icon.setImageResource(R.drawable.ic_notifications);
        }

        if (!model.isRead()) {
            holder.title.setTypeface(null, Typeface.BOLD);
        } else {
            holder.title.setAlpha(0.6f);
        }

             holder.itemView.setOnClickListener(v -> {

            Context context = v.getContext();

            // Mark as read
            FirebaseFirestore.getInstance()
                    .collection("artifacts")
                    .document("default-app-id")
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("notifications")
                    .document(model.getId())
                    .update("read", true)
                    .addOnSuccessListener(unused -> {
                        model.setRead(true); // update locally
                        notifyItemChanged(holder.getAdapterPosition());
                    });

            // 🔥 REDIRECTION LOGIC
                 if ("upcoming".equals(model.getType())) {

                     Intent intent = new Intent(context, HomeActivity.class);
                     intent.putExtra("openFragment", "schedule");
                     intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP); // ✅ FIX
                     context.startActivity(intent);
                 } else if ("draw_result".equals(model.getType())) {
                context.startActivity(new Intent(context, DrawResultsActivity.class));
            }
            // admin → no redirect
        });

        holder.deleteBtn.setOnClickListener(v -> {

            int positions = holder.getAdapterPosition();

            FirebaseFirestore.getInstance()
                    .collection("artifacts")
                    .document("default-app-id")
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("notifications")
                    .document(model.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        list.remove(positions);
                        notifyItemRemoved(positions);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, message, time;
        ImageView deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvTitle);
            message = itemView.findViewById(R.id.tvMessage);
            time = itemView.findViewById(R.id.tvTime);
            deleteBtn = itemView.findViewById(R.id.btnDelete);



        }
    }
}