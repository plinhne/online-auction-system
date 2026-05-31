package com.auction.server.dao;

import com.auction.model.item.Item;
import com.auction.model.item.ItemCategory;
import com.auction.model.pattern.factory.ItemFactory;
import com.auction.server.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    private static final Logger logger = LoggerFactory.getLogger(ItemDAO.class);
    private final DataSource dataSource = DatabaseConfig.getDataSource();

    public Item findById(int id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Item> findBySellerId(int sellerId) throws SQLException {
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        List<Item> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public List<Item> findByCategory(ItemCategory category) throws SQLException {
        String sql = "SELECT * FROM items WHERE category = ?";
        List<Item> items = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) items.add(mapRow(rs));
            }
        }
        return items;
    }

    public int save(Item item, int sellerId, ItemCategory category) throws SQLException {
        String sql = """
            INSERT INTO items (name, description, seller_id, category, image_url)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setInt(3, sellerId);
            stmt.setString(4, category.name());
            stmt.setString(5, item.getImageUrl());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Failed to save item, no ID returned");
    }

    public void update(Item item, ItemCategory category) throws SQLException {
        String sql = "UPDATE items SET name = ?, description = ?, category = ?, image_url = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getDescription());
            stmt.setString(3, category.name());
            stmt.setString(4, item.getImageUrl());
            stmt.setInt(5, item.getId());
            stmt.executeUpdate();
        }
    }

    public void updateImageUrl(int itemId, String imageUrl) throws SQLException {
        String sql = "UPDATE items SET image_url = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, imageUrl);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        int id           = rs.getInt("id");
        String name      = rs.getString("name");
        String desc      = rs.getString("description");
        ItemCategory cat = ItemCategory.valueOf(rs.getString("category"));
        String imageUrl  = rs.getString("image_url");

        Item item = ItemFactory.createItem(cat, id, name, 0.0);
        item.setDescription(desc);
        item.setImageUrl(imageUrl);
        return item;
    }
}