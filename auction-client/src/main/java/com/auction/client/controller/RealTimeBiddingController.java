package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.auction.Auction; // Đảm bảo import đúng class Auction bạn vừa gửi
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

    private Auction currentAuction; // Khai báo đúng kiểu Auction
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

    /**
     * Nhận chính xác context là đối tượng com.auction.model.auction.Auction
     */
    public void setAuctionContext(Auction auction) {
        this.currentAuction = auction;
        if (auction != null) {
            // Vì Auction chỉ có itemId, tạm thời hiển thị ID sản phẩm hoặc mã phiên
            productNameLabel.setText("Mã sản phẩm: " + auction.getItemId());
            updateAuctionRealtimeView(auction);
        }

        // Đăng ký controller với ServerListener
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

        // ĐÃ SỬA: Lấy bước giá tối thiểu thông qua hàm getMinIncrement() thực tế của Auction
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
        if (outStream == null) {
            DialogUtil.showError("Mất kết nối kết nối máy chủ!");
            return;
        }
        try {
            JsonObject bidJson = new JsonObject();
            bidJson.addProperty("auctionId", currentAuction.getId()); // Kế thừa từ Entity cha
            bidJson.addProperty("bidAmount", amount);
            bidJson.addProperty("isAutoBid", autoBidCheckBox.isSelected());

            if (autoBidCheckBox.isSelected()) {
                bidJson.addProperty("maxBid", Double.parseDouble(autoBidMaxField.getText()));
                bidJson.addProperty("increment", Double.parseDouble(autoBidIncrementField.getText()));
            }

            NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, gson.toJson(bidJson));
            outStream.writeObject(message);
            outStream.flush();

            bidAmountField.clear();
            LoggerUtil.info("Đã gửi lệnh đặt giá " + amount + "đ lên Server.");
        } catch (IOException | NumberFormatException e) {
            LoggerUtil.error("Lỗi gửi gói tin đặt giá.", e);
        }
    }

    /**
     * 🌟 Hàm realtime được gọi từ ServerListener
     */
    public void updateAuctionRealtimeView(Auction auction) {
        this.currentAuction = auction;

        // Bọc vào chạy trên UI Thread của JavaFX
        Platform.runLater(() -> {
            currentPriceLabel.setText(String.format("%,.0fđ", auction.getCurrentPrice()));

            // ĐÃ SỬA: Kiểm tra leadingBidderId thay vì getHighestBidder()
            if (auction.getLeadingBidderId() > 0) {
                currentLeaderLabel.setText("🏆 ID Người dẫn đầu: " + auction.getLeadingBidderId());
            } else {
                currentLeaderLabel.setText("🏆 Dẫn đầu: Chưa có");
            }

            // Vẽ đồ thị và cập nhật danh sách hoạt động
            String timeNow = LocalTime.now().format(timeFormatter);
            priceSeries.getData().add(new XYChart.Data<>(timeNow, auction.getCurrentPrice()));
            activities.add(0, "[" + timeNow + "] Giá mới: " + String.format("%,.0fđ", auction.getCurrentPrice()));
        });
    }
}