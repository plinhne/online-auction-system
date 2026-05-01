package com.auction.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static synchronized DataSource getDateSource() {
        if(dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlserver://localhost\\SQLEXPRESS:1433;databaseName=auctionDB;integratedSecurity=true;encrypt=false");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }
}
