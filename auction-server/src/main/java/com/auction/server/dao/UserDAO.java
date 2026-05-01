package com.auction.server.dao;

import com.auction.model.user.Admin;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.server.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private final DataSource dataSource = DatabaseConfig.getDateSource();

    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
//            stmt.setString(3, user.getPassword());
//            stmt.setString(4, user.getRole().name());
            stmt.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int id = rs.getInt("id"); //
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");

        return switch (role) {
            case "BIDDER" -> new Bidder(id, name, email, password);
            case "SELLER" -> new Seller(id, name, email, password);
            case "ADMIN"  -> new Admin(id, name, email, password);
            default -> throw new SQLException("Unknown role: " + role);
        };
}
