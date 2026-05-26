package com.auction.client.util;

/**
 * Utility đảm nhận nhiệm vụ kiểm tra tính hợp lệ của toàn bộ dữ liệu đầu vào (Form Validation) tại Client.
 * Giúp ngăn chặn dữ liệu lỗi/rác gửi qua mạng, tối ưu hóa trải nghiệm người dùng và hệ thống.
 */
public class ValidationUtil {

    // Regex chuẩn hóa quốc tế kiểm tra cấu trúc định dạng Email
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    // Regex kiểm tra tên đăng nhập (Chỉ cho phép chữ cái, số, dấu gạch dưới, độ dài từ 3-20 ký tự)
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$";

    /**
     * 1. Kiểm tra chuỗi văn bản có rỗng, null hoặc chỉ chứa ký tự khoảng trắng hay không.
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * 2. Kiểm tra định dạng cấu trúc Email hợp lệ (Dùng cho Đăng ký / Đăng nhập)[cite: 33].
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches(EMAIL_REGEX);
    }

    /**
     * 3. Kiểm tra Tên đăng nhập hợp lệ (Không chứa ký tự đặc biệt, đúng độ dài quy định).
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.matches(USERNAME_REGEX);
    }

    /**
     * 4. Kiểm tra độ bảo mật tối thiểu của Mật khẩu khi đăng ký tài khoản (Tối thiểu 6 ký tự)[cite: 33].
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.trim().length() >= 6;
    }

    /**
     * 5. Kiểm tra một chuỗi văn bản nhập vào từ TextField có phải là một số hợp lệ (Kiểu số thực) hay không.
     */
    public static boolean isNumber(String text) {
        if (text == null) return false;
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 6. Kiểm tra một giá trị số thực có phải là số dương (> 0) hay không (Dùng kiểm tra số dư ví, bước giá).
     */
    public static boolean isPositiveNumber(double value) {
        return value > 0;
    }

    /**
     * 7. NGHIỆP VỤ ĐẤU GIÁ: Kiểm tra số tiền đặt giá mới (Bid Amount) của Bidder có hợp lệ hay không[cite: 49].
     * Quy tắc logic: Giá đặt mới bắt buộc phải lớn hơn hoặc bằng (Giá cao nhất hiện tại + Bước giá tối thiểu)[cite: 48, 75].
     *
     * @param bidAmount      Số tiền người dùng thực hiện đặt giá[cite: 48].
     * @param currentPrice   Giá hiện tại cao nhất của phiên đấu giá đó[cite: 44, 48].
     * @param priceIncrement Bước giá tối thiểu được thiết lập cho sản phẩm[cite: 75].
     * @return true nếu số tiền đặt thỏa mãn điều kiện logic hệ thống[cite: 49].
     */
    public static boolean isValidBidAmount(double bidAmount, double currentPrice, double priceIncrement) {
        return bidAmount >= (currentPrice + priceIncrement);
    }

    /**
     * 8. NGHIỆP VỤ QUẢN LÝ SẢN PHẨM: Kiểm tra tính hợp lệ của mốc thời gian phiên đấu giá do Seller thiết lập[cite: 36, 45].
     * Quy tắc logic: Thời gian bắt đầu phải nhỏ hơn thời gian kết thúc và thời gian kết thúc phải ở tương lai[cite: 45].
     *
     * @param startTime Epoch millisecond của thời gian bắt đầu[cite: 45].
     * @param endTime   Epoch millisecond của thời gian kết thúc[cite: 45].
     * @return true nếu khoảng thời gian thiết lập phiên đấu giá hợp lệ.
     */
    public static boolean isValidAuctionDuration(long startTime, long endTime) {
        long currentTime = System.currentTimeMillis();
        // Thời gian kết thúc phải sau thời gian bắt đầu và phải sau thời gian hiện tại lúc tạo
        return startTime < endTime && endTime > currentTime;
    }

    /**
     * 9. NGHIỆP VỤ AUTO-BIDDING (ĐẤU GIÁ TỰ ĐỘNG): Kiểm tra cấu hình đấu giá tự động của người dùng[cite: 72].
     * Quy tắc logic: Giá tối đa đặt trước phải lớn hơn mức giá hiện tại cộng thêm ít nhất một bước giá[cite: 74, 75].
     *
     * @param maxBid       Mức giá trần tối đa người dùng sẵn sàng chi trả[cite: 74].
     * @param currentPrice Giá cao nhất hiện tại của sản phẩm phiên đấu giá[cite: 44].
     * @param increment    Bước giá quy định[cite: 75].
     * @return true nếu cấu hình Auto-Bid hợp lệ[cite: 72].
     */
    public static boolean isValidAutoBidConfig(double maxBid, double currentPrice, double increment) {
        return maxBid >= (currentPrice + increment) && increment > 0;
    }
}