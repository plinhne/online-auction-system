package com.auction.client.network;

import com.auction.client.controller.RealTimeBiddingController;
import com.auction.client.controller.AuctionListViewController;
import com.auction.client.util.LoggerUtil;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.auction.model.auction.Auction;
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

    // Dùng BufferedReader thay vì ObjectInputStream
    private final BufferedReader in;
    private final Gson gson;
    private volatile boolean isRunning;

    private RealTimeBiddingController biddingController;
    private AuctionListViewController auctionListController;

    // Constructor nhận BufferedReader đã được khởi tạo từ NetworkService
    public ServerListener(Socket socket, BufferedReader in) {
        this.socket = socket;
        this.in = in;
        this.gson = new Gson();
        this.isRunning = true;
        this.setName("Thread-Client-ServerListener");
    }

    public void setBiddingController(RealTimeBiddingController controller) {
        this.biddingController = controller;
    }

    public void setAuctionListController(AuctionListViewController controller) {
        this.auctionListController = controller;
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
                LoggerUtil.info("Nhận dữ liệu danh sách sản phẩm từ Server.");
                if (auctionListController != null) {
                    Type listType = new TypeToken<List<Auction>>(){}.getType();
                    List<Auction> auctions = gson.fromJson(payload, listType);
                    auctionListController.updateAuctionListFromServer(auctions);
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
                    com.auction.model.user.User updatedUser = gson.fromJson(payload, com.auction.model.user.User.class);
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

        try {
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            LoggerUtil.info("Đã đóng Socket ServerListener.");
        } catch (IOException e) {
            LoggerUtil.error("Gặp lỗi trong quá trình giải phóng luồng mạng.");
        }
    }
}