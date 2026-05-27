package com.auction.network;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // Luồng Authentication
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    SIGNUP_REQUEST,
    SIGNUP_RESPONSE,

    // Luồng Đấu giá chính
    PLACE_BID_REQUEST,
    PLACE_BID_RESPONSE,

    // Gói tin đẩy Realtime từ Server xuống (Dành riêng cho ServerListener)
    AUCTION_UPDATE_NOTIFICATION,
    CHAT_MESSAGE_NOTIFICATION,
    USER_BANNED_NOTIFICATION
}