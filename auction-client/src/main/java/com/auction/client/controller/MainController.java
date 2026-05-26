package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML private StackPane contentArea;

    @FXML
    public void initialize() {
        System.out.println("✓ MainView đã tải thành công!");
    }
}
