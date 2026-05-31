package com.auction;

import com.auction.model.bid.Bid;
import com.auction.model.pattern.observer.BidObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        // Khởi tạo Auction mẫu trước mỗi test
        auction = new Auction(
                1,
                100,
                10,
                200.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );
    }

    // =================================================
    // 1. KIỂM TRA KHỞI TẠO BAN ĐẦU
    // =================================================

    @Test
    void test_trang_thai_khoi_tao() {

        // Kiểm tra dữ liệu ban đầu có đúng không
        assertEquals(1, auction.getId());
        assertEquals(100, auction.getItemId());
        assertEquals(10, auction.getSellerId());
        assertEquals(200.0, auction.getStartingPrice());

        // Auction mới phải ở trạng thái SCHEDULED
        assertEquals(AuctionStatus.SCHEDULED, auction.getStatus());

        // Giá hiện tại và bước giá ban đầu = 0
        assertEquals(0.0, auction.getCurrentPrice());
        assertEquals(0.0, auction.getMinIncrement());
    }

    // =================================================
    // 2. TEST START AUCTION
    // =================================================

    @Test
    void test_bat_dau_phien_dau_gia() {

        // Chuyển trạng thái từ SCHEDULED → ACTIVE
        auction.startAuction();

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertTrue(auction.isActive());
    }

    @Test
    void test_bat_dau_sai_trang_thai_thi_loi() {

        // start 2 lần sẽ bị lỗi
        auction.startAuction();

        assertThrows(IllegalStateException.class, () -> {
            auction.startAuction();
        });
    }

    // =================================================
    // 3. TEST END AUCTION
    // =================================================

    @Test
    void test_ket_thuc_phien_dau_gia() {

        auction.startAuction();
        auction.endAuction();

        // Sau khi kết thúc phải là ENDED
        assertEquals(AuctionStatus.ENDED, auction.getStatus());
        assertFalse(auction.isActive());
    }

    @Test
    void test_ket_thuc_khi_chua_active_thi_loi() {

        // Nếu chưa ACTIVE mà end → lỗi
        assertThrows(IllegalStateException.class, () -> {
            auction.endAuction();
        });
    }

    // =================================================
    // 4. TEST OBSERVER PATTERN
    // =================================================

    @Test
    void test_them_observer_va_notify() {

        // Mock người đang theo dõi đấu giá
        BidObserver observer = mock(BidObserver.class);

        auction.addObserver(observer);

        // Gửi thông báo bid mới
        auction.notifyObservers(500.0, 99);

        // Kiểm tra observer có nhận update không
        verify(observer).updateNewBid(auction.getId(), 500.0, 99);
    }

    @Test
    void test_xoa_observer() {

        BidObserver observer = mock(BidObserver.class);

        auction.addObserver(observer);
        auction.removeObserver(observer);

        auction.notifyObservers(600.0, 88);

        // Sau khi remove thì không được gọi nữa
        verify(observer, never())
                .updateNewBid(anyInt(), anyDouble(), anyInt());
    }

    @Test
    void test_khong_them_trung_observer() {

        BidObserver observer = mock(BidObserver.class);

        auction.addObserver(observer);
        auction.addObserver(observer); // thêm trùng

        auction.notifyObservers(1000.0, 1);

        // Chỉ được gọi 1 lần
        verify(observer, times(1))
                .updateNewBid(auction.getId(), 1000.0, 1);
    }

    @Test
    void test_nhieu_observer() {

        BidObserver o1 = mock(BidObserver.class);
        BidObserver o2 = mock(BidObserver.class);

        auction.addObserver(o1);
        auction.addObserver(o2);

        auction.notifyObservers(777.0, 5);

        verify(o1).updateNewBid(auction.getId(), 777.0, 5);
        verify(o2).updateNewBid(auction.getId(), 777.0, 5);
    }

    // =================================================
    // 5. TEST SETTERS
    // =================================================

    @Test
    void test_setter_thay_doi_du_lieu() {

        auction.setCurrentPrice(999.0);
        auction.setLeadingBidderId(42);
        auction.setMinIncrement(25.0);

        assertEquals(999.0, auction.getCurrentPrice());
        assertEquals(42, auction.getLeadingBidderId());
        assertEquals(25.0, auction.getMinIncrement());
    }

    @Test
    void test_set_status_thu_cong() {

        auction.setStatus(AuctionStatus.CANCELLED);

        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
    }

    @Test
    void test_set_end_time() {

        LocalDateTime newTime = LocalDateTime.now().plusDays(1);

        auction.setEndTime(newTime);

        assertEquals(newTime, auction.getEndTime());
    }

    // =================================================
    // 6. TEST EDGE CASE
    // =================================================

    @Test
    void test_notify_khi_khong_co_observer() {

        // Không có ai theo dõi vẫn không được crash
        assertDoesNotThrow(() -> {
            auction.notifyObservers(123.0, 1);
        });
    }
}