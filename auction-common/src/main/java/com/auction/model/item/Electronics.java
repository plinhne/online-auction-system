package com.auction.model.item;

public class Electronics extends Item {

    public Electronics(int id, String name, double price) {

        super(id, name, price, ItemCategory.ELECTRONICS);
    }

    @Override
    public void printInfor() {
        System.out.println("Electronics: " + getName() +
                " | Price: " + getPrice());
    }
}