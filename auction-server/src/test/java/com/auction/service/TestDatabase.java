package com.auction.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class TestDatabase {
    public static DataSource create() {
        HikariConfig config = new HikariConfig();
        // Cấu hình chạy DB H2 trực tiếp trên RAM máy tính để chạy test siêu tốc
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MSSQLServer");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(config);
    }
}