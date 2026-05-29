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
import java.io.IOException;

/**
 * Controller chịu trách nhiệm điều khiển giao diện Đăng ký tài khoản (SignUpView.fxml)[cite: 64].
 * Thực hiện validate logic form và gửi yêu cầu đăng ký tài khoản mới lên hệ thống Máy chủ.
 */
public class
SignUpController extends BaseController {

    // --- CÁC THÀNH PHẦN ĐỒ HỌA FX INJECT TỪ FXML ---
    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<UserRole> cbAccountType; // Đổi sang Enum UserRole để đồng bộ dữ liệu
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSignUp;
    @FXML private Hyperlink linkLogin;

    /**
     * Hàm tự động chạy sau khi file FXML được nạp thành công.
     * Thiết lập cấu hình ban đầu cho ComboBox và lắng nghe các sự kiện tương tác.
     */
    @FXML
    public void initialize() {
        LoggerUtil.info("Đang khởi tạo màn hình đăng ký tài khoản (Sign Up)...");

        // 1. Chỉ đổ 2 quyền có thể tự đăng ký tự do: BIDDER và SELLER lên ComboBox
        cbAccountType.setItems(FXCollections.observableArrayList(UserRole.BIDDER, UserRole.SELLER));
        cbAccountType.getSelectionModel().select(UserRole.BIDDER); // Mặc định chọn vai trò Người đấu giá

        // 2. Gán hành động sự kiện cho Nút đăng ký và Hyperlink chuyển màn hình
        btnSignUp.setOnAction(event -> handleSignUp());
        linkLogin.setOnAction(event -> handleSwitchToLogin());
    }

    /**
     * THUẬT TOÁN ĐĂNG KÝ: Tiền kiểm tra (Validate) dữ liệu biểu mẫu tại Client trước khi gửi mạng.
     */
    private void handleSignUp() {
        String fullName = txtFullName.getText() != null ? txtFullName.getText().trim() : "";
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        UserRole selectedRole = cbAccountType.getValue();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // 1. Kiểm tra không được bỏ trống các trường bắt buộc có dấu (*)
        if (ValidationUtil.isEmpty(fullName) || ValidationUtil.isEmpty(username) ||
                ValidationUtil.isEmpty(email) || ValidationUtil.isEmpty(password) || ValidationUtil.isEmpty(confirmPassword)) {
            DialogUtil.showWarning("Vui lòng điền đầy đủ các trường thông tin bắt buộc (*).");
            return;
        }

        // 2. Kiểm tra cấu trúc định dạng Username (Không chứa ký tự đặc biệt)
        if (!ValidationUtil.isValidUsername(username)) {
            DialogUtil.showWarning("Username không hợp lệ! Chỉ chấp nhận chữ, số, dấu gạch dưới và độ dài từ 3-20 ký tự.");
            return;
        }

        // 3. Kiểm tra định dạng Email hợp lệ
        if (!ValidationUtil.isValidEmail(email)) {
            DialogUtil.showWarning("Định dạng Email không đúng quy chuẩn (Ví dụ: abc@example.com).");
            return;
        }

        // 4. Kiểm tra độ an toàn bảo mật tối thiểu của Mật khẩu
        if (!ValidationUtil.isValidPassword(password)) {
            DialogUtil.showWarning("Mật khẩu bảo mật bắt buộc phải chứa ít nhất 6 ký tự.");
            return;
        }

        // 5. Kiểm tra logic khớp chuỗi giữa Mật khẩu và Nhập lại mật khẩu
        if (!password.equals(confirmPassword)) {
            DialogUtil.showWarning("Mật khẩu nhập lại không trùng khớp! Vui lòng kiểm tra kỹ.");
            return;
        }

        // 6. Gửi gói tin lên Server thông qua luồng outStream tĩnh kế thừa từ BaseController
        sendSignUpRequestToServer(fullName, username, email, selectedRole, password);
    }

    /**
     * Đóng gói thông tin form thành cấu trúc JSON và đẩy qua đường truyền mạng Object Socket Stream.
     */
    private void sendSignUpRequestToServer(String fullName, String username, String email, UserRole role, String password) {
        if (outStream == null) {
            DialogUtil.showError("Không thể thực hiện đăng ký. Mất kết nối tới máy chủ hệ thống!");
            return;
        }

        try {
            // Đóng gói payload dữ liệu thô thành JsonObject
            JsonObject signUpPayload = new JsonObject();
            signUpPayload.addProperty("fullName", fullName);
            signUpPayload.addProperty("username", username);
            signUpPayload.addProperty("email", email);
            signUpPayload.addProperty("role", role.name()); // Truyền chuỗi vai trò: "BIDDER" hoặc "SELLER"
            signUpPayload.addProperty("password", password); // Server sẽ chịu trách nhiệm băm mã hóa mật khẩu ở tầng AuthService

            // Tạo đối tượng NetworkMessage bọc chung theo cấu trúc sơ đồ lớp dữ liệu
            NetworkMessage message = new NetworkMessage(MessageType.SIGNUP_REQUEST, signUpPayload.toString());

            // Đẩy đối tượng nhị phân qua đường ống mạng lên Server xử lý tập trung
            outStream.writeObject(message);
            outStream.flush();

            LoggerUtil.info("Đã gửi gói tin SIGNUP_REQUEST cho tài khoản: " + username);
            DialogUtil.showInfo("Yêu cầu đăng ký đã được gửi đi thành công! Vui lòng chờ phản hồi xác thực từ hệ thống.");

        } catch (IOException e) {
            LoggerUtil.error("Sự cố nghẽn luồng truyền tải gói tin đăng ký qua Socket mạng.", e);
            DialogUtil.showError("Đường truyền Socket gặp sự cố bất ngờ. Không thể gửi yêu cầu đăng ký!");
        }
    }

    /**
     * ĐIỀU HƯỚNG MÀN HÌNH: Chuyển người dùng quay trở lại giao diện Đăng nhập nếu đã có tài khoản.
     */
    private void handleSwitchToLogin() {
        LoggerUtil.info("Người dùng chuyển hướng sang giao diện Đăng nhập.");
        // Sử dụng hàm switchWindow tiện ích của lớp cha BaseController để đổi Scene
        switchWindow(linkLogin, "/fxml/LoginView.fxml");
    }
}