package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.user.UserRole;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<UserRole> cbAccountType;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSignUp;
    @FXML private Hyperlink linkLogin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Đổ dữ liệu phân quyền tự động từ Enum UserRole (gói common) vào ComboBox
        cbAccountType.setItems(FXCollections.observableArrayList(UserRole.values()));
        cbAccountType.setPromptText("Chọn vai trò tài khoản...");

        // 2. Gán sự kiện cho nút đăng ký (Sign Up)
        btnSignUp.setOnAction(event -> handleSignUp());

        // 3. Gán sự kiện chuyển hướng về màn hình đăng nhập (Log in)
        linkLogin.setOnAction(event -> navigateToLogin());
    }

    /**
     * Xử lý logic nghiệp vụ khi người dùng click Đăng ký
     */
    private void handleSignUp() {
        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        UserRole role = cbAccountType.getValue();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // Kiểm tra dữ liệu đầu vào bằng ValidationUtil của hệ thống
        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            DialogUtil.showError("Lỗi nhập liệu. Vui lòng điền đầy đủ tất cả các trường có dấu (*).");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) { // Giả định ValidationUtil có hàm kiểm tra email
            DialogUtil.showError("Lỗi nhập liệu. Định dạng Email không hợp lệ.");
            return;
        }

        if (role == null) {
            DialogUtil.showError("Lỗi nhập liệu. Vui lòng chọn loại tài khoản (Account Type).");
            return;
        }

        if (!password.equals(confirmPassword)) {
            DialogUtil.showError("Lỗi mật khẩu. Mật khẩu xác nhận không trùng khớp.");
            return;
        }

        // TODO: Đóng gói dữ liệu gửi qua mạng lên server
        // Gợi ý: NetworkMessage msg = new NetworkMessage(MessageType.SIGNUP_REQ, dataJson);
        // Gửi qua cổng OutStream của Socket...

        System.out.println("Gửi thông tin đăng ký lên Server: " + username + " với vai trò " + role);
        DialogUtil.showInfo("Thành công. Đăng ký tài khoản thành công! Quay lại màn hình đăng nhập.");
        navigateToLogin();
    }

    /**
     * Điều hướng giao diện quay trở lại màn hình Đăng Nhập
     */
    private void navigateToLogin() {
        try {
            // Tải tệp cấu hình giao diện đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            // Lấy Stage hiện tại của ứng dụng và đổi Scene
            Stage stage = (Stage) linkLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng nhập hệ thống đấu giá");
            stage.show();

        } catch (IOException e) {
            DialogUtil.showError("Lỗi hệ thống. Không thể tải giao diện đăng nhập: " + e.getMessage());
        }
    }
}
