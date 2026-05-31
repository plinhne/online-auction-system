package com.auction.client.controller;

import com.auction.client.network.NetworkService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.dto.AuctionDTO;
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

    private AuctionDTO currentAuction;
    private final Gson gson = new Gson();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private final ObservableList<String> activities = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo giao diện Phòng đấu giá trực tiếp.");
        priceHistoryChart.getData().add(priceSeries);
        bidActivityList.setItems(activities);
        NetworkService.getInstance().getServerListener().setBiddingController(this);

        placeBidButton.setOnAction(event -> handlePlaceBid());
        quickBid1Button.setOnAction(event -> handleQuickBid(50000));
        quickBid2Button.setOnAction(event -> handleQuickBid(100000));
        quickBid3Button.setOnAction(event -> handleQuickBid(500000));

        autoBidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            autoBidMaxField.setDisable(!newVal);
            autoBidIncrementField.setDisable(!newVal);
        });
    }

    /**
     * Được gọi từ ProductDetailsController khi user nhấn "Tham gia đấu giá".
     */
    public void setAuctionContext(AuctionDTO auction) {
        this.currentAuction = auction;
        if (auction != null) {
            String name = auction.getItemName() != null
                    ? auction.getItemName()
                    : "Phiên #" + auction.getId();
            productNameLabel.setText(name);
            updateAuctionRealtimeView(auction);

            // Gửi JOIN_AUCTION_REQUEST lên server
            try {
                JsonObject req = new JsonObject();
                req.addProperty("auctionId", auction.getId());
                NetworkService.getInstance().sendNetworkMessage(
                        new NetworkMessage(MessageType.JOIN_AUCTION_REQUEST, req.toString()));
            } catch (Exception e) {
                LoggerUtil.error("Lỗi gửi JOIN_AUCTION_REQUEST.", e);
            }
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
            DialogUtil.showError("Giá thầu phải cao hơn giá hiện tại ít nhất: "
                    + String.format("%,.0fđ", minIncrement));
            return;
        }

        sendBidRequestToServer(bidAmount);
        LoggerUtil.info("da gui bid ve server");
    }

    private void handleQuickBid(double extraAmount) {
        if (currentAuction == null) return;
        double recommended = currentAuction.getCurrentPrice() + extraAmount;
        bidAmountField.setText(String.valueOf((long) recommended));
        handlePlaceBid();
        LoggerUtil.info("da gui quick bid ve server");
    }

    private void sendBidRequestToServer(double amount) {
        try {
            JsonObject bidJson = new JsonObject();
            bidJson.addProperty("auctionId", currentAuction.getId());
            bidJson.addProperty("amount", amount);
            NetworkService.getInstance().sendNetworkMessage(
                    new NetworkMessage(MessageType.PLACE_BID_REQUEST, gson.toJson(bidJson)));

            if (autoBidCheckBox.isSelected()) {
                JsonObject autoJson = new JsonObject();
                autoJson.addProperty("auctionId", currentAuction.getId());
                autoJson.addProperty("maxBid", Double.parseDouble(autoBidMaxField.getText()));
                autoJson.addProperty("increment", Double.parseDouble(autoBidIncrementField.getText()));
                LoggerUtil.info(
                        "Sending JOIN_AUCTION_REQUEST auctionId=" + currentAuction.getId()
                );
                NetworkService.getInstance().sendNetworkMessage(
                        new NetworkMessage(MessageType.SET_AUTO_BID_REQUEST, gson.toJson(autoJson)));
                LoggerUtil.info("Đã gửi thiết lập Auto-Bid lên Server.");
            }

            bidAmountField.clear();
            LoggerUtil.info("Đã gửi lệnh đặt giá " + amount + "đ lên Server.");
        } catch (NumberFormatException e) {
            DialogUtil.showWarning("Lỗi định dạng Auto-Bid. Vui lòng nhập số hợp lệ.");
        }
    }

    /**
     * Được gọi từ ServerListener khi nhận AUCTION_UPDATE_NOTIFICATION.
     * Cập nhật giá và leaderboard realtime.
     */
    public void updateAuctionRealtimeView(AuctionDTO auction) {
        // Chỉ update các field động, giữ nguyên minIncrement từ currentAuction gốc
        if (this.currentAuction != null && auction.getMinIncrement() == 0) {
            auction.setMinIncrement(this.currentAuction.getMinIncrement());
        }
        this.currentAuction = auction;

        Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("%,.0fđ", auction.getCurrentPrice()));

            String leader = auction.getLeadingBidderName() != null
                    ? auction.getLeadingBidderName()
                    : (auction.getLeadingBidderId() > 0 ? "Bidder #" + auction.getLeadingBidderId() : "Chưa có");
            currentLeaderLabel.setText("🏆 " + leader);

            String timeNow = LocalTime.now().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(timeNow, auction.getCurrentPrice()));
            activities.add(0, "[" + timeNow + "] Giá mới: " + String.format("%,.0fđ", auction.getCurrentPrice()));
        });
    }
}