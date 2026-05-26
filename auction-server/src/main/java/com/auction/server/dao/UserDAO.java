package com.auction.server.dao;

import com.auction.model.user.*;
import com.auction.server.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.*;

public class UserDAO {
    private final DataSource dataSource;

    public UserDAO() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole().name());
            stmt.setString(5, user.getStatus().name());
            stmt.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        UserRole role = UserRole.valueOf(rs.getString("role"));

        return switch (role) {
            case BIDDER -> new Bidder(id, name, email, password);
            case SELLER -> new Seller(id, name, email, password);
            case ADMIN  -> new Admin(id, name, email, password);
        };
    }
}