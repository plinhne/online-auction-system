package com.auction.service;

import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.auction.server.model.User;

public class AutoBidService {

    private static final double BID_STEP = 500;

    public void enableAutoBid(
            Auction auction,
            User user,
            double maxAmount
    ) {

        auction.setAutoBidUser(user);
        auction.setAutoBidLimit(maxAmount);

        System.out.println(
                user.getUsername()
                        + " enabled auto bid up to "
                        + maxAmount
        );
    }

    public void processAutoBid(
            Auction auction,
            BidTransaction newBid
    ) {

        User autoBidUser = auction.getAutoBidUser();

        // chưa có autobid
        if (autoBidUser == null) {
            return;
        }

        // không autobid chính mình
        if (autoBidUser.getId() == newBid.getBidderID()) {
            return;
        }

        double autoBidLimit = auction.getAutoBidLimit();

        double nextBid = newBid.getAmount() + BID_STEP;

        // kiểm tra còn đủ limit không
        if (nextBid <= autoBidLimit) {

            BidTransaction autoBid = new BidTransaction(
                    999,
                    auction.getId(),
                    autoBidUser.getId(),
                    nextBid
            );

            auction.placeBid(autoBid);

            System.out.println(
                    "Auto bid placed by "
                            + autoBidUser.getUsername()
                            + ": "
                            + nextBid
            );
        }
    }
}