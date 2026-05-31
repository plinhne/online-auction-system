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
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) ->
                    java.time.LocalDateTime.parse(json.getAsString(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();
    public BidController(BidService bidService, AutoBidService autoBidService, AuctionService auctionService) {
        this.bidService = bidService;
        this.autoBidService = autoBidService;
        this.auctionService = auctionService;
    }

    public void handlePlaceBid(JsonObject request, JsonObject response, User bidder) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();
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

    private void broadcastBidUpdate(int auctionId, Bid bid, String bidderName) throws Exception {
        // Lấy auction mới nhất (đã cập nhật giá và số lượt đặt) từ Database
        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction != null) {
            // Client (RealTimeBiddingController) đang expect nhận được 1 chuỗi JSON
            // có thể cast thẳng thành class AuctionDTO, nên ta sẽ đóng gói toàn bộ đối tượng Auction.
            // Biến gson đã được khai báo ở đầu file BidController sẽ tự động map các trường.
            String auctionJsonPayload = gson.toJson(auction);

            NetworkMessage notification = new NetworkMessage(
                    MessageType.AUCTION_UPDATE_NOTIFICATION,
                    auctionJsonPayload
            );

            // Gửi phát chùm (Broadcast) cho tất cả các Client đang ở trong phòng này
            MainServer.broadcastToAuction(auctionId, gson.toJson(notification));
            logger.debug("Đã phát AUCTION_UPDATE_NOTIFICATION cho phòng {}: Giá mới = {}", auctionId, bid.getAmount());
        } else {
            logger.error("Không tìm thấy Auction ID {} để broadcast cập nhật giá.", auctionId);
        }
    }
}