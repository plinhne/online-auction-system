package com.auction.client;

import com.auction.client.network.ServerListener;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.user.User; // Nhận diện User khi đăng nhập thành công
import com.auction.network.NetworkMessage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class AuctionClientApp extends Application {

    private static Socket socket;
    private static ObjectOutputStream outStream;
    private static ObjectInputStream inStream;

    // Lưu thông tin người dùng đang đăng nhập hiện tại toàn cục
    private static User currentUser;

    @Override
    public void start(Stage stage) {
        // 1. Kết nối mạng và kích hoạt ServerListener
        initNetwork();

        try {
            // 2. Tải màn hình khởi đầu (LoginView)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            BorderPane root = loader.load();
            Scene scene = new Scene(root, 600, 700);

            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Auction System - Login");
            stage.show();
        } catch (Exception e) {
            LoggerUtil.logError("Lỗi khởi động UI", e); // Sử dụng LoggerUtil của bạn
            DialogUtil.showError("Lỗi nghiêm trọng: Không thể khởi chạy giao diện.");
        }
    }

    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8080);
            outStream = new ObjectOutputStream(socket.getOutputStream());
            inStream = new ObjectInputStream(socket.getInputStream());

            // KHỞI CHẠY THREAD LẮNG NGHE BẤT ĐỒNG BỘ (ServerListener)
            ServerListener listener = new ServerListener(inStream);
            Thread listenerThread = new Thread(listener);
            listenerThread.setDaemon(true); // Tự động tắt thread này khi tắt ứng dụng FX
            listenerThread.start();

            System.out.println(">>> Kết nối Server & Kích hoạt ServerListener thành công.");
        } catch (Exception e) {
            System.err.println("Lỗi kết nối mạng: " + e.getMessage());
            // Tránh crash app, hiển thị dialog cảnh báo
            Platform.runLater(() -> DialogUtil.showError("Không thể kết nối tới Server. Một số tính năng mạng sẽ bị vô hiệu hóa!"));
        }
    }

    // --- Các hàm static tiện ích dùng chung cho toàn bộ các Controller ---

    public static void sendNetworkMessage(NetworkMessage msg) {
        try {
            if (outStream != null) {
                outStream.writeObject(msg);
                outStream.flush();
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi gói tin: " + e.getMessage());
        }
    }

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    @Override
    public void stop() throws Exception {
        if (outStream != null) outStream.close();
        if (inStream != null) inStream.close();
        if (socket != null) socket.close();
        super.stop();
    }

    public static void main(String[] args) { launch(args); }
}
