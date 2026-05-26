package com.auction.server.model;

import com.auction.server.service.AutoBidService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    @Test
    void testAutoBidWorks() {

        // tạo user autobid
        User autoUser =
                new User(1, "Nguyen");

        // tạo user thường
        User normalUser =
                new User(2, "An");

        // tạo auction
        Auction auction = new Auction(
                1,
                100,
                10,
                5000,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(10)
        );

        // mở auction
        auction.setStatus(AuctionStatus.RUNNING);

        // tạo autobid service
        AutoBidService autoBidService =
                new AutoBidService();

        // bật autobid
        autoBidService.enableAutoBid(
                auction,
                autoUser,
                10000
        );

        // user thường bid 7000
        BidTransaction normalBid =
                new BidTransaction(
                        1,
                        1,
                        2,
                        7000
                );

        auction.placeBid(normalBid);

        // xử lý autobid
        autoBidService.processAutoBid(
                auction,
                normalBid
        );

        // kiểm tra autobid đã tăng giá chưa
        assertEquals(
                7500,
                auction.getCurrentPrice()
        );

        // kiểm tra người dẫn đầu
        assertEquals(
                1,
                auction.getLeadingBidderID()
        );
    }
}