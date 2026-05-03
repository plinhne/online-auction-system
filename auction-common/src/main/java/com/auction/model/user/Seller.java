package com.auction.model.user;

import com.auction.model.item.Art;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.model.item.Vehicle;

public class Seller extends User {

    public Seller(int id, String name, String email, String password) {
        super(id, name, email, password, "SELLER");
    }

    public Item createItem(String type, int id, String name, double price) {

        if (type.equalsIgnoreCase("electronics")) {
            return new Electronics(id, name, price);
        }

        if (type.equalsIgnoreCase("art")) {
            return new Art(id, name, price);
        }

        if (type.equalsIgnoreCase("vehicle")) {
            return new Vehicle(id, name, price);
        }

        // nếu không đúng loại nào
        throw new IllegalArgumentException("Invalid item type");
    }
}