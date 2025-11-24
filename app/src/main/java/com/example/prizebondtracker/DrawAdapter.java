package com.example.prizebondtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DrawAdapter extends RecyclerView.Adapter<DrawAdapter.DrawViewHolder> {

    private final List<DrawScheduleItem> drawList;

    public DrawAdapter(List<DrawScheduleItem> drawList) {
        this.drawList = drawList;
    }

    @NonNull
    @Override
    public DrawViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_draw_card, parent, false);
        return new DrawViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DrawViewHolder holder, int position) {
        DrawScheduleItem item = drawList.get(position);

        // Parse date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(item.getDrawDate());
            Calendar cal = Calendar.getInstance();
            if(date != null) {
                cal.setTime(date);
            }

            String monthAbbr = new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime());
            String day = new SimpleDateFormat("dd", Locale.getDefault()).format(cal.getTime());

            holder.tvDrawMonth.setText(monthAbbr.toUpperCase(Locale.getDefault()));
            holder.tvDrawDay.setText(day);
        } catch (ParseException e) {
            holder.tvDrawMonth.setText("");
            holder.tvDrawDay.setText("");
        }

        holder.tvDenominationShort.setText(item.getDenomination());
        holder.tvDenominationFull.setText("Prize Bond " + item.getDenomination());
        holder.tvDrawNumber.setText(item.getDrawNumber());
        holder.tvDrawCity.setText(item.getDrawCity());
        holder.btnCountdown.setText("25 Days Left"); // static dummy countdown
    }

    @Override
    public int getItemCount() {
        return drawList.size();
    }

    static class DrawViewHolder extends RecyclerView.ViewHolder {
        TextView tvDrawMonth, tvDrawDay, tvDenominationShort, tvDenominationFull, tvDrawNumber, tvDrawCity;
        MaterialButton btnCountdown;

        public DrawViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDrawMonth = itemView.findViewById(R.id.tvDrawMonth);
            tvDrawDay = itemView.findViewById(R.id.tvDrawDay);
            tvDenominationShort = itemView.findViewById(R.id.tvDenominationShort);
            tvDenominationFull = itemView.findViewById(R.id.tvDenominationFull);
            tvDrawNumber = itemView.findViewById(R.id.tvDrawNumber);
            tvDrawCity = itemView.findViewById(R.id.tvDrawCity);
            btnCountdown = itemView.findViewById(R.id.btnCountdown);
        }
    }
}
