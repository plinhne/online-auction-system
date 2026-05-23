package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignUpController extends BaseController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cbAccountType;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Button btnSignUp;
    @FXML private Hyperlink linkLogin;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        cbAccountType.setItems(FXCollections.observableArrayList(
                "Bidder - Người mua/Đấu giá",
                "Seller - Người bán hàng"
        ));
        cbAccountType.getSelectionModel().selectFirst();
    }

    @FXML
    void handleSignUp(ActionEvent event) {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String accountType = cbAccountType.getValue();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        if (ValidationUtil.isEmpty(fullName) ||
                ValidationUtil.isEmpty(username) ||
                ValidationUtil.isEmpty(email) ||
                ValidationUtil.isEmpty(password) ||
                ValidationUtil.isEmpty(confirmPassword)) {

            DialogUtil.showError("Vui lòng nhập đầy đủ tất cả các thông tin có dấu (*).");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            DialogUtil.showError("Địa chỉ email không chính xác. Định dạng chuẩn: example@domain.com");
            return;
        }

        if (!password.equals(confirmPassword)) {
            DialogUtil.showError("Mật khẩu xác nhận không khớp với mật khẩu đã nhập.");
            return;
        }

        LoggerUtil.log("Bắt đầu gửi yêu cầu đăng ký tài khoản: " + username);
        setLoadingState(true);

        Task<Boolean> signUpTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return authService.register(fullName.trim(), username.trim(), email.trim(), accountType, password);
            }
        };

        signUpTask.setOnSucceeded(e -> {
            setLoadingState(false);
            boolean isSuccess = signUpTask.getValue();

            if (isSuccess) {
                LoggerUtil.log("Đăng ký thành công tài khoản: " + username);
                DialogUtil.showInfo("Tài khoản của bạn đã được khởi tạo thành công!");
                navigateToLogin();
            } else {
                LoggerUtil.log("Đăng ký thất bại. Tên đăng nhập hoặc Email đã tồn tại: " + username);
                DialogUtil.showError("Tên đăng nhập hoặc Email này đã tồn tại trên hệ thống.");
            }
        });

        signUpTask.setOnFailed(e -> {
            setLoadingState(false);
            Throwable exception = signUpTask.getException();

            // FIX LỖI 2: Kiểm tra và ép kiểu hoặc bọc Throwable thành Exception trước khi gọi LoggerUtil
            if (exception instanceof Exception) {
                LoggerUtil.error("Lỗi xảy ra trong quá trình xử lý tác vụ Đăng ký mạng ngầm: ", (Exception) exception);
            } else {
                LoggerUtil.error("Lỗi hệ thống nghiêm trọng trong tác vụ Đăng ký ngầm: ", new Exception(exception));
            }

            DialogUtil.showError("Không thể kết nối đến máy chủ: " + exception.getMessage());
        });

        runAsyncTask(signUpTask);
    }

    @FXML
    void handleSwitchToLogin(ActionEvent event) {
        navigateToLogin();
    }

    private void setLoadingState(boolean isLoading) {
        btnSignUp.setDisable(isLoading);
        if (isLoading) {
            btnSignUp.setText("Processing...");
        } else {
            btnSignUp.setText("Sign Up");
        }
    }

    private void navigateToLogin() {
        switchWindow(linkLogin, "/fxml/login.fxml");
    }
}
