package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.auction.Auction;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RealTimeBiddingController extends BaseController {

    @FXML private Label productNameLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private ProgressBar timeProgressBar;
    @FXML private Label currentPriceLabel;
    @FXML private Label currentLeaderLabel;
    @FXML private Label bidCountLabel;
    @FXML private LineChart<String, Number> priceHistoryChart;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private Button quickBid1Button;
    @FXML private Button quickBid2Button;
    @FXML private Button quickBid3Button;
    @FXML private CheckBox autoBidCheckBox;
    @FXML private TextField autoBidMaxField;
    @FXML private TextField autoBidIncrementField;
    @FXML private ListView<String> bidActivityList;

    private Auction currentAuction;
    private final Gson gson = new Gson();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private final ObservableList<String> activities = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo giao diện Phòng đấu giá trực tiếp.");
        priceHistoryChart.getData().add(priceSeries);
        bidActivityList.setItems(activities);

        placeBidButton.setOnAction(event -> handlePlaceBid());
        quickBid1Button.setOnAction(event -> handleQuickBid(50000));
        quickBid2Button.setOnAction(event -> handleQuickBid(100000));
        quickBid3Button.setOnAction(event -> handleQuickBid(500000));

        autoBidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            autoBidMaxField.setDisable(!newVal);
            autoBidIncrementField.setDisable(!newVal);
        });
    }

    public void setAuctionContext(Auction auction) {
        this.currentAuction = auction;
        if (auction != null) {
            productNameLabel.setText("Mã sản phẩm: " + auction.getItemId());
            updateAuctionRealtimeView(auction);
        }
        if (serverListener != null) {
            serverListener.setBiddingController(this);
        }
    }

    private void handlePlaceBid() {
        if (currentAuction == null) return;

        String amountText = bidAmountField.getText();
        if (!ValidationUtil.isNumber(amountText)) {
            DialogUtil.showWarning("Vui lòng nhập số tiền thầu hợp lệ!");
            return;
        }

        double bidAmount = Double.parseDouble(amountText);
        double minIncrement = currentAuction.getMinIncrement();

        if (!ValidationUtil.isValidBidAmount(bidAmount, currentAuction.getCurrentPrice(), minIncrement)) {
            DialogUtil.showError("Giá thầu phải cao hơn giá hiện tại ít nhất: " + String.format("%,.0fđ", minIncrement));
            return;
        }

        sendBidRequestToServer(bidAmount);
    }

    private void handleQuickBid(double extraAmount) {
        if (currentAuction == null) return;
        double recommendedBid = currentAuction.getCurrentPrice() + extraAmount;
        bidAmountField.setText(String.valueOf((long) recommendedBid));
        handlePlaceBid();
    }

    private void sendBidRequestToServer(double amount) {
        try {
            // 1. GỬI LỆNH ĐẤU GIÁ THỦ CÔNG (BID)
            JsonObject bidJson = new JsonObject();
            bidJson.addProperty("auctionId", currentAuction.getId());
            // Giữ nguyên là "bidAmount" (Do BidController ở Server đã được sửa thành bidAmount ở lượt trước)
            bidJson.addProperty("bidAmount", amount);

            NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, gson.toJson(bidJson));
            com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(message);

            // 2. NẾU CHỌN AUTO BID -> TÁCH RA THÀNH LỆNH SET_AUTO_BID_REQUEST RIÊNG BIỆT
            if (autoBidCheckBox.isSelected()) {
                JsonObject autoBidJson = new JsonObject();
                autoBidJson.addProperty("auctionId", currentAuction.getId());
                autoBidJson.addProperty("maxBid", Double.parseDouble(autoBidMaxField.getText()));
                autoBidJson.addProperty("increment", Double.parseDouble(autoBidIncrementField.getText()));

                NetworkMessage autoMsg = new NetworkMessage(MessageType.SET_AUTO_BID_REQUEST, gson.toJson(autoBidJson));
                com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(autoMsg);
                LoggerUtil.info("Đã gửi gói lệnh thiết lập Auto-Bid lên Server.");
            }

            bidAmountField.clear();
            LoggerUtil.info("Đã gửi lệnh đặt giá " + amount + "đ lên Server.");
        } catch (NumberFormatException e) {
            DialogUtil.showWarning("Lỗi định dạng cấu hình Auto-Bid. Vui lòng nhập số hợp lệ.");
            LoggerUtil.error("Lỗi gửi gói tin đặt giá.", e);
        }
    }

    public void updateAuctionRealtimeView(Auction auction) {
        this.currentAuction = auction;

        Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("%,.0fđ", auction.getCurrentPrice()));

            if (auction.getLeadingBidderId() > 0) {
                currentLeaderLabel.setText("🏆 ID Người dẫn đầu: " + auction.getLeadingBidderId());
            } else {
                currentLeaderLabel.setText("🏆 Dẫn đầu: Chưa có");
            }

            String timeNow = LocalTime.now().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(timeNow, auction.getCurrentPrice()));
            activities.add(0, "[" + timeNow + "] Giá mới: " + String.format("%,.0fđ", auction.getCurrentPrice()));
        });
    }
}