package com.auction.service;

import com.auction.payment.AuctionResult;
import com.auction.payment.PaymentStatus;
import com.auction.model.user.User;

import java.time.LocalDateTime;

public class PaymentService {

    public void pay(AuctionResult result) {

        if (result.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException(
                    "Auction already completed"
            );
        }

        User winner = result.getWinner();

        double remainMoney =
                result.getFinalPrice()
                        - result.getDeposit().getAmount();

        winner.withdrawMoney(remainMoney);

        User seller = result.getSeller();

        seller.depositMoney(
                result.getFinalPrice()
        );

        result.setPaymentStatus(
                PaymentStatus.PAID
        );
    }
}