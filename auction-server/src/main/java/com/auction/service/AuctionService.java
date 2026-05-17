package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AuctionService {
    // Giả lập Database lưu trữ Auction
    private final Map<Integer, Auction> auctionDatabase = new HashMap<>();

    public Auction createAuction(Item item, String sellerId, double startingPrice, int durationInMinutes, double minIncrement) {
        int auctionId = (int)System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationInMinutes);

        Auction newAuction = new Auction(auctionId, item, minIncrement, sellerId, startingPrice, startTime, endTime);
        auctionDatabase.put(auctionId, newAuction);

        return newAuction;
    }

    public Auction getAuctionById(String id) {
        return auctionDatabase.get(id);
    }

    /**
     * Logic Anti-Sniping: Nếu thời gian đặt bid cách thời gian kết thúc ít hơn N phút,
     * tự động gia hạn thêm thời gian cho phiên đấu giá.
     */
    public void handleAntiSniping(Auction auction, int triggerMinutesBeforeEnd, int extendMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime triggerWindow = auction.getEndTime().minusMinutes(triggerMinutesBeforeEnd);

        if (now.isAfter(triggerWindow) && now.isBefore(auction.getEndTime())) {
            LocalDateTime newEndTime = auction.getEndTime().plusMinutes(extendMinutes);
            auction.setEndTime(newEndTime);
            System.out.println("Anti-sniping triggered! Auction " + auction.getId() + " extended to: " + newEndTime);
        }
    }
}