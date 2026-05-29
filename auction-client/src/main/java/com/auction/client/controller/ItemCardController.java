package com.auction.client.controller;

import com.auction.model.auction.Auction;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.io.IOException;

/**
 * Controller cho từng thẻ sản phẩm con
 * Hiển thị thông tin tóm tắt của một phiên đấu giá
 */
public class ItemCardController extends BaseController {

    @FXML private Label itemNameLabel;
    @FXML private Label priceLabel;
    @FXML private Button viewDetailsButton;

    // Quản lý phiên đấu giá hiện tại của tấm thẻ này
    private Auction currentAuction;

    @FXML
    public void initialize() {
        // Gán hành động sự kiện cho nút bấm "Xem chi tiết" nằm bên trong thẻ
        viewDetailsButton.setOnAction(e -> openProductDetails());
    }

    /**
     * Nhận vào đối tượng Auction để hiển thị thông tin thô lên tấm Card mẫu
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;

        // Hiển thị tạm thời ID sản phẩm (Có thể đổi thành tên sản phẩm nếu có liên kết thực thể dữ liệu Item)
        itemNameLabel.setText("Phòng đấu giá sản phẩm #" + auction.getItemId());

        // Đổ mức giá hiện tại (currentPrice) của phiên đấu giá lên nhãn hiển thị tiền tệ đã định dạng
        priceLabel.setText(FormatterUtil.formatCurrency(auction.getCurrentPrice()));
    }

    /**
     * ĐÃ CHUẨN HÓA: Điều hướng chuyển đổi scene ngay trên cửa sổ chính, hỗ trợ bung full màn hình mượt mà
     */
    private void openProductDetails() {
        if (currentAuction == null) {
            LoggerUtil.warning("Không có dữ liệu phiên đấu giá hiện tại để xem chi tiết.");
            return;
        }

        try {
            LoggerUtil.info("→ Người dùng click nút xem chi tiết đấu giá ID: " + currentAuction.getId());

            // 1. Chuyển đổi scene tập trung trên Stage hiện tại, không mở cửa sổ Stage phụ rác
            switchWindow(viewDetailsButton, "/fxml/ProductDetailsView.fxml");

            // 2. Khởi tạo bộ nạp cục bộ để đẩy dữ liệu ngữ cảnh sang màn hình chi tiết
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailsView.fxml"));

            // Ép luồng đồ họa JavaFX render layout cửa sổ mới full màn hình ổn định rồi mới nạp dữ liệu vào
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