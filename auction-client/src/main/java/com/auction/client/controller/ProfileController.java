package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;

    @FXML
    public void initialize() {
        usernameLabel.setText("👤 Demo User");
        emailLabel.setText("📧 user@auction.com");
        roleLabel.setText("🎯 Bidder");
    }
}
