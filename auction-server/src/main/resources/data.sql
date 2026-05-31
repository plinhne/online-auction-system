-- Sample data để test

MERGE INTO users (name, email, password, role)
    KEY(email)
    VALUES ('Admin',        'admin@auction.com',  'admin123',  'ADMIN'),
           ('Alice Seller', 'alice@auction.com',  'alice123',  'SELLER'),
           ('Bob Bidder',   'bob@auction.com',    'bob123',    'BIDDER'),
           ('Carol Bidder', 'carol@auction.com',  'carol123',  'BIDDER'),
           ('bidder', 'bidder@example.com', 'bidder123', 'BIDDER'),
           ('seller', 'seller@example.com', 'seller123', 'SELLER'),
           ('admin', 'admin@example.com', 'admin123', 'ADMIN');




        MERGE INTO items (name, description, seller_id, category)
    KEY(name)
    VALUES (
               'Vintage Porsche 911 Carrera',
               'Rare 1973 Porsche 911 Carrera RS 2.7 in original condition. This iconic sports car features the legendary flat-six engine and ducktail spoiler. Complete service history and matching numbers.',
               2, 'VEHICLE'
           ),
           (
               'Picasso Original Lithograph',
               'Authentic Pablo Picasso lithograph from 1960s. Professionally framed and certified by renowned art experts. Includes certificate of authenticity and provenance documentation.',
               2, 'ART'
           ),
           (
               'DJI Mavic 3 Pro Drone',
               'Professional drone with Hasselblad camera system. Triple camera setup with 4/3 CMOS sensor. Includes Fly More Combo with 3 batteries, charging hub, and carrying case.',
               2, 'ELECTRONICS'
           );

MERGE INTO bid_transactions (auction_id, bidder_id, amount, placed_at)
    KEY(auction_id, bidder_id)
    VALUES (1, 3, 200000.00, PARSEDATETIME('2026-05-27 12:00:00', 'yyyy-MM-dd HH:mm:ss')),
    (1, 4, 250000.00, PARSEDATETIME('2026-05-27 13:00:00', 'yyyy-MM-dd HH:mm:ss')),
    (1, 5, 285000.00, PARSEDATETIME('2026-05-27 14:00:00', 'yyyy-MM-dd HH:mm:ss')),
    (2, 3, 75000.00,  PARSEDATETIME('2026-05-24 13:00:00', 'yyyy-MM-dd HH:mm:ss')),
    (2, 5, 125000.00, PARSEDATETIME('2026-05-24 14:00:00', 'yyyy-MM-dd HH:mm:ss'));