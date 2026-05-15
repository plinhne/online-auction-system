package com.auction.model.user;

import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.model.item.ItemStatus;

public class Bidder extends User {

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, "BIDDER");
    }

    public boolean placeBid(Item item, User user, double amount) {

        // 1. Check trạng thái
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("Auction not active");
        }

        // 2. Check giá
        if (amount <= item.getPrice()) {
            throw new IllegalArgumentException("Bid too low");
        }

        // 3. Tạo bid mới
        Bid bid = new Bid((int) System.currentTimeMillis(), amount, user);

        // 4. Lưu vào danh sách bid
        item.getBids().add(bid);

        // 5. Update giá mới
        item.setPrice(amount);

        return true;
    }
}