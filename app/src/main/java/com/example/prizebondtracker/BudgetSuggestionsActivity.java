package com.example.prizebondtracker;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BudgetSuggestionsActivity extends AppCompatActivity {

    EditText budgetInput;
    Button generateButton;
    RecyclerView suggestionsRecycler;
    BondSuggestionAdapter adapter;
    List<BondSuggestion> suggestionList;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_suggestions);

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        budgetInput = findViewById(R.id.budgetInput);
        generateButton = findViewById(R.id.generateButton);
        suggestionsRecycler = findViewById(R.id.suggestionsRecycler);

        suggestionList = new ArrayList<>();
        adapter = new BondSuggestionAdapter(suggestionList);
        suggestionsRecycler.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecycler.setAdapter(adapter);

        generateButton.setOnClickListener(v -> {
            String budgetStr = budgetInput.getText().toString().trim();
            if (budgetStr.isEmpty()) {
                Toast.makeText(this, "Enter a valid budget", Toast.LENGTH_SHORT).show();
                return;
            }
            int budget = Integer.parseInt(budgetStr);
            generateDummySuggestions(budget);
        });
    }

    private void generateDummySuggestions(int budget) {
        suggestionList.clear();

        // Detailed allocation based on budget (dummy)
        if (budget <= 2000) {
            suggestionList.add(new BondSuggestion("A", 100, budget/100, budget));
        } else if (budget <= 5000) {
            suggestionList.add(new BondSuggestion("A", 100, 20, 2000));
            suggestionList.add(new BondSuggestion("B", 200, (budget-2000)/200, budget-2000));
        } else if (budget <= 10000) {
            suggestionList.add(new BondSuggestion("A", 100, 30, 3000));
            suggestionList.add(new BondSuggestion("B", 200, 20, 4000));
            suggestionList.add(new BondSuggestion("C", 750, 2, 1500));
            suggestionList.add(new BondSuggestion("D", 1500, 1, 1500));
        } else {
            suggestionList.add(new BondSuggestion("A", 100, 50, 5000));
            suggestionList.add(new BondSuggestion("B", 200, 20, 4000));
            suggestionList.add(new BondSuggestion("C", 750, 3, 2250));
            suggestionList.add(new BondSuggestion("D", 1500, 2, 3000));
        }

        adapter.notifyDataSetChanged();
    }
}
