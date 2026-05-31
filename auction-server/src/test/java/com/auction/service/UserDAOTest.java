package com.auction.service;

import com.auction.model.user.User;
import com.auction.server.dao.UserDAO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private DataSource dataSource;
    private UserDAO userDAO;

    // =================================================
    // CẤU HÌNH DATABASE ẢO (H2 IN-MEMORY) TẠI CHỖ
    // =================================================
    private static class TestDatabase {
        public static DataSource create() {
            HikariConfig config = new HikariConfig();
            // Khởi tạo DB chạy thẳng trên RAM máy tính để tăng tốc độ chạy Test
            config.setJdbcUrl("jdbc:h2:mem:testdb_user;DB_CLOSE_DELAY=-1;MODE=MSSQLServer");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
            return new HikariDataSource(config);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        dataSource = TestDatabase.create(); // Đã nhận diện trơn tru nhờ class tĩnh nhúng tại chỗ
        userDAO = new UserDAO(dataSource);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    password VARCHAR(100),
                    role VARCHAR(20),
                    status VARCHAR(20),
                    balance DOUBLE
                )
            """);
        }
    }

    // =================================================
    // 1. SAVE USER
    // =================================================

    @Test
    void test_save_user() throws Exception {

        User user = new com.auction.model.user.Bidder(
                0, "A", "a@gmail.com", "123"
        );

        userDAO.save(user);

        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT * FROM users")) {

            assertTrue(rs.next());
            assertEquals("a@gmail.com", rs.getString("email"));
        }
    }

    // =================================================
    // 2. FIND BY EMAIL
    // =================================================

    @Test
    void test_find_by_email() throws Exception {

        insertUser();

        User user = userDAO.findByEmail("a@gmail.com");

        assertNotNull(user);
        assertEquals("a@gmail.com", user.getEmail());
    }

    // Helper hỗ trợ chèn dữ liệu nhanh độc lập
    private void insertUser() throws Exception {

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                INSERT INTO users (name,email,password,role,status,balance)
                VALUES ('A','a@gmail.com','123','BIDDER','ACTIVE',0)
            """);
        }
    }
}