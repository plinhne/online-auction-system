package com.auction.model.auction;

import com.auction.model.item.Item;
import com.auction.model.bid.Bid;

public class Auction {

    private int id;
    private AuctionStatus status;

    private Item item;              // item đang đấu giá
    private double minIncrement;    // bước giá tối thiểu
    private Bid highestBid;         // bid cao nhất

    public Auction(int id, Item item, double minIncrement) {
        this.id = id;
        this.item = item;
        this.minIncrement = minIncrement;
        this.status = AuctionStatus.SCHEDULED;
    }

    public void startAuction() {
        this.status = AuctionStatus.ACTIVE;
    }

    public void endAuction() {
        this.status = AuctionStatus.ENDED;
    }

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE;
    }

    public Item getItem() {
        return item;
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }
}