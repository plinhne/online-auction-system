package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SellerDashboardController {

    @FXML private Label totalItemsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Button addItemButton;

    @FXML
    public void initialize() {
        totalItemsLabel.setText("📦 10 mục");
        totalRevenueLabel.setText("💰 $125,500");
        activeAuctionsLabel.setText("🔄 5 đấu giá");

        addItemButton.setOnAction(e -> openAddItemView());
    }

    private void openAddItemView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddItemView.fxml"));
            BorderPane addItemView = loader.load();

            Scene scene = new Scene(addItemView, 800, 900);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Thêm Mục Đấu Giá");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
