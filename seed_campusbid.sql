-- CampusBid Seed SQL Script
-- Inserts realistic campus flea market auction lots, photos, and bid history

USE campusbid;

-- Ensure clean data or add fresh curated items
-- 1. Trek FX 2 Disc Commuter Hybrid Bike
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Bikes & Transit',
    DATE_ADD(NOW(), INTERVAL 3 DAY),
    'GOOD',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    170.00,
    'Medium 17.5" aluminum frame, 24-speed Shimano drivetrain, Tektro hydraulic disc brakes. Perfect for riding between North and South campus. Upgraded with a rear Blackburn luggage rack and kickstand. Minor cosmetic scratches on top tube. Includes heavy-duty Kryptonite U-lock and cable.',
    10.00,
    'elena.r@campus.edu',
    'STU-001',
    'Elena Rostova',
    140.00,
    'ACTIVE',
    'Trek FX 2 Disc Commuter Hybrid Bike (Medium)',
    NOW()
);
SET @bike_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@bike_id, 'https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=800&q=80'),
(@bike_id, 'https://images.unsplash.com/photo-1507035895480-2b3156c31fc8?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@bike_id, 140.00, DATE_SUB(NOW(), INTERVAL 36 HOUR), 'STU-004', 'Marcus Vance'),
(@bike_id, 155.00, DATE_SUB(NOW(), INTERVAL 18 HOUR), 'STU-007', 'Priya Patel'),
(@bike_id, 170.00, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'STU-012', 'Jordan Bell');


-- 2. Apple iPad Air (5th Gen, M1, 64GB Space Gray) with Apple Pencil 2
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Electronics',
    DATE_ADD(NOW(), INTERVAL 18 HOUR),
    'LIKE_NEW',
    DATE_SUB(NOW(), INTERVAL 3 DAY),
    410.00,
    'Barely used M1 iPad Air with magnetic Apple Pencil 2 and matte paper-feel screen protector pre-installed. Ideal for taking lecture notes in GoodNotes or Notability. Battery health at 98%. Comes with original 20W USB-C charger and magnetic folio cover.',
    15.00,
    'liam.t@campus.edu',
    'STU-003',
    'Liam Tanaka',
    350.00,
    'ACTIVE',
    'Apple iPad Air 5 (M1, 64GB) + Apple Pencil 2',
    NOW()
);
SET @ipad_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@ipad_id, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=800&q=80'),
(@ipad_id, 'https://images.unsplash.com/photo-1561154464-82e9adf32764?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@ipad_id, 350.00, DATE_SUB(NOW(), INTERVAL 48 HOUR), 'STU-008', 'Hannah Scott'),
(@ipad_id, 380.00, DATE_SUB(NOW(), INTERVAL 24 HOUR), 'STU-015', 'Devon Brooks'),
(@ipad_id, 410.00, DATE_SUB(NOW(), INTERVAL 2 HOUR), 'STU-002', 'Aisha Rahman');


-- 3. Organic Chemistry: Structure and Function (8th Edition - Vollhardt & Schore)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Textbooks',
    DATE_ADD(NOW(), INTERVAL 5 DAY),
    'LIKE_NEW',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    60.00,
    'Hardcover textbook required for CHEM 241 & 242. Pristine condition with zero highlighter markings or margin notes. Includes unopened Sapling Learning access card code. Saved me hundreds compared to the campus bookstore price!',
    5.00,
    'chloe.d@campus.edu',
    'STU-005',
    'Chloe Dubois',
    45.00,
    'ACTIVE',
    'Organic Chemistry: Structure & Function (8th Ed)',
    NOW()
);
SET @chem_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@chem_id, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80'),
(@chem_id, 'https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@chem_id, 45.00, DATE_SUB(NOW(), INTERVAL 20 HOUR), 'STU-010', 'Lucas Meyer'),
(@chem_id, 55.00, DATE_SUB(NOW(), INTERVAL 10 HOUR), 'STU-006', 'Sophia Chen'),
(@chem_id, 60.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'STU-014', 'Mateo Ortiz');


-- 4. Keurig K-Mini Single Serve Coffee Maker (Oasis Green)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Dorm & Living',
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    'NEW',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    34.00,
    'Brand new in box! Less than 5 inches wide, fits anywhere in a cramped dorm room. Brews 6-12 oz cups quickly using standard K-cups or reusable pods. Descaled and sanitized. Retails for $89 on Amazon.',
    3.00,
    'aisha.r@campus.edu',
    'STU-002',
    'Aisha Rahman',
    25.00,
    'ACTIVE',
    'Keurig K-Mini Coffee Maker (Dorm Friendly)',
    NOW()
);
SET @keurig_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@keurig_id, 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?auto=format&fit=crop&w=800&q=80'),
(@keurig_id, 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@keurig_id, 25.00, DATE_SUB(NOW(), INTERVAL 15 HOUR), 'STU-009', 'Tyler Washington'),
(@keurig_id, 31.00, DATE_SUB(NOW(), INTERVAL 6 HOUR), 'STU-011', 'Grace Kim'),
(@keurig_id, 34.00, DATE_SUB(NOW(), INTERVAL 45 MINUTE), 'STU-004', 'Marcus Vance');


