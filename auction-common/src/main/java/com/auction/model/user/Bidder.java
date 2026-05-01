package com.auction.model.user;

public class Bidder extends User {

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, "BIDDER");
    }

    public void placeBid() {
        // TODO
    }
}