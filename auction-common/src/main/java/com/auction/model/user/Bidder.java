package com.auction.model.user;

import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;
import com.auction.model.auction.Auction;
import com.auction.model.user.UserStatus;
public class Bidder extends User {

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.BIDDER);
    }

    public boolean placeBid(Auction auction, User user, double amount) {

        // 1. check auction
        if (!auction.isActive()) {
            throw new IllegalStateException("Auction not active");
        }

        // 2. check user
        if (user.getStatus() == UserStatus.BANNED) {
            throw new IllegalStateException("User banned");
        }

        Item item = auction.getItem();

        double currentPrice = item.getPrice();
        double minIncrement = auction.getMinIncrement();

        // 3. check giá
        if (amount < currentPrice + minIncrement) {
            throw new IllegalArgumentException("Bid too low");
        }

        // 4. tạo bid
        Bid bid = new Bid((int)System.currentTimeMillis(), amount, user);

        // 5. lưu bid
        item.getBids().add(bid);

        // 6. update giá
        item.setPrice(amount);

        // 7. update highest bid
        auction.setHighestBid(bid);

        return true;
    }
}