package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.server.dao.AuctionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionDAO auctionDAO;

    public AuctionService(AuctionDAO auctionDAO) {
        this.auctionDAO = auctionDAO;
    }

    // ── Query ────────────────────────────────────────────────────────────────

    public Auction getAuctionById(int id) throws SQLException {
        return auctionDAO.findById(id);
    }

    public List<Auction> getAllAuctions() throws SQLException {
        return auctionDAO.findAll();
    }

    public List<Auction> findByStatus(AuctionStatus status) throws SQLException {
        return auctionDAO.findByStatus(status);
    }

    public List<Auction> getAuctionsBySeller(int sellerId) throws SQLException {
        return auctionDAO.findBySellerId(sellerId);
    }

    public List<Auction> getAuctionsByBidder(int bidderId) throws SQLException {
        return auctionDAO.findByBidderId(bidderId);
    }

    // ── Mutation ─────────────────────────────────────────────────────────────

    /**
     * Seller tạo phiên đấu giá mới.
     * Validate: chỉ SELLER mới được tạo, startTime phải sau now,
     * endTime phải sau startTime.
     */
    public Auction createAuction(User seller, int itemId, double startingPrice,
                                 double minIncrement, LocalDateTime startTime,
                                 LocalDateTime endTime) throws SQLException {
        if (seller.getRole() != UserRole.SELLER) {
            throw new IllegalStateException("Only sellers can create auctions");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("startTime must be in the future");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("startingPrice must be positive");
        }
        if (minIncrement <= 0) {
            throw new IllegalArgumentException("minIncrement must be positive");
        }

        Auction auction = new Auction(0, itemId, seller.getId(), startingPrice, startTime, endTime);
        auction.setMinIncrement(minIncrement);
        auction.setStatus(AuctionStatus.SCHEDULED);

        int id = auctionDAO.save(auction);
        auction.setId(id);

        logger.info("Auction created: auctionId={}, sellerId={}, itemId={}", id, seller.getId(), itemId);
        return auction;
    }

    public void startAuction(int auctionId) throws SQLException {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Auction not found: " + auctionId);
        auction.startAuction(); // validate: SCHEDULED → ACTIVE
        auctionDAO.updateStatus(auctionId, AuctionStatus.ACTIVE);
        logger.info("Auction started: auctionId={}", auctionId);
    }

    public void endAuction(int auctionId) throws SQLException {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Auction not found: " + auctionId);
        auction.endAuction(); // validate: ACTIVE → ENDED
        auctionDAO.updateStatus(auctionId, AuctionStatus.ENDED);
        Integer winnerId = auctionDAO.findWinnerbyId(auctionId);
        if (winnerId != null && winnerId > 0) {
            logger.info("Auction {} ended, winner: userId={}", auctionId, winnerId);
            // broadcast thông báo winner cho client nếu cần
        } else {
            logger.info("Auction {} ended with no winner", auctionId);
        }
    }

    /**
     * Seller/Admin huỷ phiên trước khi bắt đầu.
     * Không cho phép huỷ khi đang ACTIVE hoặc đã ENDED.
     */
    public void cancelAuction(int auctionId, User requester) throws SQLException {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Auction not found: " + auctionId);

        boolean isSeller = requester.getRole() == UserRole.SELLER
                && auction.getSellerId() == requester.getId();
        boolean isAdmin  = requester.getRole() == UserRole.ADMIN;

        if (!isSeller && !isAdmin) {
            throw new IllegalStateException("Only the seller or admin can cancel this auction");
        }
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel an active auction");
        }
        if (auction.getStatus() == AuctionStatus.ENDED) {
            throw new IllegalStateException("Cannot cancel an ended auction");
        }

        auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELLED);
        logger.info("Auction cancelled: auctionId={}, by userId={}", auctionId, requester.getId());
    }

    public void deleteAuction(int auctionId) throws SQLException {
        auctionDAO.delete(auctionId);
    }

    public void adminUpdateAuction(int auctionId, String status) throws SQLException {
        if (status != null) {
            auctionDAO.updateStatus(auctionId, AuctionStatus.valueOf(status.toUpperCase()));
        }
    }
}