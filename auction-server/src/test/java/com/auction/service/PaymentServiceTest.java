package com.auction.service;

import com.auction.model.item.Art;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.payment.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    @Test
    void paySuccess() {

        Seller seller = new Seller(
                1,
                "Seller",
                "seller@gmail.com",
                "123",
                0
        );

        Bidder bidder = new Bidder(
                2,
                "Bidder",
                "bidder@gmail.com",
                "123",
                20000
        );

        Item item = new Art(
                1,
                "Painting",
                10000
        );

        Deposit deposit =
                new Deposit(
                        bidder,
                        item,
                        2000
                );

        AuctionResult result =
                new AuctionResult(
                        null,
                        item,
                        seller,
                        bidder,
                        15000,
                        deposit
                );

        PaymentService service =
                new PaymentService();

        service.pay(result);

        assertEquals(
                PaymentStatus.PAID,
                result.getPaymentStatus()
        );
    }
}