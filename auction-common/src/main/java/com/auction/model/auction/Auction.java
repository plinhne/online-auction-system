package com.auction.model.auction;

public class Auction {
    private int id;
    private AuctionStatus status;

    public Auction(int id) {
        this.id = id;
        this.status = AuctionStatus.SCHEDULED;
    }

    public void startAuction() {
        this.status = AuctionStatus.ACTIVE;
    }

    public void endAuction() {
        this.status = AuctionStatus.ENDED;
    }
}