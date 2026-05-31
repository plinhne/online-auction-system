package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.payment.Deposit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepositServiceTest {

    private DepositService depositService;

    @BeforeEach
    void setUp() {
        depositService = new DepositService();
    }

    // =================================================
    // 1. TEST TẠO DEPOSIT THÀNH CÔNG
    // =================================================

    @Test
    void test_tao_deposit_thanh_cong() {

        // Mock User và Item
        User bidder = mock(User.class);
        Item item = mock(Item.class);

        // Giả lập giá item
        when(item.getPrice()).thenReturn(1000.0);

        // Gọi service
        Deposit deposit = depositService.createDeposit(bidder, item);

        // Kiểm tra số tiền bị trừ = 20% của 1000 = 200
        verify(bidder).withdrawMoney(200.0);

        // Kiểm tra object trả về không null
        assertNotNull(deposit);
    }

    // =================================================
    // 2. TEST CÔNG THỨC DEPOSIT
    // =================================================

    @Test
    void test_tinh_so_tien_deposit_chinh_xac() {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(item.getPrice()).thenReturn(500.0);

        depositService.createDeposit(bidder, item);

        // 20% của 500 = 100
        verify(bidder).withdrawMoney(100.0);
    }

    // =================================================
    // 3. TEST ITEM GIÁ = 0
    // =================================================

    @Test
    void test_item_gia_bang_0() {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(item.getPrice()).thenReturn(0.0);

        Deposit deposit = depositService.createDeposit(bidder, item);

        // Không trừ tiền
        verify(bidder).withdrawMoney(0.0);

        assertNotNull(deposit);
    }

    // =================================================
    // 4. TEST USER KHÔNG ĐỦ TIỀN (THROW EXCEPTION)
    // =================================================

    @Test
    void test_user_khong_du_tien() {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(item.getPrice()).thenReturn(1000.0);

        // giả lập lỗi khi rút tiền
        doThrow(new RuntimeException("Not enough balance"))
                .when(bidder).withdrawMoney(200.0);

        // phải throw exception
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> depositService.createDeposit(bidder, item)
        );

        assertEquals("Not enough balance", ex.getMessage());
    }

    // =================================================
    // 5. TEST DEPENDENCY CALLS
    // =================================================

    @Test
    void test_goi_day_du_method() {

        User bidder = mock(User.class);
        Item item = mock(Item.class);

        when(item.getPrice()).thenReturn(2000.0);

        depositService.createDeposit(bidder, item);

        // kiểm tra có gọi đúng method
        verify(item).getPrice();
        verify(bidder).withdrawMoney(400.0);
    }
}