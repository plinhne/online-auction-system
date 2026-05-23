package com.auction.client.util;

import java.time.LocalDateTime;

public class LoggerUtil {

    public static void log(String message) {
        System.out.println("[INFO] " + LocalDateTime.now() + " - " + message);
    }

    public static void error(String message, Exception e) {
        System.err.println("[ERROR] " + LocalDateTime.now() + " - " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
//Ghi lại lịch sử chạy app. Hỗ trợ việc debug và theo dõi lỗi trong quá trình phát triển.