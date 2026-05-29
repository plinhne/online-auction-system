package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.User;
import com.auction.model.user.UserRole;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;
import java.util.Optional;

public class AdminPanelController extends BaseController {

    // --- Stats Area ---
    @FXML private Label totalAuctionsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;

    // --- Auction Tab ---
    @FXML private Button approveAuctionButton;
    @FXML private Button deleteAuctionButton;
    @FXML private TableView<Auction> auctionsTable;
    @FXML private TableColumn<Auction, String> auctionIdColumn;
    @FXML private TableColumn<Auction, String> auctionNameColumn;
    @FXML private TableColumn<Auction, String> sellerColumn;
    @FXML private TableColumn<Auction, String> statusColumn;

    // --- User Tab ---
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;

    // Nút và Cột hiển thị số dư bạn vừa thêm
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private Button changeBalanceButton;

    private final ObservableList<Auction> masterAuctionList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo bảng quản trị dữ liệu của Admin.");

        // 1. Cấu hình bảng Đấu giá
        auctionIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        auctionNameColumn.setCellValueFactory(data -> new SimpleStringProperty("Mã sản phẩm: " + data.getValue().getItemId()));
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty("Seller ID: " + data.getValue().getSellerId()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        auctionsTable.setItems(masterAuctionList);

        // 2. Cấu hình bảng Người dùng
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));

        // Cột số dư của bạn
        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0fđ", data.getValue().getWalletBalance())));
        }

        usersTable.setItems(masterUserList);

        // 3. Gán sự kiện
        approveAuctionButton.setOnAction(event -> handleApproveAuction());
        deleteAuctionButton.setOnAction(event -> handleDeleteAuction());
        deleteUserButton.setOnAction(event -> handleDeleteUser());

        // Gán sự kiện nút đổi số dư của bạn
        if (changeBalanceButton != null) {
            changeBalanceButton.setOnAction(event -> handleChangeBalance());
        }
    }

    public void refreshAdminDataFromServer() {
    }

    public void updateAdminDashboard(List<Auction> auctions, List<User> users) {
        totalAuctionsLabel.setText(String.valueOf(auctions.size()));
        totalUsersLabel.setText(String.valueOf(users.size()));

        double revenue = auctions.stream().mapToDouble(Auction::getCurrentPrice).sum() * 0.1;
        totalRevenueLabel.setText(String.format("%,.0fđ", revenue));

        masterAuctionList.setAll(auctions);
        masterUserList.setAll(users);
    }

    private void handleApproveAuction() {
        Auction selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getStatus() != AuctionStatus.SCHEDULED) {
            DialogUtil.showWarning("Vui lòng chọn một cuộc đấu giá hợp lệ đang chờ duyệt (SCHEDULED)!");
            return;
        }
        sendAdminActionToServer("APPROVE_AUCTION", selected.getId());
    }

    private void handleDeleteAuction() {
        Auction selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected != null && DialogUtil.showConfirm("Xác nhận", "Xóa phiên đấu giá này?")) {
            sendAdminActionToServer("DELETE_AUCTION", selected.getId());
        }
    }

    private void handleDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getRole() != UserRole.ADMIN) {
            if (DialogUtil.showConfirm("Cảnh báo", "Khóa/Xóa vĩnh viễn tài khoản " + selected.getName() + "?")) {
                sendAdminActionToServer("DELETE_USER", selected.getId());
            }
        }
    }

    // Logic đổi số dư CHUẨN XÁC của bạn (đã thêm check số âm)
    private void handleChangeBalance() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn người dùng!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selected.getWalletBalance()));
        dialog.setTitle("Quản lý Ví");
        dialog.setContentText("Nhập số dư mới (VNĐ):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(input -> {
            try {
                double newBalance = Double.parseDouble(input.trim());

                // Bổ sung kiểm tra số âm
                if (newBalance < 0) {
                    DialogUtil.showWarning("Số dư không thể là số âm!");
                    return;
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("adminAction", "CHANGE_BALANCE");
                payload.addProperty("targetId", selected.getId());
                payload.addProperty("newBalance", newBalance);

                NetworkMessage message = new NetworkMessage(MessageType.ADMIN_ACTION_REQUEST, payload.toString());
                NetworkService.getInstance().sendNetworkMessage(message);

                selected.setWalletBalance(newBalance);
                usersTable.refresh();
            } catch (NumberFormatException e) {
                DialogUtil.showWarning("Vui lòng chỉ nhập số hợp lệ!");
            }
        });
    }

    private void sendAdminActionToServer(String subAction, int targetId) {
        try {
            JsonObject adminPayload = new JsonObject();
            adminPayload.addProperty("adminAction", subAction);
            adminPayload.addProperty("targetId", targetId);

            NetworkMessage message = new NetworkMessage(MessageType.ADMIN_ACTION_REQUEST, adminPayload.toString());
            NetworkService.getInstance().sendNetworkMessage(message);

            LoggerUtil.info("Admin thực thi lệnh: " + subAction + " trên đối tượng ID: " + targetId);
        } catch (Exception e) {
            LoggerUtil.error("Lỗi gửi gói tin lệnh Admin.", e);
        }
    }
}