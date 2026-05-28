package com.auction.model.payment;

import com.auction.model.item.Item;
import com.auction.model.user.User;

public class Deposit {

    private User bidder;

    private Item item;

    private double amount;

    private boolean transferredToSeller;

    public Deposit(
            User bidder,
            Item item,
            double amount
    ) {

        this.bidder = bidder;
        this.item = item;
        this.amount = amount;

        this.transferredToSeller = false;
    }

    public User getBidder() {
        return bidder;
    }

    public Item getItem() {
        return item;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isTransferredToSeller() {
        return transferredToSeller;
    }

    public void setTransferredToSeller(
            boolean transferredToSeller
    ) {
        this.transferredToSeller = transferredToSeller;
    }
}