package com.auction.server.controller;

import com.auction.model.auction.Auction;
import com.auction.model.bid.Bid;
import com.auction.model.user.User;
import com.auction.network.MessageType;
import com.auction.network.NetworkMessage;
import com.auction.server.MainServer;
import com.auction.service.AuctionService;
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
    private final AuctionService auctionService;
    private final Gson gson = new Gson();

    public BidController(BidService bidService, AutoBidService autoBidService, AuctionService auctionService) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.auctionService = auctionService;
    }

    public void handlePlaceBid(JsonObject request, JsonObject response, User bidder) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();
        double amount = request.get("amount").getAsDouble();

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

    private void broadcastBidUpdate(int auctionId, Bid bid, String bidderName) throws Exception {
        logger.info(
                "Broadcasting AUCTION_UPDATE_NOTIFICATION auction={} price={}",
                auctionId,
                bid.getAmount()
        );
        // Lấy auction mới nhất để client update đầy đủ thông tin
        Auction auction = auctionService.getAuctionById(auctionId);

        // Payload: full Auction object để client deserialize trực tiếp
        // Client đang expect: AUCTION_UPDATE_NOTIFICATION với payload là Auction object
        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", auctionId);
        payload.addProperty("currentPrice", bid.getAmount());
        payload.addProperty("bidderId", bid.getBidderId());
        payload.addProperty("bidderName", bidderName);
        if (auction != null) {
            payload.addProperty("endTime", auction.getEndTime().toString());
        }
        String placedAt = bid.getPlacedAt() != null
                ? bid.getPlacedAt().toString()
                : LocalDateTime.now().toString();
        payload.addProperty("placedAt", placedAt);

        // Bọc trong NetworkMessage chuẩn — client parse theo MessageType
        NetworkMessage notification = new NetworkMessage(
                MessageType.AUCTION_UPDATE_NOTIFICATION,
                payload.toString()
        );

        logger.info("Broadcasting auction update: {}", payload);
        MainServer.broadcastToAuction(auctionId, gson.toJson(notification));
        logger.debug("AUCTION_UPDATE_NOTIFICATION broadcast: auctionId={}, price={}", auctionId, bid.getAmount());
    }
}