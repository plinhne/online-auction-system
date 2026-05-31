package com.auction.client.network;

import com.auction.client.controller.RealTimeBiddingController;
import com.auction.client.controller.AuctionListViewController;
import com.auction.client.controller.ProductDetailsController;
import com.auction.client.controller.AdminPanelController;
import com.auction.client.controller.SellerDashboardController;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.dto.AuctionDTO;
import com.auction.model.user.User;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.List;

public class ServerListener extends Thread {
    private final Socket socket;
    private final BufferedReader in;
    private final Gson gson;
    private volatile boolean isRunning;

    private RealTimeBiddingController biddingController;
    private AuctionListViewController auctionListController;
    private ProductDetailsController productDetailsController;
    private AdminPanelController adminPanelController;
    private SellerDashboardController sellerDashboardController;

    // Admin data được thu thập qua 2 response riêng rồi merge
    private List<AuctionDTO> pendingAdminAuctions = null;
    private List<User> pendingAdminUsers = null;

    public ServerListener(Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
        this.gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class,
                        (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, t, ctx) ->
                                new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(java.time.LocalDateTime.class,
                        (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, t, ctx) ->
                                java.time.LocalDateTime.parse(json.getAsString()))
                .registerTypeAdapter(User.class,
                        (com.google.gson.JsonDeserializer<User>) (json, t, ctx) -> {
                            JsonObject obj = json.getAsJsonObject();
                            String role = obj.has("role") ? obj.get("role").getAsString() : "BIDDER";
                            int id = obj.has("id") ? obj.get("id").getAsInt() : 0;
                            String name = obj.has("name") ? obj.get("name").getAsString() : "";
                            String email = obj.has("email") ? obj.get("email").getAsString() : "";
                            String password = obj.has("password") ? obj.get("password").getAsString() : "";
                            double balance = obj.has("walletBalance") ? obj.get("walletBalance").getAsDouble() : 0;
                            com.auction.model.user.UserRole userRole;
                            try { userRole = com.auction.model.user.UserRole.valueOf(role); }
                            catch (Exception e) { userRole = com.auction.model.user.UserRole.BIDDER; }
                            User user = switch (userRole) {
                                case SELLER -> new com.auction.model.user.Seller(id, name, email, password);
                                case ADMIN  -> new com.auction.model.user.Admin(id, name, email, password);
                                default     -> new com.auction.model.user.Bidder(id, name, email, password);
                            };
                            user.setWalletBalance(balance);
                            return user;
                        })
                .create();
        this.isRunning = true;
        this.setName("Thread-Client-ServerListener");
    }

    // --- Setters ---
    public void setBiddingController(RealTimeBiddingController c)         { this.biddingController = c; }
    public void setAuctionListController(AuctionListViewController c)     { this.auctionListController = c; }
    public void setProductDetailsController(ProductDetailsController c)   { this.productDetailsController = c; }
    public void setAdminPanelController(AdminPanelController c)           { this.adminPanelController = c; }
    public void setSellerDashboardController(SellerDashboardController c) { this.sellerDashboardController = c; }
    public void removeBiddingController()                                  { this.biddingController = null; }

