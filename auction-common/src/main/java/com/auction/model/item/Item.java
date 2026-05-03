package com.auction.model.item;

import com.auction.model.base.Entity;

public abstract class Item extends Entity {
    private String name;
    private double price;

    public Item(int id, String name, double price) {
        super(id);
        this.name = name;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public abstract void printInfor(); // polymorphism
}