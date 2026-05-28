package com.auction.model.auction.payment;

import com.auction.model.item.Item;
import com.auction.model.user.User;


import java.time.LocalDateTime;

public class AuctionResult {

    private Item item;

    private User winner;

    private double finalPrice;

    private LocalDateTime paymentDeadline;

    private com.auction.model.payment.PaymentStatus paymentStatus;

    private com.auction.model.payment.Deposit deposit;

    public AuctionResult(
            Item item,
            User winner,
            double finalPrice,
            com.auction.model.payment.Deposit deposit
    ) {
        this.item = item;
        this.winner = winner;
        this.finalPrice = finalPrice;
        this.deposit = deposit;

        this.paymentDeadline =
                LocalDateTime.now().plusDays(1);

        this.paymentStatus =
                com.auction.model.payment.PaymentStatus.PENDING;
    }

    public Item getItem() {
        return item;
    }

    public User getWinner() {
        return winner;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public LocalDateTime getPaymentDeadline() {
        return paymentDeadline;
    }

    public com.auction.model.payment.PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(
            com.auction.model.payment.PaymentStatus paymentStatus
    ) {
        this.paymentStatus = paymentStatus;
    }

    public com.auction.model.payment.Deposit getDeposit() {
        return deposit;
    }
}

