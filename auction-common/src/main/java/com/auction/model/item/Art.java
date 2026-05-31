package com.auction.model.item;

public class Art extends Item {
    public Art(int id, String name, double price) {

        super(id, name, price, ItemCategory.ART);
    }
//    @Override
//    public void printInfor() {
//        System.out.println("Art: " + getName() +
//                " | Price: " + getPrice());
//    }
}

