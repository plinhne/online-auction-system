package com.auction.model.user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double walletBalance;

    public Bidder(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.BIDDER);
        this.walletBalance = 0.0;
    }

    public Bidder(int id, String name, String email, String password, double walletBalance) {
        super(id, name, email, password, UserRole.BIDDER);
        this.walletBalance = walletBalance;
    }
    //setter,getter
    public double getWalletBalance() {
        return walletBalance;
    }
    public void setWalletBalance(double walletBalance){
        this.walletBalance = walletBalance;
    }
    // nạp tiền
    public void depositMoney(double amount) {walletBalance += amount;}

    // trừ tiền
    public void withdrawMoney(double amount) {

        if (walletBalance < amount) {
            throw new RuntimeException("Not enough balance");
        }

        walletBalance -= amount;
    }
}