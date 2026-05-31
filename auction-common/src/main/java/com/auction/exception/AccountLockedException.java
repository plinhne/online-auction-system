package com.auction.exception;

// Ngoại lệ khi tài khoản đã bị Admin khóa nhưng vẫn cố đăng nhập
public class AccountLockedException extends Exception {
    public AccountLockedException(String message) {
        super(message);
    }
}