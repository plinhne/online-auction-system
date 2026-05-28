package com.auction.model.pattern.observer;
//Interface cho Client
public interface BidObserver {
    void updateNewBid(int auctionId, double newAmount, int bidderId);
}
