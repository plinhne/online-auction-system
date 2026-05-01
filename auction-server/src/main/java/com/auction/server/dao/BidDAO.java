package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.server.model.BidTransaction;

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

    public void save(BidTransaction bid) throws SQLException {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, amount) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bid.getAuctionID());
            stmt.setInt(2, bid.getBidderID());
            stmt.setDouble(3, bid.getAmount());
            stmt.executeUpdate();
        }
    }

    public List<BidTransaction> findByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY placed_at ASC";
        List<BidTransaction> bids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bids.add(new BidTransaction(
                        rs.getInt("id"),
                        rs.getInt("auction_id"),
                        rs.getInt("bidder_id"),
                        rs.getDouble("amount")
                ));
            }
        }
        return bids;
    }
}