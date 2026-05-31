package com.auction.model.user;

import com.auction.client.util.LoggerUtil;
import com.auction.model.auction.Auction;
import com.auction.model.auction.AuctionStatus;

public class Admin extends User {
    private static final long serialVersionUID = 1L;
    
    public Admin(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.ADMIN);
    }

    /**
     * 1. TÍNH NĂNG: QUẢN LÝ USER (Khóa hoặc mở khóa tài khoản)
     */
    public void changeUserStatus(User targetUser, boolean shouldLock) {
        if (targetUser == null) return;

        // Ngăn chặn hành vi Admin tự khóa chính mình hoặc khóa Admin khác
        if (targetUser.getRole() == UserRole.ADMIN) {
            LoggerUtil.error("Thao tác thất bại: Admin không thể tự khóa tài khoản Admin!");
            return;
        }

        targetUser.setLocked(shouldLock);
        LoggerUtil.info("Admin đã " + (shouldLock ? "KHÓA" : "MỞ KHÓA") + " tài khoản: " + targetUser.getName());
    }

    /**
     * 2. TÍNH NĂNG: XÓA / HỦY PHIÊN ĐẤU GIÁ (Khi phát hiện gian lận)
     */
    public void deleteAuction(Auction targetAuction, String reason) {
        if (targetAuction == null) return;

        // Chuyển trạng thái phiên đấu giá về CANCELLED (Hủy bỏ) thay vì xóa cứng khỏi database
        // Điều này giúp giữ lại lịch sử đối chứng (Audit Log) chuẩn OOP
        targetAuction.setStatus(AuctionStatus.CANCELLED);
        LoggerUtil.info("Admin hủy phiên đấu giá ID " + targetAuction.getId() + ". Lý do: " + reason);
    }

    /**
     * 3. TÍNH NĂNG: CAN THIỆP HỆ THỐNG (Ép cập nhật số dư, cấu hình hệ thống)
     */
    public void forceModifyWalletBalance(Bidder bidder, double forcedBalance) {
        if (bidder != null && forcedBalance >= 0) {
            bidder.setWalletBalance(forcedBalance);
            LoggerUtil.info("Admin can thiệp hệ thống: Điều chỉnh số dư của " + bidder.getName() + " thành " + forcedBalance + "đ");
        }
    }
}