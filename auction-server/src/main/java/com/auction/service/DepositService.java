package com.auction.service;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.User;
import com.auction.payment.Deposit;

public class DepositService {

    private static final double DEPOSIT_RATE = 0.2;

    public Deposit createDeposit(User bidder, Item item) {

        double depositAmount = item.getPrice() * DEPOSIT_RATE;

        bidder.withdrawMoney(depositAmount);

        System.out.println(
                bidder.getName() +
                        " deposited " +
                        depositAmount
        );

        return new Deposit(bidder, item, depositAmount);
    }
}