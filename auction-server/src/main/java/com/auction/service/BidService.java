package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UnauthorizedException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class BidService {

    // Thêm 2 DAO vào để chốt dữ liệu
    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private static final AtomicInteger bidIdGenerator = new AtomicInteger(10000);

    public BidService(AuctionDAO auctionDAO, ItemDAO itemDAO) {
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
    }

    public boolean placeBid(Auction auction, User user, double amount) throws AuctionClosedException {

        if (!(user instanceof Bidder)) {
            throw new UnauthorizedException("Only Bidders can place bids!");
        }

        // Khóa đa luồng
        ReentrantLock lock = auction.getBidLock();
        lock.lock();

        try {
            if (!auction.isActive()) {
                throw new AuctionClosedException("Auction " + auction.getId() + " is already closed!");
            }

            Item item = auction.getItem();
            if (amount < item.getPrice() + auction.getMinIncrement()) {
                throw new InvalidBidException("Bid too low. Minimum required: " + (item.getPrice() + auction.getMinIncrement()));
            }

            // Tạo Bid an toàn
            int safeBidId = bidIdGenerator.incrementAndGet();
            Bid bid = new Bid(safeBidId, amount, user, auction.getId());

            // Cập nhật Model
            if (item.getBids() != null) {
                item.getBids().add(bid);
            }
            item.setPrice(amount);
            auction.setHighestBid(bid);

            // Cập nhật thay đổi vào Database thông qua DAO
            itemDAO.update(item);
            auctionDAO.update(auction);

            // Báo cho các client khác
            auction.notifyObservers(amount, user.getName());

            return true;

        } catch (InvalidBidException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}