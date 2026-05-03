package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
public class ProductDetailsController {
    @FXML private Label productNameLabel;
    @FXML private Label priceLabel;
    @FXML private Label descriptionLabel;
    @FXML private Button placeBidButton;
    @FXML
    public void initialize() {
        productNameLabel.setText("Vintage Porsche 911 Carrera");
        priceLabel.setText("$285,000");
        descriptionLabel.setText(
                "Rare 1973 Porsche 911 Carrera RS 2.7 in original condition.\n" +
                        "Complete service history and matching numbers."
        );
        placeBidButton.setOnAction(e -> openBiddingView());
    }
    private void openBiddingView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RealTimeBiddingView.fxml"));
            BorderPane biddingView = loader.load();
            Scene scene = new Scene(biddingView, 1400, 900);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Đặt Giá - " + productNameLabel.getText());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


