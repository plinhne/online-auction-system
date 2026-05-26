package com.auction.dao.impl;

import com.auction.dao.ItemDAO;
import com.auction.model.item.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryItemDAO implements ItemDAO {
    private final Map<Integer, Item> database = new ConcurrentHashMap<>();

    @Override
    public void save(Item item) {
        database.put(item.getId(), item);
    }

    @Override
    public Item findById(Integer id) {
        return database.get(id);
    }

    @Override
    public List<Item> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void update(Item item) {
        database.put(item.getId(), item);
    }

    @Override
    public void delete(Integer id) {
        database.remove(id);
    }
}