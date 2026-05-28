package com.auction.model.item;

public class Vehicle extends Item {

    public Vehicle(int id, String name, double price) {
        super(id, name, price, ItemCategory.VEHICLE);
    }

    @Override
    public void printInfor() {
        System.out.println("Vehicle: " + getName() +
                " | Price: " + getPrice());
    }
}