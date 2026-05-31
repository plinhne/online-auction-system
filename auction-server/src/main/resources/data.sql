-- 1. users
MERGE INTO users (name, email, password, role, balance)
    KEY(email)
    VALUES ('Admin',        'admin@auction.com',  'admin123',  'ADMIN',  999999.00),
           ('Alice Seller', 'alice@auction.com',  'alice123',  'SELLER', 50000.00),
           ('Bob Bidder',   'bob@auction.com',    'bob123',    'BIDDER', 500000.00),
           ('Carol Bidder', 'carol@auction.com',  'carol123',  'BIDDER', 300000.00),
           ('bidder',       'bidder@example.com', 'bidder123', 'BIDDER', 1000000.00),
           ('seller',       'seller@example.com', 'seller123', 'SELLER', 50000.00),
           ('admin',        'admin@example.com',  'admin123',  'ADMIN',  999999.00);

-- 2. items
MERGE INTO items (name, description, seller_id, category, image_url)
    KEY(name)
    VALUES (
               'Vintage Porsche 911 Carrera',
               'Rare 1973 Porsche 911 Carrera RS 2.7 in original condition.',
               6, 'VEHICLE',
               'uploads/items/car.jpg'
           ),
           (
               'Picasso Original Lithograph',
               'Authentic Pablo Picasso lithograph from 1960s.',
               6, 'ART',
               'uploads/items/art.jpg'
           ),
           (
               'DJI Mavic 3 Pro Drone',
               'Professional drone with Hasselblad camera system.',
               6, 'ELECTRONICS',
               'uploads/items/drone.jpg'
           );

-- 3. auctions
MERGE INTO auctions (item_id, seller_id, starting_price, current_price, min_increment, status, start_time, end_time)
    KEY(item_id)
    VALUES (
               1, 6, 150000.00, 285000.00, 1000.00, 'ACTIVE',
               PARSEDATETIME('2026-05-27 11:51:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-29 14:51:00', 'yyyy-MM-dd HH:mm:ss')
           ),
           (
               2, 6, 50000.00, 125000.00, 500.00, 'ACTIVE',
               PARSEDATETIME('2026-05-24 12:03:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-29 13:03:00', 'yyyy-MM-dd HH:mm:ss')
           ),
           (
               3, 6, 1500.00, 1500.00, 50.00, 'SCHEDULED',
               PARSEDATETIME('2026-05-31 14:03:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-01 14:03:00', 'yyyy-MM-dd HH:mm:ss')
           );

-- 4. bid_transactions
MERGE INTO bid_transactions (auction_id, bidder_id, amount, placed_at)
    KEY(auction_id, bidder_id)
    VALUES (1, 3, 200000.00, PARSEDATETIME('2026-05-27 12:00:00', 'yyyy-MM-dd HH:mm:ss')),
           (1, 4, 250000.00, PARSEDATETIME('2026-05-27 13:00:00', 'yyyy-MM-dd HH:mm:ss')),
           (1, 5, 285000.00, PARSEDATETIME('2026-05-27 14:00:00', 'yyyy-MM-dd HH:mm:ss')),
           (2, 3, 75000.00,  PARSEDATETIME('2026-05-24 13:00:00', 'yyyy-MM-dd HH:mm:ss')),
           (2, 5, 125000.00, PARSEDATETIME('2026-05-24 14:00:00', 'yyyy-MM-dd HH:mm:ss'));