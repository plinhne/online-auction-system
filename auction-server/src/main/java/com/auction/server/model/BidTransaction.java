package com.auction.server.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private final int id;
    private final int auctionID;
    private final String bidderID; ///int later
    private final double amount;
    private final LocalDateTime placedAt;

    public BidTransaction(int id, int auctionID, String bidderID, double amount) {
        this.id = id;
        this.auctionID = auctionID;
        this.bidderID = bidderID;
        this.amount = amount;
        this.placedAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }
    public String getBidderID() {
        return bidderID;
    }
    public int getAuctionID() {
        return auctionID;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDateTime getPlacedAt() {
        return placedAt;
    }
}
