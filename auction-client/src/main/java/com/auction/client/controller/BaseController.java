package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.network.ServerListener;
import com.auction.model.user.User;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * Lớp trừu tượng nền tảng (Abstract Base Class) cho toàn bộ các Controller trong hệ thống.
 * Cung cấp các công cụ tiện ích dùng chung về điều hướng màn hình, quản lý Session và đa luồng.
 */
public abstract class BaseController {

    // --- THÔNG TIN SESSION TOÀN CỤC (DÙNG CHUNG CHO TẤT CẢ MÀN HÌNH CON) ---
    protected static User currentUser;                  // Người dùng đang đăng nhập hệ thống hiện tại
    protected static ObjectOutputStream outStream;      // Luồng đẩy gói tin Object lên Server
    protected static ServerListener serverListener;    // Luồng ngầm lắng nghe gói tin từ Server đổ về

    /**
     * Thiết lập cấu hình Session mạng toàn cục một lần duy nhất sau khi đăng nhập thành công.
     */
    public static void setSessionContext(User user, ObjectOutputStream out, ServerListener listener) {
        currentUser = user;
        outStream = out;
        serverListener = listener;
        LoggerUtil.info("Đã thiết lập Session cho tài khoản: " + user.getUsername());
    }

    /**
     * Xóa sạch thông tin Session khi người dùng thực hiện hành động Đăng xuất (Logout).
     */
    public static void clearSessionContext() {
        currentUser = null;
        outStream = null;
        if (serverListener != null) {
            serverListener.stopListening();
            serverListener = null;
        }
        LoggerUtil.info("Đã xóa sạch phiên làm việc (Session cleared).");
    }

    /**
     * Lấy Stage (Cửa sổ) hiện tại từ một Control bất kỳ trên giao diện.
     */
    protected Stage getStage(Control control) {
        if (control != null && control.getScene() != null) {
            return (Stage) control.getScene().getWindow();
        }
        return null;
    }

    /**
     * Thay đổi toàn bộ giao diện của cửa sổ hiện tại (Ví dụ: Đăng nhập <-> Đăng ký <-> Màn hình chính).
     * Tự động áp dụng file CSS dùng chung của hệ thống.
     *
     * @param triggerControl Control kích hoạt sự kiện để tìm Stage nền (Nút bấm, Hyperlink,...)
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới (Ví dụ: "/fxml/LoginView.fxml")
     */
    protected void switchWindow(Control triggerControl, String fxmlPath) {
        try {
            // Chỉnh sửa lại hàm gọi sang LoggerUtil.info() cho khớp với file util mới của bạn
            LoggerUtil.info("Đang chuyển đổi cấu trúc màn hình chính sang: " + fxmlPath);
            Stage stage = getStage(triggerControl);
            if (stage == null) {
                throw new IllegalStateException("Không tìm thấy Stage từ control được cung cấp.");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Tự động liên kết file CSS mặc định của hệ thống
            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi nghiêm trọng khi nạp file FXML tại đường dẫn: " + fxmlPath, e);
            DialogUtil.showError("Có lỗi hệ thống xảy ra khi chuyển đổi màn hình.");
        }
    }

    /**
     * Nạp giao diện con vào vùng trung tâm (Center) của một BorderPane (Dùng cho MainController, Dashboard).
     * Hàm này sử dụng Generics để tự động trả về Controller của View con vừa nạp, giúp dễ dàng truyền dữ liệu.
     *
     * @param container BorderPane chứa vùng trung tâm cần thay thế
     * @param fxmlPath Đường dẫn FXML phân vùng con (Ví dụ: "/fxml/ProfileView.fxml")
     * @return Controller của giao diện con vừa được nạp, hoặc null nếu thất bại.
     */
    protected <T> T loadCenterView(BorderPane container, String fxmlPath) {
        try {
            LoggerUtil.info("Đang tải phân vùng giao diện con: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();
            container.setCenter(node);
            return loader.getController();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi không thể nạp phân vùng đồ họa tại: " + fxmlPath, e);
            DialogUtil.showError("Có lỗi xảy ra khi tải nội dung giao diện con.");
            return null;
        }
    }

    /**
     * Thực thi một tác vụ ngầm bằng JavaFX Task để ngăn chặn tình trạng đơ/lag giao diện (UI Freeze)
     * khi tương tác với mạng (NetworkService) hoặc cơ sở dữ liệu.
     *
     * @param <V> Kiểu dữ liệu trả về của tác vụ ngầm
     * @param task Tác vụ cần xử lý ngầm dưới Background Thread
     */
    protected <V> void runAsyncTask(Task<V> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Đảm bảo thread tự tắt khi đóng ứng dụng chính
        thread.start();
    }
}