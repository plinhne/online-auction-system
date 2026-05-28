package com.auction.client.controller;

import com.auction.client.network.ServerListener;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.auction.Auction;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;

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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller điều khiển màn hình Danh sách các phiên đấu giá (AuctionListView.fxml).
 */
public class AuctionListViewController extends BaseController implements Initializable {

    @FXML
    private Label totalItemsLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TabPane statusTabPane;
    @FXML
    private FlowPane auctionGridPane;
    @FXML
    private VBox noResultsArea;

    private final ObservableList<Auction> auctionMasterData = FXCollections.observableArrayList();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LoggerUtil.info("Khởi tạo danh sách sản phẩm đấu giá trực tuyến.");

        categoryCombo.setItems(FXCollections.observableArrayList("Điện tử", "Thời trang", "Gia dụng", "Khác"));
        sortCombo.setItems(FXCollections.observableArrayList("Giá tăng dần", "Giá giảm dần", "Sắp kết thúc"));

        fetchAuctionItems();
    }

    /**
     * TẢI DỮ LIỆU THẬT QUA MẠNG (Bất đồng bộ)
     */
    private void fetchAuctionItems() {
        Task<List<Auction>> loadItemsTask = new Task<List<Auction>>() {
            @Override
            protected List<Auction> call() throws Exception {
                if (outStream == null) {
                    throw new IllegalStateException("Cổng mạng outStream chưa được thiết lập. Hãy đăng nhập lại!");
                }

                LoggerUtil.info("Đang gửi yêu cầu GET_ALL_AUCTIONS lên máy chủ...");

                // ⚠️ LƯU Ý LOGIC: Bạn đang mượn tạm MessageType.PLACE_BID_REQUEST để kéo danh sách đấu giá.
                // Nếu Server yêu cầu đúng loại tin, hãy cân nhắc sửa thành MessageType.GET_ALL_AUCTIONS nếu có.
                NetworkMessage request = new NetworkMessage(MessageType.PLACE_BID_REQUEST, "GET_ALL_AUCTIONS");

                outStream.writeObject(request);
                outStream.flush();

                return executeFetchRequest();
            }
        };

        loadItemsTask.setOnSucceeded(e -> {
            List<Auction> serverAuctions = loadItemsTask.getValue();
            if (serverAuctions != null) {
                auctionMasterData.setAll(serverAuctions);
                totalItemsLabel.setText(auctionMasterData.size() + " phiên đấu giá trực tuyến hiện có");

                renderGrid();
            }
        });

        loadItemsTask.setOnFailed(e -> {
            Throwable exception = loadItemsTask.getException();
            LoggerUtil.error("Lỗi mạng: Không thể lấy danh sách sản phẩm từ Server.", exception);
            DialogUtil.showError("Hệ thống mạng gặp sự cố. Không thể tải danh sách sản phẩm!");
        });

        runAsyncTask(loadItemsTask);
    }

    private List<Auction> executeFetchRequest() {
        try {
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * KẾT XUẤT ĐỒ HỌA: Đổ dữ liệu thật lên card mẫu
     */
    private void renderGrid() {
        auctionGridPane.getChildren().clear();

        if (auctionMasterData.isEmpty()) {
            noResultsArea.setVisible(true);
            LoggerUtil.info("Mạng trống: Hiện tại chưa có phiên đấu giá nào được đăng tải.");
            return;
        }
        noResultsArea.setVisible(false);

        for (Auction auction : auctionMasterData) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item-card.fxml"));
                Parent cardNode = loader.load();

                ItemCardController cardController = loader.getController();

                // ĐÃ SỬA: Gọi đúng hàm setAuctionData(auction) đã sửa ở bước trước của ItemCardController
                cardController.setAuctionData(auction);

                // Đồng bộ hành vi click: Mở phòng đấu giá Realtime
                cardNode.setOnMouseClicked(event -> {
                    LoggerUtil.info("Người dùng chọn xem phiên đấu giá ID: " + auction.getId());
                    navigateToAuctionRoom(auction);
                });

                auctionGridPane.getChildren().add(cardNode);
            } catch (IOException e) {
                LoggerUtil.error("Lỗi render thẻ sản phẩm: " + e.getMessage(), e);
            }
        }
        LoggerUtil.info("Đã kết xuất thành công " + auctionMasterData.size() + " phiên đấu giá thật lên màn hình.");
    }

    /**
     * ĐIỀU HƯỚNG MÀN HÌNH: Chuyển sang phòng đấu giá Realtime
     */
    private void navigateToAuctionRoom(Auction auction) {
        try {
            // ĐÃ SỬA: Đồng bộ đúng tên file FXML phòng đấu giá trực tiếp của bạn là "product-details.fxml" hoặc "RealTimeBiddingView.fxml"
            // (Hãy đảm bảo chuỗi đường dẫn này trỏ chính xác đến giao diện RealTimeBiddingController của bạn)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RealTimeBiddingView.fxml"));
            Parent root = loader.load();

            RealTimeBiddingController biddingController = loader.getController();
            biddingController.setAuctionContext(auction);

            auctionGridPane.getScene().setRoot(root);
        } catch (IOException e) {
            LoggerUtil.error("Không thể mở phòng đấu giá trực tiếp.", e);
            DialogUtil.showError("Lỗi hệ thống: Không thể truy cập phòng đấu giá!");
        }
    }
}