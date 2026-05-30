package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.function.Consumer;

public class PaginationController {

    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label pageInfo;

    private int currentPage = 1;
    private int totalPages = 1;

    // Biến này dùng để thông báo cho màn hình chính biết khi nào người dùng chuyển trang
    private Consumer<Integer> onPageChangeListener;

    /**
     * Khởi tạo cấu hình phân trang từ màn hình chính.
     * @param totalPages Tổng số trang
     * @param listener Hàm xử lý khi chuyển trang
     */
    public void setupPagination(int totalPages, Consumer<Integer> listener) {
        this.totalPages = Math.max(1, totalPages);
        this.currentPage = 1;
        this.onPageChangeListener = listener;
        updateUI();
    }

    @FXML
    private void handlePrev() {
        if (currentPage > 1) {
            currentPage--;
            updateUI();
            notifyListener();
        }
    }

    @FXML
    private void handleNext() {
        if (currentPage < totalPages) {
            currentPage++;
            updateUI();
            notifyListener();
        }
    }

    /**
     * Cập nhật chữ và bật/tắt nút bấm tùy vào trang hiện tại
     */
    private void updateUI() {
        pageInfo.setText("Trang " + currentPage + " / " + totalPages);
        prevBtn.setDisable(currentPage <= 1);
        nextBtn.setDisable(currentPage >= totalPages);
    }

    /**
     * Kích hoạt hàm báo cáo về cho màn hình chính
     */
    private void notifyListener() {
        if (onPageChangeListener != null) {
            onPageChangeListener.accept(currentPage);
        }
    }
}