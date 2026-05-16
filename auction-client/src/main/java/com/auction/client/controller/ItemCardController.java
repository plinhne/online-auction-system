package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ItemCardController {

    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel; // Trong FXML của bạn thường đặt là currentPriceLabel
    @FXML private Button viewDetailsButton;

    @FXML
    public void initialize() {
        // Hiển thị dữ liệu mẫu để kiểm tra giao diện
        if (itemNameLabel != null) itemNameLabel.setText("Sản phẩm mẫu");
        if (currentPriceLabel != null) currentPriceLabel.setText("$0.00");

        // Gán sự kiện cho nút bấm
        if (viewDetailsButton != null) {
            viewDetailsButton.setOnAction(e -> openProductDetails());
        }
    }

    private void openProductDetails() {
        try {
            // Tải màn hình chi tiết sản phẩm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailsView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Chi tiết sản phẩm");

            Scene scene = new Scene(root, 1200, 800);

            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            System.err.println("Lỗi khi mở ProductDetailsView: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
