package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.network.MessageType;
import com.auction.network.NetworkMessage;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SellerDashboardController extends BaseController {

    // --- CÁC THẺ THỐNG KÊ ---
    @FXML private Label totalItemsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label totalBidsLabel; // ĐÃ BỔ SUNG TỪ FXML

    // --- NÚT BẤM ---
    @FXML private Button addItemButton;
    @FXML private Button editButton;    // ĐÃ BỔ SUNG TỪ FXML
    @FXML private Button deleteButton;  // ĐÃ BỔ SUNG TỪ FXML

    // --- BẢNG DANH SÁCH SẢN PHẨM --- (ĐÃ BỔ SUNG TỪ FXML)
    @FXML private TableView<Auction> itemsTable;
    @FXML private TableColumn<Auction, String> nameColumn;
    @FXML private TableColumn<Auction, String> categoryColumn;
    @FXML private TableColumn<Auction, String> statusColumn;
    @FXML private TableColumn<Auction, String> priceColumn;
    @FXML private TableColumn<Auction, String> bidsColumn;
    @FXML private TableColumn<Auction, String> endTimeColumn;

    private final ObservableList<Auction> sellerAuctionsList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        LoggerUtil.info("✓ SellerDashboardController bắt đầu khởi tạo.");

        // 1. Cấu hình bảng hiển thị dữ liệu
        setupTableView();

        // 2. Gán sự kiện cho các nút bấm
        addItemButton.setOnAction(e -> openAddItemView());
        editButton.setOnAction(e -> handleEditItem());
        deleteButton.setOnAction(e -> handleDeleteItem());

        // 3. Tải dữ liệu ban đầu
        loadSellerStatistics();
        fetchMyAuctions();
    }

    /**
     * Cấu hình cột cho TableView
     */
    private void setupTableView() {
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty("Mã SP: " + cell.getValue().getItemId()));
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty("N/A")); // Đợi Load Item thật
        statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        priceColumn.setCellValueFactory(cell -> new SimpleStringProperty(FormatterUtil.formatCurrency(cell.getValue().getCurrentPrice())));
        bidsColumn.setCellValueFactory(cell -> new SimpleStringProperty("..."));

        endTimeColumn.setCellValueFactory(cell -> {
            if (cell.getValue().getEndTime() != null) {
                return new SimpleStringProperty(cell.getValue().getEndTime().format(timeFormatter));
            }
            return new SimpleStringProperty("N/A");
        });

        itemsTable.setItems(sellerAuctionsList);
    }

    /**
     * Mở cửa sổ Thêm sản phẩm (AddEditItemView)
     */
    private void openAddItemView() {
        try {
            LoggerUtil.info("→ Tiến hành mở giao diện nhập form Thêm sản phẩm đấu giá mới.");

            // ĐÃ SỬA LỖI: Sửa lại đường dẫn chuẩn khớp với tên file thực tế
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddItemView.fxml"));
            Parent addItemView = loader.load();

            Scene scene = new Scene(addItemView, 800, 900);

            // Xử lý CSS an toàn
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("➕ Thêm Mục Đấu Giá Mới");

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getStage(addItemButton));
            stage.centerOnScreen();

            stage.showAndWait();

            LoggerUtil.info("✓ Biểu mẫu nhập sản phẩm mới đã đóng lại.");

            // Làm mới lại bảng và số liệu sau khi thêm
            loadSellerStatistics();
            fetchMyAuctions();

        } catch (IOException e) {
            LoggerUtil.error("Lỗi nghiêm trọng không thể nạp giao diện form thêm sản phẩm: ", e);
            DialogUtil.showError("Có lỗi xảy ra khi tải phân vùng giao diện Nhập sản phẩm.");
        }
    }

    private void handleEditItem() {
        Auction selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn một phiên đấu giá trong bảng để chỉnh sửa!");
            return;
        }
        DialogUtil.showInfo("Chức năng chỉnh sửa đang được hoàn thiện.");
    }

    private void handleDeleteItem() {
        Auction selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn một phiên đấu giá trong bảng để xóa!");
            return;
        }
        if (DialogUtil.showConfirm("Bạn có chắc chắn muốn hủy phiên đấu giá này không?")) {
            // Gửi gói tin CANCEL_AUCTION_REQUEST lên Server tại đây
            DialogUtil.showInfo("Đã gửi yêu cầu xóa lên hệ thống.");
        }
    }

    /**
     * Gửi yêu cầu lấy danh sách các phiên đấu giá của người bán này từ Server
     */
    private void fetchMyAuctions() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                NetworkMessage msg = new NetworkMessage(MessageType.GET_MY_AUCTIONS_REQUEST, "{}");
                com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(msg);
                return null;
            }
        };
        runAsyncTask(task);
    }

    /**
     * Tải dữ liệu thống kê bán hàng (Tạm thời dùng Mock, sau này Server trả về sẽ đẩy vào đây)
     */
    private void loadSellerStatistics() {
        Task<SellerStats> loadStatsTask = new Task<>() {
            @Override
            protected SellerStats call() throws Exception {
                Thread.sleep(150);
                return new SellerStats(10, 125500.0, 5, 42); // Thêm 42 lượt đặt
            }
        };

        loadStatsTask.setOnSucceeded(e -> {
            SellerStats stats = loadStatsTask.getValue();
            totalItemsLabel.setText(String.valueOf(stats.getTotalItems()));
            activeAuctionsLabel.setText(String.valueOf(stats.getActiveAuctions()));
            totalRevenueLabel.setText(FormatterUtil.formatCurrency(stats.getTotalRevenue()));
            if (totalBidsLabel != null) {
                totalBidsLabel.setText(String.valueOf(stats.getTotalBids()));
            }
        });

        runAsyncTask(loadStatsTask);
    }

    private static class SellerStats {
        private final int totalItems;
        private final double totalRevenue;
        private final int activeAuctions;
        private final int totalBids;

        public SellerStats(int totalItems, double totalRevenue, int activeAuctions, int totalBids) {
            this.totalItems = totalItems;
            this.totalRevenue = totalRevenue;
            this.activeAuctions = activeAuctions;
            this.totalBids = totalBids;
        }

        public int getTotalItems() { return totalItems; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getActiveAuctions() { return activeAuctions; }
        public int getTotalBids() { return totalBids; }
    }
}