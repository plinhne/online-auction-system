package com.auction.model.bid;

import com.auction.model.user.User;

public class Bid {
    private int id;
    private double amount;
    private BidStatus status;
    private User bidder;

    public Bid(int id, double amount, User bidder) {
        this.id = id;
        this.amount = amount;
        this.bidder = bidder;
        this.status = BidStatus.PENDING;
    }

    public double getAmount() {
        return amount;
    }
}