package com.auction.service;

import com.auction.model.user.User;
import com.auction.model.user.UserStatus;
import com.auction.exception.UnauthorizedException;
import com.auction.service.UserService;

import java.sql.SQLException;

public class AuthService {
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public User login(String email , String password) throws SQLException {
        User user = userService.findByEmail(email);

        if (user == null) {
            throw new UnauthorizedException("Account does not exist!");
        }
        if (!user.getPassword().equals(password)) {
            throw new UnauthorizedException("Incorrect password!");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is locked by Admin!");
        }

        System.out.println("Login successful! Welcome " + user.getName());
        return user;
    }
}