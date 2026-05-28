package com.auction.service;

import com.auction.server.dao.UserDAO;
import com.auction.model.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User registerUser(String name, String email, String password, UserRole role) throws SQLException {
        // UserDAO dùng email làm định danh duy nhất
        if (userDAO.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // id = 0: DB tự generate qua IDENTITY(1,1)
        User newUser = switch (role) {
            case BIDDER -> new Bidder(0, name, email, password);
            case SELLER -> new Seller(0, name, email, password);
            case ADMIN  -> new Admin(0, name, email, password);
        };

        userDAO.save(newUser);
        logger.info("User registered: email={}, role={}", email, role);
        return newUser;
    }

    public User findById(int id) throws SQLException {
        return userDAO.findById(id);
    }

    public User findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }
}