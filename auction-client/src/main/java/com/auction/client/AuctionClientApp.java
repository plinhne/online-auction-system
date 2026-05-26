package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class AuctionClientApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            // 1. Tải file LoginView.fxml 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            BorderPane root = loader.load();
            
            // 2. Tạo scene 600x700
            Scene scene = new Scene(root, 600, 700);
            
            // 3. ĐƯỜNG DẪN CSS: 
            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());
            
            // 4. Thiết lập Stage
            stage.setScene(scene);
            stage.setTitle("Auction System - Login");
            
            // Hiển thị
            stage.show();
        } catch (Exception e) {
            System.err.println("Lỗi khởi động ứng dụng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
