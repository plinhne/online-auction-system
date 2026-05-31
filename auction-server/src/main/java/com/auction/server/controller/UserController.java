package com.auction.server.controller;

import com.auction.model.user.*;
import com.auction.service.UserService;
import com.auction.service.AuctionService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuctionService auctionService;
    private final Gson gson;

    public UserController(UserService userService, AuctionService auctionService, Gson gson) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.gson = gson;
    }

    // ── USER CRUD ─────────────────────────────────────────────────────────────

    public void handleGetAllUsers(JsonObject response) throws Exception {
        List<User> users = userService.findAll();
        response.addProperty("status", "OK");
        response.add("users", gson.toJsonTree(users));
    }

    public void handleCreateUser(JsonObject request, JsonObject response) throws Exception {
        String name     = request.get("name").getAsString();
        String email    = request.get("email").getAsString();
        String password = request.get("password").getAsString();
        UserRole role   = UserRole.valueOf(request.get("role").getAsString().toUpperCase());

        User created = userService.registerUser(name, email, password, role);
        response.addProperty("status", "OK");
        response.add("user", gson.toJsonTree(created));
        logger.info("Admin created user: email={}", email);
    }

    public void handleUpdateUser(JsonObject request, JsonObject response, User admin) throws Exception {
        requireAdmin(admin);

        int userId  = request.get("userId").getAsInt();
        String name = request.has("name") ? request.get("name").getAsString() : null;
        String role = request.has("role") ? request.get("role").getAsString() : null;

        userService.updateUser(userId, name, role);
        response.addProperty("status", "OK");
        response.addProperty("message", "User updated: " + userId);
        logger.info("Admin updated user: userId={}", userId);
    }

    public void handleDeleteUser(JsonObject request, JsonObject response, User admin) throws Exception {
        requireAdmin(admin);

        int userId = request.get("userId").getAsInt();
        userService.deleteUser(userId);
        response.addProperty("status", "OK");
        response.addProperty("message", "User deleted: " + userId);
        logger.info("Admin deleted user: userId={}", userId);
    }

    // ── BALANCE ───────────────────────────────────────────────────────────────

    public void handleUpdateBalance(JsonObject request, JsonObject response, User admin) throws Exception {
        requireAdmin(admin);

        int userId       = request.get("userId").getAsInt();
        double newBalance = request.get("balance").getAsDouble();

        userService.updateBalance(userId, newBalance);
        response.addProperty("status", "OK");
        response.addProperty("message", "Balance updated for userId: " + userId);
        logger.info("Admin updated balance: userId={}, balance={}", userId, newBalance);
    }

    // ── AUCTION ADMIN ─────────────────────────────────────────────────────────

    public void handleAdminGetAllAuctions(JsonObject response) throws Exception {
        var auctions = auctionService.getAllAuctions();
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(auctions));
    }

    public void handleAdminDeleteAuction(JsonObject request, JsonObject response, User admin) throws Exception {
        requireAdmin(admin);

        int auctionId = request.get("auctionId").getAsInt();
        auctionService.deleteAuction(auctionId);
        response.addProperty("status", "OK");
        response.addProperty("message", "Auction deleted: " + auctionId);
        logger.info("Admin deleted auction: auctionId={}", auctionId);
    }

    public void handleAdminUpdateAuction(JsonObject request, JsonObject response, User admin) throws Exception {
        requireAdmin(admin);

        int auctionId = request.get("auctionId").getAsInt();
        String status = request.has("status") ? request.get("status").getAsString() : null;

        auctionService.adminUpdateAuction(auctionId, status);
        response.addProperty("status", "OK");
        response.addProperty("message", "Auction updated: " + auctionId);
        logger.info("Admin updated auction: auctionId={}, status={}", auctionId, status);
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    private void requireAdmin(User user) {
        if (user == null || user.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Admin access required");
        }
    }
}