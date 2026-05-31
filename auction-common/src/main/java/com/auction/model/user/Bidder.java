package com.auction.model.user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.BIDDER);
    }
}