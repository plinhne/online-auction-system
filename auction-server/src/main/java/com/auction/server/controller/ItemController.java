package com.auction.server.controller;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.service.ItemService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    private final ItemService itemService;
    private final Gson gson = new Gson();

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public void handleGetItem(JsonObject request, JsonObject response) throws Exception {
        int itemId = request.get("itemId").getAsInt();
        Item item = itemService.getItemById(itemId);
        response.addProperty("status", "OK");
        response.add("item", gson.toJsonTree(item));
    }

    public void handleGetMyItems(JsonObject response, User seller) throws Exception {
        List<Item> items = itemService.getItemsBySeller(seller.getId());
        response.addProperty("status", "OK");
        response.add("items", gson.toJsonTree(items));
    }

    public void handleCreateItem(JsonObject request, JsonObject response, User seller) throws Exception {
        String name        = request.get("name").getAsString();
        String description = request.get("description").getAsString();
        double price       = request.get("startingPrice").getAsDouble();
        ItemCategory cat   = ItemCategory.valueOf(request.get("category").getAsString());

        Item item = itemService.createItem(seller, name, description, price, cat);
        response.addProperty("status", "OK");
        response.add("item", gson.toJsonTree(item));
    }

    public void handleUpdateItem(JsonObject request, JsonObject response, User seller) throws Exception {
        int itemId         = request.get("itemId").getAsInt();
        String name        = request.get("name").getAsString();
        String description = request.get("description").getAsString();
        ItemCategory cat   = ItemCategory.valueOf(request.get("category").getAsString());

        itemService.updateItem(seller, itemId, name, description, cat);
        response.addProperty("status", "OK");
        response.addProperty("message", "Item updated: " + itemId);
    }

    public void handleDeleteItem(JsonObject request, JsonObject response, User requester) throws Exception {
        int itemId = request.get("itemId").getAsInt();
        itemService.deleteItem(requester, itemId);
        response.addProperty("status", "OK");
        response.addProperty("message", "Item deleted: " + itemId);
    }

    public void handleGetItemDetails(JsonObject request, JsonObject response) {
        try {
            if (!request.has("itemId")) {
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Thiếu mã sản phẩm (itemId)");
                return;
            }

            int itemId = request.get("itemId").getAsInt();

            Item item = itemService.getItemById(itemId);

            if (item != null) {
                response.addProperty("status", "OK");
                response.add("item", gson.toJsonTree(item));
            } else {
                response.addProperty("status", "ERROR");
                response.addProperty("message", "Không tìm thấy sản phẩm với ID: " + itemId);
            }
        } catch (Exception e) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Lỗi Server khi lấy chi tiết sản phẩm: " + e.getMessage());
            logger.error("Lỗi khi lấy chi tiết sản phẩm", e);
        }
    }
}