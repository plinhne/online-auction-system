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

MERGE INTO auctions (item_id, seller_id, starting_price, current_price, min_increment, status, start_time, end_time)
    KEY(item_id)
    VALUES (
               1, 2, 150000.00, 285000.00, 1000.00, 'ACTIVE',
               PARSEDATETIME('2026-05-27 11:51:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-05 14:51:00', 'yyyy-MM-dd HH:mm:ss')
           ),
           (
               2, 2, 50000.00, 125000.00, 500.00, 'ACTIVE',
               PARSEDATETIME('2026-05-24 12:03:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-05 13:03:00', 'yyyy-MM-dd HH:mm:ss')
           ),
           (
               3, 2, 1500.00, 1500.00, 50.00, 'SCHEDULED',
               PARSEDATETIME('2026-06-05 14:03:00', 'yyyy-MM-dd HH:mm:ss'),
               PARSEDATETIME('2026-06-10 14:03:00', 'yyyy-MM-dd HH:mm:ss')
           );