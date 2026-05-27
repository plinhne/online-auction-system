package com.auction.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

/**
 * Utility quản lý việc hiển thị các hộp thoại thông báo (Pop-up Alerts) trên giao diện JavaFX[cite: 62].
 * Hỗ trợ phân cấp các mức độ thông báo: Thông tin, Cảnh báo nghiệp vụ, Lỗi hệ thống và Xác nhận hành động.
 */
public class DialogUtil {

    /**
     * 1. Thông báo Thông tin thông thường (Information - Icon Xanh lam ℹ️)
     * Dùng khi thông báo một hành động đã hoàn thành tốt đẹp.
     */
    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null); // Để null giúp giao diện gọn gàng, tập trung vào message
        alert.showAndWait();
    }

    /**
     * 2. BỔ SUNG: Thông báo Cảnh báo nghiệp vụ (Warning - Icon Tam giác vàng ⚠️)
     * Dùng khi người dùng nhập liệu sai luật đấu giá, sai định dạng form cần sửa đổi.
     */
    public static void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Cảnh báo nghiệp vụ");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * 3. Thông báo Lỗi nghiêm trọng (Error - Icon Vòng tròn đỏ 🛑)
     * Dùng khi hệ thống phát sinh biến cố bất khả kháng hoặc sập kết nối đường truyền.
     */
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Lỗi hệ thống");
        alert.setHeaderText("Đã xảy ra sự cố nghiêm trọng");
        alert.showAndWait();
    }

    /**
     * 4. Hộp thoại Xác nhận hành động (Confirmation - Icon Hỏi chấm ❓)
     * Dùng khi người dùng thực hiện hành động nguy hiểm cần hỏi lại (Xóa sản phẩm, Khóa user, Đăng xuất)[cite: 55].
     *
     * @param title Tiêu đề hộp thoại (Ví dụ: "Xác nhận xóa")
     * @param message Câu hỏi xác nhận (Ví dụ: "Bạn có chắc chắn muốn gỡ sản phẩm này?")
     * @return true nếu người dùng bấm YES/OK, false nếu bấm NO/CANCEL
     */
    public static boolean showConfirm(String title, String message) {
        // Sử dụng nút YES và NO trực quan hơn cho người dùng lựa chọn
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /**
     * Hàm overload giữ lại cấu hình cũ để không làm lỗi các file cũ trong dự án của nhóm nếu có gọi
     */
    public static boolean showConfirm(String message) {
        return showConfirm("Xác nhận", message);
    }
}