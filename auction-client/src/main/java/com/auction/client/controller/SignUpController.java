package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.user.UserRole;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.JsonObject;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller chịu trách nhiệm điều khiển giao diện Đăng ký tài khoản (SignUpView.fxml).
 */
public class SignUpController extends BaseController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<UserRole> cbAccountType;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSignUp;
    @FXML private Button linkLogin;

    @FXML
    public void initialize() {
        LoggerUtil.info("Đang khởi tạo màn hình đăng ký tài khoản (Sign Up)...");
        cbAccountType.setItems(FXCollections.observableArrayList(UserRole.BIDDER, UserRole.SELLER));
        cbAccountType.getSelectionModel().select(UserRole.BIDDER);

        btnSignUp.setOnAction(event -> handleSignUp());
        linkLogin.setOnAction(event -> handleSwitchToLogin());
    }

    private void handleSignUp() {
        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        UserRole selectedRole = cbAccountType.getValue();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (ValidationUtil.isEmpty(fullName) || ValidationUtil.isEmpty(username) ||
                ValidationUtil.isEmpty(email) || ValidationUtil.isEmpty(password) || ValidationUtil.isEmpty(confirmPassword)) {
            DialogUtil.showWarning("Vui lòng điền đầy đủ các trường thông tin bắt buộc (*).");
            return;
        }

        if (!ValidationUtil.isValidUsername(username)) {
            DialogUtil.showWarning("Username không hợp lệ! Chỉ chấp nhận chữ, số, dấu gạch dưới và độ dài từ 3-20 ký tự.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            DialogUtil.showWarning("Định dạng Email không đúng quy chuẩn (Ví dụ: abc@example.com).");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            DialogUtil.showWarning("Mật khẩu bảo mật bắt buộc phải chứa ít nhất 6 ký tự.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            DialogUtil.showWarning("Mật khẩu nhập lại không trùng khớp! Vui lòng kiểm tra kỹ.");
            return;
        }

        sendSignUpRequestToServer(fullName, username, email, selectedRole, password);
    }

    private void sendSignUpRequestToServer(String fullName, String username, String email, UserRole role, String password) {
        try {
            JsonObject signUpPayload = new JsonObject();
            // ĐÃ SỬA: Đổi từ "fullName" thành "name" để Server đọc được
            signUpPayload.addProperty("name", fullName);
            signUpPayload.addProperty("username", username);
            signUpPayload.addProperty("email", email);
            signUpPayload.addProperty("role", role.name());
            signUpPayload.addProperty("password", password);

            NetworkMessage message = new NetworkMessage(MessageType.SIGNUP_REQUEST, signUpPayload.toString());
            com.auction.client.network.NetworkService.getInstance().sendNetworkMessage(message);

            LoggerUtil.info("Đã gửi gói tin SIGNUP_REQUEST cho tài khoản: " + email);

            // LƯU Ý: Không show popup thành công ở đây nữa. ServerListener sẽ xử lý khi nhận được SIGNUP_RESPONSE

        } catch (Exception e) {
            LoggerUtil.error("Sự cố nghẽn luồng truyền tải gói tin đăng ký qua Socket mạng.", e);
            DialogUtil.showError("Đường truyền Socket gặp sự cố bất ngờ. Không thể gửi yêu cầu đăng ký!");
        }
    }

    private void handleSwitchToLogin() {
        LoggerUtil.info("Người dùng chuyển hướng sang giao diện Đăng nhập.");
        switchWindow(linkLogin, "/fxml/LoginView.fxml");
    }
}