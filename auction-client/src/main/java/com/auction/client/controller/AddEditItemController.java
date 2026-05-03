package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddEditItemController {

    @FXML private TextField itemNameField;
    @FXML private TextField priceField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> {
            System.out.println("✓ Đã lưu mục: " + itemNameField.getText());
            System.out.println("✓ Giá: $" + priceField.getText());
            closeWindow();
        });

        cancelButton.setOnAction(e -> {
            System.out.println("✗ Đã hủy");
            closeWindow();
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
