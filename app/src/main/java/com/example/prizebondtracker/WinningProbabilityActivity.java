package com.example.prizebondtracker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WinningProbabilityActivity extends AppCompatActivity {

    private RecyclerView recyclerBonds;
    private List<Bond> registeredBonds;

    public static class Bond {
        String number;
        int denomination;
        int historicalWins;
        int drawsChecked;

        public Bond(String number, int denomination, int historicalWins, int drawsChecked) {
            this.number = number;
            this.denomination = denomination;
            this.historicalWins = historicalWins;
            this.drawsChecked = drawsChecked;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winning_probability);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerBonds = findViewById(R.id.recyclerBonds);
        recyclerBonds.setLayoutManager(new LinearLayoutManager(this));

        loadDummyBonds();
        BondProbabilityAdapter adapter = new BondProbabilityAdapter(this, registeredBonds);
        recyclerBonds.setAdapter(adapter);
    }

    private void loadDummyBonds() {
        registeredBonds = new ArrayList<>();

        registeredBonds.add(new Bond("205001", 1500, 6, 160)); // High
        registeredBonds.add(new Bond("123456", 1500, 4, 160)); // Moderate
        registeredBonds.add(new Bond("777111", 100, 1, 160));  // Low
        registeredBonds.add(new Bond("019945", 750, 3, 160));  // Moderate
        registeredBonds.add(new Bond("000002", 25000, 0, 160));// Low
    }

}
