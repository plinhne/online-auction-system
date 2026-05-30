package com.auction.network;

import java.io.Serializable;

public enum MessageType implements Serializable {
    // ── Luồng Authentication (Xác thực tài khoản) ──
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    SIGNUP_REQUEST,
    SIGNUP_RESPONSE,
    LOGOUT_REQUEST,          // MỚI THÊM: Xử lý đăng xuất
    LOGOUT_RESPONSE,         // MỚI THÊM

    // ── Luồng Truy vấn dữ liệu Sản phẩm chung ──
    GET_ALL_AUCTIONS_REQUEST,
    GET_ALL_AUCTIONS_RESPONSE,

    // ── Luồng Quản lý Sản phẩm (Của Seller/Admin) ──
    ADD_ITEM_REQUEST,
    ADD_ITEM_RESPONSE,
    EDIT_ITEM_REQUEST,
    EDIT_ITEM_RESPONSE,

    // ── Luồng Quản lý Phiên đấu giá (Của Seller) ──
    CREATE_AUCTION_REQUEST,  // MỚI THÊM: Tạo phiên đấu giá
    CREATE_AUCTION_RESPONSE, // MỚI THÊM
    CANCEL_AUCTION_REQUEST,  // MỚI THÊM: Hủy phiên đấu giá
    CANCEL_AUCTION_RESPONSE, // MỚI THÊM
    GET_MY_AUCTIONS_REQUEST, // MỚI THÊM: Lấy danh sách phiên đấu giá của tôi (Seller)
    GET_MY_AUCTIONS_RESPONSE,// MỚI THÊM

    // ── Luồng Phòng đấu giá (Join / Leave) ──
    JOIN_AUCTION_REQUEST,    // MỚI THÊM: Vào phòng đấu giá
    JOIN_AUCTION_RESPONSE,   // MỚI THÊM
    LEAVE_AUCTION_REQUEST,   // MỚI THÊM: Thoát phòng đấu giá
    LEAVE_AUCTION_RESPONSE,  // MỚI THÊM

    // ── Luồng Đấu giá chính (Bidding) ──
    PLACE_BID_REQUEST,
    PLACE_BID_RESPONSE,
    SET_AUTO_BID_REQUEST,    // MỚI THÊM: Thiết lập đấu giá tự động (Auto-bid)
    SET_AUTO_BID_RESPONSE,   // MỚI THÊM
    GET_MY_BIDS_REQUEST,     // MỚI THÊM: Lịch sử đấu giá của tôi (Bidder)
    GET_MY_BIDS_RESPONSE,    // MỚI THÊM

    // ── Gói tin đẩy Realtime từ Server xuống (Broadcast) ──
    AUCTION_UPDATE_NOTIFICATION,
    CHAT_MESSAGE_NOTIFICATION,
    USER_BANNED_NOTIFICATION,
    USER_BALANCE_UPDATE_NOTIFICATION, // Tín hiệu đẩy Realtime về số dư

    // ── Luồng quản trị của Admin ──
    ADMIN_ACTION_REQUEST,
    ADMIN_ACTION_RESPONSE,

    // ── Ping/Pong kiểm tra kết nối mạng ──
    PING,                    // MỚI THÊM: Dành cho chức năng Heartbeat giữ kết nối
    PONG                     // MỚI THÊM
}