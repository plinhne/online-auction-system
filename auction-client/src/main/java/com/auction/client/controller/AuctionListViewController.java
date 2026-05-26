package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AuctionListViewController {

    @FXML private Label titleLabel;
    @FXML private VBox contentBox; // Container chứa danh sách các nút hoặc thẻ sản phẩm

    @FXML
    public void initialize() {
        // 1. Đặt tiêu đề
        titleLabel.setText("\uD83D\uDCCB Danh Sách Đấu Giá");

        // 2. Tạo thử một nút sản phẩm (Người mới thường tạo trực tiếp bằng code như thế này)
        Button detailsBtn = new Button("\uD83D\uDC41️ Xem Chi Tiết Sản Phẩm");
        detailsBtn.setPrefWidth(300);
        detailsBtn.setStyle("-fx-padding: 10; -fx-cursor: hand;");

        // 3. Gán sự kiện mở chi tiết
        detailsBtn.setOnAction(e -> openProductDetails());

        // 4. Thêm vào giao diện
        contentBox.getChildren().add(detailsBtn);
    }

    private void openProductDetails() {
        try {
            // Tải file FXML chi tiết sản phẩm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailsView.fxml"));
            Parent root = loader.load();

            // Tạo một cửa sổ mới (Stage) để xem chi tiết
            Stage stage = new Stage();
            stage.setTitle("Chi Tiết Sản Phẩm");

            Scene scene = new Scene(root, 1000, 700);
            // Load CSS
            scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

            System.out.println("Mở chi tiết sản phẩm thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi: Không tìm thấy ProductDetailsView.fxml");
            e.printStackTrace();
        }
    }
}
