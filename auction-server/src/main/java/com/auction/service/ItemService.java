package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.model.pattern.factory.ItemFactory;
import com.auction.server.dao.ItemDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    private final ItemDAO itemDAO;

    public ItemService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    // ── Query ────────────────────────────────────────────────────────────────

    public Item getItemById(int id) throws SQLException {
        Item item = itemDAO.findById(id);
        if (item == null) throw new IllegalArgumentException("Item not found: " + id);
        return item;
    }

    public List<Item> getItemsBySeller(int sellerId) throws SQLException {
        return itemDAO.findBySellerId(sellerId);
    }

    public List<Item> getItemsByCategory(ItemCategory category) throws SQLException {
        return itemDAO.findByCategory(category);
    }

    // ── Mutation ─────────────────────────────────────────────────────────────

    public Item createItem(User seller, String name, String description,
                           double startingPrice, ItemCategory category) throws SQLException {
        if (seller.getRole() != UserRole.SELLER) {
            throw new IllegalStateException("Only sellers can create items");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Starting price must be positive");
        }

        Item item = ItemFactory.createItem(category, 0, name, startingPrice);
        item.setDescription(description);

        int id = itemDAO.save(item, seller.getId(), category);
        item.setId(id);

        logger.info("Item created: itemId={}, sellerId={}, category={}", id, seller.getId(), category);
        return item;
    }

    public void updateItem(User seller, int itemId, String name,
                           String description, ItemCategory category) throws SQLException {
        Item item = getItemById(itemId);

        boolean isSeller = seller.getRole() == UserRole.SELLER;
        boolean isAdmin  = seller.getRole() == UserRole.ADMIN;
        if (!isSeller && !isAdmin) {
            throw new IllegalStateException("Only sellers or admins can update items");
        }

        item.setName(name);
        item.setDescription(description);
        itemDAO.update(item, category);

        logger.info("Item updated: itemId={}, by userId={}", itemId, seller.getId());
    }

    public void updateImageUrl(User requester, int itemId, String imageUrl) throws SQLException {
        itemDAO.updateImageUrl(itemId, imageUrl);
        logger.info("Image URL updated: itemId={}, url={}", itemId, imageUrl);
    }

    public void deleteItem(User requester, int itemId) throws SQLException {
        Item item = getItemById(itemId);

        boolean isSeller = requester.getRole() == UserRole.SELLER;
        boolean isAdmin  = requester.getRole() == UserRole.ADMIN;
        if (!isSeller && !isAdmin) {
            throw new IllegalStateException("Only sellers or admins can delete items");
        }

        itemDAO.delete(itemId);
        logger.info("Item deleted: itemId={}, by userId={}", itemId, requester.getId());
    }
}