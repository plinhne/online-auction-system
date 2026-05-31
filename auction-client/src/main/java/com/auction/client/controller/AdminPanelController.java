package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.network.ServerListener;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.dto.AuctionDTO;
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

    // ĐÃ NÂNG CẤP LÊN DTO
    @FXML private TableView<AuctionDTO> auctionsTable;
    @FXML private TableColumn<AuctionDTO, String> auctionIdColumn;
    @FXML private TableColumn<AuctionDTO, String> auctionNameColumn;
    @FXML private TableColumn<AuctionDTO, String> sellerColumn;
    @FXML private TableColumn<AuctionDTO, String> statusColumn;

    // --- User Tab ---
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private Button changeBalanceButton;

    // ĐÃ NÂNG CẤP LÊN DTO
    private final ObservableList<AuctionDTO> masterAuctionList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo bảng quản trị dữ liệu của Admin.");

        // 1. Cấu hình bảng Đấu giá (Sử dụng dữ liệu thật từ DTO)
        auctionIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        auctionNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getItemName())); // Lấy tên thật
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty("Seller ID: " + data.getValue().getSellerId()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        auctionsTable.setItems(masterAuctionList);

        // 2. Cấu hình bảng Người dùng
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));

        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(data -> new SimpleStringProperty(String.format("%,.0fđ", data.getValue().getWalletBalance())));
        }

        usersTable.setItems(masterUserList);

        // 3. Gán sự kiện
        approveAuctionButton.setOnAction(event -> handleApproveAuction());
        deleteAuctionButton.setOnAction(event -> handleDeleteAuction());
        deleteUserButton.setOnAction(event -> handleDeleteUser());

        if (changeBalanceButton != null) {
            changeBalanceButton.setOnAction(event -> handleChangeBalance());
        }

        refreshAdminDataFromServer();
    }

    public void refreshAdminDataFromServer() {
        LoggerUtil.info("AdminPanel đang gửi yêu cầu tải dữ liệu Dashboard...");
        ServerListener listener = NetworkService.getInstance().getServerListener();
        if (listener != null) {
            listener.setAdminPanelController(this);
        }

        try {
            NetworkMessage getAuctionsMsg = new NetworkMessage(MessageType.GET_ALL_AUCTIONS_REQUEST, "{}");
            NetworkService.getInstance().sendNetworkMessage(getAuctionsMsg);
        } catch (Exception e) {
            LoggerUtil.error("Lỗi khi yêu cầu dữ liệu Admin từ Server.", e);
        }
    }

    // ĐÃ NÂNG CẤP LÊN DTO
    public void updateAdminDashboard(List<AuctionDTO> auctions, List<User> users) {
        LoggerUtil.info("--- TRẠM 2: Đang đổ dữ liệu vào giao diện Admin ---");
        totalAuctionsLabel.setText(String.valueOf(auctions.size()));
        totalUsersLabel.setText(String.valueOf(users.size()));

        double revenue = auctions.stream().mapToDouble(AuctionDTO::getCurrentPrice).sum() * 0.1;
        totalRevenueLabel.setText(String.format("%,.0fđ", revenue));

        masterAuctionList.setAll(auctions);
        masterUserList.setAll(users);
    }

    private void handleApproveAuction() {
        AuctionDTO selected = auctionsTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getStatus() != AuctionStatus.SCHEDULED) {
            DialogUtil.showWarning("Vui lòng chọn một cuộc đấu giá hợp lệ đang chờ duyệt (SCHEDULED)!");
            return;
        }
        sendAdminActionToServer("APPROVE_AUCTION", selected.getId());
    }

    private void handleDeleteAuction() {
        AuctionDTO selected = auctionsTable.getSelectionModel().getSelectedItem();
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