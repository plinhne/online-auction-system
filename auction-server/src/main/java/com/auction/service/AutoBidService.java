package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class AutoBidService {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);

    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;

    /**
     * Shared cache với BidService — đảm bảo lock trên cùng Auction object.
     */
    private final ConcurrentHashMap<Integer, Auction> auctionCache;

    /**
     * Key: auctionId → PriorityQueue<AutoBidEntry> sắp xếp FIFO theo thời gian đăng ký.
     * Khi tie, người đăng ký trước thắng.
     */
    private final ConcurrentHashMap<Integer, PriorityBlockingQueue<AutoBidEntry>> autoBidMap
            = new ConcurrentHashMap<>();

    /**
     * Lưu auto-bid vừa trigger gần nhất — BidController dùng để broadcast.
     */
    private final ConcurrentHashMap<Integer, Bid> lastTriggeredMap = new ConcurrentHashMap<>();

    public AutoBidService(AuctionDAO auctionDAO, BidDAO bidDAO,
                          ConcurrentHashMap<Integer, Auction> auctionCache) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.auctionCache = auctionCache;
    }

    /**
     * Đăng ký auto-bid cho một auction.
     * Mỗi user chỉ có 1 entry — ghi đè nếu đăng ký lại.
     * Nhiều user có thể đăng ký cùng lúc → PriorityQueue xử lý FIFO.
     */
    public void register(int auctionId, int bidderId, double maxBid, double increment)
            throws SQLException {
        Auction auction = getAuction(auctionId);

        if (maxBid <= auction.getCurrentPrice()) {
            throw new IllegalArgumentException(
                    "maxBid must be greater than current price: " + auction.getCurrentPrice()
            );
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("increment must be positive");
        }

        PriorityBlockingQueue<AutoBidEntry> queue = autoBidMap.computeIfAbsent(
                auctionId,
                id -> new PriorityBlockingQueue<>(10, Comparator.comparingLong(e -> e.registeredAt))
        );

        // Xoá entry cũ nếu user đăng ký lại
        queue.removeIf(e -> e.bidderId == bidderId);
        queue.add(new AutoBidEntry(bidderId, maxBid, increment, System.currentTimeMillis()));

        logger.info("Auto-bid registered: auctionId={}, bidderId={}, maxBid={}, increment={}",
                auctionId, bidderId, maxBid, increment);
    }

    /**
     * Trigger auto-bid từ các đối thủ sau mỗi bid thủ công.
     * Gọi trong lock block của BidService → thread-safe.
     * Chỉ 1 auto-bid được accept mỗi lượt, FIFO khi tie.
     */
    public Bid trigger(Auction auction, int lastBidderId) throws SQLException {
        PriorityBlockingQueue<AutoBidEntry> queue = autoBidMap.get(auction.getId());
        if (queue == null || queue.isEmpty()) return null;

        for (AutoBidEntry entry : queue) {
            // Bỏ qua người vừa bid
            if (entry.bidderId == lastBidderId) continue;
            // Bỏ qua nếu đã hết maxBid
            if (auction.getCurrentPrice() >= entry.maxBid) continue;

            double autoBidAmount = Math.min(
                    auction.getCurrentPrice() + entry.increment,
                    entry.maxBid
            );

            // id = 0: DB tự generate
            Bid autoBid = new Bid(0, autoBidAmount, entry.bidderId, auction.getId());
            bidDAO.save(autoBid);
            auctionDAO.updateLeadingBidder(auction.getId(), autoBidAmount, entry.bidderId);

            // Cập nhật in-memory
            auction.setCurrentPrice(autoBidAmount);
            auction.setLeadingBidderId(entry.bidderId);

            lastTriggeredMap.put(auction.getId(), autoBid);
            logger.info("Auto-bid accepted: auctionId={}, bidderId={}, amount={}",
                    auction.getId(), entry.bidderId, autoBidAmount);

            // Chỉ 1 auto-bid mỗi lượt
            return autoBid;
        }

        lastTriggeredMap.remove(auction.getId());
        return null;
    }

    public Bid getLastTriggered(int auctionId) {
        return lastTriggeredMap.get(auctionId);
    }

    public void clearAutoBids(int auctionId) {
        autoBidMap.remove(auctionId);
        lastTriggeredMap.remove(auctionId);
        logger.debug("Auto-bids cleared: auctionId={}", auctionId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Auction getAuction(int auctionId) throws SQLException {
        Auction auction = auctionCache.get(auctionId);
        if (auction == null) {
            auction = auctionDAO.findById(auctionId);
            if (auction == null) throw new IllegalArgumentException("Auction not found: " + auctionId);
            auctionCache.put(auctionId, auction);
        }
        return auction;
    }

    // ── Inner class ──────────────────────────────────────────────────────────
    private static class AutoBidEntry {
        final int bidderId;
        final double maxBid;
        final double increment;
        final long registeredAt;

        AutoBidEntry(int bidderId, double maxBid, double increment, long registeredAt) {
            this.bidderId = bidderId;
            this.maxBid = maxBid;
            this.increment = increment;
            this.registeredAt = registeredAt;
        }
    }
}