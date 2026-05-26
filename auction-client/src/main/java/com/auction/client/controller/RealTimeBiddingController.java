package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
public class RealTimeBiddingController {
    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML
    public void initialize() {
        productNameLabel.setText("Vintage Porsche Carrera");
        currentPriceLabel.setText("$285,000");
        timeRemainingLabel.setText("⏱️ 3h 45m 30s");
        placeBidButton.setOnAction(e -> {
            String bidAmount = bidAmountField.getText();
            System.out.println("✓ Đã đặt giá: $" + bidAmount);
            System.out.println("✓ Bid được cập nhật!");
        });
    }
}

