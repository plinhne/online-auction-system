package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;

public class BidService {

    public boolean placeBid(Auction auction, User user, double amount) {

        if (!auction.isActive()) {
            throw new IllegalStateException("Auction not active");
        }

        if (user.getRole() != UserRole.BIDDER) {
            throw new IllegalStateException("Only bidder can bid");
        }

        Item item = auction.getItem();

        if (amount < item.getPrice() + auction.getMinIncrement()) {
            throw new IllegalArgumentException("Bid too low");
        }

        Bid bid = new Bid(
                (int)System.currentTimeMillis(),
                amount,
                user.getId(),
                auction.getId()
        );

        //Cập nhật thông tin vào Model
        if (item.getBids() != null) {
            item.getBids().add(bid);
        }
        item.setPrice(amount); // Cập nhật lại giá hiện tại của sản phẩm
        auction.setHighestBid(bid); // Cập nhật lượt bid cao nhất cho phiên

        return true;
    }
}