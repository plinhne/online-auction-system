package com.auction.server;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.ItemController;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageRouter: Nhận raw JSON string từ ClientHandler,
 * giải mã thành đối tượng NetworkMessage, xác định MessageType
 * và phối hợp điều hướng sang đúng Controller xử lý.
 * Giữ session state riêng biệt cho mỗi Client kết nối: currentUser, currentAuctionId.
 */
public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);

    private final AuthController authController;
    private final AuctionController auctionController;
    private final BidController bidController;
    private final ItemController itemController;
    private final Gson gson = new Gson();

    // Session state — Mỗi kết nối ClientHandler sở hữu riêng một instance MessageRouter
    private User currentUser = null;
    private int currentAuctionId = -1;

    public MessageRouter(AuthController authController,
                         AuctionController auctionController,
                         BidController bidController,
                         ItemController itemController) {
        this.authController = authController;
        this.auctionController = auctionController;
        this.bidController = bidController;
        this.itemController = itemController;
    }

    /**
     * Hàm điều phối xử lý gói tin chính.
     * @param rawJson Chuỗi JSON thuần nhận được từ luồng đọc Socket của Client.
     * @return Chuỗi JSON đã được bọc trong cấu trúc NetworkMessage tiêu chuẩn để phản hồi về Client.
     */
    public String route(String rawJson) {
        JsonObject responsePayload = new JsonObject();
        MessageType responseType = null;

        try {
            // 1. Dịch ngược chuỗi thô từ Client thành cấu trúc gói tin NetworkMessage tiêu chuẩn
            NetworkMessage clientMsg = gson.fromJson(rawJson, NetworkMessage.class);
            if (clientMsg == null || clientMsg.getType() == null) {
                responsePayload.addProperty("status", "ERROR");
                responsePayload.addProperty("message", "Định dạng gói tin mạng không hợp lệ.");
                return gson.toJson(new NetworkMessage(null, responsePayload.toString()));
            }

            MessageType type = clientMsg.getType();

            // 2. Thiết lập trước kiểu tin nhắn phản hồi (Response Type) tương ứng với Request Type
            responseType = switch (type) {
                case LOGIN_REQUEST            -> MessageType.LOGIN_RESPONSE;
                case SIGNUP_REQUEST           -> MessageType.SIGNUP_RESPONSE;
                case LOGOUT_REQUEST           -> MessageType.LOGOUT_RESPONSE;
                case GET_ALL_AUCTIONS_REQUEST -> MessageType.GET_ALL_AUCTIONS_RESPONSE;
                case JOIN_AUCTION_REQUEST     -> MessageType.JOIN_AUCTION_RESPONSE;
                case LEAVE_AUCTION_REQUEST    -> MessageType.LEAVE_AUCTION_RESPONSE;
                case CREATE_AUCTION_REQUEST   -> MessageType.CREATE_AUCTION_RESPONSE;
                case CANCEL_AUCTION_REQUEST   -> MessageType.CANCEL_AUCTION_RESPONSE;
                case GET_MY_AUCTIONS_REQUEST  -> MessageType.GET_MY_AUCTIONS_RESPONSE;
                case GET_MY_BIDS_REQUEST      -> MessageType.GET_MY_BIDS_RESPONSE;
                case PLACE_BID_REQUEST        -> MessageType.PLACE_BID_RESPONSE;
                case SET_AUTO_BID_REQUEST     -> MessageType.SET_AUTO_BID_RESPONSE;
                case PING                     -> MessageType.PONG;
                default                       -> null;
            };

            // 3. Trích xuất phần dữ liệu lõi (Payload) của yêu cầu dưới dạng JsonObject
            JsonObject request = null;
            if (clientMsg.getPayload() != null && !clientMsg.getPayload().isEmpty()) {
                request = JsonParser.parseString(clientMsg.getPayload()).getAsJsonObject();
            }

            logger.info("Đang điều phối gói tin: type={}, userId={}", type, currentUser != null ? currentUser.getId() : "Khách");

            // 4. Cây quyết định switch-case định tuyến xử lý logic theo từng gói tin mạng cụ thể
            switch (type) {
                case PING -> {
                    responsePayload.addProperty("status", "OK");
                    responsePayload.addProperty("message", "PONG");
                }

                // ── LUỒNG AUTHENTICATION (XÁC THỰC TÀI KHOẢN) ──────────────────
                case SIGNUP_REQUEST -> authController.handleRegister(request, responsePayload);

                case LOGIN_REQUEST -> {
                    currentUser = authController.handleLogin(request, responsePayload);
                }

                case LOGOUT_REQUEST -> {
                    authController.handleLogout(responsePayload);
                    currentUser = null;
                    currentAuctionId = -1;
                }

                // ── LUỒNG AUCTION (QUẢN LÝ PHIÊN ĐẤU GIÁ) ──────────────────────
                case GET_ALL_AUCTIONS_REQUEST -> auctionController.handleGetAuctions(responsePayload);

                case JOIN_AUCTION_REQUEST -> {
                    Auction auction = auctionController.handleJoinAuction(request, responsePayload);
                    if (auction != null) currentAuctionId = auction.getId();
                }

                case LEAVE_AUCTION_REQUEST -> {
                    auctionController.handleLeaveAuction(responsePayload);
                    currentAuctionId = -1;
                }

                case CREATE_AUCTION_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        auctionController.handleCreateAuction(request, responsePayload, currentUser);
                    }
                }

                case CANCEL_AUCTION_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        auctionController.handleCancelAuction(request, responsePayload, currentUser);
                    }
                }

                case GET_MY_AUCTIONS_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        auctionController.handleGetMyAuctions(responsePayload, currentUser);
                    }
                }

                case GET_MY_BIDS_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        auctionController.handleGetMyBids(responsePayload, currentUser);
                    }
                }

                // ── LUỒNG BIDDING (ĐẶT GIÁ THẦU) ───────────────────────────────
                case PLACE_BID_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        bidController.handlePlaceBid(request, responsePayload, currentUser);
                    }
                }

                case SET_AUTO_BID_REQUEST -> {
                    requireLogin(responsePayload);
                    if (isOk(responsePayload)) {
                        bidController.handleSetAutoBid(request, responsePayload, currentUser);
                    }
                }

                default -> {
                    responsePayload.addProperty("status", "ERROR");
                    responsePayload.addProperty("message", "Kiểu thông điệp mạng chưa được Server hỗ trợ: " + type);
                    logger.warn("Nhận được gói tin chưa có cấu hình định tuyến: {}", type);
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            responsePayload.addProperty("status", "ERROR");
            responsePayload.addProperty("message", e.getMessage());
        } catch (Exception e) {
            responsePayload.addProperty("status", "ERROR");
            responsePayload.addProperty("message", "Lỗi xử lý hệ thống tại máy chủ: " + e.getMessage());
            logger.error("Gặp sự cố nghiêm trọng không lường trước khi định tuyến gói tin", e);
        }

        // 5. Đồng bộ cấu trúc mạng: Bọc toàn bộ payload kết quả vào thực thể NetworkMessage chuẩn và trả về chuỗi text JSON công khai
        NetworkMessage outMsg = new NetworkMessage(responseType, responsePayload.toString());
        return gson.toJson(outMsg);
    }

    /**
     * Phương thức kiểm soát bảo mật tầng biên đảm bảo người dùng bắt buộc phải đăng nhập.
     */
    private void requireLogin(JsonObject response) {
        if (currentUser == null) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Hành động yêu cầu quyền truy cập. Bạn chưa đăng nhập tài khoản!");
        }
    }

    /**
     * Kiểm tra trạng thái hiện thời của gói tin trung gian.
     */
    private boolean isOk(JsonObject response) {
        return !response.has("status") || "OK".equals(response.get("status").getAsString());
    }

    // Các phương thức getter hỗ trợ lấy trạng thái ngữ cảnh Session
    public User getCurrentUser() { return currentUser; }
    public int getCurrentAuctionId() { return currentAuctionId; }
}