package com.auction.model.exception;

public class UserNotFoundException extends AuctionException {
    public UserNotFoundException() {
        super("User not found");
    }
}