package com.auction.model.item;

public class Vehicle extends Item {

    public Vehicle(int id, String name, double price, String model) {
        super(id, name, price);
    }

    @Override
    public void printInfor() {
        System.out.println("Vehicle: " + getName() +
                " | Price: " + getPrice());
    }
}