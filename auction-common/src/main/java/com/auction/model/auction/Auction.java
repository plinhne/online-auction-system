package com.auction.model.auction;

import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.bid.Bid;
import java.time.LocalDateTime;

public class Auction extends Entity {

    private AuctionStatus status;
    private int itemId;              // id item đang đấu giá
    private double minIncrement;    // bước giá tối thiểu
    private final int sellerId; // Người khởi tạo phiên đấu giá
    private final double startingPrice; // Giá khởi điểm
    private double currentPrice; //không cần biết chi tiết thông tin bid, chỉ cần mức giá
    private int leadingBidderId; //id người có bid cao nhất hiện tại
    private final LocalDateTime startTime; // Thời gian bắt đầu
    private LocalDateTime endTime; // Không để final để xử lý anti-sniping

    public Auction(int id, int itemId, int sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.status = AuctionStatus.SCHEDULED; //Trạng thái đã lên lịch
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = 0;
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
    public int getItemId() {
        return itemId;
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
}