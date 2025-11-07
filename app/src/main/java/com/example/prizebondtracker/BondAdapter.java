package com.example.prizebondtracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BondAdapter extends RecyclerView.Adapter<BondAdapter.BondViewHolder> {

    private List<Bond> bondList;

    public BondAdapter(List<Bond> bondList) {
        this.bondList = bondList;
    }

    @NonNull
    @Override
    public BondViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bond_card, parent, false);
        return new BondViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BondViewHolder holder, int position) {
        Bond bond = bondList.get(position);
        holder.tvBondNumber.setText(bond.getBondNumber());
        holder.tvDenomination.setText(bond.getDenomination());
        holder.tvPurchaseDate.setText("Purchased: " + bond.getPurchaseDate());
        holder.tvDrawStatus.setText(bond.getDrawStatus());
        holder.tvAiInsight.setText(bond.getAiInsight());
    }

    @Override
    public int getItemCount() {
        return bondList.size();
    }

    static class BondViewHolder extends RecyclerView.ViewHolder {
        TextView tvBondNumber, tvDenomination, tvPurchaseDate, tvDrawStatus, tvAiInsight;
        ImageButton btnMore;

        BondViewHolder(View itemView) {
            super(itemView);
            tvBondNumber = itemView.findViewById(R.id.tvBondNumber);
            tvDenomination = itemView.findViewById(R.id.tvDenomination);
            tvPurchaseDate = itemView.findViewById(R.id.tvPurchaseDate);
            tvDrawStatus = itemView.findViewById(R.id.tvDrawStatus);
            tvAiInsight = itemView.findViewById(R.id.tvAiInsight);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
