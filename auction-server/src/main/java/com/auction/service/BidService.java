package com.auction.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BidService {
    private static final Logger logger = LoggerFactory.getLogger(BidService.class);

    private final AuctionDAO auctionDAO;
    private final BidDAO bidDAO;
    private final AutoBidService autoBidService;
    private final AntiSnipingService antiSnipingService;

    /**
     * Cache auction object theo auctionId.
     * Đảm bảo lock trong Auction luôn là cùng 1 object
     * dù gọi findById() nhiều lần.
     */
    private final ConcurrentHashMap<Integer, Auction> auctionCache = new ConcurrentHashMap<>();

    public BidService(AuctionDAO auctionDAO, BidDAO bidDAO) {
        this.auctionDAO = auctionDAO;
        this.bidDAO = bidDAO;
        this.antiSnipingService = new AntiSnipingService(auctionDAO);
        this.autoBidService = new AutoBidService(auctionDAO, bidDAO, auctionCache);
    }

    public Bid placeBid(int auctionId, int bidderId, double amount)
            throws SQLException, AuctionClosedException, InvalidBidException {

        Auction auction = getOrLoadAuction(auctionId);

        // Dùng ReentrantLock trong Auction object — cùng object nên cùng lock
        ReentrantLock lock = auction.getBidLock();
        lock.lock();
        try {
            syncStatus(auction);

            if (!auction.isActive()) {
                logger.warn("Bid rejected — auction not active: auctionId={}", auctionId);
                throw new AuctionClosedException("Auction " + auctionId + " is already closed!");
            }

            double minRequired = auction.getCurrentPrice() + auction.getMinIncrement();
            if (amount < minRequired) {
                logger.warn("Bid too low: auctionId={}, required={}, got={}", auctionId, minRequired, amount);
                throw new InvalidBidException(
                        String.format("Bid too low. Minimum required: %.2f", minRequired)
                );
            }

            // Anti-sniping: gia hạn nếu bid trong cửa sổ cuối
            antiSnipingService.handle(auction);

            // id = 0: DB sẽ tự generate qua IDENTITY(1,1)
            Bid bid = new Bid(0, amount, bidderId, auctionId);
            bidDAO.save(bid);
            auctionDAO.updateLeadingBidder(auctionId, amount, bidderId);
            auctionDAO.updateEndTime(auctionId, auction.getEndTime());

            // Cập nhật in-memory
            auction.setCurrentPrice(amount);
            auction.setLeadingBidderId(bidderId);
            auction.notifyObservers(amount, bidderId);

            logger.info("Bid accepted: auctionId={}, bidderId={}, amount={}", auctionId, bidderId, amount);

            // Trigger auto-bid từ đối thủ
            autoBidService.trigger(auction, bidderId);

            return bid;

        } finally {
            lock.unlock();
        }
    }

    public void setAutoBid(int auctionId, int bidderId, double maxBid, double increment)
            throws SQLException {
        getOrLoadAuction(auctionId);
        autoBidService.register(auctionId, bidderId, maxBid, increment);
    }

    public AutoBidService getAutoBidService() {
        return autoBidService;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    //đảm bảo luôn trả về cùng một obj nếu cùng auctionId
    private Auction getOrLoadAuction(int auctionId) throws SQLException {
        Auction cached = auctionCache.get(auctionId);
        if (cached != null) return cached;

        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Auction not found: " + auctionId);

        auctionCache.put(auctionId, auction);
        return auction;
    }

    private void syncStatus(Auction auction) throws SQLException {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (auction.getStatus() == com.auction.model.auction.AuctionStatus.SCHEDULED
                && now.isAfter(auction.getStartTime())) {
            auction.setStatus(com.auction.model.auction.AuctionStatus.ACTIVE);
            auctionDAO.updateStatus(auction.getId(), com.auction.model.auction.AuctionStatus.ACTIVE);
            logger.info("Auction auto-started: auctionId={}", auction.getId());
        } else if (auction.getStatus() == com.auction.model.auction.AuctionStatus.ACTIVE
                && now.isAfter(auction.getEndTime())) {
            auction.setStatus(com.auction.model.auction.AuctionStatus.ENDED);
            auctionDAO.updateStatus(auction.getId(), com.auction.model.auction.AuctionStatus.ENDED);
            autoBidService.clearAutoBids(auction.getId());
            logger.info("Auction auto-ended: auctionId={}", auction.getId());
        }
    }
}