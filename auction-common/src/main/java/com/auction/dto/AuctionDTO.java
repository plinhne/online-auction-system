package com.auction.dto;

import com.auction.model.auction.AuctionStatus;
import java.time.LocalDateTime;

public class AuctionDTO {
    // 1. Dữ liệu của phiên đấu giá (Auction)
    private int id;
    private int itemId;
    private int sellerId;
    private double startingPrice;
    private double currentPrice;
    private double minIncrement;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 2. Dữ liệu lấy thêm từ Sản phẩm (Item)
    private String itemName;
    private String itemDescription;
    private String itemCategory;

    public AuctionDTO() {}

    // --- BẠN HÃY GEN RA TẤT CẢ GETTER & SETTER CHO CÁC BIẾN TRÊN ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double minIncrement) { this.minIncrement = minIncrement; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }

    public int getLeadingBidderId() {
        return 0;
    }
}