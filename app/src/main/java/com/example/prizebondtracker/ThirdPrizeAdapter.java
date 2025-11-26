package com.example.prizebondtracker;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ThirdPrizeAdapter extends RecyclerView.Adapter<ThirdPrizeAdapter.ViewHolder> {

    private List<String> prizeList;
    private String dummyUserBond = "000126"; // highlighted as user bond

    public ThirdPrizeAdapter(List<String> prizeList) {
        this.prizeList = prizeList;
    }

    @NonNull
    @Override
    public ThirdPrizeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextSize(16f);
        tv.setPadding(16, 8, 16, 8);
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ThirdPrizeAdapter.ViewHolder holder, int position) {
        String number = prizeList.get(position);
        ((TextView) holder.itemView).setText(number);

        if(number.equals(dummyUserBond)){
            holder.itemView.setBackgroundColor(Color.GREEN);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return prizeList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) { super(itemView); }
    }
}
