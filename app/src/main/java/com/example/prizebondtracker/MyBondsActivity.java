package com.example.prizebondtracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyBondsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    BondAdapter adapter;
    List<Bond> bondList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bonds);

        // ✅ Setup Toolbar and enable back navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Handle click on the toolbar back icon
        toolbar.setNavigationOnClickListener(v -> {
            // Go back to HomeActivity
            Intent intent = new Intent(MyBondsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // ✅ Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerViewBonds);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Sample data (you can replace with Firestore later)
        bondList.add(new Bond("A-123456", "Rs. 750", "10/05/2023", "Upcoming Draw", "High (12%)"));
        bondList.add(new Bond("B-987654", "Rs. 1500", "02/02/2024", "Not Won", "Medium (7%)"));
        bondList.add(new Bond("C-112233", "Rs. 25000", "12/01/2023", "Won", "Low (3%)"));

        adapter = new BondAdapter(bondList);
        recyclerView.setAdapter(adapter);
    }


}
