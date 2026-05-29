package com.auction.network;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // Luồng Authentication
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    SIGNUP_REQUEST,
    SIGNUP_RESPONSE,

    // Luồng Truy vấn dữ liệu Sản phẩm
    GET_ALL_AUCTIONS_REQUEST,
    GET_ALL_AUCTIONS_RESPONSE,

    // Luồng Quản lý Sản phẩm (BỔ SUNG MỚI ĐỂ SỬA LỖI ADD/EDIT ITEM)
    ADD_ITEM_REQUEST,
    ADD_ITEM_RESPONSE,
    EDIT_ITEM_REQUEST,
    EDIT_ITEM_RESPONSE,

    // Luồng Đấu giá chính
    PLACE_BID_REQUEST,
    PLACE_BID_RESPONSE,

    // Gói tin đẩy Realtime từ Server xuống
    AUCTION_UPDATE_NOTIFICATION,
    CHAT_MESSAGE_NOTIFICATION,
    USER_BANNED_NOTIFICATION,

    // Luồng quản trị của Admin
    ADMIN_ACTION_REQUEST,
    ADMIN_ACTION_RESPONSE,

    // Tín hiệu đẩy Realtime từ Server về Client
    USER_BALANCE_UPDATE_NOTIFICATION
}