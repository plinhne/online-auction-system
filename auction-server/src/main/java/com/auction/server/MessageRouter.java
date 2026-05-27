package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageRouter: nhận raw JSON string từ ClientHandler,
 * parse action và điều phối sang đúng controller.
 * Giữ session state: currentUser, currentAuctionId.
 */
public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);

    private final AuthController authController;
    private final AuctionController auctionController;
    private final BidController bidController;

    // Session state — mỗi ClientHandler có 1 MessageRouter riêng
    private User currentUser = null;
    private int currentAuctionId = -1;

    public MessageRouter(AuthController authController,
                         AuctionController auctionController,
                         BidController bidController) {
        this.authController = authController;
        this.auctionController = auctionController;
        this.bidController = bidController;
    }

    public String route(String rawJson) {
        JsonObject response = new JsonObject();
        try {
            JsonObject request = JsonParser.parseString(rawJson).getAsJsonObject();
            String action = request.get("action").getAsString();
            logger.debug("Routing action={}, userId={}", action, currentUser != null ? currentUser.getId() : "guest");

            switch (action) {
                case "PING" -> {
                    response.addProperty("status", "OK");
                    response.addProperty("message", "PONG");
                }

                // ── AUTH ──────────────────────────────────────────────────────
                case "REGISTER" -> authController.handleRegister(request, response);

                case "LOGIN" -> {
                    currentUser = authController.handleLogin(request, response);
                }

                case "LOGOUT" -> {
                    authController.handleLogout(response);
                    currentUser = null;
                    currentAuctionId = -1;
                }

                // ── AUCTION ───────────────────────────────────────────────────
                case "GET_AUCTIONS" -> auctionController.handleGetAuctions(response);

                case "JOIN_AUCTION" -> {
                    Auction auction = auctionController.handleJoinAuction(request, response);
                    if (auction != null) currentAuctionId = auction.getId();
                }

                case "LEAVE_AUCTION" -> {
                    auctionController.handleLeaveAuction(response);
                    currentAuctionId = -1;
                }

                case "CREATE_AUCTION" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        auctionController.handleCreateAuction(request, response, currentUser);
                    }
                }

                case "CANCEL_AUCTION" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        auctionController.handleCancelAuction(request, response, currentUser);
                    }
                }

                case "GET_MY_AUCTIONS" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        auctionController.handleGetMyAuctions(response, currentUser);
                    }
                }

                case "GET_MY_BIDS" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        auctionController.handleGetMyBids(response, currentUser);
                    }
                }

                // ── BID ───────────────────────────────────────────────────────
                case "PLACE_BID" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        bidController.handlePlaceBid(request, response, currentUser);
                    }
                }

                case "SET_AUTO_BID" -> {
                    requireLogin(response);
                    if (isOk(response)) {
                        bidController.handleSetAutoBid(request, response, currentUser);
                    }
                }

                default -> {
                    response.addProperty("status", "ERROR");
                    response.addProperty("message", "Unknown action: " + action);
                    logger.warn("Unknown action received: {}", action);
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", e.getMessage());
        } catch (Exception e) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Server error: " + e.getMessage());
            logger.error("Unexpected error routing action", e);
        }

        return response.toString();
    }

    private void requireLogin(JsonObject response) {
        if (currentUser == null) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Not logged in");
        }
    }

    private boolean isOk(JsonObject response) {
        return !response.has("status") || "OK".equals(response.get("status").getAsString());
    }

    public User getCurrentUser() { return currentUser; }
    public int getCurrentAuctionId() { return currentAuctionId; }
}