package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.Seller;
import com.auction.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionService {

    private final AuctionDAO auctionDAO;
    private static final AtomicInteger auctionIdGenerator = new AtomicInteger(5000);

    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    public Auction createAuction(Item item, User creator, double startingPrice, int durationInMinutes, double minIncrement) {
        // Chỉ Seller mới được tạo phiên đấu giá
        if (!(creator instanceof Seller)) {
            throw new UnauthorizedException("Access Denied: Only Sellers can create auctions!");
        }

        int auctionId = auctionIdGenerator.incrementAndGet();
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusMinutes(durationInMinutes);

        // Giả sử constructor của Auction nhận creator.getId()
        Auction newAuction = new Auction(auctionId, item, minIncrement, creator.getId(), startingPrice, startTime, endTime);

        // Lưu qua DAO
        auctionDAO.save(newAuction);

        return newAuction;
    }

    public Auction getAuctionById(int id) {
        return auctionDAO.findById(id);
    }

    public void handleAntiSniping(Auction auction, int triggerMinutesBeforeEnd, int extendMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime triggerWindow = auction.getEndTime().minusMinutes(triggerMinutesBeforeEnd);

        if (now.isAfter(triggerWindow) && now.isBefore(auction.getEndTime())) {
            LocalDateTime newEndTime = auction.getEndTime().plusMinutes(extendMinutes);
            auction.setEndTime(newEndTime);

            // Gọi DAO để cập nhật thời gian mới vào Database
            auctionDAO.update(auction);

            System.out.println("Anti-sniping triggered! Auction " + auction.getId() + " extended to: " + newEndTime);
        }
    }
}