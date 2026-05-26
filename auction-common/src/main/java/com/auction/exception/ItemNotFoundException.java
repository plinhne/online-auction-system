package com.auction.exception;

public class ItemNotFoundException extends AuctionException {
    public ItemNotFoundException() {
        super("Item not found");
    }
}