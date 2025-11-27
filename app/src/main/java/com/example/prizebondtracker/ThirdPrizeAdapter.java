package com.example.prizebondtracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ThirdPrizeAdapter extends RecyclerView.Adapter<ThirdPrizeAdapter.VH> {

    private List<String> data;
    private String highlight = null;

    public ThirdPrizeAdapter(List<String> data){ this.data = data; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_third_prize, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String num = data.get(position);
        holder.tvNumber.setText(num);
        if(num.equals(highlight)){
            holder.itemView.setBackgroundColor(Color.parseColor("#C8E6C9")); // light green
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    public void updateList(List<String> newList){
        this.data = newList;
        this.highlight = null;
        notifyDataSetChanged();
    }

    // return true if found
    public boolean highlightAndScrollTo(String numberToFind){
        for(int i=0;i<data.size();i++){
            if(data.get(i).equals(numberToFind)){
                highlight = numberToFind;
                notifyItemChanged(i);
                return true;
            }
        }
        return false;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNumber;
        VH(@NonNull View v){
            super(v);
            tvNumber = v.findViewById(R.id.tvItemNumber);
        }
    }
}
