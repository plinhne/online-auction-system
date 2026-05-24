package com.auction.model.pattern.factory;
import com.auction.model.item.*;
public class ItemFactory {
    public static Item createItem(String type, int id, String name, double price) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                return new Electronics(id, name, price);

            case "ART":
                return new Art(id, name, price);

            case "VEHICLE":
                return new Vehicle(id, name, price);

            default:
                throw new IllegalArgumentException("Unknown Item Type!");
        }
    }
}
