package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.server.MainServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AuctionScheduler.class);
    private static final int CHECK_INTERVAL_SECONDS = 10;

    private final AuctionService auctionService;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAuctions, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.info("AuctionScheduler started, checking every {}s", CHECK_INTERVAL_SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        logger.info("AuctionScheduler stopped");
    }

    private void checkAuctions() {
        try {
            checkScheduled();
            checkActive();
        } catch (SQLException e) {
            logger.error("AuctionScheduler error during check", e);
        }
    }

    /**
     * Tìm auction SCHEDULED có startTime <= now → start và notify client.
     */
    private void checkScheduled() throws SQLException {
        List<Auction> scheduled = auctionService.findByStatus(AuctionStatus.SCHEDULED);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : scheduled) {
            if (now.isAfter(auction.getStartTime())) {
                auctionService.startAuction(auction.getId());
                broadcastAuctionEvent("AUCTION_STARTED", auction.getId());
            }
        }
    }

    /**
     * Tìm auction ACTIVE có endTime <= now → end và notify client.
     */
    private void checkActive() throws SQLException {
        List<Auction> active = auctionService.findByStatus(AuctionStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : active) {
            if (now.isAfter(auction.getEndTime())) {
                auctionService.endAuction(auction.getId());
                broadcastAuctionEvent("AUCTION_ENDED", auction.getId());
            }
        }
    }

    private void broadcastAuctionEvent(String event, int auctionId) {
        JsonObject notify = new JsonObject();
        notify.addProperty("event", event);
        notify.addProperty("auctionId", auctionId);
        MainServer.broadcastToAuction(auctionId, gson.toJson(notify));
        logger.info("Broadcast {}: auctionId={}", event, auctionId);
    }
}