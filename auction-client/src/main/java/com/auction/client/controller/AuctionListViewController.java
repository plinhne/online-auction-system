package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller điều khiển màn hình Danh sách các phiên đấu giá (AuctionListView.fxml).
 */
public class AuctionListViewController extends BaseController implements Initializable {

    @FXML private Label totalItemsLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TabPane statusTabPane;
    @FXML private FlowPane auctionGridPane;
    @FXML private VBox noResultsArea;

    private final ObservableList<Auction> auctionMasterData = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LoggerUtil.info("Khởi tạo danh sách sản phẩm đấu giá trực tuyến.");

        categoryCombo.setItems(FXCollections.observableArrayList("Electronics", "Vehicles", "Arts", "Others"));
        sortCombo.setItems(FXCollections.observableArrayList("Giá tăng dần", "Giá giảm dần", "Sắp kết thúc"));

        fetchAuctionItems();
    }

    /**
     * Gửi yêu cầu lấy danh sách lên Server (Chỉ gửi, không đợi nhận ở đây)
     */
    private void fetchAuctionItems() {
        Task<Void> sendRequestTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                LoggerUtil.info("Đang gửi yêu cầu GET_ALL_AUCTIONS_REQUEST lên máy chủ...");
                NetworkMessage request = new NetworkMessage(MessageType.GET_ALL_AUCTIONS_REQUEST, "GET_ALL_AUCTIONS");
                // ĐÃ SỬA: Đẩy gói tin cho NetworkService xử lý ngầm (Tự động chuyển thành JSON và gửi đi)
                com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(request);

                return null;
            }
        };

        sendRequestTask.setOnFailed(e -> {
            LoggerUtil.error("Lỗi mạng: Không thể gửi yêu cầu lấy danh sách sản phẩm.", sendRequestTask.getException());
            DialogUtil.showError("Hệ thống mạng gặp sự cố. Không thể tải danh sách sản phẩm!");
        });

        runAsyncTask(sendRequestTask);
    }

    /**
     * HÀM MỚI: ServerListener sẽ gọi hàm này và truyền danh sách vào khi nhận được phản hồi từ Server
     */
    public void updateAuctionListFromServer(List<Auction> serverAuctions) {
        Platform.runLater(() -> {
            if (serverAuctions != null) {
                auctionMasterData.setAll(serverAuctions);
                totalItemsLabel.setText(auctionMasterData.size() + " phiên đấu giá trực tuyến hiện có");
                renderGrid();
            }
        });
    }

    /**
     * KẾT XUẤT ĐỒ HỌA: Duyệt vòng lặp vẽ và đổ dữ liệu thực thể lên các tấm Card mẫu
     */
    private void renderGrid() {
        auctionGridPane.getChildren().clear();

        if (auctionMasterData.isEmpty()) {
            noResultsArea.setVisible(true);
            return;
        }
        noResultsArea.setVisible(false);

        for (Auction auction : auctionMasterData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item-card.fxml"));
                Parent cardNode = loader.load();

                ItemCardController cardController = loader.getController();
                cardController.setAuctionData(auction);

                // Đồng bộ hành vi click vào thẻ Card
                cardNode.setOnMouseClicked(event -> {
                    LoggerUtil.info("Người dùng click chọn thẻ Card phiên đấu giá ID: " + auction.getId());
                    navigateToAuctionRoom(auction);
                });

                auctionGridPane.getChildren().add(cardNode);
            } catch (IOException e) {
                LoggerUtil.error("Lỗi render đồ họa thẻ sản phẩm: " + e.getMessage(), e);
            }
        }
    }

    /**
     * ĐIỀU HƯỚNG MÀN HÌNH: Đã sửa lỗi Load FXML 2 lần gây mất dữ liệu
     */
    private void navigateToAuctionRoom(Auction auction) {
        try {
            // 1. Nạp file FXML duy nhất 1 lần
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductDetailsView.fxml"));
            Parent root = loader.load();

            // 2. Lấy Controller đích và truyền dữ liệu
            ProductDetailsController detailsController = loader.getController();
            if (detailsController != null) {
                detailsController.setAuctionDetails(auction);
            }

            // 3. Gắn giao diện mới lên Scene hiện tại
            auctionGridPane.getScene().setRoot(root);

        } catch (Exception e) {
            LoggerUtil.error("Không thể mở màn hình chi tiết sản phẩm.", e);
            DialogUtil.showError("Lỗi hệ thống: Không thể truy cập phân vùng chi tiết!");
        }
    }
}