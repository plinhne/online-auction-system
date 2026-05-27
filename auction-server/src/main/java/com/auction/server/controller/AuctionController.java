package com.auction.server.controller;

import com.auction.model.auction.Auction;
import com.auction.model.user.User;
import com.auction.service.AuctionService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;
    private final Gson gson = new Gson();

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    //Query

    public void handleGetAuctions(JsonObject response) throws Exception {
        List<Auction> auctions = auctionService.getAllAuctions();
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(auctions));
    }

    public Auction handleJoinAuction(JsonObject request, JsonObject response) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            response.addProperty("status", "ERROR");
            response.addProperty("message", "Auction not found: " + auctionId);
            logger.warn("Client tried to join non-existent auction: {}", auctionId);
            return null;
        }
        response.addProperty("status", "OK");
        response.add("auction", gson.toJsonTree(auction));
        return auction;
    }

    public void handleLeaveAuction(JsonObject response) {
        response.addProperty("status", "OK");
    }

    public void handleGetMyAuctions(JsonObject response, User seller) throws Exception {
        List<Auction> auctions = auctionService.getAuctionsBySeller(seller.getId());
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(auctions));
    }

    public void handleGetMyBids(JsonObject response, User bidder) throws Exception {
        List<Auction> auctions = auctionService.getAuctionsByBidder(bidder.getId());
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(auctions));
    }

    //Mutation

    public Auction handleCreateAuction(JsonObject request, JsonObject response, User seller) throws Exception {
        int itemId           = request.get("itemId").getAsInt();
        double startingPrice = request.get("startingPrice").getAsDouble();
        double minIncrement  = request.get("minIncrement").getAsDouble();
        LocalDateTime startTime = LocalDateTime.parse(request.get("startTime").getAsString());
        LocalDateTime endTime   = LocalDateTime.parse(request.get("endTime").getAsString());

        Auction auction = auctionService.createAuction(
                seller, itemId, startingPrice, minIncrement, startTime, endTime
        );

        response.addProperty("status", "OK");
        response.add("auction", gson.toJsonTree(auction));
        return auction;
    }

    public void handleCancelAuction(JsonObject request, JsonObject response, User requester) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();
        auctionService.cancelAuction(auctionId, requester);
        response.addProperty("status", "OK");
        response.addProperty("message", "Auction cancelled: " + auctionId);
    }
}