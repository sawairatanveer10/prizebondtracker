package com.example.prizebondtracker;

public class Bond {
    private String bondNumber;
    private String denomination;
    private String purchaseDate;
    private String drawStatus;
    private String aiInsight;

    public Bond() {} // empty constructor (required for Firestore later)

    public Bond(String bondNumber, String denomination, String purchaseDate,
                String drawStatus, String aiInsight) {
        this.bondNumber = bondNumber;
        this.denomination = denomination;
        this.purchaseDate = purchaseDate;
        this.drawStatus = drawStatus;
        this.aiInsight = aiInsight;
    }

    public String getBondNumber() { return bondNumber; }
    public String getDenomination() { return denomination; }
    public String getPurchaseDate() { return purchaseDate; }
    public String getDrawStatus() { return drawStatus; }
    public String getAiInsight() { return aiInsight; }
}
