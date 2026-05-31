package com.auction.client.network;

import com.auction.client.controller.RealTimeBiddingController;
import com.auction.client.controller.AuctionListViewController;
import com.auction.client.controller.ProductDetailsController;
import com.auction.client.controller.AdminPanelController;
import com.auction.client.util.LoggerUtil;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerListener extends Thread {
    private final Socket socket;

    // Dùng BufferedReader thay vì ObjectInputStream
    private final BufferedReader in;
    private final Gson gson;
    private volatile boolean isRunning;

    private RealTimeBiddingController biddingController;
    private AuctionListViewController auctionListController;
    private ProductDetailsController productDetailsController;

    // ĐÃ BỔ SUNG: Khai báo biến controller cho Admin Panel
    private AdminPanelController adminPanelController;

    // Constructor nhận BufferedReader đã được khởi tạo từ NetworkService
    public ServerListener(Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
        this.gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
                .create();
        this.isRunning = true;
        this.setName("Thread-Client-ServerListener");
    }

    public void setBiddingController(RealTimeBiddingController controller) {
        this.biddingController = controller;
    }

    public void setAuctionListController(AuctionListViewController controller) {
        this.auctionListController = controller;
    }

    public void setProductDetailsController(ProductDetailsController controller) {
        this.productDetailsController = controller;
    }

    // ĐÃ BỔ SUNG: Hàm setter cho AdminPanelController
    public void setAdminPanelController(AdminPanelController controller) {
        this.adminPanelController = controller;
    }

    public void removeBiddingController() {
        this.biddingController = null;
    }

    @Override
    public void run() {
        LoggerUtil.info("ServerListener phía Client đã kích hoạt...");
        String jsonLine;

        try {
            // Đọc từng dòng Text bằng readLine()
            while (isRunning && !socket.isClosed() && (jsonLine = in.readLine()) != null) {
                try {
                    // Dịch ngược chuỗi JSON thành đối tượng NetworkMessage
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
            case LOGIN_RESPONSE:
                LoggerUtil.info("Nhận phản hồi Đăng nhập từ Server (Đã xử lý ở LoginController).");
                break;

            case SIGNUP_RESPONSE:
                LoggerUtil.info("Nhận phản hồi Đăng ký từ Server.");
                if (payload != null && !payload.isEmpty()) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    String status = resp.has("status") ? resp.get("status").getAsString() : "ERROR";

                    if ("OK".equals(status)) {
                        Platform.runLater(() -> {
                            com.auction.client.util.DialogUtil.showInfo("Đăng ký thành công! Vui lòng quay lại màn hình và đăng nhập.");
                        });
                    } else {
                        String errMsg = resp.has("message") ? resp.get("message").getAsString() : "Đăng ký thất bại không rõ nguyên nhân.";
                        Platform.runLater(() -> {
                            com.auction.client.util.DialogUtil.showError("Lỗi đăng ký: " + errMsg);
                        });
                    }
                }
                break;

            case GET_ALL_AUCTIONS_RESPONSE:
                LoggerUtil.info("--- TRẠM 1: Đã nhận dữ liệu JSON ---");
                LoggerUtil.info("Nhận dữ liệu danh sách sản phẩm từ Server.");
                if (auctionListController != null && payload != null) {
                    try {
                        JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();

                        if (resp.has("auctions")) {
                            Type listType = new TypeToken<List<com.auction.dto.AuctionDTO>>(){}.getType();
                            List<com.auction.dto.AuctionDTO> auctions = gson.fromJson(resp.get("auctions"), listType);

                            Platform.runLater(() -> auctionListController.updateAuctionListFromServer(auctions));
                        } else if (resp.has("status") && "ERROR".equals(resp.get("status").getAsString())) {
                            LoggerUtil.error("Server báo lỗi: " + resp.get("message").getAsString());
                        }
                    } catch (Exception e) {
                        LoggerUtil.error("Lỗi bóc tách dữ liệu danh sách đấu giá: " + e.getMessage(), e);
                    }
                }
                break;

            case GET_ITEM_DETAILS_RESPONSE:
                LoggerUtil.info("Nhận thông tin chi tiết Item từ Server.");
                if (payload != null && !payload.isEmpty()) {
                    JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();
                    if (resp.has("item")) {
                        com.auction.model.item.Item itemDetail = gson.fromJson(resp.get("item"), com.auction.model.item.Item.class);
                        if (productDetailsController != null) {
                            Platform.runLater(() -> productDetailsController.setItemDetails(itemDetail));
                        }
                    }
                }
                break;

            // ĐÃ BỔ SUNG: Xử lý dữ liệu trả về cho Admin Panel
            case ADMIN_ACTION_RESPONSE: // Lưu ý: Hãy đảm bảo tên này khớp chính xác với MessageType Server trả về
                LoggerUtil.info("Nhận dữ liệu tổng hợp cho Admin Panel từ Server.");
                if (adminPanelController != null && payload != null) {
                    try {
                        JsonObject resp = JsonParser.parseString(payload).getAsJsonObject();

                        // 1. Lấy danh sách Auctions
                        // 1. Lấy danh sách Auctions (ĐÃ ĐỔI SANG DTO)
                        List<com.auction.dto.AuctionDTO> adminAuctions = new java.util.ArrayList<>();
                        if (resp.has("auctions")) {
                            java.lang.reflect.Type auctionListType = new com.google.gson.reflect.TypeToken<List<com.auction.dto.AuctionDTO>>(){}.getType();
                            adminAuctions = gson.fromJson(resp.get("auctions"), auctionListType);
                        }

// 2. Lấy danh sách Users (Giữ nguyên)
                        List<com.auction.model.user.User> adminUsers = new java.util.ArrayList<>();
                        if (resp.has("users")) {
                            java.lang.reflect.Type userListType = new com.google.gson.reflect.TypeToken<List<com.auction.model.user.User>>(){}.getType();
                            adminUsers = gson.fromJson(resp.get("users"), userListType);
                        }

// 3. Đẩy cả 2 danh sách vào giao diện (ĐÃ ĐỔI SANG DTO)
                        final List<com.auction.dto.AuctionDTO> finalAuctions = adminAuctions;
                        final List<com.auction.model.user.User> finalUsers = adminUsers;

                        Platform.runLater(() -> adminPanelController.updateAdminDashboard(finalAuctions, finalUsers));
                    } catch (Exception e) {
                        LoggerUtil.error("Lỗi bóc tách dữ liệu Admin Panel: " + e.getMessage(), e);
                    }
                }
                break;

            case AUCTION_UPDATE_NOTIFICATION:
                LoggerUtil.info("Nhận tín hiệu Broadcast cập nhật phiên đấu giá Realtime.");
                if (biddingController != null) {
                    Auction updatedAuction = gson.fromJson(payload, Auction.class);
                    Platform.runLater(() -> {
                        biddingController.updateAuctionRealtimeView(updatedAuction);
                    });
                }
                break;

            case USER_BALANCE_UPDATE_NOTIFICATION:
                LoggerUtil.info("Nhận tín hiệu cập nhật số dư từ Admin.");
                if (payload != null && !payload.isEmpty()) {
                    User updatedUser = gson.fromJson(payload, User.class);
                    NetworkService.getInstance().setCurrentUser(updatedUser);
                    Platform.runLater(() -> {
                        com.auction.client.util.DialogUtil.showInfo("Số dư tài khoản của bạn vừa được hệ thống cập nhật thành công!");
                    });
                }
                break;

            case PONG:
                LoggerUtil.info("Đã nhận PONG từ Server - Kết nối mạng ổn định.");
                break;

            default:
                LoggerUtil.warning("Gói tin MessageType chưa được hỗ trợ lắng nghe ở Client: " + type);
                break;
        }
    }

    private void triggerDisconnectionUI() {
        Platform.runLater(() -> {
            LoggerUtil.warning("Hệ thống mạng ngầm Client đã ngắt tín hiệu kết nối.");
        });
    }

    public synchronized void stopListening() {
        if (!isRunning) return;
        this.isRunning = false;
        this.biddingController = null;
        this.auctionListController = null;
        this.productDetailsController = null;

        // ĐÃ BỔ SUNG: Dọn dẹp bộ nhớ cho Admin Panel Controller
        this.adminPanelController = null;

        try {
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            LoggerUtil.info("Đã đóng Socket ServerListener.");
        } catch (IOException e) {
            LoggerUtil.error("Gặp lỗi trong quá trình giải phóng luồng mạng.");
        }
    }
}