package com.auction.service;

import com.auction.model.user.User;
import com.auction.model.user.UserStatus;

public class AuthService {
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public User login(String name, String password) {
        User user = userService.findByUsername(name);

        if (user == null) {
            throw new IllegalArgumentException("Account does not exist!");
        }
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Incorrect password!");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is locked!");
        }

        System.out.println("Login successful! Welcome " + user.getName());
        return user;
    }
}