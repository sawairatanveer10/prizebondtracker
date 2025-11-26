package com.example.prizebondtracker;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Arrays;
import java.util.List;

public class DrawResultsActivity extends AppCompatActivity {

    private Spinner spinnerYear, spinnerDenomination, spinnerDrawDate;
    private RecyclerView rvThirdPrize;
    private Button btnViewPDF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw_results);

        // Toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("View Draw Results");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize spinners
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerDenomination = findViewById(R.id.spinnerDenomination);
        spinnerDrawDate = findViewById(R.id.spinnerDrawDate);


        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(
                this, R.array.dummy_years, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<CharSequence> denomAdapter = ArrayAdapter.createFromResource(
                this, R.array.dummy_denominations, android.R.layout.simple_spinner_item);
        denomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDenomination.setAdapter(denomAdapter);

        ArrayAdapter<CharSequence> dateAdapter = ArrayAdapter.createFromResource(
                this, R.array.dummy_draw_dates, android.R.layout.simple_spinner_item);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDrawDate.setAdapter(dateAdapter);

        // View PDF button (dummy)
        btnViewPDF = findViewById(R.id.btnViewPDF);
        btnViewPDF.setOnClickListener(v ->
                Toast.makeText(this, "View PDF clicked (dummy)", Toast.LENGTH_SHORT).show()
        );

        // RecyclerView setup
        rvThirdPrize = findViewById(R.id.rvThirdPrize);
        rvThirdPrize.setLayoutManager(new LinearLayoutManager(this));

        List<String> thirdPrizeNumbers = Arrays.asList(
                "000123","000124","000125","000126","000127",
                "000128","000129","000130"
        );

        ThirdPrizeAdapter adapter = new ThirdPrizeAdapter(thirdPrizeNumbers);
        rvThirdPrize.setAdapter(adapter);


    }
}
