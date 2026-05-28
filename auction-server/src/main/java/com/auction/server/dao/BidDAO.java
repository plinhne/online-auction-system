package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.model.bid.Bid;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {
    private final DataSource dataSource;

    public BidDAO() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public BidDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(Bid bid) throws SQLException {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount, placed_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bid.getAuctionId());
            stmt.setInt(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4,Timestamp.valueOf(bid.getPlacedAt()));
            stmt.executeUpdate();
        }
    }

    public List<Bid> findByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY placed_at ASC";
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bids.add(new Bid(
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getInt("bidder_id"),
                        rs.getInt("auction_id")
                ));
            }
        }
        return bids;
    }
}