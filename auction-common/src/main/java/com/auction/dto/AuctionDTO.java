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
    private int leadingBidderId;
    private String leadingBidderName;

    // 2. Dữ liệu lấy thêm từ Sản phẩm (Item)
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private String itemImage_url;

    // BỔ SUNG: Thêm trường lưu số lượng lượt đặt giá
    private int bidCount;

    public AuctionDTO() {}

    // --- GETTER & SETTER ---
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

    // Giữ nguyên hàm cũ
    public String getItemCategory() { return itemCategory; }
    public void setItemCategory(String itemCategory) { this.itemCategory = itemCategory; }

    // SỬA LỖI 1: Thêm alias getCategory() và setCategory() để Controller gọi không bị lỗi
    public String getCategory() { return itemCategory; }
    public void setCategory(String category) { this.itemCategory = category; }

    public int getLeadingBidderId() { return leadingBidderId; }
    public void setLeadingBidderId(int leadingBidderId) { this.leadingBidderId = leadingBidderId; }

    public String getItemImage_url() { return itemImage_url; }
    public void setItemImage_url(String itemImage_url) { this.itemImage_url = itemImage_url; }

    public String getLeadingBidderName() { return leadingBidderName; }
    public void setLeadingBidderName(String leadingBidderName) { this.leadingBidderName = leadingBidderName; }

    // SỬA LỖI 2: Thêm Getter & Setter cho thuộc tính bidCount mới bổ sung
    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }
}