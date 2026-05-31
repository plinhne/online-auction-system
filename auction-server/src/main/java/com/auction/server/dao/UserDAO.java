package com.auction.server.dao;

import com.auction.model.user.*;
import com.auction.server.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

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

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    public void update(int userId, String name, String role) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        boolean hasName = name != null && !name.isEmpty();
        boolean hasRole = role != null && !role.isEmpty();

        if (hasName) sql.append("name = ?");
        if (hasName && hasRole) sql.append(", ");
        if (hasRole) sql.append("role = ?");
        sql.append(" WHERE id = ?");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasName) stmt.setString(idx++, name);
            if (hasRole) stmt.setString(idx++, role);
            stmt.setInt(idx, userId);
            stmt.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public void updateBalance(int userId, double balance) throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, balance);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int id       = rs.getInt("id");
        String name  = rs.getString("name");
        String email = rs.getString("email");
        String pass  = rs.getString("password");
        double balance = rs.getDouble("balance");
        UserRole role = UserRole.valueOf(rs.getString("role"));

        User user = switch (role) {
            case BIDDER -> new Bidder(id, name, email, pass);
            case SELLER -> new Seller(id, name, email, pass);
            case ADMIN  -> new Admin(id, name, email, pass);
        };
        user.setWalletBalance(balance);
        return user;
    }
}