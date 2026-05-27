package com.auction.exception;

public class UserNotFoundException extends AuctionException {
    public UserNotFoundException() {
        super("User not found");
    }
}