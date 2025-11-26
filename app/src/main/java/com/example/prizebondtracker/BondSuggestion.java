package com.example.prizebondtracker;
public class BondSuggestion {
    private String series;
    private int denomination;
    private int numberOfBonds;
    private int cost;

    public BondSuggestion(String series, int denomination, int numberOfBonds, int cost) {
        this.series = series;
        this.denomination = denomination;
        this.numberOfBonds = numberOfBonds;
        this.cost = cost;
    }

    public String getSeries() { return series; }
    public int getDenomination() { return denomination; }
    public int getNumberOfBonds() { return numberOfBonds; }
    public int getCost() { return cost; }
}
