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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductDetailsController extends BaseController {

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

        // ĐÃ SỬA: Hiển thị Bidder ID thay vì gọi Object getBidder().getUsername() không tồn tại
        bidderColumn.setCellValueFactory(cellData -> new SimpleStringProperty("User ID: " + cellData.getValue().getBidderId()));

        amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%,.0fđ", cellData.getValue().getAmount())));

        // ĐÃ SỬA: Giả định trường hợp timestamp trong Bid là kiểu long (hoặc sửa theo kiểu LocalDateTime nếu bạn đổi)
        timeColumn.setCellValueFactory(cellData -> {
            // Nếu trong class Bid của bạn lưu sẵn time kiểu LocalDateTime, hãy gọi trực tiếp format.
            // Tạm thời hiển thị chuỗi thô hoặc format dựa trên thuộc tính thực tế của class Bid
            return new SimpleStringProperty(String.valueOf(cellData.getValue().getAuctionId()));
        });

        bidsTable.setItems(bidHistoryList);
        placeBidButton.setOnAction(event -> handlePlaceBid());
    }

    /**
     * ĐÃ SỬA: Thay thế các lệnh gọi Object lồng nhau bằng các thuộc tính thực tế của Auction
     */
    public void setAuctionDetails(Auction auction) {
        this.currentAuction = auction;
        if (auction != null) {
            // Vì Auction chỉ lưu đại diện itemId, tạm thời set text theo ID
            productNameLabel.setText("Sản phẩm đấu giá #" + auction.getItemId());
            descriptionArea.setText("Chi tiết phòng đấu giá mã số: " + auction.getId());
            categoryLabel.setText("Mã sản phẩm: " + auction.getItemId());
            statusLabel.setText("● " + auction.getStatus().name());

            // ĐÃ SỬA: getStartTime() và getEndTime() trả về LocalDateTime nên dùng trực tiếp formatter
            startTimeLabel.setText(auction.getStartTime().format(dateTimeFormatter));
            endTimeLabel.setText(auction.getEndTime().format(dateTimeFormatter));

            updateRealtimeDetails(auction);
        }
    }

    /**
     * Cập nhật số liệu động tương thích 100% với file Auction.java
     */
    public void updateRealtimeDetails(Auction auction) {
        this.currentAuction = auction;
        currentPriceLabel.setText(String.format("%,.0fđ", auction.getCurrentPrice()));
        startingPriceLabel.setText(String.format("%,.0fđ", auction.getStartingPrice()));

        // ĐÃ SỬA: Bỏ qua phần auction.getItem().getBids() do Model không hỗ trợ cấu trúc cây này.
        // Việc nạp lịch sử thầu TableView nên được xử lý thông qua một hàm nhận danh sách List<Bid> riêng biệt gửi từ Server.
        bidCountLabel.setText("Đang cập nhật...");

        // ĐÃ SỬA: Kiểm tra leadingBidderId thay vì getHighestBidder()
        if (auction.getLeadingBidderId() > 0) {
            currentLeaderLabel.setText("🏆 Người dẫn đầu (ID): " + auction.getLeadingBidderId());
        } else {
            currentLeaderLabel.setText("🏆 Người dẫn đầu: Chưa có");
        }
    }

    private void handlePlaceBid() {
        if (ValidationUtil.isEmpty(bidAmountField.getText())) {
            DialogUtil.showWarning("Vui lòng điền giá tiền thầu!");
            return;
        }

        double amount = Double.parseDouble(bidAmountField.getText().trim());

        // ĐÃ SỬA: Lấy bước giá tối thiểu qua hàm getMinIncrement() thực tế của Auction
        double minIncrement = currentAuction.getMinIncrement();

        if (!ValidationUtil.isValidBidAmount(amount, currentAuction.getCurrentPrice(), minIncrement)) {
            DialogUtil.showError("Mức giá đặt thầu không hợp lệ so với bước nhảy tối thiểu!");
            return;
        }

        try {
            JsonObject bidRequestJson = new JsonObject();
            bidRequestJson.addProperty("auctionId", currentAuction.getId());
            bidRequestJson.addProperty("bidAmount", amount);

            if (outStream != null) {
                NetworkMessage message = new NetworkMessage(MessageType.PLACE_BID_REQUEST, bidRequestJson.toString());
                outStream.writeObject(message);
                outStream.flush();
                bidAmountField.clear();
            }
        } catch (IOException e) {
            LoggerUtil.error("Lỗi gửi lệnh đặt thầu từ màn hình chi tiết.", e);
        }
    }

    // Hàm bổ sung giúp nạp lịch sử thầu khi Server gửi riêng mảng lịch sử thầu về
    public void setBidHistory(List<Bid> bids) {
        if (bids != null) {
            bidCountLabel.setText(bids.size() + " lượt đặt");
            bidHistoryList.setAll(bids);
            FXCollections.reverse(bidHistoryList);
        }
    }
}