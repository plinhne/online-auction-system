package com.auction.server.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction {
    private final int id;
    private final String itemId; ///đổi thành int sau
    private final String sellerId; ///đổi thành int sau
    private final double startingPrice;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private AuctionStatus status;
    private double currentPrice;
    private String leadingBidderID;
    private final List<BidTransaction> bidHistory;

    private static final int SNIPE_WINDOW_SECONDS = 20;
    private static final int EXTENSION_SECONDS = 40;

    public Auction(int id, String itemId, String sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = startingPrice;
        this.status = AuctionStatus.OPEN;
        this.leadingBidderID = null;
        this.bidHistory = new CopyOnWriteArrayList<>();
    }

    public synchronized boolean placeBid(BidTransaction bid) {
        if(status != AuctionStatus.RUNNING || bid.getAmount() <= currentPrice) return false;

        currentPrice = bid.getAmount();
        leadingBidderID = bid.getBidderID();
        bidHistory.add(bid);

        //anti-sniping: nếu có bid tới trong 20s cuối -> gia hạn thêm 40s
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(endTime.minusSeconds(SNIPE_WINDOW_SECONDS))) {
            endTime = endTime.plusSeconds(EXTENSION_SECONDS);
        }

        return true;
    }

    public synchronized void updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if(status == AuctionStatus.OPEN && !now.isBefore(startTime)) {
            status = AuctionStatus.RUNNING;
        } else if (status == AuctionStatus.RUNNING && !now.isBefore(endTime)) {
            status = AuctionStatus.FINISHED;
        }
    }

    public int getId() {
        return id;
    }
    public String getItemId() {
        return itemId;
    }
    public String getSellerId() {
        return sellerId;
    }
    public double getStartingPrice() {
        return startingPrice;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public AuctionStatus getStatus() {
        return status;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public String getLeadingBidderID() {
        return leadingBidderID;
    }
    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
}
