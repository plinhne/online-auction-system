package com.auction.dao.impl;

import com.auction.dao.AuctionDAO;
import com.auction.model.auction.Auction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAuctionDAO implements AuctionDAO {
    private final Map<Integer, Auction> database = new ConcurrentHashMap<>();

    @Override
    public void save(Auction auction) {
        database.put(auction.getId(), auction);
    }

    @Override
    public Auction findById(Integer id) {
        return database.get(id);
    }

    @Override
    public List<Auction> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void update(Auction auction) {
        database.put(auction.getId(), auction);
    }

    @Override
    public void delete(Integer id) {
        database.remove(id);
    }
}