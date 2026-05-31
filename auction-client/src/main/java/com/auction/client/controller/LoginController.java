package com.auction.client.controller;

import com.auction.client.network.ServerListener;
import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.util.ValidationUtil;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.Admin;
import com.auction.network.NetworkMessage;
import com.auction.network.MessageType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.net.Socket;

/**
 * Controller chịu trách nhiệm điều khiển giao diện Đăng nhập (LoginView.fxml).
 * Xử lý xác thực tài khoản qua Socket Network và thiết lập ngữ cảnh Session cho toàn hệ thống.
 */
public class LoginController extends BaseController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button bidderDemoButton;
    @FXML private Button sellerDemoButton;
    @FXML private Button adminDemoButton;
    @FXML private Button signUpButton;

    private final Gson gson = new Gson();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    @FXML
    public void initialize() {
        LoggerUtil.info("Đang khởi tạo màn hình Đăng nhập (Login Pro)...");
        loginButton.setOnAction(event -> handleLogin());
        bidderDemoButton.setOnAction(event -> handleDemoLogin("bidder@example.com", "bidder123"));
        sellerDemoButton.setOnAction(event -> handleDemoLogin("seller@example.com", "seller123"));
        adminDemoButton.setOnAction(event -> handleDemoLogin("admin@example.com", "admin123"));
        signUpButton.setOnAction(event -> handleNavigateToSignUp());
    }

    private void handleLogin() {
        errorLabel.setText("");
        String emailOrUsername = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText();

        if (ValidationUtil.isEmpty(emailOrUsername) || ValidationUtil.isEmpty(password)) {
            showLoginError("Tên đăng nhập và mật khẩu không được để trống!");
            return;
        }

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return executeNetworkAuth(emailOrUsername, password);
            }
        };

        loginTask.setOnSucceeded(workerStateEvent -> {
            User authenticatedUser = loginTask.getValue();
            if (authenticatedUser != null) {
                navigateToDashboard(authenticatedUser);
            } else {
                showLoginError("Tài khoản hoặc mật khẩu không chính xác!");
            }
        });

        loginTask.setOnFailed(workerStateEvent -> {
            Throwable exception = loginTask.getException();
            LoggerUtil.error("Đăng nhập thất bại do sự cố mạng kết nối.", (Exception) exception);
            DialogUtil.showError("Không thể kết nối đến Máy chủ đấu giá. Vui lòng bật Server và thử lại!");
        });

        runAsyncTask(loginTask);
    }

    private void handleDemoLogin(String email, String password) {
        LoggerUtil.info("Kích hoạt chế độ truy cập nhanh Demo bằng tài khoản: " + email);
        usernameField.setText(email);
        passwordField.setText(password);
        handleLogin();
    }

    private void handleNavigateToSignUp() {
        LoggerUtil.info("Người dùng yêu cầu mở màn hình đăng ký hệ thống mới...");
        switchWindow(signUpButton, "/fxml/SignUpView.fxml");
    }

    private User executeNetworkAuth(String email, String password) throws Exception {
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
        java.io.PrintWriter tempOut = new java.io.PrintWriter(socket.getOutputStream(), true);
        java.io.BufferedReader tempIn = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));

        JsonObject loginPayload = new JsonObject();
        // ĐÃ SỬA: Đổi từ "username" sang "email" để khớp với Server
        loginPayload.addProperty("email", email);
        loginPayload.addProperty("password", password);

        NetworkMessage authRequest = new NetworkMessage(MessageType.LOGIN_REQUEST, loginPayload.toString());
        tempOut.println(gson.toJson(authRequest));

        String responseLine = tempIn.readLine();
        NetworkMessage response = gson.fromJson(responseLine, NetworkMessage.class);

        if (response != null && response.getType() == MessageType.LOGIN_RESPONSE) {
            JsonObject resultJson = com.google.gson.JsonParser.parseString(response.getPayload()).getAsJsonObject();
            String status = resultJson.has("status") ? resultJson.get("status").getAsString() : "ERROR";

            // ĐÃ SỬA: Kiểm tra status "OK" thay vì "SUCCESS"
            if ("OK".equalsIgnoreCase(status)) {
                String roleStr = resultJson.get("role").getAsString();

                // ĐÃ SỬA: Lấy "userId" thay vì "id", và lấy "name" thay vì "email"
                int id = resultJson.get("userId").getAsInt();
                String name = resultJson.has("name") ? resultJson.get("name").getAsString() : "User";

                User user;
                if ("ADMIN".equalsIgnoreCase(roleStr)) {
                    user = new Admin(id, name, email, password);
                } else if ("SELLER".equalsIgnoreCase(roleStr)) {
                    user = new Seller(id, name, email, password);
                } else {
                    user = new Bidder(id, name, email, password);
                }

                com.auction.client.network.NetworkService.getInstance().attachConnection(socket, tempOut, tempIn);
                ServerListener listener = com.auction.client.network.NetworkService.getInstance().getServerListener();
                BaseController.setSessionContext(user, listener);

                return user;
            }
        }

        socket.close();
        return null;
    }

    private void navigateToDashboard(User user) {
        LoggerUtil.info("Xác thực thành công. Điều hướng giao diện theo phân quyền: " + user.getRole());
        switchWindow(loginButton, "/fxml/MainView.fxml");
    }

    private void showLoginError(String errorMsg) {
        errorLabel.setText(errorMsg);
        LoggerUtil.warning("Đăng nhập thất bại: " + errorMsg);
    }
}