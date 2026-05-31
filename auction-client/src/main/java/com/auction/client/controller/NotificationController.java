package com.auction.client.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class NotificationController {

    @FXML
    private HBox rootContainer;

    @FXML
    private Label messageLabel;

    /**
     * Hàm này để các Controller khác gọi vào và set nội dung thông báo.
     */
    public void setMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    /**
     * Hàm bắt sự kiện khi bấm nút X.
     * Làm mờ thông báo trong 0.3s rồi xóa hẳn nó khỏi giao diện.
     */
    @FXML
    private void close() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), rootContainer);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> {
            // Sau khi mờ đi, xóa HBox này khỏi Parent chứa nó (VBox, FlowPane...)
            if (rootContainer.getParent() instanceof Pane) {
                ((Pane) rootContainer.getParent()).getChildren().remove(rootContainer);
            }
        });
        fadeOut.play();
    }
}