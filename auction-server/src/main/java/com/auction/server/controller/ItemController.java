package com.auction.server.controller;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.service.ImageService;
import com.auction.service.ItemService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    private final ItemService itemService;
    private final ImageService imageService;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, type, ctx) ->
                            new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, type, ctx) ->
                            java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public ItemController(ItemService itemService, ImageService imageService) {
        this.itemService = itemService;
        this.imageService = imageService;
    }

    public void handleGetItemDetails(JsonObject request, JsonObject response) throws Exception {
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

        // Lưu ảnh nếu có
        if (request.has("imageData") && !request.get("imageData").getAsString().isEmpty()) {
            String base64    = request.get("imageData").getAsString();
            String extension = request.has("imageExtension")
                    ? request.get("imageExtension").getAsString()
                    : "jpg";
            String imagePath = imageService.saveImage(base64, item.getId(), extension);
            itemService.updateImageUrl(seller, item.getId(), imagePath);
            item.setImageUrl(imagePath);
        }

        response.addProperty("status", "OK");
        response.add("item", gson.toJsonTree(item));
        logger.info("Item created: itemId={}, sellerId={}", item.getId(), seller.getId());
    }

    public void handleUpdateItem(JsonObject request, JsonObject response, User seller) throws Exception {
        int itemId         = request.get("itemId").getAsInt();
        String name        = request.get("name").getAsString();
        String description = request.get("description").getAsString();
        ItemCategory cat   = ItemCategory.valueOf(request.get("category").getAsString());

        itemService.updateItem(seller, itemId, name, description, cat);

        // Cập nhật ảnh nếu có upload mới
        if (request.has("imageData") && !request.get("imageData").getAsString().isEmpty()) {
            String base64    = request.get("imageData").getAsString();
            String extension = request.has("imageExtension")
                    ? request.get("imageExtension").getAsString()
                    : "jpg";
            imageService.deleteImage(itemId);
            String imagePath = imageService.saveImage(base64, itemId, extension);
            itemService.updateImageUrl(seller, itemId, imagePath);
        }

        response.addProperty("status", "OK");
        response.addProperty("message", "Item updated: " + itemId);
        logger.info("Item updated: itemId={}, by userId={}", itemId, seller.getId());
    }

    public void handleDeleteItem(JsonObject request, JsonObject response, User requester) throws Exception {
        int itemId = request.get("itemId").getAsInt();
        itemService.deleteItem(requester, itemId);
        imageService.deleteImage(itemId);
        response.addProperty("status", "OK");
        response.addProperty("message", "Item deleted: " + itemId);
        logger.info("Item deleted: itemId={}, by userId={}", itemId, requester.getId());
    }
}