-- 5. Patagonia Better Sweater 1/4-Zip Fleece (Men''s M, Stonewash)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Clothing',
    DATE_ADD(NOW(), INTERVAL 4 DAY),
    'GOOD',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    46.00,
    'Classic campus fleece pullover made from 100% recycled polyester knitted fleece. Dyed with a low-impact process. Zippered left-chest pocket, flat-seam construction. Keeps you super warm in winter library study sessions. No stains or tears.',
    4.00,
    'marcus.v@campus.edu',
    'STU-004',
    'Marcus Vance',
    30.00,
    'ACTIVE',
    'Patagonia Better Sweater 1/4-Zip Fleece (Medium)',
    NOW()
);
SET @fleece_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@fleece_id, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=800&q=80'),
(@fleece_id, 'https://images.unsplash.com/photo-1578587018452-892bacefd3f2?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@fleece_id, 30.00, DATE_SUB(NOW(), INTERVAL 30 HOUR), 'STU-013', 'Noah Garcia'),
(@fleece_id, 38.00, DATE_SUB(NOW(), INTERVAL 14 HOUR), 'STU-007', 'Priya Patel'),
(@fleece_id, 46.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'STU-003', 'Liam Tanaka');


-- 6. Ergotron Adjustable Laptop Desk Riser & Dual Monitor Arm
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Dorm & Living',
    DATE_ADD(NOW(), INTERVAL 6 DAY),
    'LIKE_NEW',
    DATE_SUB(NOW(), INTERVAL 5 HOUR),
    35.00,
    'Heavy duty gas-spring desk clamp stand with dual arms: one holds up to a 17" laptop, the other holds up to a 32" monitor (VESA 75/100). Integrated cable management clips. Huge space saver for dorm study desks!',
    5.00,
    'sophia.c@campus.edu',
    'STU-006',
    'Sophia Chen',
    35.00,
    'ACTIVE',
    'Ergotron Dual Arm Laptop & Monitor Desk Riser',
    NOW()
);
SET @riser_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@riser_id, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80');


-- 7. Organic Chemistry 240-Piece Molecular Model Kit with Bond Tool
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Supplies',
    DATE_ADD(NOW(), INTERVAL 2 DAY),
    'NEW',
    DATE_SUB(NOW(), INTERVAL 18 HOUR),
    19.00,
    'Complete ball-and-stick and space-filling molecular model set. 86 atoms (carbon, hydrogen, oxygen, nitrogen, sulfur, halogen, metal) and 153 flexible links. Essential for 3D stereochemistry and chair conformations.',
    2.00,
    'hannah.s@campus.edu',
    'STU-008',
    'Hannah Scott',
    15.00,
    'ACTIVE',
    '240-Piece Molecular Model Kit for Chemistry',
    NOW()
);
SET @kit_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@kit_id, 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@kit_id, 15.00, DATE_SUB(NOW(), INTERVAL 12 HOUR), 'STU-010', 'Lucas Meyer'),
(@kit_id, 19.00, DATE_SUB(NOW(), INTERVAL 3 HOUR), 'STU-005', 'Chloe Dubois');


