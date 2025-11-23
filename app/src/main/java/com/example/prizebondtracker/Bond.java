package com.example.prizebondtracker;

public class Bond {
    private String id;            // Firestore auto ID
    private String number;
    private String denomination;
    private String purchaseDate;
    private String series;
    private String drawCity;

    public Bond() {}

    public Bond(String number, String denomination, String purchaseDate, String series, String drawCity) {
        this.number = number;
        this.denomination = denomination;
        this.purchaseDate = purchaseDate;
        this.series = series;
        this.drawCity = drawCity;
    }

    // Firestore ID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumber() { return number != null ? number : ""; }
    public void setNumber(String number) { this.number = number; }

    public String getDenomination() { return denomination != null ? denomination : ""; }
    public void setDenomination(String denomination) { this.denomination = denomination; }

    public String getPurchaseDate() { return purchaseDate != null ? purchaseDate : ""; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getSeries() { return series != null ? series : ""; }
    public void setSeries(String series) { this.series = series; }

    public String getDrawCity() { return drawCity != null ? drawCity : ""; }
    public void setDrawCity(String drawCity) { this.drawCity = drawCity; }
}
