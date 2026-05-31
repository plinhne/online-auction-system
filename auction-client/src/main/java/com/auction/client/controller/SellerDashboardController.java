package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.dto.AuctionDTO;
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
    @FXML private TableView<AuctionDTO> itemsTable;
    @FXML private TableColumn<AuctionDTO, String> nameColumn;
    @FXML private TableColumn<AuctionDTO, String> categoryColumn;
    @FXML private TableColumn<AuctionDTO, String> statusColumn;
    @FXML private TableColumn<AuctionDTO, String> priceColumn;
    @FXML private TableColumn<AuctionDTO, String> bidsColumn;
    @FXML private TableColumn<AuctionDTO, String> endTimeColumn;

    private final ObservableList<AuctionDTO> sellerAuctionsList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        LoggerUtil.info("✓ SellerDashboardController bắt đầu khởi tạo.");
        setupTableView();
        addItemButton.setOnAction(e -> openAddItemView());
        editButton.setOnAction(e -> handleEditItem());
        deleteButton.setOnAction(e -> handleDeleteItem());

        if (NetworkService.getInstance().getServerListener() != null) {
            NetworkService.getInstance().getServerListener().setSellerDashboardController(this);
        }
        fetchMyAuctions();
    }

    private void setupTableView() {
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getItemName() != null
                        ? cell.getValue().getItemName()
                        : "Sản phẩm #" + cell.getValue().getItemId()));

        categoryColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getItemCategory() != null
                        ? cell.getValue().getItemCategory()
                        : "N/A"));

        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus().name()));

        priceColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(FormatterUtil.formatCurrency(cell.getValue().getCurrentPrice())));

        bidsColumn.setCellValueFactory(cell ->
                new SimpleStringProperty("..."));

        endTimeColumn.setCellValueFactory(cell -> {
            if (cell.getValue().getEndTime() != null) {
                return new SimpleStringProperty(cell.getValue().getEndTime().format(timeFormatter));
            }
            return new SimpleStringProperty("N/A");
        });

        itemsTable.setItems(sellerAuctionsList);
    }

    private void openAddItemView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddItemView.fxml"));
            Parent addItemView = loader.load();
            Scene scene = new Scene(addItemView, 550, 650);
            scene.setFill(Color.TRANSPARENT);
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getStage(addItemButton));
            stage.centerOnScreen();
            stage.showAndWait();
            fetchMyAuctions();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi nạp giao diện thêm sản phẩm.", e);
            DialogUtil.showError("Có lỗi xảy ra khi tải giao diện Nhập sản phẩm.");
        }
    }

    private void handleEditItem() {
        AuctionDTO selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn một phiên đấu giá để chỉnh sửa!");
            return;
        }
        DialogUtil.showInfo("Chức năng chỉnh sửa đang được hoàn thiện.");
    }

    private void handleDeleteItem() {
        AuctionDTO selected = itemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.showWarning("Vui lòng chọn một phiên đấu giá để xóa!");
            return;
        }
        if (DialogUtil.showConfirm("Bạn có chắc chắn muốn hủy phiên đấu giá này không?")) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    com.google.gson.JsonObject req = new com.google.gson.JsonObject();
                    req.addProperty("auctionId", selected.getId());
                    NetworkService.getInstance().sendNetworkMessage(
                            new NetworkMessage(MessageType.CANCEL_AUCTION_REQUEST, req.toString())
                    );
                    return null;
                }
            };
            runAsyncTask(task);
        }
    }

    private void fetchMyAuctions() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                NetworkService.getInstance().sendNetworkMessage(
                        new NetworkMessage(MessageType.GET_MY_AUCTIONS_REQUEST, "{}")
                );
                return null;
            }
        };
        runAsyncTask(task);
    }

    public void handleServerResponse(NetworkMessage message) {
        LoggerUtil.info(">>> SELLER PAYLOAD: " + message.getPayload());
        Platform.runLater(() -> {
            try {
                Gson gson = new com.google.gson.GsonBuilder()
                        .registerTypeAdapter(java.time.LocalDateTime.class,
                                (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, t, ctx) ->
                                        new com.google.gson.JsonPrimitive(src.toString()))
                        .registerTypeAdapter(java.time.LocalDateTime.class,
                                (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, t, ctx) ->
                                        java.time.LocalDateTime.parse(json.getAsString()))
                        .create();

                com.google.gson.JsonObject jsonObject =
                        com.google.gson.JsonParser.parseString(message.getPayload()).getAsJsonObject();

                if (jsonObject.has("auctions")) {
                    Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
                    List<AuctionDTO> dtos =
                            gson.fromJson(jsonObject.get("auctions"), listType);
                    sellerAuctionsList.setAll(dtos);
                    calculateAndDisplayStats(dtos);
                } else if ("ERROR".equals(jsonObject.get("status").getAsString())) {
                    DialogUtil.showError("Lỗi từ Server: " + jsonObject.get("message").getAsString());
                }
            } catch (Exception e) {
                LoggerUtil.error("Lỗi parse dữ liệu từ Server", e);
            }
        });
    }

    private void calculateAndDisplayStats(List<AuctionDTO> auctions) {
        int total  = auctions.size();
        int active = 0;
        double revenue = 0.0;

        for (AuctionDTO dto : auctions) {
            if (dto.getStatus() == AuctionStatus.ACTIVE) active++;
            if (dto.getStatus() == AuctionStatus.PAID) revenue += dto.getCurrentPrice();
        }

        if (totalItemsLabel != null)    totalItemsLabel.setText(String.valueOf(total));
        if (activeAuctionsLabel != null) activeAuctionsLabel.setText(String.valueOf(active));
        if (totalRevenueLabel != null)  totalRevenueLabel.setText(FormatterUtil.formatCurrency(revenue));
        if (totalBidsLabel != null)     totalBidsLabel.setText("0");
    }
}