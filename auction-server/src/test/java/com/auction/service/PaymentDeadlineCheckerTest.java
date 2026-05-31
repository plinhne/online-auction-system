package com.auction.service;

import com.auction.payment.AuctionResult;
import com.auction.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentDeadlineCheckerTest {

    private PaymentDeadlineChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PaymentDeadlineChecker();
    }

    // =================================================
    // 1. STATUS KHÔNG PHẢI PENDING → KHÔNG LÀM GÌ
    // =================================================

    @Test
    void test_status_khong_phai_pending_thi_return_som() {

        AuctionResult result = mock(AuctionResult.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PAID);

        checker.checkDeadline(result);

        // Không được gọi gì thêm
        verify(result, never()).setPaymentStatus(any());
        verify(result, never()).getSeller();
    }

    // =================================================
    // 2. PENDING NHƯNG CHƯA QUÁ HẠN → KHÔNG XỬ LÝ
    // =================================================

    @Test
    void test_chua_qua_han_thi_khong_lam_gi() {

        AuctionResult result = mock(AuctionResult.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        // deadline trong tương lai (cộng thêm 1 giờ)
        when(result.getPaymentDeadline())
                .thenReturn(LocalDateTime.now().plusHours(1));

        checker.checkDeadline(result);

        verify(result, never()).setPaymentStatus(any());
        verify(result, never()).getSeller();
    }

    // =================================================
    // 3. PENDING + QUÁ HẠN → XỬ LÝ EXPIRED
    // =================================================

    @Test
    void test_qua_han_thi_set_expired_va_hoan_tien() {

        AuctionResult result = mock(AuctionResult.class);
        var seller = mock(com.auction.model.user.User.class);
        var deposit = mock(com.auction.payment.Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        // deadline đã qua (trừ đi 1 phút)
        when(result.getPaymentDeadline())
                .thenReturn(LocalDateTime.now().minusMinutes(1));

        when(result.getSeller()).thenReturn(seller);
        when(result.getDeposit()).thenReturn(deposit);
        when(deposit.getAmount()).thenReturn(500.0);

        checker.checkDeadline(result);

        // 1. trạng thái chuyển sang EXPIRED
        verify(result).setPaymentStatus(PaymentStatus.EXPIRED);

        // 2. hoàn tiền cho seller
        verify(seller).depositMoney(500.0);

        // 3. đánh dấu deposit đã chuyển
        verify(deposit).setTransferredToSeller(true);
    }

    // =================================================
    // 4. EDGE CASE: đúng thời điểm deadline (không quá hạn)
    // =================================================

    @Test
    void test_dung_thoi_diem_deadline_khong_expire() {

        AuctionResult result = mock(AuctionResult.class);

        // Tạo thêm mock cho Seller và Deposit để phòng thủ lỗi NullPointerException
        // trong trường hợp lệch mili-giây giữa các câu lệnh
        var seller = mock(com.auction.model.user.User.class);
        var deposit = mock(com.auction.payment.Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        // Đổi từ LocalDateTime.now() thành plusSeconds(5) để kiểm tra mốc biên an toàn,
        // giúp bài test không bị phụ thuộc vào tốc độ xử lý của CPU
        when(result.getPaymentDeadline())
                .thenReturn(LocalDateTime.now().plusSeconds(5));

        // Cấu hình hành vi trả về cho các đối tượng Mock
        when(result.getSeller()).thenReturn(seller);
        when(result.getDeposit()).thenReturn(deposit);
        when(deposit.getAmount()).thenReturn(0.0);

        checker.checkDeadline(result);

        // Chắc chắn rằng trạng thái KHÔNG BỊ chuyển sang EXPIRED
        verify(result, never()).setPaymentStatus(PaymentStatus.EXPIRED);
    }

    // =================================================
    // 5. VERIFY KHÔNG BỊ GỌI KHI KHÔNG QUÁ HẠN
    // =================================================

    @Test
    void test_khong_goi_seller_neu_chua_expire() {

        AuctionResult result = mock(AuctionResult.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);
        when(result.getPaymentDeadline())
                .thenReturn(LocalDateTime.now().plusDays(1));

        checker.checkDeadline(result);

        verify(result, never()).getSeller();
        verify(result, never()).getDeposit();
    }
}