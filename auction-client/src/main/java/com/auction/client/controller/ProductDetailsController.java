package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.model.item.Item;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductDetailsController extends BaseController {

    @FXML private Button backButton;
    @FXML private ImageView productImage;
    @FXML private Label startTimeLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label productNameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label statusLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentLeaderLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Label descriptionArea;
    @FXML private Label bidCountLabel;

    @FXML private TableView<Bid> bidsTable;
    @FXML private TableColumn<Bid, String> bidderColumn;
    @FXML private TableColumn<Bid, String> amountColumn;
    @FXML private TableColumn<Bid, String> timeColumn;

    private Auction currentAuction;
    private final ObservableList<Bid> bidHistoryList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo cấu trúc bảng Lịch sử thầu TableView.");

        if (backButton != null) {
            backButton.setOnAction(e -> switchWindow(backButton, "/fxml/AuctionListView.fxml"));
        }

        bidderColumn.setCellValueFactory(cellData -> new SimpleStringProperty("User ID: " + cellData.getValue().getBidderId()));
        amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,.0fđ", cellData.getValue().getAmount())));

        timeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getPlacedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getPlacedAt().format(dateTimeFormatter));
            }
            return new SimpleStringProperty("N/A");
        });

        bidsTable.setItems(bidHistoryList);
        placeBidButton.setOnAction(event -> handlePlaceBid());
    }

    public void setAuctionDetails(Auction auction) {
        this.currentAuction = auction;
        if (auction != null) {
            statusLabel.setText("● " + auction.getStatus().name());
            startTimeLabel.setText(auction.getStartTime().format(dateTimeFormatter));
            endTimeLabel.setText(auction.getEndTime().format(dateTimeFormatter));

            updateRealtimeDetails(auction);

            productNameLabel.setText("Đang tải...");
            descriptionArea.setText("Đang tải chi tiết...");
            categoryLabel.setText("...");

            if (com.auction.client.network.NetworkService.getInstance().getServerListener() != null) {
                com.auction.client.network.NetworkService.getInstance().getServerListener().setProductDetailsController(this);
            }
            fetchItemDetails(auction.getItemId());
        }
    }

    private void fetchItemDetails(int itemId) {
        try {
            JsonObject req = new JsonObject();
            req.addProperty("itemId", itemId);
            NetworkMessage msg = new NetworkMessage(MessageType.GET_ITEM_DETAILS_REQUEST, req.toString());
            com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(msg);
        } catch (Exception e) {
            LoggerUtil.error("Lỗi khi gửi yêu cầu lấy thông tin sản phẩm", e);
        }
    }

    // Xử lý phản hồi từ Server
    public void onMessageReceived(NetworkMessage message) {
        if (message.getType() == MessageType.GET_ITEM_DETAILS_RESPONSE) {
            JsonObject payload = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            if ("SUCCESS".equals(payload.get("status").getAsString())) {
                Item item = new Gson().fromJson(payload.get("item"), Item.class);
                setItemDetails(item);
            }
        }
    }

    public void setItemDetails(Item item) {
        if (item != null) {
            Platform.runLater(() -> {
                productNameLabel.setText(item.getName());
                descriptionArea.setText(item.getDescription());
                categoryLabel.setText(item.getCategory() != null ? "Danh mục: " + item.getCategory().name() : "Danh mục: Khác");
            });
        }
    }

    public void updateRealtimeDetails(Auction auction) {
        this.currentAuction = auction;
        currentPriceLabel.setText(String.format("%,.0fđ", auction.getCurrentPrice()));
        startingPriceLabel.setText(String.format("%,.0fđ", auction.getStartingPrice()));

        if (auction.getLeadingBidderId() > 0) {
            currentLeaderLabel.setText("🏆 Người dẫn đầu (ID): " + auction.getLeadingBidderId());
        } else {
            currentLeaderLabel.setText("🏆 Người dẫn đầu: Chưa có");
        }
    }

    private void handlePlaceBid() {
        if (currentUser == null) {
            DialogUtil.showWarning("Vui lòng đăng nhập!");
            return;
        }

        if (ValidationUtil.isEmpty(bidAmountField.getText())) {
            DialogUtil.showWarning("Vui lòng điền giá!");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException ex) {
            DialogUtil.showWarning("Giá không hợp lệ!");
            return;
        }

        double minIncrement = currentAuction.getMinIncrement();
        if (!ValidationUtil.isValidBidAmount(amount, currentAuction.getCurrentPrice(), minIncrement)) {
            DialogUtil.showError("Mức giá không hợp lệ!");
            return;
        }

        placeBidButton.setDisable(true);
        Task<Void> bidTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonObject bidRequestJson = new JsonObject();
                bidRequestJson.addProperty("auctionId", currentAuction.getId());
                bidRequestJson.addProperty("bidderId", currentUser.getId());
                bidRequestJson.addProperty("bidAmount", amount);

                NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, bidRequestJson.toString());
                com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(message);
                return null;
            }
        };

        bidTask.setOnSucceeded(e -> {
            placeBidButton.setDisable(false);
            bidAmountField.clear();
        });

        bidTask.setOnFailed(e -> {
            placeBidButton.setDisable(false);
            DialogUtil.showError("Lỗi kết nối!");
        });

        runAsyncTask(bidTask);
    }

    public void setBidHistory(List<Bid> bids) {
        if (bids != null) {
            bidCountLabel.setText(bids.size() + " lượt đặt");
            bidHistoryList.setAll(bids);
            FXCollections.reverse(bidHistoryList);
        }
    }
}