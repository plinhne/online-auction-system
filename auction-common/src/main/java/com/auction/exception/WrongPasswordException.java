package com.auction.exception;

// Ngoại lệ khi người dùng nhập sai mật khẩu
public class WrongPasswordException extends Exception {
    public WrongPasswordException(String message) {
        super(message);
    }
}