package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.dto.AuctionDTO;
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
    @FXML private Button placeBidButton;
    @FXML private Label descriptionArea;
    @FXML private Label bidCountLabel;

    @FXML private TableView<Bid> bidsTable;
    @FXML private TableColumn<Bid, String> bidderColumn;
    @FXML private TableColumn<Bid, String> amountColumn;
    @FXML private TableColumn<Bid, String> timeColumn;

    private AuctionDTO currentAuction;
    private final ObservableList<Bid> bidHistoryList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        LoggerUtil.info("Khởi tạo cấu trúc bảng Lịch sử thầu TableView.");

        // XỬ LÝ NÚT QUAY LẠI KHÔNG CẦN MAIN_CONTROLLER
        if (backButton != null) {
            backButton.setOnAction(e -> {
                try {
                    // Quét toàn bộ màn hình để tìm lại khung Navbar
                    javafx.scene.Scene currentScene = backButton.getScene();
                    javafx.scene.layout.VBox contentArea = (currentScene != null) ?
                            (javafx.scene.layout.VBox) currentScene.lookup("#contentArea") : null;

                    if (contentArea != null) {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/AuctionListView.fxml"));
                        contentArea.getChildren().setAll((javafx.scene.Node) loader.load());
                    } else {
                        switchWindow(backButton, "/fxml/MainView.fxml");
                    }
                } catch (Exception ex) {
                    LoggerUtil.error("Lỗi khi quay lại màn hình danh sách.", ex);
                }
            });
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

        if (placeBidButton != null) {
            placeBidButton.setOnAction(event -> navigateToRealTimeBidding());
        }
    }

    public void setAuctionDetails(AuctionDTO auction) {
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

    public void updateRealtimeDetails(AuctionDTO update) {

        if (currentAuction == null) {
            currentAuction = update;
        } else {
            currentAuction.setCurrentPrice(update.getCurrentPrice());
            currentAuction.setLeadingBidderId(update.getLeadingBidderId());
            currentAuction.setLeadingBidderName(update.getLeadingBidderName());

            if (update.getEndTime() != null) {
                currentAuction.setEndTime(update.getEndTime());
            }
        }

        AuctionDTO auction = currentAuction;

        Platform.runLater(() -> {
            currentPriceLabel.setText(
                    String.format("%,.0fđ", auction.getCurrentPrice())
            );

            currentLeaderLabel.setText(
                    auction.getLeadingBidderName() != null
                            ? "🏆 " + auction.getLeadingBidderName()
                            : "Chưa có"
            );
        });
    }

    public void setBidHistory(List<Bid> bids) {
        if (bids != null) {
            bidCountLabel.setText(bids.size() + " lượt đặt");
            bidHistoryList.setAll(bids);
            FXCollections.reverse(bidHistoryList);
        }
    }

    // XỬ LÝ VÀO PHÒNG KHÔNG CẦN MAIN_CONTROLLER
    private void navigateToRealTimeBidding() {
        if (currentAuction == null) {
            DialogUtil.showWarning("Không tìm thấy dữ liệu phiên đấu giá hiện tại!");
            return;
        }

        try {
            // Quét màn hình để tìm Navbar thay vì gọi từ MainController
            javafx.scene.Scene currentScene = placeBidButton.getScene();
            javafx.scene.layout.VBox contentArea = (currentScene != null) ?
                    (javafx.scene.layout.VBox) currentScene.lookup("#contentArea") : null;

            if (contentArea != null) {
                JsonObject req = new JsonObject();
                req.addProperty("auctionId", currentAuction.getId());

                NetworkMessage msg = new NetworkMessage(
                        MessageType.JOIN_AUCTION_REQUEST,
                        req.toString()
                );

                com.auction.client.network.NetworkService
                        .getInstance()
                        .sendNetworkMessage(msg);

                LoggerUtil.info("JOIN_AUCTION_REQUEST sent: " + currentAuction.getId());

                Object controller = loadCenterView(contentArea, "/fxml/RealTimeBiddingView.fxml");

                if (controller instanceof RealTimeBiddingController) {
                    ((RealTimeBiddingController) controller).updateAuctionRealtimeView(currentAuction);
                }

                LoggerUtil.info("Đã chuyển sang phòng đấu giá Real-time bên dưới Navbar.");
            } else {
                LoggerUtil.warning("Không tìm thấy Navbar (#contentArea). Mở toàn màn hình.");
                switchWindow(placeBidButton, "/fxml/RealTimeBiddingView.fxml");
            }
        } catch (Exception e) {
            LoggerUtil.error("Lỗi khi nạp giao diện phòng đấu giá", e);
        }
    }
}