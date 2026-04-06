package com.example.prizebondtracker;

public class DrawScheduleItem {
    private String denomination;
    private String drawDate; // yyyy-MM-dd
    private String drawNumber;
    private String drawCity;

    public DrawScheduleItem(String denomination, String drawDate, String drawNumber, String drawCity) {
        this.denomination = denomination;
        this.drawDate = drawDate;
        this.drawNumber = drawNumber;
        this.drawCity = drawCity;
    }

    public String getDenomination() { return denomination; }
    public String getDrawDate() { return drawDate; }
   public String getDrawNumber() { return drawNumber; }
    public String getDrawCity() { return drawCity; }
}
