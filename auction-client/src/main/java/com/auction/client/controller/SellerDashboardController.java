package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.network.MessageType;
import com.auction.network.NetworkMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Platform;
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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SellerDashboardController extends BaseController {

    // --- CÁC THẺ THỐNG KÊ ---
    @FXML private Label totalItemsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label totalBidsLabel;

    // --- NÚT BẤM ---
    @FXML private Button addItemButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // --- BẢNG DANH SÁCH SẢN PHẨM ---
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

        // 3. Đăng ký nhận tin nhắn từ ServerListener
        if (NetworkService.getInstance().getServerListener() != null) {
            NetworkService.getInstance().getServerListener().setSellerDashboardController(this);
        }

        // 4. Tải dữ liệu THỰC từ Server
        fetchMyAuctions();
    }

    /**
     * Cấu hình cột cho TableView
     */
    private void setupTableView() {
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty("Mã SP: " + cell.getValue().getItemId()));
        categoryColumn.setCellValueFactory(cell -> new SimpleStringProperty("N/A")); // Đợi Server trả về category
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
     * Mở cửa sổ Thêm sản phẩm dạng Popup không viền
     */
    private void openAddItemView() {
        try {
            LoggerUtil.info("→ Tiến hành mở giao diện popup Thêm sản phẩm đấu giá mới.");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddItemView.fxml"));
            Parent addItemView = loader.load();

            Scene scene = new Scene(addItemView, 550, 650);
            scene.setFill(Color.TRANSPARENT);

            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.initStyle(StageStyle.TRANSPARENT); // Bỏ thanh tiêu đề
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getStage(addItemButton));
            stage.centerOnScreen();

            stage.showAndWait();

            LoggerUtil.info("✓ Biểu mẫu nhập sản phẩm mới đã đóng lại.");

            // Sau khi thêm sản phẩm xong, tự động load lại dữ liệu bảng
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
                NetworkService.getInstance().sendNetworkMessage(msg);
                return null;
            }
        };
        runAsyncTask(task);
    }

    /**
     * HÀM MỚI: Xử lý gói tin Server trả về (Được gọi từ ServerListener)
     */
    public void handleServerResponse(NetworkMessage message) {
        Platform.runLater(() -> {
            try {
                // 1. Cấu hình Gson an toàn có hỗ trợ LocalDateTime
                Gson gson = new com.google.gson.GsonBuilder()
                        .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
                        .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
                        .create();

                // 2. Phân tích gói tin thành JsonObject trước
                com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(message.getPayload()).getAsJsonObject();

                // 3. Kiểm tra xem Server có trả về danh sách "auctions" không
                if (jsonObject.has("auctions")) {
                    // Trích xuất riêng mảng "auctions" để ép kiểu thành List<Auction>
                    Type listType = new TypeToken<List<Auction>>(){}.getType();
                    List<Auction> realAuctions = gson.fromJson(jsonObject.get("auctions"), listType);

                    // 4. Cập nhật giao diện
                    sellerAuctionsList.clear();
                    sellerAuctionsList.addAll(realAuctions);
                    calculateAndDisplayStats(realAuctions);

                } else if (jsonObject.has("status") && "ERROR".equals(jsonObject.get("status").getAsString())) {
                    // Xử lý trường hợp Server báo lỗi
                    DialogUtil.showError("Lỗi từ Server: " + jsonObject.get("message").getAsString());
                }

            } catch (Exception e) {
                LoggerUtil.error("Lỗi khi parse dữ liệu danh sách đấu giá từ Server", e);
                DialogUtil.showError("Không thể tải dữ liệu sản phẩm từ máy chủ.");
            }
        });
    };

    /**
     * HÀM MỚI: Tính toán các thẻ thống kê dựa trên danh sách đấu giá thực tế
     */
    private void calculateAndDisplayStats(List<Auction> auctions) {
        int totalItems = auctions.size();
        int active = 0;
        double revenue = 0.0;
        int totalBids = 0;

        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                active++;
            }
            if (auction.getStatus() == AuctionStatus.PAID) {
                revenue += auction.getCurrentPrice();
            }
        }

        // Đổ dữ liệu lên giao diện
        if (totalItemsLabel != null) totalItemsLabel.setText(String.valueOf(totalItems));
        if (activeAuctionsLabel != null) activeAuctionsLabel.setText(String.valueOf(active));
        if (totalRevenueLabel != null) totalRevenueLabel.setText(FormatterUtil.formatCurrency(revenue) + " USD");
        if (totalBidsLabel != null) totalBidsLabel.setText(String.valueOf(totalBids));
    }
}