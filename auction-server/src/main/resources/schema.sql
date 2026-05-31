CREATE TABLE IF NOT EXISTS users (
                                     id       INT AUTO_INCREMENT PRIMARY KEY,
                                     name     VARCHAR(100) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    status   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
    );

CREATE TABLE IF NOT EXISTS items (
                                     id          INT AUTO_INCREMENT PRIMARY KEY,
                                     name        VARCHAR(200) NOT NULL,
    description CLOB,
    seller_id   INT NOT NULL REFERENCES users(id),
    category    VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS auctions (
                                        id                INT AUTO_INCREMENT PRIMARY KEY,
                                        item_id           INT           NOT NULL REFERENCES items(id),
    seller_id         INT           NOT NULL REFERENCES users(id),
    starting_price    DECIMAL(18,2) NOT NULL,
    current_price     DECIMAL(18,2) NOT NULL,
    min_increment     DECIMAL(18,2) NOT NULL DEFAULT 1.00,
    status            VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    start_time        TIMESTAMP     NOT NULL,
    end_time          TIMESTAMP     NOT NULL,
    leading_bidder_id INT REFERENCES users(id)
    );

CREATE TABLE IF NOT EXISTS bid_transactions (
                                    id         INT AUTO_INCREMENT PRIMARY KEY,
                                    auction_id INT           NOT NULL REFERENCES auctions(id),
    bidder_id  INT           NOT NULL REFERENCES users(id),
    amount     DECIMAL(18,2) NOT NULL,
    placed_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_seller ON auctions(seller_id);
CREATE INDEX IF NOT EXISTS idx_bids_auction    ON bid_transactions(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder     ON bid_transactions(bidder_id);