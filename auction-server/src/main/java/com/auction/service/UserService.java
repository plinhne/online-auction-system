package com.auction.service;

import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import java.util.HashMap;
import java.util.Map;

public class UserService {
    // Giả lập Database lưu trữ User
    private final Map<Integer, User> userDatabase = new HashMap<>();

    public User registerUser(String name, String email, String password, UserRole role) {
        if (userDatabase.values().stream().anyMatch(u -> u.getName().equals(name))) {
            throw new IllegalArgumentException("Username already exists!");
        }
        int id = (int)System.currentTimeMillis();
        User newUser = new User(id, name, email, password, role);
        userDatabase.put(id, newUser);
        return newUser;
    }

    public User findById(int id) {
        return userDatabase.get(id);
    }

    public User findByUsername(String username) {
        return userDatabase.values().stream()
                .filter(u -> u.getName().equals(username))
                .findFirst()
                .orElse(null);
    }
}
