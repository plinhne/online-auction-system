package com.auction.client.network;

import com.auction.client.controller.RealTimeBiddingController;
import com.auction.client.util.LoggerUtil;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.auction.model.auction.Auction;
import com.google.gson.Gson;

import javafx.application.Platform;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * ServerListener là một luồng chạy ngầm độc lập (Background Thread) ở Client.
 * Chịu trách nhiệm liên tục chờ đợi và đón nhận các đối tượng NetworkMessage
 * được tuần tự hóa (Serialized) từ Server gửi về qua cổng Socket.
 */
public class ServerListener extends Thread {
    private final Socket socket;
    private final ObjectInputStream in;
    private final Gson gson;
    private volatile boolean isRunning;

    // Tham chiếu đến Controller điều khiển phòng đấu giá trực tiếp để cập nhật UI
    private RealTimeBiddingController biddingController;

    /**
     * Khởi tạo luồng lắng nghe máy chủ.
     * @param socket Cổng Socket kết nối đã được thiết lập thành công ở Client
     * @throws IOException Nếu xảy ra lỗi khởi tạo luồng vào dữ liệu (InputStream)
     */
    public ServerListener(Socket socket) throws IOException {
        this.socket = socket;

        // Cực kỳ quan trọng: Phía Server khởi tạo ObjectOutputStream trước,
        // thì phía Client phải khởi tạo ObjectInputStream tương ứng để khớp dòng nối.
        this.in = new ObjectInputStream(socket.getInputStream());

        this.gson = new Gson();
        this.isRunning = true;

        // Đặt tên luồng rõ ràng để phục vụ Debug đa luồng sạch sẽ
        this.setName("Thread-Client-ServerListener");
    }

    /**
     * Đăng ký tham chiếu RealTimeBiddingController khi người dùng truy cập phòng đấu giá.
     */
    public void setBiddingController(RealTimeBiddingController controller) {
        this.biddingController = controller;
    }

    /**
     * Hủy đăng ký tham chiếu Controller khi người dùng thoát khỏi phòng đấu giá.
     */
    public void removeBiddingController() {
        this.biddingController = null;
    }

    @Override
    public void run() {
        LoggerUtil.info("ServerListener phía Client đã kích hoạt, sẵn sàng đón nhận gói tin Object...");

        while (isRunning && !socket.isClosed()) {
            try {
                // Đọc trực tiếp đối tượng NetworkMessage tuần tự hóa (Lệnh nghẽn - Blocking Method)
                Object receivedObject = in.readObject();

                if (receivedObject instanceof NetworkMessage) {
                    NetworkMessage message = (NetworkMessage) receivedObject;
                    // Chuyển tiếp phân tách xử lý dựa trên nhãn lệnh MessageType
                    handleIncomingMessage(message);
                }

            } catch (IOException | ClassNotFoundException e) {
                // Xử lý ngoại lệ khi mất kết nối đột ngột (Ví dụ: Server bị sập hoặc đứt mạng)
                if (isRunning) {
                    LoggerUtil.error("Ngắt kết nối đột ngột từ hệ thống Máy chủ (Server đã đóng hoặc lỗi mạng).");
                    triggerDisconnectionUI();
                    stopListening();
                }
                break;
            }
        }
    }

    /**
     * Phân rã nội dung gói tin dựa theo nhãn định danh MessageType theo sơ đồ lớp.
     */
    private void handleIncomingMessage(NetworkMessage message) {
        MessageType type = message.getType();
        String payload = message.getPayload();

        switch (type) {
            case LOGIN_RESPONSE:
                LoggerUtil.info("Nhận phản hồi trạng thái Đăng nhập từ Server.");
                // Xử lý logic chuyển màn hình hoặc báo lỗi đăng nhập (Nếu bọc qua Session)
                break;

            case AUCTION_UPDATE_NOTIFICATION: // Tín hiệu đẩy Realtime từ mẫu thiết kế Observer ở Server
                LoggerUtil.info("Nhận tín hiệu phát sóng (Broadcast) cập nhật phiên đấu giá Realtime.");

                if (biddingController != null) {
                    // Giải mã chuỗi JSON thô nằm trong vỏ bọc mạng thành thực thể đối tượng Auction
                    Auction updatedAuction = gson.fromJson(payload, Auction.class);

                    // Đồng bộ hóa luồng giao diện UI JavaFX an toàn, chặn lỗi IllegalStateException
                    Platform.runLater(() -> {
                        biddingController.updateAuctionRealtimeView(updatedAuction);
                    });
                }
                break;

            default:
                LoggerUtil.warning("Nhận được gói tin có cấu trúc MessageType chưa được phân rã xử lý: " + type);
                break;
        }
    }

    /**
     * Thông báo hiển thị cảnh báo cho người dùng trên giao diện JavaFX khi mất kết nối mạng.
     */
    private void triggerDisconnectionUI() {
        Platform.runLater(() -> {
            LoggerUtil.warning("Hệ thống mạng ngầm Client đã ngắt tín hiệu kết nối.");
            // Nhóm có thể gọi DialogUtil.showError("Mất kết nối tới Server!") tại đây để bật Pop-up lên màn hình
        });
    }

    /**
     * Đóng luồng mạng, giải phóng tài nguyên hệ thống một cách an toàn (Graceful Shutdown).
     */
    public synchronized void stopListening() {
        if (!isRunning) return;

        this.isRunning = false;
        this.biddingController = null;

        try {
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LoggerUtil.info("Đã dọn dẹp luồng mạng ServerListener và đóng Socket thành công.");
        } catch (IOException e) {
            LoggerUtil.error("Gặp lỗi trong quá trình đóng giải phóng luồng mạng Client.");
        }
    }
}