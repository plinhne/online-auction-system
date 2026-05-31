package com.auction.model.pattern.factory;
import com.auction.model.item.*;
public class ItemFactory {
    public static Item createItem(ItemCategory category, int id, String name, double price) {

        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null!");
        }
        switch (category) {
            case ELECTRONICS:
                return new Electronics(id, name, price);

            case ART:
                return new Art(id, name, price);

            case VEHICLE:
                return new Vehicle(id, name, price);

            default:
                throw new IllegalArgumentException("Unknown Item Type!");
        }
    }
}
