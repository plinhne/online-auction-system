package com.auction.service;

import com.auction.payment.AuctionResult;
import com.auction.payment.PaymentStatus;

import java.time.LocalDateTime;

public class PaymentDeadlineChecker {

    public void checkDeadline(AuctionResult result) {

        if (result.getPaymentStatus()
                != PaymentStatus.PENDING) {
            return;
        }

        if (LocalDateTime.now().isAfter(
                result.getPaymentDeadline())) {

            result.setPaymentStatus(
                    PaymentStatus.EXPIRED
            );

            result.getSeller()
                    .depositMoney(
                            result.getDeposit()
                                    .getAmount()
                    );

            result.getDeposit()
                    .setTransferredToSeller(
                            true
                    );

            System.out.println(
                    "Payment expired. Deposit transferred to seller."
            );
        }
    }
}