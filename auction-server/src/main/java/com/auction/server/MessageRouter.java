package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.network.MessageType;
import com.auction.network.NetworkMessage;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.ItemController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);

    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();    private User currentUser = null;
    private int currentAuctionId = -1;

    private final Map<MessageType, RouteHandler> handlers = new EnumMap<>(MessageType.class);
    private final Map<MessageType, MessageType> responseTypeMap = new EnumMap<>(MessageType.class);

    public MessageRouter(AuthController authController,
                         AuctionController auctionController,
                         BidController bidController,
                         ItemController itemController) {
        registerHandlers(authController, auctionController, bidController, itemController);
        registerResponseTypes();
    }

    private void registerHandlers(AuthController auth,
                                  AuctionController auction,
                                  BidController bid,
                                  ItemController item) {
        // ── PING ──
        handlers.put(MessageType.PING, (req, res) -> {
            res.addProperty("status", "OK");
            res.addProperty("message", "PONG");
        });

        // ── AUTH ──
        handlers.put(MessageType.SIGNUP_REQUEST,  (req, res) -> auth.handleRegister(req, res));
        handlers.put(MessageType.LOGIN_REQUEST,   (req, res) -> currentUser = auth.handleLogin(req, res));
        handlers.put(MessageType.LOGOUT_REQUEST,  (req, res) -> {
            auth.handleLogout(res);
            currentUser = null;
            currentAuctionId = -1;
        });

        // ── AUCTION ──
        handlers.put(MessageType.GET_ALL_AUCTIONS_REQUEST, (req, res) -> auction.handleGetAuctions(res));
        handlers.put(MessageType.JOIN_AUCTION_REQUEST, (req, res) -> {
            Auction a = auction.handleJoinAuction(req, res);
            if (a != null) currentAuctionId = a.getId();
        });
        handlers.put(MessageType.LEAVE_AUCTION_REQUEST, (req, res) -> {
            auction.handleLeaveAuction(res);
            currentAuctionId = -1;
        });
        handlers.put(MessageType.CREATE_AUCTION_REQUEST,  requireLogin((req, res) -> auction.handleCreateAuction(req, res, currentUser)));
        handlers.put(MessageType.CANCEL_AUCTION_REQUEST,  requireLogin((req, res) -> auction.handleCancelAuction(req, res, currentUser)));
        handlers.put(MessageType.GET_MY_AUCTIONS_REQUEST, requireLogin((req, res) -> auction.handleGetMyAuctions(res, currentUser)));
        handlers.put(MessageType.GET_MY_BIDS_REQUEST,     requireLogin((req, res) -> auction.handleGetMyBids(res, currentUser)));

        // ── ITEM (ĐÃ CHUẨN HÓA LIÊN HOÀN & LẤY CHI TIẾT) ──
        handlers.put(MessageType.GET_ITEM_DETAILS_REQUEST, (req, res) -> item.handleGetItemDetails(req, res));

        handlers.put(MessageType.ADD_ITEM_REQUEST, requireLogin((req, res) -> {
            // 1. Lưu Item
            item.handleCreateItem(req, res, currentUser);
            boolean isOk = !res.has("status") || "OK".equals(res.get("status").getAsString());

            // 2. Chuyển tiếp ID để tạo Auction
            if (isOk && res.has("item")) {
                int newItemId = res.get("item").getAsJsonObject().get("id").getAsInt();
                req.addProperty("itemId", newItemId);

                JsonObject auctionResponse = new JsonObject();
                auction.handleCreateAuction(req, auctionResponse, currentUser);

                boolean isAuctionOk = !auctionResponse.has("status") || "OK".equals(auctionResponse.get("status").getAsString());
                if (isAuctionOk) {
                    res.add("auction", auctionResponse.get("auction"));
                    res.addProperty("message", "Đã tạo Sản phẩm và Phiên đấu giá thành công!");
                } else {
                    res.addProperty("status", "ERROR");
                    res.addProperty("message", "Lỗi tạo phòng đấu giá: " + auctionResponse.get("message").getAsString());
                }
            }
        }));

        handlers.put(MessageType.EDIT_ITEM_REQUEST,     requireLogin((req, res) -> item.handleUpdateItem(req, res, currentUser)));
        handlers.put(MessageType.DELETE_ITEM_REQUEST,   requireLogin((req, res) -> item.handleDeleteItem(req, res, currentUser)));
        handlers.put(MessageType.GET_MY_ITEMS_REQUEST,  requireLogin((req, res) -> item.handleGetMyItems(res, currentUser)));

        // ── BID ──
        handlers.put(MessageType.PLACE_BID_REQUEST,   requireLogin((req, res) -> bid.handlePlaceBid(req, res, currentUser)));
        handlers.put(MessageType.SET_AUTO_BID_REQUEST, requireLogin((req, res) -> bid.handleSetAutoBid(req, res, currentUser)));

        // ── ADMIN ──
        handlers.put(MessageType.ADMIN_ACTION_REQUEST, requireLogin((req, res) -> {
            res.addProperty("status", "OK");
            res.addProperty("message", "Đã tiếp nhận lệnh Admin");
        }));
    }

    private void registerResponseTypes() {
        responseTypeMap.put(MessageType.PING,                     MessageType.PONG);
        responseTypeMap.put(MessageType.SIGNUP_REQUEST,           MessageType.SIGNUP_RESPONSE);
        responseTypeMap.put(MessageType.LOGIN_REQUEST,            MessageType.LOGIN_RESPONSE);
        responseTypeMap.put(MessageType.LOGOUT_REQUEST,           MessageType.LOGOUT_RESPONSE);

        responseTypeMap.put(MessageType.GET_ALL_AUCTIONS_REQUEST, MessageType.GET_ALL_AUCTIONS_RESPONSE);
        responseTypeMap.put(MessageType.GET_ITEM_DETAILS_REQUEST, MessageType.GET_ITEM_DETAILS_RESPONSE); // Map mới

        responseTypeMap.put(MessageType.JOIN_AUCTION_REQUEST,     MessageType.JOIN_AUCTION_RESPONSE);
        responseTypeMap.put(MessageType.LEAVE_AUCTION_REQUEST,    MessageType.LEAVE_AUCTION_RESPONSE);
        responseTypeMap.put(MessageType.CREATE_AUCTION_REQUEST,   MessageType.CREATE_AUCTION_RESPONSE);
        responseTypeMap.put(MessageType.CANCEL_AUCTION_REQUEST,   MessageType.CANCEL_AUCTION_RESPONSE);
        responseTypeMap.put(MessageType.GET_MY_AUCTIONS_REQUEST,  MessageType.GET_MY_AUCTIONS_RESPONSE);
        responseTypeMap.put(MessageType.GET_MY_BIDS_REQUEST,      MessageType.GET_MY_BIDS_RESPONSE);
        responseTypeMap.put(MessageType.ADD_ITEM_REQUEST,         MessageType.ADD_ITEM_RESPONSE);
        responseTypeMap.put(MessageType.EDIT_ITEM_REQUEST,        MessageType.EDIT_ITEM_RESPONSE);
        responseTypeMap.put(MessageType.DELETE_ITEM_REQUEST,      MessageType.DELETE_ITEM_RESPONSE);
        responseTypeMap.put(MessageType.GET_MY_ITEMS_REQUEST,     MessageType.GET_MY_ITEMS_RESPONSE);
        responseTypeMap.put(MessageType.PLACE_BID_REQUEST,        MessageType.PLACE_BID_RESPONSE);
        responseTypeMap.put(MessageType.SET_AUTO_BID_REQUEST,     MessageType.SET_AUTO_BID_RESPONSE);
        responseTypeMap.put(MessageType.ADMIN_ACTION_REQUEST,     MessageType.ADMIN_ACTION_RESPONSE); // Map mới
    }

    public String route(String rawJson) {
        JsonObject responsePayload = new JsonObject();
        MessageType responseType = null;

        try {
            NetworkMessage clientMsg = gson.fromJson(rawJson, NetworkMessage.class);
            if (clientMsg == null || clientMsg.getType() == null) {
                responsePayload.addProperty("status", "ERROR");
                responsePayload.addProperty("message", "Invalid network message format");
                return gson.toJson(new NetworkMessage(null, responsePayload.toString()));
            }

            MessageType type = clientMsg.getType();
            responseType = responseTypeMap.get(type);

            JsonObject request = null;
            if (clientMsg.getPayload() != null && !clientMsg.getPayload().isEmpty()) {
                request = JsonParser.parseString(clientMsg.getPayload()).getAsJsonObject();
            }

            logger.debug("Routing: type={}, userId={}", type,
                    currentUser != null ? currentUser.getId() : "guest");

            RouteHandler handler = handlers.get(type);
            if (handler == null) {
                responsePayload.addProperty("status", "ERROR");
                responsePayload.addProperty("message", "Unsupported message type: " + type);
                logger.warn("No handler registered for type: {}", type);
            } else {
                handler.handle(request, responsePayload);
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            responsePayload.addProperty("status", "ERROR");
            responsePayload.addProperty("message", e.getMessage());
        } catch (Exception e) {
            responsePayload.addProperty("status", "ERROR");
            responsePayload.addProperty("message", "Server error: " + e.getMessage());
            logger.error("Unexpected error routing message", e);
        }

        return gson.toJson(new NetworkMessage(responseType, responsePayload.toString()));
    }

    private RouteHandler requireLogin(RouteHandler handler) {
        return (req, res) -> {
            if (currentUser == null) {
                res.addProperty("status", "ERROR");
                res.addProperty("message", "Not logged in");
                return;
            }
            handler.handle(req, res);
        };
    }

    public User getCurrentUser() { return currentUser; }
    public int getCurrentAuctionId() { return currentAuctionId; }

    @FunctionalInterface
    private interface RouteHandler {
        void handle(JsonObject request, JsonObject response) throws Exception;
    }
}