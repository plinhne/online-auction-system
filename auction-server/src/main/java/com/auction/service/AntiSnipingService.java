package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.server.dao.AuctionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class AntiSnipingService {
    private static final Logger logger = LoggerFactory.getLogger(AntiSnipingService.class);

    private static final int SNIPE_WINDOW_SECONDS = 20;
    private static final int EXTENSION_SECONDS    = 40;

    private final AuctionDAO auctionDAO;

    public AntiSnipingService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    /**
     * Kiểm tra và gia hạn auction nếu bid xảy ra trong SNIPE_WINDOW_SECONDS giây cuối.
     * Gọi trong lock block của BidService → thread-safe.
     * Trả về true nếu đã gia hạn.
     */
    public boolean handle(Auction auction) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime triggerWindow = auction.getEndTime().minusSeconds(SNIPE_WINDOW_SECONDS);

        if (now.isAfter(triggerWindow) && now.isBefore(auction.getEndTime())) {
            LocalDateTime newEndTime = auction.getEndTime().plusSeconds(EXTENSION_SECONDS);
            auction.setEndTime(newEndTime);
            auctionDAO.updateEndTime(auction.getId(), newEndTime);
            logger.warn("Anti-sniping triggered: auctionId={}, newEndTime={}", auction.getId(), newEndTime);
            return true;
        }
        return false;
    }
}