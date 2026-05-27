package com.auction.model.bid;

import com.auction.model.user.User;

import java.time.LocalDateTime;

public class Bid {
    private int id;
    private double amount;
    private BidStatus status;
    private int bidderId;
    private final int auctionId; // Gắn liền với một phiên đấu giá tồn tại
    private final LocalDateTime placedAt; // Thời gian đặt bid

    public Bid(int id, double amount, User bidderId, int auctionId) {
        this.id = id;
        this.amount = amount;
        this.bidderId = bidderId;
        this.status = BidStatus.PENDING;
        this.auctionId = auctionId;
        this.placedAt = LocalDateTime.now(); // Tự động lấy thời gian hiện tại khi tạo bid
    }
//getters
    public int getId() { return id; }
    public int getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
    public int getBidderId() { return bidderId; }
    public LocalDateTime getPlacedAt() { return placedAt; }
}