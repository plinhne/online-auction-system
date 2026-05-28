package com.auction.model.pattern.observer;
// Interface cho Subject
public interface AuctionSubject {
    void addObserver(BidObserver observer);
    void removeObserver(BidObserver observer);
    void notifyObservers(double newAmount, int bidderId);
}
