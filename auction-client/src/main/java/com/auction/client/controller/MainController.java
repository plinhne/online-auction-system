package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
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
        LoggerUtil.log("Khởi tạo MainController với phân quyền nút Dashboard.");

        // Xử lý sự kiện click nút Dashboard trên Navbar chính
        dashboardButton.setOnAction(e -> {
            handleDashboardNavigation();
        });

        if (auctionsButton != null) {
            auctionsButton.setOnAction(e -> loadCenterView(contentArea, "/fxml/AuctionListView.fxml"));
        }

        // Khởi tạo và liên kết menu thả xuống của Avatar
        initUserContextMenu();
        avatarButton.setOnMouseClicked(event -> {
            if (userContextMenu != null) {
                userContextMenu.show(avatarButton, Side.BOTTOM, 0, 0);
            }
        });

        // Mặc định nạp view danh sách sản phẩm khi vừa mở ứng dụng
        loadCenterView(contentArea, "/fxml/AuctionListView.fxml");
    }

    /**
     * Hàm điều hướng Dashboard thông minh dựa vào vai trò (Role) của người dùng hiện tại
     */
    private void handleDashboardNavigation() {
        // Đảm bảo có session người dùng trước khi check role
        if (currentUser == null || currentUser.getRole() == null) {
            LoggerUtil.log("Lỗi: Không tìm thấy thông tin phiên làm việc của User.");
            return;
        }

        String role = currentUser.getRole().name().toUpperCase();
        LoggerUtil.log("Người dùng kích hoạt Dashboard với quyền hạn: " + role);

        switch (role) {
            case "ADMIN":
                // Nếu Admin bấm vào Dashboard trên thanh chung, nạp phân vùng AdminPanel vào vùng trung tâm
                loadCenterView(contentArea, "/fxml/AdminPanelView.fxml");
                break;

            case "SELLER":
                // Nếu là Người bán, nạp giao diện Dashboard quản lý bán hàng
                loadCenterView(contentArea, "/fxml/SellerDashboardView.fxml");
                break;

            case "BUYER":
            default:
                // Nếu bạn có màn hình thống kê hoặc Dashboard dành riêng cho người mua (Buyer)
                loadCenterView(contentArea, "/fxml/BuyerDashboardView.fxml"); // hoặc một view mặc định bất kỳ
                break;
        }
    }

    /**
     * Nạp file FXML menu nhỏ và cấu hình sự kiện cho các nút bên trong
     */
    private void initUserContextMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/UserDropdownMenu.fxml"));
            VBox menuContent = loader.load();

            Button profileMenuBtn = (Button) menuContent.lookup("#profileMenuBtn");
            Button dashboardMenuBtn = (Button) menuContent.lookup("#dashboardMenuBtn"); // Nút Dashboard trong Menu nhỏ
            Button logoutMenuBtn = (Button) menuContent.lookup("#logoutMenuBtn");

            if (profileMenuBtn != null) {
                profileMenuBtn.setOnAction(e -> {
                    userContextMenu.hide();
                    loadCenterView(contentArea, "/fxml/ProfileView.fxml");
                });
            }

            // Tận dụng chung hàm handleDashboardNavigation() cho nút Dashboard trong Menu nhỏ thả xuống
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
            LoggerUtil.log("Không thể khởi tạo menu Avatar nhỏ: " + e.getMessage());
        }
    }

    private void handleLogout() {
        if (DialogUtil.showConfirm("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?")) {
            LoggerUtil.log("Người dùng thực hiện đăng xuất.");
            clearSessionContext(); // Xóa sạch session cũ trước khi chuyển cửa sổ
            switchWindow(avatarButton, "/fxml/LoginView.fxml");
        }
    }
}
