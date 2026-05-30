package com.auction.server.controller;

import com.auction.model.bid.Bid;
import com.auction.model.user.User;
import com.auction.server.MainServer;
import com.auction.service.AutoBidService;
import com.auction.service.BidService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class BidController {
    private static final Logger logger = LoggerFactory.getLogger(BidController.class);

    private final BidService bidService;
    private final AutoBidService autoBidService;
    private final Gson gson = new Gson();

    public BidController(BidService bidService, AutoBidService autoBidService) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
    }

    public void handlePlaceBid(JsonObject request, JsonObject response, User bidder) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();

        // ĐÃ SỬA: Đổi từ "amount" thành "bidAmount" để khớp với JSON của Client gửi lên
        double amount = request.get("bidAmount").getAsDouble();

        // placeBid trả về bid thủ công đã được accept
        Bid bid = bidService.placeBid(auctionId, bidder.getId(), amount);
        response.addProperty("status", "OK");
        response.addProperty("newPrice", bid.getAmount());

        // Broadcast bid thủ công
        broadcastBidUpdate(auctionId, bid, bidder.getName());

        // Broadcast auto-bid nếu có trigger — autoBidService giữ kết quả trigger gần nhất
        Bid autoBid = autoBidService.getLastTriggered(auctionId);
        if (autoBid != null) {
            broadcastBidUpdate(auctionId, autoBid, "Auto-Bid");
        }
    }

    public void handleSetAutoBid(JsonObject request, JsonObject response, User bidder) throws Exception {
        int auctionId    = request.get("auctionId").getAsInt();
        double maxBid    = request.get("maxBid").getAsDouble();
        double increment = request.get("increment").getAsDouble();

        bidService.setAutoBid(auctionId, bidder.getId(), maxBid, increment);

        response.addProperty("status", "OK");
        response.addProperty("message", "Auto-bid registered. Max: " + maxBid + ", Increment: " + increment);
    }

    private void broadcastBidUpdate(int auctionId, Bid bid, String bidderName) {
        JsonObject notify = new JsonObject();
        notify.addProperty("event", "BID_UPDATE");
        notify.addProperty("auctionId", auctionId);
        notify.addProperty("newPrice", bid.getAmount());
        notify.addProperty("bidderId", bid.getBidderId());
        notify.addProperty("bidderName", bidderName);

        // Tránh NPE nếu placedAt chưa được set
        String placedAt = bid.getPlacedAt() != null
                ? bid.getPlacedAt().toString()
                : LocalDateTime.now().toString();
        notify.addProperty("placedAt", placedAt);

        MainServer.broadcastToAuction(auctionId, gson.toJson(notify));
        logger.debug("BID_UPDATE broadcast: auctionId={}, price={}", auctionId, bid.getAmount());
    }
}