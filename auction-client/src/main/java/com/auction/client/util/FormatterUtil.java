package com.auction.client.util;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatterUtil {

    /**
     * Định dạng tiền tệ Việt Nam Đồng (đ)
     */
    public static String formatCurrency(double amount) {
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat vnFormat = NumberFormat.getCurrencyInstance(localeVN);
        return vnFormat.format(amount);
    }

    /**
     * ĐÃ SỬA: Thay đổi Constants.DATE_FORMAT thành chuỗi pattern tường minh để tránh lỗi compile
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        // Sử dụng pattern chuẩn đồng bộ hiển thị cho Client đấu giá
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}