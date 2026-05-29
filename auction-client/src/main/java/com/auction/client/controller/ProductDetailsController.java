package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.JsonObject;

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

        // Gán sự kiện cho nút Quay lại danh sách
        if (backButton != null) {
            backButton.setOnAction(e -> switchWindow(backButton, "/fxml/AuctionListView.fxml"));
        }

        bidderColumn.setCellValueFactory(cellData -> new SimpleStringProperty("User ID: " + cellData.getValue().getBidderId()));
        amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,.0fđ", cellData.getValue().getAmount())));

        timeColumn.setCellValueFactory(cellData -> {
            return new SimpleStringProperty("Mã phòng: " + cellData.getValue().getAuctionId());
        });

        bidsTable.setItems(bidHistoryList);
        placeBidButton.setOnAction(event -> handlePlaceBid());
    }

    public void setAuctionDetails(Auction auction) {
        this.currentAuction = auction;
        if (auction != null) {
            productNameLabel.setText("Sản phẩm đấu giá #" + auction.getItemId());
            descriptionArea.setText("Chi tiết phòng đấu giá mã số: " + auction.getId());
            categoryLabel.setText("Mã sản phẩm: " + auction.getItemId());
            statusLabel.setText("● " + auction.getStatus().name());

            startTimeLabel.setText(auction.getStartTime().format(dateTimeFormatter));
            endTimeLabel.setText(auction.getEndTime().format(dateTimeFormatter));

            updateRealtimeDetails(auction);
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
        // Kiểm tra quyền truy cập (Session)
        if (currentUser == null) {
            DialogUtil.showWarning("Vui lòng đăng nhập để thực hiện đặt giá!");
            return;
        }

        if (ValidationUtil.isEmpty(bidAmountField.getText())) {
            DialogUtil.showWarning("Vui lòng điền giá tiền thầu!");
            return;
        }

        double amount;
        // Bắt lỗi NumberFormatException tránh sập App
        try {
            amount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException ex) {
            DialogUtil.showWarning("Vui lòng chỉ nhập số hợp lệ vào ô giá thầu!");
            return;
        }

        double minIncrement = currentAuction.getMinIncrement();
        if (!ValidationUtil.isValidBidAmount(amount, currentAuction.getCurrentPrice(), minIncrement)) {
            DialogUtil.showError("Mức giá đặt thầu không hợp lệ so với bước nhảy tối thiểu!");
            return;
        }

        // Đóng gói JSON & Đẩy lệnh lên Server ngầm
        Task<Void> bidTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonObject bidRequestJson = new JsonObject();
                bidRequestJson.addProperty("auctionId", currentAuction.getId());
                bidRequestJson.addProperty("bidderId", currentUser.getId());
                bidRequestJson.addProperty("bidAmount", amount);

                NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, bidRequestJson.toString());

                //ĐÃ SỬA: Gửi đi qua NetworkService (Nó sẽ tự chuyển sang JSON và gửi bằng PrintWriter)
                com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(message);
                return null;
            }
        };

        bidTask.setOnSucceeded(e -> {
            LoggerUtil.info("Đã gửi lệnh đặt thầu thành công.");
            bidAmountField.clear();
        });

        bidTask.setOnFailed(e -> {
            LoggerUtil.error("Sự cố gửi lệnh đặt thầu.", bidTask.getException());
            DialogUtil.showError("Lỗi kết nối mạng, không thể gửi yêu cầu đặt giá!");
        });

        runAsyncTask(bidTask);
    }

    public void setBidHistory(List<Bid> bids) {
        if (bids != null) {
            bidCountLabel.setText(bids.size() + " lượt đặt");
            bidHistoryList.setAll(bids);
            FXCollections.reverse(bidHistoryList); // Đảo ngược để bid mới nhất lên đầu
        }
    }
}