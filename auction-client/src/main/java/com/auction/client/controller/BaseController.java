package com.auction.client.controller;

import com.auction.client.util.DialogUtil;
import com.auction.client.util.LoggerUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class BaseController {

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
     * @param fxmlPath Đường dẫn tuyệt đối đến file FXML mới (Ví dụ: "/fxml/login.fxml")
     */
    protected void switchWindow(Control triggerControl, String fxmlPath) {
        try {
            LoggerUtil.log("Đang chuyển đổi màn hình chính sang: " + fxmlPath);
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
            LoggerUtil.error("Lỗi khi nạp file FXML tại: " + fxmlPath, e);
            DialogUtil.showError("Có lỗi hệ thống xảy ra khi chuyển đổi màn hình.");
        }
    }

    /**
     * Nạp giao diện con vào vùng trung tâm (Center) của một BorderPane (Dùng cho MainController, Dashboard).
     * Hàm này sử dụng Generics để tự động trả về Controller của View con vừa nạp, giúp dễ dàng truyền dữ liệu.
     *
     * @param container BorderPane chứa vùng trung tâm cần thay thế
     * @param fxmlPath Đường dẫn FXML phân vùng con (Ví dụ: "/fxml/profile.fxml")
     * @return Controller của giao diện con vừa được nạp, hoặc null nếu thất bại.
     */
    protected <T> T loadCenterView(BorderPane container, String fxmlPath) {
        try {
            LoggerUtil.log("Đang tải phân vùng con: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();
            container.setCenter(node);
            return loader.getController();
        } catch (IOException e) {
            LoggerUtil.error("Lỗi không thể nạp phân vùng tại: " + fxmlPath, e);
            DialogUtil.showError("Có lỗi xảy ra khi tải nội dung giao diện.");
            return null;
        }
    }

    /**
     * Thực thi một tác vụ ngầm bằn JavaFX Task để ngăn chặn tình trạng đơ/lag giao diện (UI Freeze)
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
