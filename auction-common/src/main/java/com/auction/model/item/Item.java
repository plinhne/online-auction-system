package com.auction.model.item;

import com.auction.model.base.Entity;
import com.auction.model.item.ItemStatus;
import com.auction.model.bid.Bid;

import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity {

    private String name;
    private double price;
    private ItemStatus status;

    private List<Bid> bids = new ArrayList<>();

    public Item(int id, String name, double price) {
        super(id);
        this.name = name;
        this.price = price;
        this.status = ItemStatus.ACTIVE;
    }

    // getter setter
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    // polymorphism
    public abstract void printInfor();
}