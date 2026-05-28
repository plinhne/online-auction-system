package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller Dashboard Seller
 * Chức năng:
 * - Hiển thị thống kê bán hàng
 * - Quản lý sản phẩm
 * - Thêm/sửa/xóa mục đấu giá
 */
public class SellerDashboardController extends BaseController {

    @FXML private Label totalItemsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Button addItemButton;

    // TODO: Khởi tạo hoặc Inject SellerService khi kết nối dữ liệu mạng thực tế từ Server
    // private final SellerService sellerService = new SellerService();

    /**
     * Khởi tạo controller
     */
    @FXML
    public void initialize() {
        LoggerUtil.log("✓ SellerDashboardController bắt đầu khởi tạo.");

        // Gán sự kiện cho nút Thêm Mục Đấu Giá
        addItemButton.setOnAction(e -> openAddItemView());

        // Gọi hàm nạp số liệu thống kê bất đồng bộ từ máy chủ
        loadSellerStatistics();
    }

    /**
     * Tải dữ liệu thống kê bán hàng từ Server độc lập dưới luồng ngầm chống đơ UI
     */
    private void loadSellerStatistics() {
        Task<SellerStats> loadStatsTask = new Task<>() {
            @Override
            protected SellerStats call() throws Exception {
                // Giả lập độ trễ phản hồi mạng từ phía Server
                Thread.sleep(150);

                // Trả về dữ liệu mẫu (Sau này thay thế bằng: sellerService.getSellerStats())
                return new SellerStats(10, 125500.0, 5);
            }
        };

        // Cập nhật số liệu lên giao diện khi tác vụ ngầm hoàn tất thành công
        loadStatsTask.setOnSucceeded(e -> {
            SellerStats stats = loadStatsTask.getValue();

            totalItemsLabel.setText("📦 " + stats.getTotalItems() + " mục");
            activeAuctionsLabel.setText("🔄 " + stats.getActiveAuctions() + " đấu giá");

            // Tận dụng lớp FormatterUtil của bạn để định dạng tiền tệ VNĐ đồng bộ toàn bộ ứng dụng
            totalRevenueLabel.setText("💰 " + FormatterUtil.formatCurrency(stats.getTotalRevenue()));

            LoggerUtil.log("Đã nạp và hiển thị thành công số liệu thống kê bán hàng.");
        });

        // Bắt lỗi khi mất kết nối mạng
        loadStatsTask.setOnFailed(e -> {
            Throwable exception = loadStatsTask.getException();
            LoggerUtil.error("Lỗi xảy ra khi tải số liệu thống kê của người bán (Seller): ", exception);
            DialogUtil.showError("Không thể tải thông tin thống kê bán hàng từ máy chủ.");
        });

        // Kích hoạt chạy ngầm thông qua hàm dùng chung trong BaseController
        runAsyncTask(loadStatsTask);
    }

    /**
     * Mở cửa sổ AddEditItemView (Màn hình Thêm sản phẩm) dưới dạng Modal Dialog chặn cửa sổ gốc
     */
    private void openAddItemView() {
        try {
            LoggerUtil.log("→ Tiến hành mở giao diện nhập form Thêm sản phẩm đấu giá mới.");

            // Sửa lại đúng cấu trúc tên file chữ thường và vị trí phân cấp của dự án: /fxml/add-edit-item.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-edit-item.fxml"));
            Parent addItemView = loader.load();

            Scene scene = new Scene(addItemView, 800, 900);

            // Ép cấu trúc CSS đồng bộ từ thư mục tài nguyên hệ thống /css/style.css thay vì /fxml/style.css cũ
            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("➕ Thêm Mục Đấu Giá Mới");

            // Thiết lập chế độ Modal chặn thao tác ngoài cửa sổ Dashboard cho đến khi đóng form nhập xong
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(getStage(addItemButton)); // Tận dụng hàm getStage() kế thừa từ BaseController
            stage.centerOnScreen();

            // Hiển thị và treo luồng cho đến khi đóng cửa sổ form lại
            stage.showAndWait();

            LoggerUtil.log("✓ Biểu mẫu nhập sản phẩm mới (AddEditItemView) đã đóng lại thành công.");

            // Sau khi form đóng, tiến hành tự động làm mới số liệu thống kê đề phòng có hàng hóa vừa được đăng
            loadSellerStatistics();

        } catch (IOException e) {
            LoggerUtil.error("Lỗi nghiêm trọng không thể nạp giao diện form thêm sản phẩm: ", e);
            DialogUtil.showError("Có lỗi xảy ra khi tải phân vùng giao diện Nhập sản phẩm.");
        }
    }

    /**
     * Lớp cấu trúc nội bộ (Inner Class) để đóng gói dữ liệu thống kê của Seller
     */
    private static class SellerStats {
        private final int totalItems;
        private final double totalRevenue;
        private final int activeAuctions;

        public SellerStats(int totalItems, double totalRevenue, int activeAuctions) {
            this.totalItems = totalItems;
            this.totalRevenue = totalRevenue;
            this.activeAuctions = activeAuctions;
        }

        public int getTotalItems() { return totalItems; }
        public double getTotalRevenue() { return totalRevenue; }
        public int getActiveAuctions() { return activeAuctions; }
    }
}
