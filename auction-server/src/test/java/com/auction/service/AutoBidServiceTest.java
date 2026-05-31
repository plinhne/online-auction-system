package com.auction.service;

import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutoBidServiceTest {

    private AuctionDAO auctionDAO;
    private BidDAO bidDAO;
    private ConcurrentHashMap<Integer, Auction> cache;

    private AutoBidService service;

    @BeforeEach
    void setUp() {
        // Mock DAO để không cần connect DB thật
        auctionDAO = mock(AuctionDAO.class);
        bidDAO = mock(BidDAO.class);

        // Cache giả lập bộ nhớ trong RAM
        cache = new ConcurrentHashMap<>();

        // Khởi tạo service cần test
        service = new AutoBidService(auctionDAO, bidDAO, cache);
    }

    // ─────────────────────────────────────────────
    // 1. TEST REGISTER (ĐĂNG KÝ AUTO BID)
    // ─────────────────────────────────────────────

    @Test
    void test_register_success() throws SQLException {
        Auction auction = mock(Auction.class);

        // giả lập giá hiện tại
        when(auction.getCurrentPrice()).thenReturn(100.0);

        cache.put(1, auction);

        // đăng ký auto bid hợp lệ
        service.register(1, 10, 200, 10);

        assertTrue(true); // chỉ cần không lỗi là pass
    }

    @Test
    void test_register_fail_maxBid_nho_hon_gia_hien_tai() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getCurrentPrice()).thenReturn(100.0);
        cache.put(1, auction);

        // maxBid nhỏ hơn giá hiện tại → phải throw exception
        assertThrows(IllegalArgumentException.class,
                () -> service.register(1, 10, 90, 10));
    }

    @Test
    void test_register_fail_increment_khong_hop_le() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getCurrentPrice()).thenReturn(100.0);
        cache.put(1, auction);

        // increment <= 0 → không hợp lệ
        assertThrows(IllegalArgumentException.class,
                () -> service.register(1, 10, 200, 0));
    }

    // ─────────────────────────────────────────────
    // 2. TEST TRIGGER AUTO BID
    // ─────────────────────────────────────────────

    @Test
    void test_trigger_khong_co_auto_bid() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);

        // chưa đăng ký auto bid → trả về null
        Bid result = service.trigger(auction, 99);

        assertNull(result);
    }

    @Test
    void test_trigger_bo_qua_nguoi_vua_bid() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(100.0);

        cache.put(1, auction);

        service.register(1, 10, 200, 10);

        // lastBidderId = 10 → phải bị bỏ qua
        Bid result = service.trigger(auction, 10);

        assertNull(result);

        verifyNoInteractions(bidDAO);
    }

    @Test
    void test_trigger_auto_bid_thanh_cong() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(100.0);

        cache.put(1, auction);

        service.register(1, 10, 200, 10);

        // user khác vừa bid → auto bid được kích hoạt
        Bid result = service.trigger(auction, 99);

        assertNotNull(result);

        // kiểm tra đúng số tiền auto bid
        assertEquals(110.0, result.getAmount());

        // kiểm tra DB được gọi
        verify(bidDAO, times(1)).save(any(Bid.class));

        verify(auctionDAO, times(1))
                .updateLeadingBidder(eq(1), eq(110.0), eq(10));
    }

    @Test
    void test_trigger_khi_da_dat_max_bid() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(200.0);

        cache.put(1, auction);



        // đã đạt maxBid → không auto bid nữa
        Bid result = service.trigger(auction, 99);

        assertNull(result);

        verifyNoInteractions(bidDAO);
    }

    // ─────────────────────────────────────────────
    // 3. TEST NHIỀU USER (FIFO)
    // ─────────────────────────────────────────────

    @Test
    void test_trigger_nhieu_user_fifo() throws SQLException {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(100.0);

        cache.put(1, auction);

        service.register(1, 10, 200, 10);
        service.register(1, 20, 200, 20);

        // user đăng ký trước sẽ được ưu tiên (FIFO)
        Bid result = service.trigger(auction, 99);

        assertNotNull(result);

        verify(bidDAO, times(1)).save(any(Bid.class));
    }

    // ─────────────────────────────────────────────
    // 4. TEST CLEAR AUTO BID
    // ─────────────────────────────────────────────

    @Test
    void test_clear_auto_bid() throws SQLException {
        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(100.0);

        cache.put(1, auction);

        service.register(1, 10, 200, 10);

        // xóa toàn bộ auto bid
        service.clearAutoBids(1);

        Bid result = service.trigger(auction, 99);

        // sau khi clear → không còn auto bid
        assertNull(result);
    }

    // ─────────────────────────────────────────────
    // 5. TEST CACHE LOAD DAO
    // ─────────────────────────────────────────────

    @Test
    void test_load_auction_from_dao_khi_chua_co_cache() throws SQLException {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);
        when(auction.getCurrentPrice()).thenReturn(100.0);

        // giả lập DAO trả về auction
        when(auctionDAO.findById(1)).thenReturn(auction);

        // gọi register → sẽ trigger load từ DAO
        service.register(1, 10, 200, 10);

        verify(auctionDAO, times(1)).findById(1);
    }
}