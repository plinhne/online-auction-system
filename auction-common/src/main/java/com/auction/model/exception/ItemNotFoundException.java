package com.auction.model.exception;

public class ItemNotFoundException extends AuctionException {
    public ItemNotFoundException() {
        super("Item not found");
    }
}