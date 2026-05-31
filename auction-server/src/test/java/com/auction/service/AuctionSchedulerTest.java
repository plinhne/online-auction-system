package com.auction.service;

import com.auction.Auction;
import com.auction.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

class AuctionSchedulerTest {

    private AuctionService auctionService;
    private AuctionScheduler scheduler;

    @BeforeEach
    void setUp() {
        auctionService = mock(AuctionService.class);
        scheduler = new AuctionScheduler(auctionService);
    }

    // =================================================
    // 1. TEST START SCHEDULE (KHÔNG TEST THREAD THẬT)
    // =================================================

    @Test
    void test_start_scheduler_khong_loi() {

        // chỉ test gọi start không crash
        scheduler.start();

        // không assert behavior vì thread chạy nền
        scheduler.stop();
    }

    // =================================================
    // 2. TEST STOP SCHEDULER
    // =================================================

    @Test
    void test_stop_scheduler() {

        scheduler.start();
        scheduler.stop();

        // nếu không crash là pass
    }

    // =================================================
    // 3. SCHEDULED → START AUCTION
    // =================================================

    @Test
    void test_check_scheduled_auction_start() throws Exception {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(1);

        // startTime đã qua → phải start
        when(auction.getStartTime())
                .thenReturn(LocalDateTime.now().minusMinutes(5));

        when(auctionService.findByStatus(AuctionStatus.SCHEDULED))
                .thenReturn(List.of(auction));

        // gọi private logic thông qua reflection
        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);
        method.invoke(scheduler);

        verify(auctionService).startAuction(1);
    }

    // =================================================
    // 4. SCHEDULED → CHƯA TỚI GIỜ → KHÔNG START
    // =================================================

    @Test
    void test_scheduled_not_yet_time() throws Exception {

        Auction auction = mock(Auction.class);

        when(auction.getStartTime())
                .thenReturn(LocalDateTime.now().plusHours(1));

        when(auctionService.findByStatus(AuctionStatus.SCHEDULED))
                .thenReturn(List.of(auction));

        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);
        method.invoke(scheduler);

        verify(auctionService, never()).startAuction(anyInt());
    }

    // =================================================
    // 5. ACTIVE → END AUCTION
    // =================================================

    @Test
    void test_active_auction_should_end() throws Exception {

        Auction auction = mock(Auction.class);

        when(auction.getId()).thenReturn(2);

        when(auction.getEndTime())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        when(auctionService.findByStatus(AuctionStatus.ACTIVE))
                .thenReturn(List.of(auction));

        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);
        method.invoke(scheduler);

        verify(auctionService).endAuction(2);
    }

    // =================================================
    // 6. ACTIVE NHƯNG CHƯA HẾT GIỜ
    // =================================================

    @Test
    void test_active_chua_het_gio() throws Exception {

        Auction auction = mock(Auction.class);

        when(auction.getEndTime())
                .thenReturn(LocalDateTime.now().plusMinutes(10));

        when(auctionService.findByStatus(AuctionStatus.ACTIVE))
                .thenReturn(List.of(auction));

        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);
        method.invoke(scheduler);

        verify(auctionService, never()).endAuction(anyInt());
    }

    // =================================================
    // 7. TEST EXCEPTION SQL → KHÔNG CRASH SYSTEM
    // =================================================

    @Test
    void test_sql_exception_handling() throws Exception {

        when(auctionService.findByStatus(any()))
                .thenThrow(new java.sql.SQLException("DB error"));

        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);

        // phải không crash
        method.invoke(scheduler);
    }

    // =================================================
    // 8. TEST EMPTY LIST
    // =================================================

    @Test
    void test_empty_auction_list() throws Exception {

        when(auctionService.findByStatus(AuctionStatus.SCHEDULED))
                .thenReturn(List.of());

        when(auctionService.findByStatus(AuctionStatus.ACTIVE))
                .thenReturn(List.of());

        var method = AuctionScheduler.class
                .getDeclaredMethod("checkAuctions");
        method.setAccessible(true);

        method.invoke(scheduler);

        verify(auctionService, never()).startAuction(anyInt());
        verify(auctionService, never()).endAuction(anyInt());
    }
}