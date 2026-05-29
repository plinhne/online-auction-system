package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import com.auction.client.network.ServerListener;
import com.auction.client.network.NetworkService;
import com.auction.model.user.User;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;

public abstract class BaseController {

    protected static User currentUser;
    // ĐÃ XÓA: protected static ObjectOutputStream outStream; (Không dùng luồng nhị phân nữa)
    protected static ServerListener serverListener;

    // ĐÃ SỬA: Bỏ tham số outStream
    public static void setSessionContext(User user, ServerListener listener) {
        currentUser = user;
        serverListener = listener;
        LoggerUtil.info("Đã thiết lập Session cho tài khoản: " + user.getName());
    }

    public static void clearSessionContext() {
        currentUser = null;
        if (serverListener != null) {
            serverListener.stopListening();
            serverListener = null;
        }
        // Gọi NetworkService đóng toàn bộ Socket và Thread
        NetworkService.getInstance().close();
        LoggerUtil.info("Đã xóa sạch phiên làm việc (Session cleared).");
    }
    /**
     * Lấy Stage (Cửa sổ) hiện tại từ một Node bất kỳ trên giao diện (Hỗ trợ cả Control và các thẻ Layout).
     */
    protected Stage getStage(Node node) {
        if (node != null && node.getScene() != null) {
            return (Stage) node.getScene().getWindow();
        }
        return null;
    }

    /**
     * NÂNG CẤP: Thay đổi toàn bộ giao diện hỗ trợ cho mọi cấu trúc Node (StackPane, VBox, Button,...)
     * Đảm bảo cửa sổ mới luôn được phóng to tối đa vừa khít màn hình bằng cách ép luồng render chạy Maximized sau cùng.
     *
     * @param triggerNode Node kích hoạt sự kiện để tìm Stage nền (Nút bấm, StackPane avatar,...)
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới
     */
    protected void switchWindow(Node triggerNode, String fxmlPath) {
        try {
            LoggerUtil.info("Đang chuyển đổi cấu trúc màn hình chính sang: " + fxmlPath);
            Stage stage = getStage(triggerNode);
            if (stage == null) {
                throw new IllegalStateException("Không tìm thấy Stage từ node được cung cấp.");
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            String cssPath = "/css/style.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            }

            stage.setScene(scene);

            // CẬP NHẬT SỬA LỖI KHÔNG FULL MÀN HÌNH:
            // Đặt lệnh maximized vào Platform.runLater để JavaFX tái cấu trúc kích thước sau khi layout đã ổn định ổn định ổn định
            Platform.runLater(() -> {
                stage.setMaximized(false); // Đưa về trạng thái thường để xóa bộ nhớ đệm render của HĐH
                stage.setMaximized(true);  // Ép buộc bung lấp đầy toàn bộ màn hình chính một cách đồng bộ
            });

            stage.show();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi nghiêm trọng khi nạp file FXML tại đường dẫn: " + fxmlPath, e);
            DialogUtil.showError("Có lỗi hệ thống xảy ra khi chuyển đổi màn hình.");
        }
    }

    /**
     * GIỮ NGUYÊN ĐỂ BẢO TOÀN CÁC FILE ĐANG CHẠY: Gọi bắc cầu từ Control sang Node
     */
    protected void switchWindow(Control triggerControl, String fxmlPath) {
        switchWindow((Node) triggerControl, fxmlPath);
    }

    /**
     * GIỮ NGUYÊN ĐỂ BẢO TOÀN CÁC FILE CŨ: Nạp giao diện con vào vùng Center của BorderPane
     */
    protected <T> T loadCenterView(BorderPane container, String fxmlPath) {
        try {
            LoggerUtil.info("Đang tải phân vùng giao diện con vào BorderPane: " + fxmlPath);
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
     * BỔ SUNG MỚI: Nạp giao diện con cho mọi loại Container Layout (VBox, HBox, AnchorPane,...)
     * Giúp xử lý gọn ghẽ vùng `contentArea` kiểu VBox của bạn mà không làm hỏng cấu trúc cũ.
     *
     * @param container Bất kỳ lớp Layout nào kế thừa từ Pane (VBox, HBox, AnchorPane,...)
     * @param fxmlPath Đường dẫn FXML phân vùng con
     * @return Controller của giao diện con vừa được nạp
     */
    protected <T> T loadCenterView(Pane container, String fxmlPath) {
        try {
            LoggerUtil.info("Đang tải phân vùng giao diện con vào Pane/VBox: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();

            // Xóa toàn bộ phần tử cũ và nạp giao diện mới vào
            container.getChildren().setAll(node);

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
     */
    protected <V> void runAsyncTask(Task<V> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true); // Đảm bảo thread tự tắt khi đóng ứng dụng chính
        thread.start();
    }
}