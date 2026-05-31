package com.auction.model.auction;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {


    // Kiểm tra tự động xem hàm startAuction có hoạt động đúng ko
    @Test
    void startAuctionSuccess() {

        Auction auction = new Auction(
                1,
                100,
                10,
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );

        auction.startAuction();

        assertEquals(
                AuctionStatus.ACTIVE,
                auction.getStatus()
        );
    }

    // kiểm tra trạng thái của phiên đấu giá có hợp lệ hay ko
    @Test
    void startAuctionFailWhenAlreadyActive() {

        Auction auction = new Auction(
                1,
                100,
                10,
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );

        auction.startAuction();

        assertThrows(
                IllegalStateException.class,
                auction::startAuction
        );
    }

    // kiểm tra xem phiên đã kết thúc thành công hay chưa
    @Test
    void endAuctionSuccess() {

        Auction auction = new Auction(
                1,
                100,
                10,
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );

        auction.startAuction();

        auction.endAuction();

        assertEquals(
                AuctionStatus.ENDED,
                auction.getStatus()
        );
    }

    @Test
    void isActiveReturnsTrueWhenAuctionStarted() {

        Auction auction = new Auction(
                1,
                100,
                10,
                1000,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)
        );

        auction.startAuction();

        assertTrue(auction.isActive());
    }
}