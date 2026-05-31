package com.auction.service;

import com.auction.payment.AuctionResult;
import com.auction.payment.Deposit;
import com.auction.payment.PaymentStatus;
import com.auction.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    // =================================================
    // 1. STATUS KHÔNG PHẢI PENDING → THROW EXCEPTION
    // =================================================

    @Test
    void test_payment_khi_da_xu_ly_roi_thi_loi() {

        AuctionResult result = mock(AuctionResult.class);

        when(result.getPaymentStatus())
                .thenReturn(PaymentStatus.PAID);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.pay(result)
        );

        assertEquals("Auction already completed", ex.getMessage());

        // Không được gọi thêm logic nào
        verify(result, never()).getWinner();
    }

    // =================================================
    // 2. PAYMENT THÀNH CÔNG (CASE CHÍNH)
    // =================================================

    @Test
    void test_thanh_toan_thanh_cong() {

        AuctionResult result = mock(AuctionResult.class);
        User winner = mock(User.class);
        User seller = mock(User.class);
        Deposit deposit = mock(Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        when(result.getWinner()).thenReturn(winner);
        when(result.getSeller()).thenReturn(seller);

        when(result.getFinalPrice()).thenReturn(1000.0);
        when(result.getDeposit()).thenReturn(deposit);
        when(deposit.getAmount()).thenReturn(200.0);

        paymentService.pay(result);

        // kiểm tra winner bị trừ tiền = 1000 - 200 = 800
        verify(winner).withdrawMoney(800.0);

        // seller nhận đủ tiền
        verify(seller).depositMoney(1000.0);

        // trạng thái chuyển sang PAID
        verify(result).setPaymentStatus(PaymentStatus.PAID);
    }

    // =================================================
    // 3. EDGE CASE: deposit = 0
    // =================================================

    @Test
    void test_deposit_bang_0() {

        AuctionResult result = mock(AuctionResult.class);
        User winner = mock(User.class);
        User seller = mock(User.class);
        Deposit deposit = mock(Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        when(result.getWinner()).thenReturn(winner);
        when(result.getSeller()).thenReturn(seller);

        when(result.getFinalPrice()).thenReturn(500.0);
        when(result.getDeposit()).thenReturn(deposit);
        when(deposit.getAmount()).thenReturn(0.0);

        paymentService.pay(result);

        // winner phải trả full tiền
        verify(winner).withdrawMoney(500.0);

        // seller nhận đủ
        verify(seller).depositMoney(500.0);

        verify(result).setPaymentStatus(PaymentStatus.PAID);
    }

    // =================================================
    // 4. EDGE CASE: finalPrice nhỏ hơn deposit (logic lạ nhưng test vẫn cần)
    // =================================================

    @Test
    void test_final_price_nho_hon_deposit() {

        AuctionResult result = mock(AuctionResult.class);
        User winner = mock(User.class);
        User seller = mock(User.class);
        Deposit deposit = mock(Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        when(result.getWinner()).thenReturn(winner);
        when(result.getSeller()).thenReturn(seller);

        when(result.getFinalPrice()).thenReturn(300.0);
        when(result.getDeposit()).thenReturn(deposit);
        when(deposit.getAmount()).thenReturn(500.0);

        paymentService.pay(result);

        // remainMoney = -200
        verify(winner).withdrawMoney(-200.0);

        verify(seller).depositMoney(300.0);

        verify(result).setPaymentStatus(PaymentStatus.PAID);
    }

    // =================================================
    // 5. VERIFY FLOW KHÔNG BỊ LỆCH
    // =================================================

    @Test
    void test_verify_dung_quy_trinh() {

        AuctionResult result = mock(AuctionResult.class);
        User winner = mock(User.class);
        User seller = mock(User.class);
        Deposit deposit = mock(Deposit.class);

        when(result.getPaymentStatus()).thenReturn(PaymentStatus.PENDING);

        when(result.getWinner()).thenReturn(winner);
        when(result.getSeller()).thenReturn(seller);
        when(result.getFinalPrice()).thenReturn(1000.0);
        when(deposit.getAmount()).thenReturn(100.0);
        when(result.getDeposit()).thenReturn(deposit);

        paymentService.pay(result);

        // đảm bảo đúng sequence logic
        verify(result).getWinner();
        verify(result).getSeller();
        verify(result).getFinalPrice();
        verify(result).getDeposit();
    }
}