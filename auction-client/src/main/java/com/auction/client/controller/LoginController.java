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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Controller chịu trách nhiệm điều khiển giao diện Đăng nhập (LoginView.fxml).
 * Xử lý xác thực tài khoản qua Socket Network và thiết lập ngữ cảnh Session cho toàn hệ thống.
 */
public class LoginController extends BaseController {

    // --- CÁC THÀNH PHẦN ĐỒ HỌA FX INJECT TỪ FXML ---
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button bidderDemoButton;
    @FXML private Button sellerDemoButton;
    @FXML private Button adminDemoButton;

    private final Gson gson = new Gson();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    /**
     * Hàm tự động chạy sau khi file FXML được nạp thành công.
     * Gán sự kiện cho nút đăng nhập chính thống và các nút truy cập nhanh Demo.
     */
    @FXML
    public void initialize() {
        LoggerUtil.info("Đang khởi tạo màn hình Đăng nhập (Login Pro)...");

        // Gán sự kiện cho luồng đăng nhập chính thức
        loginButton.setOnAction(event -> handleLogin());

        // Gán sự kiện cho luồng truy cập nhanh bằng tài khoản Demo để nghiệm thu BTL
        bidderDemoButton.setOnAction(event -> handleDemoLogin("demo_bidder", "password123"));
        sellerDemoButton.setOnAction(event -> handleDemoLogin("demo_seller", "password123"));
        adminDemoButton.setOnAction(event -> handleDemoLogin("demo_admin", "password123"));
    }

    /**
     * THUẬT TOÁN ĐĂNG NHẬP CHÍNH THỨC: Validate form tuyến đầu và kích hoạt Task chạy mạng ngầm.
     */
    private void handleLogin() {
        errorLabel.setText(""); // Xóa thông báo lỗi cũ
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText();

        // 1. Tiền kiểm tra dữ liệu thô tại chỗ (Client-side Validation)
        if (ValidationUtil.isEmpty(username) || ValidationUtil.isEmpty(password)) {
            showLoginError("Tên đăng nhập và mật khẩu không được để trống!");
            return;
        }

        // 2. Sử dụng hàm runAsyncTask kế thừa từ BaseController để chạy ngầm tác vụ mạng Socket
        // Giúp giao diện Client không bị đơ cứng (UI Freeze) khi Server xử lý chậm hoặc mất mạng
        Task<User> loginTask = new Task() {
            @Override
            protected User call() throws Exception {
                return executeNetworkAuth(username, password);
            }
        };

        // Lắng nghe khi tác vụ mạng kết thúc thành công
        loginTask.setOnSucceeded(workerStateEvent -> {
            User authenticatedUser = loginTask.getValue();
            if (authenticatedUser != null) {
                navigateToDashboard(authenticatedUser);
            } else {
                showLoginError("Tài khoản hoặc mật khẩu không chính xác!");
            }
        });

        // Lắng nghe khi tác vụ mạng thất bại (Lỗi kết nối Socket, Server sập,...)
        loginTask.setOnFailed(workerStateEvent -> {
            Throwable exception = loginTask.getException();
            LoggerUtil.error("Đăng nhập thất bại do sự cố mạng kết nối.", (Exception) exception);
            DialogUtil.showError("Không thể kết nối đến Máy chủ đấu giá. Vui lòng bật Server và thử lại!");
        });

        // Kích hoạt chạy luồng ngầm cho Task qua BaseController
        runAsyncTask(loginTask);
    }

    /**
     * THUẬT TOÁN DEMO ACCESS: Tự động điền tài khoản mẫu và kích hoạt đăng nhập nhanh.
     */
    private void handleDemoLogin(String username, String password) {
        LoggerUtil.info("Kích hoạt chế độ truy cập nhanh Demo bằng tài khoản: " + username);
        usernameField.setText(username);
        passwordField.setText(password);
        handleLogin();
    }

