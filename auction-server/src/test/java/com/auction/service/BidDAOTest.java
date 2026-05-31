package com.auction.service; // Đồng bộ đúng với thư mục vật lý chứa file

import com.auction.model.bid.Bid;
import com.auction.server.dao.BidDAO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidDAOTest {

    private DataSource dataSource;
    private BidDAO bidDAO;

    // =================================================
    // CẤU HÌNH DATABASE ẢO (H2 IN-MEMORY) TẠI CHỎ
    // =================================================
    private static class TestDatabase {
        public static DataSource create() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:testdb_bid;DB_CLOSE_DELAY=-1;MODE=MSSQLServer");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
            return new HikariDataSource(config);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        dataSource = TestDatabase.create();
        bidDAO = new BidDAO(dataSource);

        // Khởi tạo bảng ảo trong bộ nhớ trước khi chạy mỗi hàm test
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE bid_transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    auction_id INT,
                    bidder_id INT,
                    amount DOUBLE,
                    placed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }

    // =================================================
    // 1. SAVE BID
    // =================================================

    @Test
    void test_save_bid() throws Exception {
        // ĐÃ SỬA: Rút gọn về đúng 4 tham số chuẩn theo đúng Constructor thực tế của model Bid
        // Cấu trúc: Bid(id, amount, bidderId, auctionId)
        Bid bid = new Bid(0, 100.0, 1, 1);

        bidDAO.save(bid);

        // Kiểm tra dữ liệu thực tế đã được chèn vào DB ảo chưa
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT * FROM bid_transactions")) {

            assertTrue(rs.next());
            assertEquals(100.0, rs.getDouble("amount"));
            assertEquals(1, rs.getInt("bidder_id"));
            assertEquals(1, rs.getInt("auction_id"));
        }
    }

    // =================================================
    // 2. FIND BY AUCTION ID
    // =================================================

    @Test
    void test_find_by_auction_id() throws Exception {
        // Giả lập dữ liệu có sẵn trong database trước khi tìm kiếm
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                INSERT INTO bid_transactions (auction_id, bidder_id, amount, placed_at)
                VALUES (1, 1, 200, CURRENT_TIMESTAMP)
            """);
        }

        // Thực hiện hàm gọi tìm kiếm từ DAO
        List<Bid> bids = bidDAO.findByAuctionId(1);

        // Kiểm tra xem dữ liệu trả về có khớp với DB không
        assertEquals(1, bids.size());
        assertEquals(200, bids.get(0).getAmount());
    }
}