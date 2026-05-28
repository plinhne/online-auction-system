package com.auction.model.auction;

import com.auction.model.base.Entity;
import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.model.pattern.observer.AuctionSubject;
import com.auction.model.pattern.observer.BidObserver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity implements AuctionSubject {

    private final ReentrantLock bidLock = new ReentrantLock();

    // Danh sách những người đang xem phiên đấu giá (Observer Pattern)
    private final List<BidObserver> observers = new ArrayList<>();
    private final Item item;
    private AuctionStatus status;
    private int itemId;              // id item đang đấu giá
    private double minIncrement;    // bước giá tối thiểu
    private final int sellerId; // Người khởi tạo phiên đấu giá
    private final double startingPrice; // Giá khởi điểm
    private double currentPrice; //không cần biết chi tiết thông tin bid, chỉ cần mức giá
    private int leadingBidderId; //id người có bid cao nhất hiện tại
    private final LocalDateTime startTime; // Thời gian bắt đầu
    private LocalDateTime endTime; // Không để final để xử lý anti-sniping
    private Bid highestBid;

    public Auction(int id, Item item, double minIncrement, int sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.minIncrement = minIncrement;
        this.sellerId = sellerId;
        this.status = AuctionStatus.SCHEDULED; //Trạng thái đã lên lịch
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = 0;
    }
    @Override
    public void addObserver(BidObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(double newAmount, String bidderName) {
        // Mỗi khi có người đặt giá mới, báo cho tất cả client đang xem
        for (BidObserver observer : observers) {
            // Lấy ID từ Entity cha
            observer.updateNewBid(this.getId(), newAmount, bidderName);
        }
    }

    public ReentrantLock getBidLock() {
        return bidLock;
    }

    public void startAuction() {
        if (this.status != AuctionStatus.SCHEDULED) {
            throw new IllegalStateException("Cannot start the auction. Current status is: " + this.status);
        }
        this.status = AuctionStatus.ACTIVE;
    }

    public void endAuction() {
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Only active auctions can be ended!");
        }
        this.status = AuctionStatus.ENDED;
    }

    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE;
    }

    // getters
    public Item getItem() {
        return item;
    }

    public int getSellerId() {
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

    public double getMinIncrement() {
        return minIncrement;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public int getLeadingBidderId() {
        return leadingBidderId;
    }


    //setters
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public void setLeadingBidderId(int leadingBidderId) {
        this.leadingBidderId = leadingBidderId;
    }

    public void setMinIncrement(double minIncrement) {
        this.minIncrement = minIncrement;
    }

    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }
}