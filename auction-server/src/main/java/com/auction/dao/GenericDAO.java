package com.auction.dao;

import java.util.List;

// T: Loại Model (User, Item, Auction...)
// ID: Kiểu dữ liệu của khóa chính
public interface GenericDAO<T, ID> {
    void save(T entity);           // Thêm mới
    T findById(ID id);             // Tìm theo ID
    List<T> findAll();             // Lấy tất cả
    void update(T entity);         // Cập nhật
    void delete(ID id);            // Xóa
}