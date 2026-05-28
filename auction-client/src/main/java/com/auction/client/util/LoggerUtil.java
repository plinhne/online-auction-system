package com.auction.client.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility giúp ghi lại lịch sử chạy ứng dụng (Log Console).
 * Hỗ trợ đắc lực cho việc kiểm thử, debug đa luồng và theo dõi lỗi phát sinh.
 */
public class LoggerUtil {

    // Định dạng thời gian chuẩn yyyy-MM-dd HH:mm:ss dễ nhìn hơn mặc định của LocalDateTime
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * In thông tin log thông thường hệ thống (Hợp thức hóa với ServerListener)
     */
    public static void info(String message) {
        System.out.println(String.format("[%s] [INFO] [%s] - %s",
                LocalDateTime.now().format(FORMATTER),
                Thread.currentThread().getName(),
                message));
    }

    /**
     * In thông tin cảnh báo hệ thống
     */
    public static void warning(String message) {
        System.err.println(String.format("[%s] [WARN] [%s] - %s",
                LocalDateTime.now().format(FORMATTER),
                Thread.currentThread().getName(),
                message));
    }

    /**
     * Bổ sung hàm in lỗi chỉ kèm chuỗi thông báo (Hợp thức hóa với ServerListener)
     */
    public static void error(String message) {
        error(message, null);
    }

    /**
     * In lỗi hệ thống kèm theo dấu vết ngoại lệ (Exception StackTrace)
     */
    public static void error(String message, Throwable e) {
        System.err.println(String.format("[%s] [ERROR] [%s] - %s",
                LocalDateTime.now().format(FORMATTER),
                Thread.currentThread().getName(),
                message));
        if (e != null) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        info(message);
    }
}