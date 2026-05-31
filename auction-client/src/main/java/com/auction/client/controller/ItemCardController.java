package com.auction.client.controller;

import com.auction.dto.AuctionDTO;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.io.IOException;

/**
 * Controller cho từng thẻ sản phẩm con
 * Hiển thị thông tin tóm tắt của một phiên đấu giá
 */
public class ItemCardController extends BaseController {

    // Các biến đã có
    @FXML private Label itemNameLabel;
    @FXML private Label priceLabel;
    @FXML private Button viewDetailsButton;

    // BỔ SUNG CÁC BIẾN ĐỂ HIỂN THỊ THÊM THÔNG TIN TỪ FXML
    @FXML private Label descriptionLabel;
    @FXML private Label categoryBadge;
    @FXML private Label statusBadge;
    @FXML private Label timeRemainingBadge;

    private AuctionDTO currentAuction;

    @FXML
    public void initialize() {
        viewDetailsButton.setOnAction(e -> openProductDetails());
    }

    /**
     * Đổ dữ liệu THẬT từ DTO lên giao diện
     */
    public void setAuctionData(AuctionDTO auction) {
        if (auction == null) return;
        this.currentAuction = auction;

        // 1. HIỂN THỊ TÊN THẬT TỪ DATABASE
        itemNameLabel.setText(auction.getItemName());

        // 2. HIỂN THỊ GIÁ TIỀN
        priceLabel.setText(FormatterUtil.formatCurrency(auction.getCurrentPrice()));

        // 3. HIỂN THỊ MÔ TẢ VÀ DANH MỤC (Kiểm tra null đề phòng FXML chưa gắn ID)
        if (descriptionLabel != null) {
            descriptionLabel.setText(auction.getItemDescription());
        }
        if (categoryBadge != null) {
            categoryBadge.setText(auction.getItemCategory());
        }

        // 4. HIỂN THỊ TRẠNG THÁI VÀ MÀU SẮC
        if (statusBadge != null && auction.getStatus() != null) {
            String status = auction.getStatus().name();
            if ("ACTIVE".equals(status)) {
                statusBadge.setText("ĐANG CHẠY");
                statusBadge.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;");
            } else if ("SCHEDULED".equals(status)) {
                statusBadge.setText("SẮP DIỄN RA");
                statusBadge.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;");
            } else {
                statusBadge.setText("ĐÃ KẾT THÚC");
                statusBadge.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;");
            }
        }

        // 5. HIỂN THỊ THỜI GIAN
        if (timeRemainingBadge != null && auction.getEndTime() != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm");
            timeRemainingBadge.setText("Hạn: " + auction.getEndTime().format(formatter));
        }
    }

    private void openProductDetails() {
        if (currentAuction == null) {
            LoggerUtil.warning("Không có dữ liệu phiên đấu giá hiện tại để xem chi tiết.");
            return;
        }

        try {
            LoggerUtil.info("→ Người dùng click nút xem chi tiết đấu giá ID: " + currentAuction.getId());
            switchWindow(viewDetailsButton, "/fxml/ProductDetailsView.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailsView.fxml"));

            Platform.runLater(() -> {
                try {
                    loader.load();
                    ProductDetailsController detailsController = loader.getController();
                    if (detailsController != null) {
                        detailsController.setAuctionDetails(currentAuction);
                    }
                } catch (IOException ex) {
                    LoggerUtil.error("Lỗi nạp dữ liệu chi tiết sản phẩm từ nút bấm thẻ Card.", ex);
                }
            });

        } catch (Exception e) {
            LoggerUtil.error("Lỗi nghiêm trọng không thể nạp giao diện chi tiết sản phẩm: ", e);
            DialogUtil.showError("Có lỗi xảy ra khi tải phân vùng giao diện Chi tiết sản phẩm.");
        }
    }
}