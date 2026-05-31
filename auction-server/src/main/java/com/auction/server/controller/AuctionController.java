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
    private final com.auction.service.ItemService itemService;
    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonSerializer<java.time.LocalDateTime>) (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(java.time.LocalDateTime.class, (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> java.time.LocalDateTime.parse(json.getAsString()))
            .create();

    public AuctionController(AuctionService auctionService, com.auction.service.ItemService itemService) {
        this.auctionService = auctionService;
        this.itemService = itemService;
    }

    private com.auction.dto.AuctionDTO toDTO(Auction a) {
        com.auction.dto.AuctionDTO dto = new com.auction.dto.AuctionDTO();
        dto.setId(a.getId());
        dto.setItemId(a.getItemId());
        dto.setSellerId(a.getSellerId());
        dto.setStartingPrice(a.getStartingPrice());
        dto.setCurrentPrice(a.getCurrentPrice());
        dto.setMinIncrement(a.getMinIncrement());
        dto.setStatus(a.getStatus());
        dto.setStartTime(a.getStartTime());
        dto.setEndTime(a.getEndTime());
        dto.setLeadingBidderId(a.getLeadingBidderId());

        try {
            com.auction.model.item.Item item = itemService.getItemById(a.getItemId());
            if (item != null) {
                dto.setItemName(item.getName());
                dto.setItemDescription(item.getDescription());
                dto.setItemCategory(item.getCategory().name());
                dto.setItemImage_url(item.getImageUrl());
            }
        } catch (Exception e) {
            logger.warn("Cannot load item for auctionId={}", a.getId());
            dto.setItemName("Sản phẩm #" + a.getItemId());
            dto.setItemDescription("Đang cập nhật...");
            dto.setItemCategory("OTHER");
            dto.setItemImage_url(null);
        }
        return dto;
    }

    public void handleGetAuctions(JsonObject response) throws Exception {
        List<Auction> auctions = auctionService.getAllAuctions();
        List<com.auction.dto.AuctionDTO> dtos = auctions.stream()
                .map(this::toDTO)
                .toList();
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(dtos));
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
        response.add("auction", gson.toJsonTree(toDTO(auction))); // dùng DTO
        return auction;
    }

    public void handleLeaveAuction(JsonObject response) {
        response.addProperty("status", "OK");
    }

    public void handleGetMyAuctions(JsonObject response, User seller) throws Exception {
        List<Auction> auctions = auctionService.getAuctionsBySeller(seller.getId());
        List<com.auction.dto.AuctionDTO> dtos = auctions.stream()
                .map(this::toDTO)
                .toList();
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(dtos));
    }

    public void handleGetMyBids(JsonObject response, User bidder) throws Exception {
        List<Auction> auctions = auctionService.getAuctionsByBidder(bidder.getId());
        List<com.auction.dto.AuctionDTO> dtos = auctions.stream()
                .map(this::toDTO)
                .toList();
        response.addProperty("status", "OK");
        response.add("auctions", gson.toJsonTree(dtos));
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
        response.add("auction", gson.toJsonTree(toDTO(auction))); // dùng DTO
        return auction;
    }

    public void handleCancelAuction(JsonObject request, JsonObject response, User requester) throws Exception {
        int auctionId = request.get("auctionId").getAsInt();
        auctionService.cancelAuction(auctionId, requester);
        response.addProperty("status", "OK");
        response.addProperty("message", "Auction cancelled: " + auctionId);
    }
}