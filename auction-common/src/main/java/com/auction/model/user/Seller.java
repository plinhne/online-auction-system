package com.auction.model.user;

public class Seller extends User {

    public Seller(int id, String name, String email, String password) {
        super(id, name, email, password, "SELLER");
    }

    public void createItem() {
        // TODO
    }
}