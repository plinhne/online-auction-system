package com.auction.server.controller;

import com.auction.model.user.User;
import com.auction.service.AuthService;
import com.auction.service.UserService;
import com.auction.model.user.UserRole;
import com.google.gson.JsonObject;

public class AuthController {
    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    public User handleLogin(JsonObject request, JsonObject response) {
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();
        User user = authService.login(username, password);
        response.addProperty("status", "OK");
        response.addProperty("userId", user.getId());
        response.addProperty("role", user.getRole().name());
        response.addProperty("name", user.getName());
        return user;
    }

    public void handleLogout(JsonObject response) {
        response.addProperty("status", "OK");
    }

    public void handleRegister(JsonObject request, JsonObject response) {
        String name     = request.get("name").getAsString();
        String email    = request.get("email").getAsString();
        String password = request.get("password").getAsString();
        UserRole role   = UserRole.valueOf(request.get("role").getAsString());
        userService.registerUser(name, email, password, role);
        response.addProperty("status", "OK");
        response.addProperty("message", "Registered successfully");
    }
}