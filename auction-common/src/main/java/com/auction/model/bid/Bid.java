package com.auction.model.bid;

public class Bid {
    private int id;
    private double amount;
    private BidStatus status;

    public Bid(int id, double amount) {
        this.id = id;
        this.amount = amount;
        this.status = BidStatus.PENDING;
    }

    public double getAmount() {
        return amount;
    }
}