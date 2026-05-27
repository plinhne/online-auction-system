package com.auction.service;

import com.auction.dao.UserDAO;
import com.auction.model.user.*; // Import các class con Bidder, Seller, Admin
import java.util.concurrent.atomic.AtomicInteger;

public class UserService {

    private final UserDAO userDAO;

    // Sinh ID tự động tăng
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User registerUser(String name, String email, String password, UserRole role) {
        // Dùng DAO để tra cứu
        if (userDAO.findByName(name) != null) {
            throw new IllegalArgumentException("Username already exists!");
        }

        int id = idGenerator.incrementAndGet(); // Tránh lỗi tràn số âm
        User newUser;

        // Đa hình
        switch (role) {
            case BIDDER:
                newUser = new Bidder(id, name, email, password);
                break;
            case SELLER:
                newUser = new Seller(id, name, email, password);
                break;
            case ADMIN:
                newUser = new Admin(id, name, email, password);
                break;
            default:
                throw new IllegalArgumentException("Invalid role!");
        }

        // Lưu vào cơ sở dữ liệu qua DAO
        userDAO.save(newUser);
        return newUser;
    }

    public User findById(int id) {
        return userDAO.findById(id);
    }

    public User findByUsername(String username) {
        return userDAO.findByName(username);
    }
}