    /**
     * LOGIC LẬP TRÌNH MẠNG (TỰ HỌC TUẦN 9-10): Thiết lập cổng kết nối Object Stream song phương với Server[cite: 250, 265].
     */
    private User executeNetworkAuth(String username, String password) throws IOException, ClassNotFoundException {
        // 1. Tạo kết nối Socket mới đến Server
        Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

        // 2. Thiết lập cấu hình Object Stream gửi nhận dữ liệu tuần tự
        ObjectOutputStream tempOut = new ObjectOutputStream(socket.getOutputStream());
        tempOut.flush(); // Giải phóng vùng đệm stream đầu ra
        ObjectInputStream tempIn = new ObjectInputStream(socket.getInputStream());

        // 3. Đóng gói thông tin đăng nhập thành chuỗi cấu trúc JSON
        JsonObject loginPayload = new JsonObject();
        loginPayload.addProperty("username", username);
        loginPayload.addProperty("password", password);

        // 4. Bắn gói tin LOGIN_REQUEST lên Server theo đúng sơ đồ cấu trúc
        NetworkMessage authRequest = new NetworkMessage(MessageType.LOGIN_REQUEST, loginPayload.toString());
        tempOut.writeObject(authRequest);
        tempOut.flush();

        // 5. Đợi phản hồi tối cao từ Máy chủ trả về (Lệnh nghẽn - Blocking)
        Object receivedObject = tempIn.readObject();
        if (receivedObject instanceof NetworkMessage) {
            NetworkMessage response = (NetworkMessage) receivedObject;

            if (response.getType() == MessageType.LOGIN_RESPONSE) {
                JsonObject resultJson = com.google.gson.JsonParser.parseString(response.getPayload()).getAsJsonObject();
                String status = resultJson.get("status").getAsString();

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    // Trích xuất thông tin đối tượng User do Server tạo và trả về dựa theo đa hình vai trò
                    String roleStr = resultJson.get("role").getAsString();
                    User user;
                    int id = resultJson.get("id").getAsInt();
                    String email = resultJson.get("email").getAsString();

                    // Sử dụng đa hình khởi tạo đúng đối tượng thực thể User con [cite: 114, 115, 121]
                    if ("ADMIN".equalsIgnoreCase(roleStr)) {
                        user = new Admin(id, username, email, password);
                    } else if ("SELLER".equalsIgnoreCase(roleStr)) {
                        user = new Seller(id, username, email, password);
                    } else {
                        user = new Bidder(id, username, email, password);
                    }

                    // 6. KHỞI TẠO LUỒNG NGHE MẠNG NGẦM (SERVER LISTENER) ĐÃ ĐƯỢC ĐỒNG BỘ
                    ServerListener listener = new ServerListener(socket);
                    listener.start(); // Kích hoạt chạy ngầm song song

                    // 7. LƯU TRỮ SESSION TĨNH TOÀN CỤC LÊN LỚP CHA BASECONTROLLER
                    BaseController.setSessionContext(user, tempOut, listener);

                    return user;
                }
            }
        }

        // Nếu không thành công, dọn dẹp đóng socket tạm thời
        socket.close();
        return null;
    }

    /**
     * THUẬT TOÁN ĐIỀU HƯỚNG VAI TRÒ (Phân quyền đồ họa UI): Tách biệt màn hình dựa theo chức năng của User[cite: 32].
     */
    private void navigateToDashboard(User user) {
        LoggerUtil.info("Xác thực thành công. Điều hướng giao diện theo phân quyền: " + user.getRole());

        switch (user.getRole()) {
            case ADMIN -> switchWindow(loginButton, "/fxml/AdminPanelView.fxml"); // Chuyển sang bảng quản trị Admin
            case SELLER -> switchWindow(loginButton, "/fxml/SellerDashboardView.fxml"); // Chuyển sang Dashboard của người bán
            case BIDDER -> switchWindow(loginButton, "/fxml/MainView.fxml"); // Chuyển sang giao diện lưới sản phẩm của người mua
            default -> DialogUtil.showError("Vai trò tài khoản hệ thống không được nhận diện!");
        }
    }

    private void showLoginError(String errorMsg) {
        errorLabel.setText(errorMsg);
        LoggerUtil.warning("Đăng nhập thất bại: " + errorMsg);
    }
}