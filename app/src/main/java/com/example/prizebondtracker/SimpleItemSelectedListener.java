package com.example.prizebondtracker;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private final Runnable runnable;

    public SimpleItemSelectedListener(Runnable runnable){
        this.runnable = runnable;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
        if(runnable != null) runnable.run();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}
}
