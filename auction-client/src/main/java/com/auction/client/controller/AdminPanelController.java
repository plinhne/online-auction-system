package com.auction.client.controller;

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
import java.io.IOException;
import java.util.List;

public class AdminPanelController extends BaseController {

    @FXML private Label totalAuctionsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalRevenueLabel;

    @FXML private Button approveAuctionButton;
    @FXML private Button deleteAuctionButton;
    @FXML private TableView<Auction> auctionsTable;
    @FXML private TableColumn<Auction, String> auctionIdColumn;
    @FXML private TableColumn<Auction, String> auctionNameColumn;
    @FXML private TableColumn<Auction, String> sellerColumn;
    @FXML private TableColumn<Auction, String> statusColumn;

    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;

    private final ObservableList<Auction> masterAuctionList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo bảng quản trị dữ liệu của Admin.");

        // ĐÃ SỬA: Cấu hình bảng Đấu giá tương thích với Model Auction thực tế
        auctionIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        // Sửa từ .getItem().getName() thành .getItemId() vì Model chỉ lưu ID nguyên thủy
        auctionNameColumn.setCellValueFactory(data -> new SimpleStringProperty("Mã sản phẩm: " + data.getValue().getItemId()));

        // Sửa từ .getId() thành .getSellerId() để hiển thị đúng ID người bán
        sellerColumn.setCellValueFactory(data -> new SimpleStringProperty("Seller ID: " + data.getValue().getSellerId()));

        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        auctionsTable.setItems(masterAuctionList);

        // Cấu hình bảng Người dùng (Giữ nguyên giả định Model User của bạn chạy đúng)
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().name()));
        usersTable.setItems(masterUserList);

        // Đăng ký sự kiện nút điều hành
        approveAuctionButton.setOnAction(event -> handleApproveAuction());
        deleteAuctionButton.setOnAction(event -> handleDeleteAuction());
        deleteUserButton.setOnAction(event -> handleDeleteUser());
    }

    public void refreshAdminDataFromServer() {
        // Gửi lệnh lên server yêu cầu lấy danh sách mới nhất nếu cần
    }

    public void updateAdminDashboard(List<Auction> auctions, List<User> users) {
        totalAuctionsLabel.setText(String.valueOf(auctions.size()));
        totalUsersLabel.setText(String.valueOf(users.size()));

        // Tính tổng doanh thu dựa trên hàm getCurrentPrice() của Auction
        double revenue = auctions.stream().mapToDouble(Auction::getCurrentPrice).sum() * 0.1;
        totalRevenueLabel.setText(String.format("%,.0fđ", revenue));

        masterAuctionList.setAll(auctions);
        masterUserList.setAll(users);
    }

    private void handleApproveAuction() {
        Auction selected = auctionsTable.getSelectionModel().getSelectedItem();

        // ĐÃ SỬA: Kiểm tra nếu đấu giá đang ở trạng thái SCHEDULED (chờ duyệt) thì mới cho phép bấm Duyệt
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

    private void sendAdminActionToServer(String subAction, int targetId) {
        if (outStream == null) return;
        try {
            JsonObject adminPayload = new JsonObject();
            adminPayload.addProperty("adminAction", subAction);
            adminPayload.addProperty("targetId", targetId);

            // ⚠️ LƯU Ý LOGIC: Bạn đang gửi lệnh quản trị bằng `MessageType.PLACE_BID_REQUEST`.
            // Đáng lẽ ra nên dùng một MessageType riêng cho admin như MessageType.ADMIN_ACTION_REQUEST
            NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, adminPayload.toString());

            outStream.writeObject(message);
            outStream.flush();
            LoggerUtil.info("Admin thực thi lệnh: " + subAction + " trên đối tượng ID: " + targetId);
        } catch (IOException e) {
            LoggerUtil.error("Lỗi gửi gói tin lệnh Admin.", e);
        }
    }
}