-- 8. Bose QuietComfort 35 II Wireless Headphones (Silver)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Electronics',
    DATE_ADD(NOW(), INTERVAL 1 DAY),
    'GOOD',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    99.00,
    'Legendary noise cancellation with three levels of world-class quiet. Bluetooth pairing with multi-device switching. Brand new replacement ear pads installed last month. Comes with original hardshell travel case, 3.5mm audio cable, and micro-USB charging cord.',
    8.00,
    'elena.r@campus.edu',
    'STU-001',
    'Elena Rostova',
    75.00,
    'ACTIVE',
    'Bose QC35 II Noise Cancelling Headphones',
    NOW()
);
SET @bose_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@bose_id, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80'),
(@bose_id, 'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@bose_id, 75.00, DATE_SUB(NOW(), INTERVAL 28 HOUR), 'STU-011', 'Grace Kim'),
(@bose_id, 83.00, DATE_SUB(NOW(), INTERVAL 12 HOUR), 'STU-014', 'Mateo Ortiz'),
(@bose_id, 99.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), 'STU-009', 'Tyler Washington');


-- 9. TI-84 Plus CE Color Graphing Calculator (Rose Gold Edition)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Electronics',
    DATE_ADD(NOW(), INTERVAL 3 DAY),
    'LIKE_NEW',
    DATE_SUB(NOW(), INTERVAL 1 DAY),
    70.00,
    'High-resolution backlit color screen with rechargeable battery. Approved for SAT, ACT, AP Calc, and Stats exams. Includes slide case, USB cable, and wall adapter. Zero dead pixels or scratches.',
    5.00,
    'priya.p@campus.edu',
    'STU-007',
    'Priya Patel',
    55.00,
    'ACTIVE',
    'TI-84 Plus CE Color Graphing Calculator (Rose Gold)',
    NOW()
);
SET @ti84_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@ti84_id, 'https://images.unsplash.com/photo-1594980596870-8aa52a78d8cd?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@ti84_id, 55.00, DATE_SUB(NOW(), INTERVAL 16 HOUR), 'STU-002', 'Aisha Rahman'),
(@ti84_id, 65.00, DATE_SUB(NOW(), INTERVAL 7 HOUR), 'STU-015', 'Devon Brooks'),
(@ti84_id, 70.00, DATE_SUB(NOW(), INTERVAL 20 MINUTE), 'STU-006', 'Sophia Chen');


-- 10. North Face Borealis 28L Campus Backpack (Asphalt Grey)
INSERT INTO listings (category, closing_time, item_condition, created_at, current_price, description, min_bid_increment, seller_email, seller_id, seller_name, starting_price, status, title, updated_at)
VALUES (
    'Supplies',
    DATE_ADD(NOW(), INTERVAL 4 DAY),
    'GOOD',
    DATE_SUB(NOW(), INTERVAL 2 DAY),
    45.00,
    'FlexVent suspension system certified by the American Chiropractic Association. Padded 15" laptop sleeve, fleece-lined tablet pocket, front bungee stash system, external dual water bottle pockets. Perfect daily commuter pack.',
    5.00,
    'tyler.w@campus.edu',
    'STU-009',
    'Tyler Washington',
    35.00,
    'ACTIVE',
    'The North Face Borealis 28L Campus Backpack',
    NOW()
);
SET @backpack_id = LAST_INSERT_ID();

INSERT INTO listing_photos (listing_id, photo_url) VALUES
(@backpack_id, 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80');

INSERT INTO listing_bids (listing_id, amount, bid_time, bidder_id, bidder_name) VALUES
(@backpack_id, 35.00, DATE_SUB(NOW(), INTERVAL 22 HOUR), 'STU-012', 'Jordan Bell'),
(@backpack_id, 45.00, DATE_SUB(NOW(), INTERVAL 4 HOUR), 'STU-008', 'Hannah Scott');
