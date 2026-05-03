package com.auction.model.user;

import com.auction.model.item.Item;

public class Bidder extends User {

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, "BIDDER");
    }

    public boolean placeBid(Item item, double amount) {
        if (amount <= item.getPrice()) {
            return false;
        }
        return true;
    }
}