package com.example.prizebondtracker;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BondProbabilityAdapter extends RecyclerView.Adapter<BondProbabilityAdapter.ViewHolder> {

    private Context context;
    private List<WinningProbabilityActivity.Bond> bondList;

    public BondProbabilityAdapter(Context context, List<WinningProbabilityActivity.Bond> bondList) {
        this.context = context;
        this.bondList = bondList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bond_probability, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WinningProbabilityActivity.Bond bond = bondList.get(position);
        holder.tvBondNumber.setText("Bond A-" + bond.number);
        holder.tvDenomination.setText("Denomination: Rs. " + bond.denomination);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BondDetailsActivity.class);
            intent.putExtra("bond_number", bond.number);
            intent.putExtra("denomination", bond.denomination);
            intent.putExtra("historicalWins", bond.historicalWins);
            intent.putExtra("drawsChecked", bond.drawsChecked);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bondList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBondNumber, tvDenomination;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBondNumber = itemView.findViewById(R.id.tvBondNumber);
            tvDenomination = itemView.findViewById(R.id.tvDenomination);
        }
    }
}
