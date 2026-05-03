package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminPanelController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;

    @FXML
    public void initialize() {
        totalAuctionsLabel.setText("📊 100 đấu giá");
        totalUsersLabel.setText("👥 50 người dùng");
        totalRevenueLabel.setText("💵 $5,250,000");
    }
}
