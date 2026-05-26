package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button bidderDemoButton;
    @FXML private Button sellerDemoButton;
    @FXML private Button adminDemoButton;

    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> {
            if (usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
                errorLabel.setText("Vui lòng nhập đầy đủ!");
            } else {
                goToMainView();
            }
        });

        bidderDemoButton.setOnAction(e -> goToMainView());
        sellerDemoButton.setOnAction(e -> goToMainView());
        adminDemoButton.setOnAction(e -> goToMainView());
    }

    private void goToMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            BorderPane mainView = loader.load();

            Scene scene = new Scene(mainView, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu giá - Trang chủ");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) errorLabel.setText("Khong the mo MainView!");
        }
    }
}