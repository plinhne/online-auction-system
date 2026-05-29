package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.FormatterUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.model.user.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ProfileController extends BaseController {

    // --- CÁC THÀNH PHẦN ĐỒ HỌA FX INJECT TỪ FXML ---
    @FXML private Button btnBack; // Thêm nếu bạn muốn bắt sự kiện nút Quay lại
    @FXML private Label fullNameLabel;
    @FXML private Label roleBadge;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label accountIdLabel;
    @FXML private Label balanceLabel; // Nhãn hiển thị số dư mới bổ sung

    @FXML
    public void initialize() {
        LoggerUtil.info("✓ ProfileController bắt đầu khởi tạo hồ sơ cá nhân.");

        // Cấu hình sự kiện nút quay lại (nếu cần thiết, chuyển về màn hình danh sách đấu giá)
        if (btnBack != null) {
            btnBack.setOnAction(e -> switchWindow(btnBack, "/fxml/AuctionListView.fxml"));
        }

        // Kích hoạt luồng tải dữ liệu an toàn
        loadUserProfile();
    }

    /**
     * Nạp dữ liệu thông tin cá nhân của người dùng bất đồng bộ từ Session hiện tại
     */
    private void loadUserProfile() {
        // Sử dụng Task chạy ngầm để bảo đảm an toàn dữ liệu luồng đồ họa JavaFX
        Task<User> loadProfileTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // Giả lập thời gian trễ phản hồi cực ngắn từ bộ nhớ Session
                Thread.sleep(100);

                if (currentUser == null) {
                    throw new IllegalStateException("Mất thông tin phiên làm việc hiện tại. Hãy đăng nhập lại!");
                }

                // Trả về trực tiếp thực thể User tĩnh kế thừa từ lớp cha BaseController
                return currentUser;
            }
        };

        // Cập nhật đồng bộ thông tin lên toàn bộ các nhãn Label khi Task thành công
        loadProfileTask.setOnSucceeded(e -> {
            User user = loadProfileTask.getValue();

            // Đổ dữ liệu động vào các trường giao diện
            if (fullNameLabel != null) fullNameLabel.setText(user.getName());
            if (usernameLabel != null) usernameLabel.setText(user.getName());
            if (emailLabel != null) emailLabel.setText(user.getEmail());

            // Định dạng mã tài khoản theo quy chuẩn thiết kế
            if (accountIdLabel != null) accountIdLabel.setText("USR-" + user.getId());

            // Định dạng hiển thị nhãn Quyền hạn vai trò
            if (roleBadge != null && user.getRole() != null) {
                roleBadge.setText(user.getRole().name().toUpperCase());
            }

            // ĐỒNG BỘ SỐ DƯ: Định dạng số dư tiền tệ thực tế thông qua FormatterUtil có sẵn
            if (balanceLabel != null) {
                balanceLabel.setText(FormatterUtil.formatCurrency(user.getWalletBalance()));
            }

            LoggerUtil.info("Đã tải thông tin hồ sơ cá nhân thành công cho tài khoản: " + user.getName());
        });

        // Xử lý khi xảy ra sự cố bất ngờ
        loadProfileTask.setOnFailed(e -> {
            Throwable exception = loadProfileTask.getException();
            LoggerUtil.error("Lỗi khi nạp dữ liệu thông tin hồ sơ: ", exception);
            DialogUtil.showError("Không thể hiển thị thông tin hồ sơ cá nhân.");
        });

        // Kích hoạt tiến trình ngầm thông qua hàm dùng chung trong BaseController
        runAsyncTask(loadProfileTask);
    }
}