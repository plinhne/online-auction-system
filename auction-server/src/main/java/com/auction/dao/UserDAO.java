package com.auction.dao;

import com.auction.model.user.User;

public interface UserDAO extends GenericDAO<User, Integer> {
    // Khai báo thêm hàm đặc thù chỉ User mới có
    User findByName(String name);
}