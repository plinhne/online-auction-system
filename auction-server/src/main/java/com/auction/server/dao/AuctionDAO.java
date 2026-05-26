package com.auction.server.dao;

import com.auction.server.config.DatabaseConfig;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private final DataSource dataSource;

    public AuctionDAO() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public AuctionDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    //tạo query với dữ liệu nhập vào thông qua các entity (obj -> sql)
    public int save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions (item_id, seller_id, starting_price, current_price, status, start_time, end_time, min_increment)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auction.getItemId());
            stmt.setInt(2, auction.getSellerId());
            stmt.setDouble(3, auction.getStartingPrice());
            stmt.setDouble(4, auction.getCurrentPrice());
            stmt.setString(5, auction.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            stmt.setDouble(8,auction.getMinIncrement());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if(keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    //tìm toàn bộ auctions
    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) auctions.add(mapRow(rs));
        }
        return auctions;
    }

    //tìm tất cả thông tin của auction có id là id
    public Auction findById(int id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            //try-with-resource tự động đóng nếu không có tài nguyên
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    //tìm winner của auction có id là id
    //vde: nế
    public Integer findWinnerbyId(int id) throws SQLException {
        String sql = "SELECT leading_bidder_id FROM auctions WHERE id = ? AND status = 'ENDED'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()){
                if(rs.next()) return rs.getInt("leading_bidder_id");
            }
        }
        return null;
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

    public void updateLeadingBidder(int auctionId, double newPrice, int leadingBidderId) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, leading_bidder_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setInt(2, leadingBidderId);
            stmt.setInt(3, auctionId);
            stmt.executeUpdate();
        }
    }

    public void updateEndTime(int auctionId, java.time.LocalDateTime newEndTime) throws SQLException {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            stmt.setInt(2, auctionId);
            stmt.executeUpdate();
        }
    }


    private Auction mapRow(ResultSet rs) throws SQLException {
        Auction auction = new Auction(
                rs.getInt("id"),
                rs.getInt("item_id"),
                rs.getInt("seller_id"),
                rs.getDouble("starting_price"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time").toLocalDateTime()
        );
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setLeadingBidderId(rs.getInt("leading_bidder_id"));
        return auction;
    }
}