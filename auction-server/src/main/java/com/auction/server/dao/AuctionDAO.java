package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.model.Auction;
import com.auction.server.model.AuctionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private final DataSource dataSource;

    public AuctionDAO() {
        this.dataSource = DatabaseConfig.getDateSource();
    }

    public AuctionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions (item_id, seller_id, starting_price, current_price, status, start_time, end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auction.getItemId());
            stmt.setInt(2, Integer.parseInt(auction.getSellerId()));
            stmt.setDouble(3, auction.getStartingPrice());
            stmt.setDouble(4, auction.getCurrentPrice());
            stmt.setString(5, auction.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            stmt.executeUpdate();
        }
    }

    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                auctions.add(mapRow(rs));
            }
        }
        return auctions;
    }

    public Auction findById(int id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public void updateCurrentPrice(int auctionId, double newPrice, String leadingBidderId) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, winner_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setInt(2, Integer.parseInt(leadingBidderId));
            stmt.setInt(3, auctionId);
            stmt.executeUpdate();
        }
    }

    public void updateStatus(int auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, auctionId);
            stmt.executeUpdate();
        }
    }

    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction auction = new Auction(
                rs.getInt("id"),
                String.valueOf(rs.getInt("seller_id")),
                rs.getInt("item_id"),
                rs.getDouble("starting_price"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()
        );
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        return auction;
    }
}