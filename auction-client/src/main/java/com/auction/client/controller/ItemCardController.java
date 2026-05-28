package com.auction.client.controller;

import com.auction.model.auction.Auction;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Controller cho từng thẻ sản phẩm
 * Hiển thị thông tin tóm tắt của một đấu giá
 */
public class ItemCardController extends BaseController {

    @FXML private Label itemNameLabel;
    @FXML private Label priceLabel;
    @FXML private Button viewDetailsButton;

    // ĐÃ SỬA: Quản lý phiên đấu giá hiện tại
    private Auction currentAuction;

    @FXML
    public void initialize() {
        viewDetailsButton.setOnAction(e -> openProductDetails());
    }

    /**
     * ĐÃ SỬA: Nhận vào đối tượng Auction thay vì Item
     */
    public void setAuctionData(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;

        // Vì Auction chỉ lưu itemId, tạm thời hiển thị ID sản phẩm
        // (Nếu có DTO hoặc hàm lấy Item, bạn có thể đổi thành auction.getItem().getName())
        itemNameLabel.setText("Phòng đấu giá sản phẩm #" + auction.getItemId());

        // ĐÃ SỬA: Lấy giá hiện tại (currentPrice) từ phiên đấu giá để hiển thị lên thẻ
        priceLabel.setText(FormatterUtil.formatCurrency(auction.getCurrentPrice()));
    }

    /**
     * Mở màn hình chi tiết sản phẩm và truyền dữ liệu sang Controller đích
     */
    private void openProductDetails() {
        if (currentAuction == null) {
            LoggerUtil.log("Không có dữ liệu phiên đấu giá hiện tại để xem chi tiết.");
            return;
        }

        try {
            LoggerUtil.log("→ Người dùng xem chi tiết đấu giá ID " + currentAuction.getId());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product-details.fxml"));
            Parent detailsView = loader.load();

            // Lấy Controller của màn hình chi tiết và gọi hàm setAuctionDetails chính xác
            ProductDetailsController detailsController = loader.getController();
            detailsController.setAuctionDetails(currentAuction); // Khớp 100% với hàm đã sửa ở ProductDetailsController

            // Thiết lập Scene và Stage mới
            Scene scene = new Scene(detailsView, 1200, 800);

            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Chi Tiết Đấu Giá #" + currentAuction.getId());
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            LoggerUtil.error("Lỗi nghiêm trọng không thể nạp giao diện chi tiết sản phẩm: ", e);
            DialogUtil.showError("Có lỗi xảy ra khi tải phân vùng giao diện Chi tiết sản phẩm.");
        }
    }
}