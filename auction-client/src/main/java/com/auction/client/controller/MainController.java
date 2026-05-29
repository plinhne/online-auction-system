package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController extends BaseController implements Initializable {

    @FXML private Button auctionsButton;
    @FXML private Button dashboardButton; // Nút Dashboard trên Navbar chính
    @FXML private StackPane avatarButton;
    @FXML private VBox contentArea;

    private ContextMenu userContextMenu;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LoggerUtil.info("Khởi tạo MainController - Phân luồng điều hướng Dashboard thông minh.");

        // Xử lý sự kiện click nút Dashboard trên Navbar chính
        if (dashboardButton != null) {
            dashboardButton.setOnAction(e -> {
                handleDashboardNavigation();
            });
        }

        if (auctionsButton != null) {
            auctionsButton.setOnAction(e -> loadCenterView(contentArea, "/fxml/AuctionListView.fxml"));
        }

        // Khởi tạo và liên kết menu thả xuống của Avatar
        initUserContextMenu();

        avatarButton.setOnMouseClicked(event -> {
            if (userContextMenu != null) {
                // Làm mới lại số dư trước khi hiển thị (phòng trường hợp số dư thay đổi trong phiên làm việc)
                updateDropdownBalance();
                userContextMenu.show(avatarButton, Side.BOTTOM, 0, 0);
            }
        });

        // Mặc định nạp view danh sách sản phẩm khi vừa mở ứng dụng
        loadCenterView(contentArea, "/fxml/AuctionListView.fxml");
    }

    /**
     * Hàm điều hướng Dashboard dựa vào vai trò (Role) của người dùng hiện tại
     */
    private void handleDashboardNavigation() {
        if (currentUser == null || currentUser.getRole() == null) {
            LoggerUtil.error("Lỗi: Không tìm thấy thông tin phiên làm việc của User.");
            return;
        }

        String role = currentUser.getRole().name().toUpperCase();
        LoggerUtil.info("Người dùng kích hoạt Dashboard với quyền hạn: " + role);

        switch (role) {
            case "ADMIN":
                loadCenterView(contentArea, "/fxml/AdminPanelView.fxml");
                break;

            case "SELLER":
                loadCenterView(contentArea, "/fxml/SellerDashboardView.fxml");
                break;

            case "BUYER":
            default:
                LoggerUtil.info("Tài khoản BUYER kích hoạt Dashboard -> Tự động nạp AuctionListView.");
                loadCenterView(contentArea, "/fxml/AuctionListView.fxml");
                break;
        }
    }

    /**
     * Nạp tệp FXML menu nhỏ thả xuống và cấu hình sự kiện cho các nút bên trong
     */
    private void initUserContextMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDropdownMenu.fxml"));
            VBox menuContent = loader.load();

            Button profileMenuBtn = (Button) menuContent.lookup("#profileMenuBtn");
            Button dashboardMenuBtn = (Button) menuContent.lookup("#dashboardMenuBtn");
            Button logoutMenuBtn = (Button) menuContent.lookup("#logoutMenuBtn");

            // Đổ số dư lần đầu
            updateDropdownBalance(menuContent);

            if (profileMenuBtn != null) {
                profileMenuBtn.setOnAction(e -> {
                    userContextMenu.hide();
                    loadCenterView(contentArea, "/fxml/ProfileView.fxml");
                });
            }

            if (dashboardMenuBtn != null) {
                dashboardMenuBtn.setOnAction(e -> {
                    userContextMenu.hide();
                    handleDashboardNavigation();
                });
            }

            if (logoutMenuBtn != null) {
                logoutMenuBtn.setOnAction(e -> {
                    userContextMenu.hide();
                    handleLogout();
                });
            }

            CustomMenuItem customMenuItem = new CustomMenuItem(menuContent, false);
            userContextMenu = new ContextMenu();
            userContextMenu.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            userContextMenu.getItems().add(customMenuItem);

        } catch (IOException e) {
            LoggerUtil.error("Không thể khởi tạo menu Avatar nhỏ: " + e.getMessage());
        }
    }

    /**
     * Tiện ích giúp cập nhật số dư liên tục mà không cần nạp lại file FXML
     */
    private void updateDropdownBalance(VBox menuContent) {
        if (menuContent != null && currentUser != null) {
            Label dropdownBalanceLabel = (Label) menuContent.lookup("#dropdownBalanceLabel");
            if (dropdownBalanceLabel != null) {
                dropdownBalanceLabel.setText(FormatterUtil.formatCurrency(currentUser.getWalletBalance()));
            }
        }
    }

    private void updateDropdownBalance() {
        if (userContextMenu != null && !userContextMenu.getItems().isEmpty()) {
            CustomMenuItem customItem = (CustomMenuItem) userContextMenu.getItems().get(0);
            if (customItem.getContent() instanceof VBox) {
                updateDropdownBalance((VBox) customItem.getContent());
            }
        }
    }

    private void handleLogout() {
        if (DialogUtil.showConfirm("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?")) {
            LoggerUtil.info("Người dùng thực hiện đăng xuất.");
            clearSessionContext();
            switchWindow(avatarButton, "/fxml/LoginView.fxml");
        }
    }
}