package com.auction.model.pattern.singleton;
import com.auction.model.auction.Auction;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private static volatile AuctionManager instance;

    private final ConcurrentHashMap<Integer, Auction> activeAuctions;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public Auction getAuction(int id) {
        return activeAuctions.get(id);
    }
}

