package com.auction.client.network;

import com.auction.client.controller.RealTimeBiddingController;
import com.auction.client.controller.AuctionListViewController;
import com.auction.client.controller.ProductDetailsController;
import com.auction.client.controller.AdminPanelController;
import com.auction.client.controller.SellerDashboardController;
import com.auction.client.util.LoggerUtil;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.auction.model.user.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class ServerListener extends Thread {
    private final java.net.Socket socket;
    private final BufferedReader in;
    private final Gson gson;
    private volatile boolean isRunning;

    private RealTimeBiddingController biddingController;
    private AuctionListViewController auctionListController;
    private ProductDetailsController productDetailsController;
    private AdminPanelController adminPanelController;
    private SellerDashboardController sellerDashboardController;

    public ServerListener(java.net.Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
        this.gson = new com.google.gson.GsonBuilder()
                // 2 dòng cấu hình LocalDateTime cũ của bạn
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))

                // ĐÃ BỔ SUNG: Dạy Gson cách tạo Object User từ các class con dựa vào Role
                .registerTypeAdapter(com.auction.model.user.User.class, (com.google.gson.JsonDeserializer<com.auction.model.user.User>) (json, typeOfT, context) -> {
                    JsonObject jsonObject = json.getAsJsonObject();
                    String role = jsonObject.has("role") ? jsonObject.get("role").getAsString() : "BIDDER";
                    switch (role) {
                        case "ADMIN":
                            return context.deserialize(json, com.auction.model.user.Admin.class);
                        case "SELLER":
                            return context.deserialize(json, com.auction.model.user.Seller.class);
                        default:
                            return context.deserialize(json, com.auction.model.user.Bidder.class);
                    }
                })
                .create();
        this.isRunning = true;
        this.setName("Thread-Client-ServerListener");
    }

    // --- Các hàm Setter để đăng ký Controller ---
    public void setBiddingController(RealTimeBiddingController controller) { this.biddingController = controller; }
    public void setAuctionListController(AuctionListViewController controller) { this.auctionListController = controller; }
    public void setProductDetailsController(ProductDetailsController controller) { this.productDetailsController = controller; }
    public void setAdminPanelController(AdminPanelController controller) { this.adminPanelController = controller; }
    public void setSellerDashboardController(SellerDashboardController controller) { this.sellerDashboardController = controller; }
    public void removeBiddingController() { this.biddingController = null; }

    @Override
    public void run() {
        LoggerUtil.info("ServerListener phía Client đã kích hoạt...");
        String jsonLine;

        try {
            while (isRunning && !socket.isClosed() && (jsonLine = in.readLine()) != null) {
                try {
                    NetworkMessage message = gson.fromJson(jsonLine, NetworkMessage.class);
                    if (message != null && message.getType() != null) {
                        handleIncomingMessage(message);
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi giải mã JSON từ Server.", e);
                }
            }
        } catch (IOException e) {
            if (isRunning) triggerDisconnectionUI();
        } finally {
            stopListening();
        }
    }

    private void handleIncomingMessage(NetworkMessage message) {
        MessageType type = message.getType();
        String payload = message.getPayload();

        switch (type) {
            // ==========================================
            // NHÓM AUTH (ĐĂNG NHẬP / ĐĂNG XUẤT)
            // ==========================================
            case LOGIN_RESPONSE:
                LoggerUtil.info("Nhận phản hồi Đăng nhập từ Server.");
                break;

            case SIGNUP_RESPONSE:
                LoggerUtil.info("Nhận phản hồi Đăng ký từ Server.");
                if (payload != null && !payload.isEmpty()) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
                    String msg = resp.has("message") ? resp.get("message").getAsString() : "Lỗi không xác định";

                    Platform.runLater(() -> {
                        if ("OK".equals(status)) com.auction.client.util.DialogUtil.showInfo("Đăng ký thành công!");
                        else com.auction.client.util.DialogUtil.showError("Lỗi đăng ký: " + msg);
                    });
                }
                break;

            case LOGOUT_RESPONSE:
                LoggerUtil.info("Đã đăng xuất thành công.");
                Platform.runLater(() -> {
                    try {
                        // Chuyển thẳng về màn hình đăng nhập
                        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
                        javafx.stage.Stage stage = (javafx.stage.Stage) javafx.stage.Window.getWindows().get(0);
                        stage.setScene(new javafx.scene.Scene(root));
                        com.auction.client.util.DialogUtil.showInfo("Bạn đã đăng xuất khỏi hệ thống.");
                    } catch (IOException e) {
                        LoggerUtil.error("Lỗi chuyển màn hình đăng xuất", e);
                    }
                });
                break;

            // ==========================================
            // NHÓM TRUY VẤN DỮ LIỆU TĨNH (GET)
            // ==========================================
            case GET_ALL_AUCTIONS_RESPONSE:
                if (payload != null) {
                    try {
                        JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                        if (resp.has("auctions")) {
                            Type listType = new TypeToken<List<com.auction.dto.AuctionDTO>>(){}.getType();
                            List<com.auction.dto.AuctionDTO> auctions = gson.fromJson(resp.get("auctions"), listType);
                            Platform.runLater(() -> {
                                if (auctionListController != null) auctionListController.updateAuctionListFromServer(auctions);
                                if (adminPanelController != null) adminPanelController.updateAdminDashboard(auctions, null);
                            });
                        }
                    } catch (Exception e) { LoggerUtil.error("Lỗi parse list auction: ", e); }
                }
                break;

            case GET_ALL_USERS_RESPONSE:
                if (adminPanelController != null && payload != null) {
                    try {
                        JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                        if (resp.has("users")) {
                            Type userListType = new TypeToken<List<com.auction.model.user.User>>(){}.getType();
                            List<com.auction.model.user.User> users = gson.fromJson(resp.get("users"), userListType);
                            Platform.runLater(() -> adminPanelController.updateAdminDashboard(null, users));
                        }
                    } catch (Exception e) { LoggerUtil.error("Lỗi parse list users: ", e); }
                }
                break;

            case GET_ITEM_DETAILS_RESPONSE:
                if (payload != null && !payload.isEmpty() && productDetailsController != null) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    if (resp.has("item")) {
                        com.auction.model.item.Item itemDetail = gson.fromJson(resp.get("item"), com.auction.model.item.Item.class);
                        Platform.runLater(() -> productDetailsController.setItemDetails(itemDetail));
                    }
                }
                break;

            case GET_MY_AUCTIONS_RESPONSE:
            case GET_MY_ITEMS_RESPONSE:
                if (sellerDashboardController != null) {
                    sellerDashboardController.handleServerResponse(message);
                }
                break;

            // ==========================================
            // NHÓM NGƯỜI BÁN (SELLER ACTIONS)
            // ==========================================
            case ADD_ITEM_RESPONSE:
            case EDIT_ITEM_RESPONSE:
            case DELETE_ITEM_RESPONSE:
            case CREATE_AUCTION_RESPONSE:
            case CANCEL_AUCTION_RESPONSE:
                LoggerUtil.info("Nhận phản hồi lệnh Seller: " + type);
                if (sellerDashboardController != null) {
                    sellerDashboardController.handleServerResponse(message);
                } else {
                    // Nếu không ở màn hình Seller nhưng vẫn nhận kết quả
                    showGenericStatusDialog(payload, "Thao tác quản lý sản phẩm");
                }
                break;

            // ==========================================
            // NHÓM QUẢN TRỊ VIÊN (ADMIN ACTIONS)
            // ==========================================
            case CREATE_USER_RESPONSE:
            case UPDATE_USER_RESPONSE:
            case DELETE_USER_RESPONSE:
            case UPDATE_BALANCE_RESPONSE:
            case ADMIN_DELETE_AUCTION_RESPONSE:
            case ADMIN_UPDATE_AUCTION_RESPONSE:
                LoggerUtil.info("Nhận phản hồi lệnh Admin: " + type);
                if (payload != null) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
                    String msg = resp.has("message") ? resp.get("message").getAsString() : "Hoàn tất thao tác.";

                    Platform.runLater(() -> {
                        if ("OK".equals(status)) {
                            com.auction.client.util.DialogUtil.showInfo(msg);
                            // Auto-refresh lại bảng dữ liệu sau khi sửa/xóa thành công
                            if (adminPanelController != null) {
                                adminPanelController.refreshAdminDataFromServer();
                            }
                        } else {
                            com.auction.client.util.DialogUtil.showError("Lỗi thực thi lệnh Admin: " + msg);
                        }
                    });
                }
                break;

            // ==========================================
            // NHÓM ĐẤU GIÁ (BIDDING & ROOM)
            // ==========================================
            case JOIN_AUCTION_RESPONSE:
            case LEAVE_AUCTION_RESPONSE:
                LoggerUtil.info("Trạng thái phòng đấu giá: " + type);
                break;

            case PLACE_BID_RESPONSE:
            case SET_AUTO_BID_RESPONSE:
                LoggerUtil.info("Nhận phản hồi đặt giá: " + type);
                if (payload != null) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";

                    Platform.runLater(() -> {
                        if (!"OK".equals(status)) {
                            // Nếu lỗi (ví dụ thiếu tiền, giá thấp hơn) -> Bật bảng cảnh báo
                            String msg = resp.has("message") ? resp.get("message").getAsString() : "Lỗi đặt giá.";
                            com.auction.client.util.DialogUtil.showError(msg);
                        } else {
                            // Cập nhật số dư tiền hiển thị ở thanh header (nếu Server có trả về newBalance)
                            if (resp.has("newBalance")) {
                                User currentUser = NetworkService.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    currentUser.setWalletBalance(resp.get("newBalance").getAsDouble());
                                }
                            }
                        }
                    });
                }
                break;

            // ==========================================
            // NHÓM THÔNG BÁO REAL-TIME BROADCAST
            // ==========================================
            case AUCTION_UPDATE_NOTIFICATION:
                if (biddingController != null && payload != null) {
                    try {
                        com.auction.dto.AuctionDTO updatedAuction = gson.fromJson(payload, com.auction.dto.AuctionDTO.class);
                        Platform.runLater(() -> biddingController.updateAuctionRealtimeView(updatedAuction));
                    } catch (Exception e) { LoggerUtil.error("Lỗi parse Realtime update", e); }
                }
                break;

            case AUCTION_STARTED_NOTIFICATION:
                LoggerUtil.info("Một phiên đấu giá vừa bắt đầu!");
                Platform.runLater(() -> com.auction.client.util.DialogUtil.showInfo("Một phiên đấu giá mới vừa chính thức bắt đầu!"));
                break;

            case AUCTION_ENDED_NOTIFICATION:
                LoggerUtil.info("Nhận tín hiệu kết thúc đấu giá.");
                if (payload != null) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String messageText = resp.has("message") ? resp.get("message").getAsString() : "Một phiên đấu giá đã kết thúc!";
                    Platform.runLater(() -> com.auction.client.util.DialogUtil.showInfo(messageText));
                }
                break;

            case CHAT_MESSAGE_NOTIFICATION:
                // Dự phòng nếu sau này làm tính năng live chat trong phòng đấu giá
                LoggerUtil.info("Tin nhắn chat: " + payload);
                break;

            case USER_BANNED_NOTIFICATION:
                LoggerUtil.warning("Tài khoản của bạn đã bị quản trị viên khóa.");
                Platform.runLater(() -> {
                    com.auction.client.util.DialogUtil.showError("Tài khoản của bạn đã bị khóa! Bạn sẽ bị đăng xuất.");
                    try {
                        NetworkService.getInstance().sendNetworkMessage(new NetworkMessage(MessageType.LOGOUT_REQUEST, "{}"));
                    } catch (Exception ignored) {}
                });
                break;

            case USER_BALANCE_UPDATE_NOTIFICATION:
                if (payload != null) {
                    User updatedUser = gson.fromJson(payload, User.class);
                    NetworkService.getInstance().setCurrentUser(updatedUser);
                    Platform.runLater(() -> com.auction.client.util.DialogUtil.showInfo("Số dư của bạn vừa được Admin cập nhật!"));
                }
                break;

            case PING:
                // Trả lời PONG để giữ kết nối không bị timeout
                try {
                    NetworkService.getInstance().sendNetworkMessage(new NetworkMessage(MessageType.PONG, "{}"));
                } catch (Exception ignored) {}
                break;

            case PONG:
                LoggerUtil.info("Nhận PONG từ Server.");
                break;

            default:
                LoggerUtil.warning("Chưa có cấu hình bắt gói tin: " + type);
                break;
        }
    }

    /**
     * Hàm hỗ trợ hiển thị popup thông báo chung
     */
    private void showGenericStatusDialog(String payload, String actionName) {
        if (payload != null && !payload.isEmpty()) {
            JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
            String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";
            String msg = resp.has("message") ? resp.get("message").getAsString() : actionName + " hoàn tất.";
            Platform.runLater(() -> {
                if ("OK".equals(status)) com.auction.client.util.DialogUtil.showInfo(msg);
                else com.auction.client.util.DialogUtil.showError(actionName + " thất bại: " + msg);
            });
        }
    }

    private void triggerDisconnectionUI() {
        Platform.runLater(() -> LoggerUtil.warning("Ngắt kết nối mạng với Server."));
    }

    public synchronized void stopListening() {
        if (!isRunning) return;
        this.isRunning = false;
        this.biddingController = null;
        this.auctionListController = null;
        this.productDetailsController = null;
        this.adminPanelController = null;
        this.sellerDashboardController = null;

        try {
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi đóng Socket.", e);
        }
    }
}