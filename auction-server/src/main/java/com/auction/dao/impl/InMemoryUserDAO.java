package com.auction.dao.impl;

import com.auction.dao.UserDAO;
import com.auction.model.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserDAO implements UserDAO {

    private final Map<Integer, User> database = new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        database.put(user.getId(), user);
    }

    @Override
    public User findById(Integer id) {
        return database.get(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void update(User user) {
        database.put(user.getId(), user); // Ghi đè dữ liệu mới
    }

    @Override
    public void delete(Integer id) {
        database.remove(id);
    }

    @Override
    public User findByName(String name) {
        return database.values().stream()
                .filter(u -> u.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}