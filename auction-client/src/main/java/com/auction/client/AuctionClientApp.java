package com.auction.client;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AuctionClientApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Khởi tạo và kết nối mạng thông qua Service tập trung
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
            LoggerUtil.logError("Lỗi khởi động UI", e);
            DialogUtil.showError("Lỗi nghiêm trọng: Không thể khởi chạy giao diện.");
        }
    }

    private void initNetwork() {
        // Chạy khởi tạo mạng ngầm để tránh block UI thread khi kết nối chậm
        Thread connectionThread = new Thread(() -> {
            try {
                NetworkService.getInstance().connect("localhost", 8080);
                System.out.println(">>> Kết nối Server & Kích hoạt Hệ thống mạng thành công.");
            } catch (Exception e) {
                System.err.println("Lỗi kết nối mạng: " + e.getMessage());
                Platform.runLater(() ->
                        DialogUtil.showError("Không thể kết nối tới Server. Một số tính năng mạng sẽ bị vô hiệu hóa!")
                );
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    @Override
    public void stop() throws Exception {
        // Đóng toàn bộ kết nối và dọn dẹp các thread ngầm của Service
        NetworkService.getInstance().close();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
