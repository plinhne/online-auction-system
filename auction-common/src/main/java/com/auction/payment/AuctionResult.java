package com.auction.payment;

import com.auction.model.auction.Auction;
import com.auction.model.item.Item;
import com.auction.model.user.User;


import java.time.LocalDateTime;

public class AuctionResult {

    private Auction auction;

    private Item item;

    private User seller;

    private User winner;

    private double finalPrice;

    private LocalDateTime paymentDeadline;

    private PaymentStatus paymentStatus;

    private Deposit deposit;

    public AuctionResult(
            Auction auction,
            Item item,
            User seller,
            User winner,
            double finalPrice,
            Deposit deposit
    ) {
        this.auction = auction;
        this.item = item;
        this.seller = seller;
        this.winner = winner;
        this.finalPrice = finalPrice;
        this.deposit = deposit;

        this.paymentDeadline =
                LocalDateTime.now().plusDays(1);

        this.paymentStatus =
                PaymentStatus.PENDING;
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

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Auction getAuction(){
        return auction;
    }

    public User getSeller() {
        return seller;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }
    public void setPaymentStatus(
            PaymentStatus paymentStatus
    ) {
        this.paymentStatus = paymentStatus;
    }

    public Deposit getDeposit() {
        return deposit;
    }
}

