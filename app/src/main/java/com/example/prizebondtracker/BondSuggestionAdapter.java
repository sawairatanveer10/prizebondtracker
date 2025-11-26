package com.example.prizebondtracker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BondSuggestionAdapter extends RecyclerView.Adapter<BondSuggestionAdapter.ViewHolder> {

    private List<BondSuggestion> suggestions;

    public BondSuggestionAdapter(List<BondSuggestion> suggestions) {
        this.suggestions = suggestions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bond_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BondSuggestion s = suggestions.get(position);
        holder.series.setText("Series to Buy: " + s.getSeries());
        holder.denomination.setText("Bond Value: " + s.getDenomination() + " PKR");
        holder.number.setText("Number of Bonds: " + s.getNumberOfBonds());
        holder.cost.setText("Total Cost: " + s.getCost() + " PKR");
    }

    @Override
    public int getItemCount() {
        return suggestions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView series, denomination, number, cost;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            series = itemView.findViewById(R.id.seriesText);
            denomination = itemView.findViewById(R.id.denominationText);
            number = itemView.findViewById(R.id.numberText);
            cost = itemView.findViewById(R.id.costText);
        }
    }
}
