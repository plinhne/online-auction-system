package com.auction.dao;

import com.auction.model.auction.Auction;

import java.util.List;

public interface AuctionDAO extends GenericDAO<Auction, Integer> {
    List<Auction> findByBidderId(int bidderId);
}