    @Override
    public void run() {
        LoggerUtil.info("ServerListener phía Client đã kích hoạt...");
        String jsonLine;
        try {
            while (isRunning && !socket.isClosed() && (jsonLine = in.readLine()) != null) {
                try {
                    LoggerUtil.info("RAW FROM SERVER = " + jsonLine);
                    NetworkMessage message = gson.fromJson(jsonLine, NetworkMessage.class);
                    if (message != null && message.getType() != null) {
                        handleIncomingMessage(message);
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi giải mã JSON từ Server: " + jsonLine, e);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                LoggerUtil.error("Ngắt kết nối đột ngột từ Server.");
                triggerDisconnectionUI();
            }
        } finally {
            stopListening();
        }
    }

    private void handleIncomingMessage(NetworkMessage message) {
        MessageType type = message.getType();
        String payload = message.getPayload();

        switch (type) {

            // ── AUTH ────────────────────────────────────────────────────────────
            case LOGIN_RESPONSE:
                // Xử lý ở LoginController trực tiếp qua blocking read
                break;

            case SIGNUP_RESPONSE:
                handleSimpleStatusResponse(payload, "Đăng ký thành công! Vui lòng đăng nhập.");
                break;

            case LOGOUT_RESPONSE:
                LoggerUtil.info("Đã đăng xuất khỏi Server.");
                break;

            // ── AUCTION LIST ─────────────────────────────────────────────────────
            case GET_ALL_AUCTIONS_RESPONSE: {
                LoggerUtil.info("Nhận danh sách tất cả phiên đấu giá từ Server.");
                if (payload == null) break;
                try {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    if (!resp.has("auctions")) break;

                    Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                    List<AuctionDTO> auctions = gson.fromJson(resp.get("auctions"), listType);

                    // Dispatch tới AuctionList (Bidder/Seller view)
                    if (auctionListController != null) {
                        Platform.runLater(() -> auctionListController.updateAuctionListFromServer(auctions));
                    }

                    // Dispatch tới Admin (Admin gửi GET_ALL_AUCTIONS_REQUEST để lấy danh sách auction)
                    if (adminPanelController != null) {
                        synchronized (this) { pendingAdminAuctions = auctions; }
                        tryFlushAdminDashboard();
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse GET_ALL_AUCTIONS_RESPONSE: " + e.getMessage(), e);
                }
                break;
            }

            // ── ITEM DETAILS ─────────────────────────────────────────────────────
            case GET_ITEM_DETAILS_RESPONSE: {
                LoggerUtil.info("Nhận thông tin chi tiết Item từ Server.");
                if (payload == null) break;
                try {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    if (resp.has("item") && productDetailsController != null) {
                        com.auction.model.item.Item item =
                                gson.fromJson(resp.get("item"), com.auction.model.item.Item.class);
                        Platform.runLater(() -> productDetailsController.setItemDetails(item));
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse GET_ITEM_DETAILS_RESPONSE: " + e.getMessage(), e);
                }
                break;
            }

            // ── SELLER DASHBOARD ─────────────────────────────────────────────────
            case GET_MY_AUCTIONS_RESPONSE:
                LoggerUtil.info("Nhận dữ liệu danh sách phiên đấu giá của Seller từ Server.");
                if (sellerDashboardController != null) {
                    sellerDashboardController.handleServerResponse(message);
                } else {
                    LoggerUtil.warning("GET_MY_AUCTIONS_RESPONSE: SellerDashboardController chưa đăng ký.");
                }
                break;

            // ── ADMIN USERS ──────────────────────────────────────────────────────
            case GET_ALL_USERS_RESPONSE: {
                LoggerUtil.info("Nhận danh sách người dùng từ Server (Admin).");
                if (payload == null || adminPanelController == null) break;
                try {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    if (!resp.has("users")) break;

                    Type userListType = new TypeToken<List<User>>(){}.getType();
                    List<User> users = gson.fromJson(resp.get("users"), userListType);

                    synchronized (this) { pendingAdminUsers = users; }
                    tryFlushAdminDashboard();
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse GET_ALL_USERS_RESPONSE: " + e.getMessage(), e);
                }
                break;
            }

            case CREATE_USER_RESPONSE:
            case UPDATE_USER_RESPONSE:
            case DELETE_USER_RESPONSE:
            case UPDATE_BALANCE_RESPONSE:
                handleSimpleStatusResponse(payload, "Thao tác người dùng thành công.");
                if (adminPanelController != null) {
                    Platform.runLater(() -> adminPanelController.refreshAdminDataFromServer());
                }
                break;

            // ── ADMIN AUCTIONS ───────────────────────────────────────────────────
            case ADMIN_DELETE_AUCTION_RESPONSE:
            case ADMIN_UPDATE_AUCTION_RESPONSE:
            case CANCEL_AUCTION_RESPONSE:
                handleSimpleStatusResponse(payload, "Thao tác phiên đấu giá thành công.");
                if (adminPanelController != null) {
                    Platform.runLater(() -> adminPanelController.refreshAdminDataFromServer());
                }
                break;

            case ADMIN_ACTION_RESPONSE:
                // Legacy handler — giữ để backward-compatible nếu server vẫn dùng
                LoggerUtil.info("Nhận ADMIN_ACTION_RESPONSE (legacy).");
                break;

            // ── ITEM MANAGEMENT (Seller) ─────────────────────────────────────────
            case ADD_ITEM_RESPONSE:
                handleSimpleStatusResponse(payload, "Đã thêm sản phẩm và tạo phiên đấu giá thành công!");
                if (sellerDashboardController != null) {
                    NetworkService.getInstance().sendNetworkMessage(
                            new NetworkMessage(MessageType.GET_MY_AUCTIONS_REQUEST, "{}"));
                }
                break;

            case EDIT_ITEM_RESPONSE:
                handleSimpleStatusResponse(payload, "Đã cập nhật sản phẩm thành công.");
                break;

            case DELETE_ITEM_RESPONSE:
                handleSimpleStatusResponse(payload, "Đã xóa sản phẩm thành công.");
                break;

            case GET_MY_ITEMS_RESPONSE:
                LoggerUtil.info("Nhận GET_MY_ITEMS_RESPONSE (chưa có UI xử lý).");
                break;

            case CREATE_AUCTION_RESPONSE:
                handleSimpleStatusResponse(payload, "Đã tạo phiên đấu giá thành công.");
                break;

            // ── BIDDING ──────────────────────────────────────────────────────────
            case JOIN_AUCTION_RESPONSE: {
                LoggerUtil.info("Nhận phản hồi tham gia phiên đấu giá.");
                if (payload == null) break;
                try {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
                    if (!"OK".equals(status)) {
                        String msg = resp.has("message") ? resp.get("message").getAsString() : "Không thể tham gia phiên đấu giá.";
                        Platform.runLater(() -> DialogUtil.showError(msg));
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse JOIN_AUCTION_RESPONSE: " + e.getMessage(), e);
                }
                break;
            }

            case LEAVE_AUCTION_RESPONSE:
                LoggerUtil.info("Đã rời phiên đấu giá.");
                break;

            case PLACE_BID_RESPONSE: {
                // Bid thành công → cập nhật UI qua AUCTION_UPDATE_NOTIFICATION (broadcast)
                // Bid thất bại → hiện lỗi ngay
                LoggerUtil.info("Nhận phản hồi đặt giá từ Server.");
                if (payload == null) break;
                try {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
                    LoggerUtil.info("PLACE_BID_RESPONSE payload = " + payload);
                    if (!"OK".equals(status)) {
                        String msg = resp.has("message") ? resp.get("message").getAsString() : "Đặt giá thất bại.";
                        Platform.runLater(() -> DialogUtil.showError("Đặt giá thất bại: " + msg));
                    }
                    // OK → không cần làm gì, AUCTION_UPDATE_NOTIFICATION sẽ broadcast giá mới
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse PLACE_BID_RESPONSE: " + e.getMessage(), e);
                }
                break;
            }

            case SET_AUTO_BID_RESPONSE:
                handleSimpleStatusResponse(payload, "Đã bật Auto-Bidding thành công.");
                break;

            case GET_MY_BIDS_RESPONSE:
                LoggerUtil.info("Nhận GET_MY_BIDS_RESPONSE (chưa có UI xử lý).");
                break;

            // ── BROADCAST NOTIFICATIONS ──────────────────────────────────────────
            case AUCTION_UPDATE_NOTIFICATION: {
                LoggerUtil.info("Nhận broadcast cập nhật giá realtime.");
                LoggerUtil.info("realTimeController = " + biddingController );
                if (biddingController == null || payload == null) break;
                try {
                    // Server gửi flat JSON: {auctionId, currentPrice, bidderId, bidderName, endTime, placedAt}
                    // → map thủ công vào AuctionDTO để client dùng
                    JsonObject p = JsonParser.parseString(payload).getAsJsonObject();
                    AuctionDTO dto = new AuctionDTO();
                    if (p.has("auctionId"))    dto.setId(p.get("auctionId").getAsInt());
                    if (p.has("currentPrice")) dto.setCurrentPrice(p.get("currentPrice").getAsDouble());
                    if (p.has("bidderId"))     dto.setLeadingBidderId(p.get("bidderId").getAsInt());
                    if (p.has("bidderName"))   dto.setLeadingBidderName(p.get("bidderName").getAsString());
                    if (p.has("endTime"))      dto.setEndTime(java.time.LocalDateTime.parse(p.get("endTime").getAsString()));
                    Platform.runLater(() -> biddingController.updateAuctionRealtimeView(dto));
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi parse AUCTION_UPDATE_NOTIFICATION: " + e.getMessage(), e);
                }
                break;
            }

            case AUCTION_STARTED_NOTIFICATION:
                LoggerUtil.info("Nhận thông báo phiên đấu giá mới bắt đầu.");
                break;

            case AUCTION_ENDED_NOTIFICATION: {
                LoggerUtil.info("Nhận thông báo phiên đấu giá kết thúc.");
                if (payload != null) {
                    try {
                        JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                        String winner = resp.has("winnerName") ? resp.get("winnerName").getAsString() : "N/A";
                        Platform.runLater(() -> DialogUtil.showInfo("Phiên đấu giá đã kết thúc! Người thắng: " + winner));
                    } catch (Exception ignored) {}
                }
                break;
            }

            case USER_BALANCE_UPDATE_NOTIFICATION: {
                LoggerUtil.info("Nhận thông báo cập nhật số dư tài khoản.");
                if (payload != null) {
                    try {
                        User updated = gson.fromJson(payload, User.class);
                        NetworkService.getInstance().setCurrentUser(updated);
                        Platform.runLater(() -> DialogUtil.showInfo("Số dư tài khoản của bạn đã được cập nhật!"));
                    } catch (Exception ignored) {}
                }
                break;
            }

            case USER_BANNED_NOTIFICATION:
                Platform.runLater(() -> DialogUtil.showError("Tài khoản của bạn đã bị Admin khóa."));
                break;

            case CHAT_MESSAGE_NOTIFICATION:
                // TODO: implement chat UI
                break;

            case PONG:
                LoggerUtil.info("Nhận PONG — kết nối mạng ổn định.");
                break;

            default:
                LoggerUtil.warning("MessageType chưa được xử lý ở Client: " + type);
                break;
        }
    }

    /**
     * Khi cả auctions lẫn users đã có đủ → đẩy vào AdminPanelController.
     * Admin gửi 2 request song song (GET_ALL_AUCTIONS + GET_ALL_USERS),
     * response về không theo thứ tự → merge ở đây.
     */
    private synchronized void tryFlushAdminDashboard() {
        if (adminPanelController == null) return;
        if (pendingAdminAuctions == null || pendingAdminUsers == null) return;

        List<AuctionDTO> auctions = pendingAdminAuctions;
        List<User> users = pendingAdminUsers;
        pendingAdminAuctions = null;
        pendingAdminUsers = null;

        Platform.runLater(() -> adminPanelController.updateAdminDashboard(auctions, users));
    }

    /**
     * Helper: parse status/message và hiện dialog phù hợp.
     */
    private void handleSimpleStatusResponse(String payload, String successMsg) {
        if (payload == null) return;
        try {
            JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
            String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
            if ("OK".equals(status)) {
                Platform.runLater(() -> DialogUtil.showInfo(successMsg));
            } else {
                String msg = resp.has("message") ? resp.get("message").getAsString() : "Có lỗi xảy ra.";
                Platform.runLater(() -> DialogUtil.showError(msg));
            }
        } catch (Exception ignored) {}
    }

    private void triggerDisconnectionUI() {
        Platform.runLater(() -> LoggerUtil.warning("Mất kết nối với Server."));
    }

    public synchronized void stopListening() {
        if (!isRunning) return;
        isRunning = false;
        biddingController = null;
        auctionListController = null;
        productDetailsController = null;
        adminPanelController = null;
        sellerDashboardController = null;
        pendingAdminAuctions = null;
        pendingAdminUsers = null;
        try {
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            LoggerUtil.info("Đã đóng ServerListener.");
        } catch (IOException e) {
            LoggerUtil.error("Lỗi đóng socket ServerListener.");
        }
    }
}