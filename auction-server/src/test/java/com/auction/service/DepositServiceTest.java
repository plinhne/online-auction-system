package com.auction.service;


import com.auction.model.item.Art;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.payment.Deposit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DepositServiceTest {

    @Test
    void createDepositSuccess() {

        Bidder bidder = new Bidder(
                1,
                "Linh",
                "linh@gmail.com",
                "123",
                10000
        );

        Item item = new Art(
                1,
                "Painting",
                5000
        );

        DepositService service =
                new DepositService();

        Deposit deposit =
                service.createDeposit(
                        bidder,
                        item
                );

        assertEquals(
                1000,
                deposit.getAmount()
        );

        assertEquals(
                9000,
                bidder.getWalletBalance()
        );
    }

    @Test
    void createDepositFail_NotEnoughMoney() {

        Bidder bidder = new Bidder(
                1,
                "Linh",
                "linh@gmail.com",
                "123",
                500
        );

        Item item = new Art(
                1,
                "Painting",
                5000
        );

        DepositService service =
                new DepositService();

        assertThrows(
                RuntimeException.class,
                () -> service.createDeposit(
                        bidder,
                        item
                )
        );
    }
}