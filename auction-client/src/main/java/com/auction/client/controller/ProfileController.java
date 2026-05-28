package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController extends BaseController {

    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;

    // TODO: Khởi tạo hoặc Inject UserService khi kết nối dữ liệu thực tế từ máy chủ
    // private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        LoggerUtil.log("✓ ProfileController bắt đầu khởi tạo.");
        loadUserProfile();
    }

    /**
     * Nạp dữ liệu thông tin cá nhân của người dùng bất đồng bộ bằng JavaFX Task
     */
    private void loadUserProfile() {
        // Bọc tiến trình gọi mạng/database vào Task để chống đơ UI Thread
        Task<UserWrapper> loadProfileTask = new Task<>() {
            @Override
            protected UserWrapper call() throws Exception {
                // Giả lập thời gian trễ phản hồi từ Server
                Thread.sleep(150);

                // Ở đây sau này bạn sẽ gọi Service thực tế, ví dụ: userService.getCurrentUser()
                return new UserWrapper("Demo User", "user@auction.com", "Bidder");
            }
        };

        // Cập nhật thông tin lên các Label khi lấy dữ liệu ngầm thành công
        loadProfileTask.setOnSucceeded(e -> {
            UserWrapper user = loadProfileTask.getValue();

            usernameLabel.setText(user.getUsername());
            emailLabel.setText(user.getEmail());
            roleLabel.setText(user.getRole());

            LoggerUtil.log("Đã tải thông tin hồ sơ cá nhân thành công cho tài khoản: " + user.getUsername());
        });

        // Xử lý khi xảy ra sự cố kết nối mạng trong tiến trình ngầm
        loadProfileTask.setOnFailed(e -> {
            Throwable exception = loadProfileTask.getException();
            LoggerUtil.error("Lỗi khi tải thông tin hồ sơ cá nhân người dùng: ", exception);
            DialogUtil.showError("Không thể tải thông tin hồ sơ từ máy chủ.");
        });

        // Kích hoạt tiến trình ngầm thông qua hàm dùng chung trong BaseController
        runAsyncTask(loadProfileTask);
    }

    /**
     * Lớp nội bộ (Inner Class) tạm thời dùng để đóng gói thông tin người dùng.
     * Bạn có thể thay thế bằng class Model User thực tế của dự án nếu có sẵn.
     */
    private static class UserWrapper {
        private final String username;
        private final String email;
        private final String role;

        public UserWrapper(String username, String email, String role) {
            this.username = username;
            this.email = email;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}
