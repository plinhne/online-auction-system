package com.auction.model.auction;

import com.auction.model.item.Item;
import com.auction.model.bid.Bid;
import java.time.LocalDateTime;

public class Auction {

    private int id;
    private AuctionStatus status;
    private Item item;              // item đang đấu giá
    private double minIncrement;    // bước giá tối thiểu
    private Bid highestBid;         // bid cao nhất
    private final String sellerId; // Người khởi tạo phiên đấu giá
    private final double startingPrice; // Giá khởi điểm
    private final LocalDateTime startTime; // Thời gian bắt đầu
    private LocalDateTime endTime; // Không để final để xử lý anti-sniping

    public Auction(int id, Item item, double minIncrement, String sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.minIncrement = minIncrement;
        this.sellerId = sellerId;
        this.status = AuctionStatus.SCHEDULED; //Trạng thái đã lên lịch
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void startAuction() {
        // Chỉ cho phép bắt đầu nếu phiên đấu giá đang ở trạng thái đã lên lịch
        if (this.status != AuctionStatus.SCHEDULED) {
            throw new IllegalStateException("Không thể bắt đầu một phiên đấu giá khi trạng thái hiện tại là: " + this.status);
        }
        this.status = AuctionStatus.ACTIVE;
    }

    public void endAuction() {
        // Chỉ cho phép kết thúc nếu phiên đấu giá đang diễn ra
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể kết thúc phiên đấu giá đang trong trạng thái ACTIVE!");
        }
        this.status = AuctionStatus.ENDED;
    }

    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE;
    }

    // getters
    public int getId() {
        return id;
    }

    public Item getItem() {
        return item;
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

    public Bid getHighestBid() {
        return highestBid;
    }

    public double getMinIncrement() {
        return minIncrement;
    }

    //setters
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}