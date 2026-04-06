package com.example.prizebondtracker;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        if (!model.isRead()) {
            holder.title.setTypeface(null, Typeface.BOLD);
        } else {
            holder.title.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {

            FirebaseFirestore.getInstance()
                    .collection("artifacts")
                    .document("default-app-id")
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("notifications")
                    .document(model.getId())
                    .update("read", true);
        });

        holder.itemView.setOnLongClickListener(v -> {

            FirebaseFirestore.getInstance()
                    .collection("artifacts")
                    .document("default-app-id")
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .collection("notifications")
                    .document(model.getId())
                    .delete();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title, message, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvTitle);
            message = itemView.findViewById(R.id.tvMessage);
            time = itemView.findViewById(R.id.tvTime);
        }
    }
}