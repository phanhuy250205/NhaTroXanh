INSERT INTO rooms (acreage, category_id, hostel_id, max_tenants, price, description, namerooms, status, address)
VALUES 
    (25.5, 1, 1, 4, 3500000.00, 'Phòng đẹp, thoáng mát', 'Phòng 101', 1, '123 Đường ABC, Quận 1, TP.HCM'),
    (30.0, 2, 1, 6, 4500000.00, 'Phòng rộng, có ban công', 'Phòng 102', 1, '125 Đường ABC, Quận 1, TP.HCM'),
    (20.0, 1, 1, 3, 2800000.00, 'Phòng nhỏ, giá rẻ', 'Phòng 103', 0, '127 Đường ABC, Quận 1, TP.HCM');

INSERT INTO contracts (room_id, user_id, owner_id, unregistered_tenant_id, contract_date, created_at, start_date, end_date, price, deposit, duration, terms, status, tenant_phone)
VALUES 
    (7, 209, 207, NULL, '2025-07-08', '2025-07-08', '2025-07-08', '2025-12-31', 3500000.00, 7000000.00, 6.0, 'Hợp đồng thuê phòng 6 tháng, thanh toán hàng tháng', 1, '0901234567')
   
    (2, 1, 207, NULL, '2025-07-08', '2025-07-08', '2025-07-08', '2025-12-31', 4500000.00, 9000000.00, 6.0, 'Hợp đồng thuê phòng 6 tháng, thanh toán hàng tháng', 1, '0901234568'),
    (3, 1, 207, NULL, '2025-07-08', '2025-07-08', '2025-07-08', '2025-12-31', 2800000.00, 5600000.00, 6.0, 'Hợp đồng thuê phòng 6 tháng, thanh toán hàng tháng', 1, '0901234569');

    SELECT hostel_id FROM hostels;


    INSERT INTO payments (contract_id, total_amount, due_date, payment_date, payment_status, payment_method)
VALUES 
    (2, 4000000.00, '2025-08-10', NULL, 'CHƯA_THANH_TOÁN', NULL);
UPDATE rooms SET status = 'ACTIVE' WHERE status = '1' OR status = 1;
UPDATE rooms SET status = 'INACTIVE' WHERE status = '0' OR status = 0;
UPDATE notifications SET room_id = 7 WHERE notification_id = 23;


SELECT * FROM payments WHERE payment_id = 12;
SELECT * FROM contracts WHERE contract_id = (SELECT contract_id FROM payments WHERE payment_id = 11);
SELECT * FROM rooms WHERE room_id = (SELECT room_id FROM contracts WHERE contract_id = (SELECT contract_id FROM payments WHERE payment_id = 11));
SELECT * FROM hostels WHERE hostel_id = (SELECT hostel_id FROM rooms WHERE room_id = (SELECT room_id FROM contracts WHERE contract_id = (SELECT contract_id FROM payments WHERE payment_id = 11)));
SELECT * FROM address WHERE id = (SELECT address_id FROM hostels WHERE hostel_id = (SELECT hostel_id FROM rooms WHERE room_id = (SELECT room_id FROM contracts WHERE contract_id = (SELECT contract_id FROM payments WHERE payment_id = 11))));
SELECT * FROM ward WHERE id = (SELECT ward_id FROM address WHERE id = <address_id>);
SELECT * FROM district WHERE id = (SELECT district_id FROM ward WHERE id = <ward_id>);
SELECT * FROM province WHERE id = (SELECT province_id FROM district WHERE id = <district_id>);

SELECT * FROM payments WHERE app_trans_id = '250717_9561' OR id = 17;


ALTER TABLE table_name ADD COLUMN created_at DATETIME;

DESCRIBE posts;

ALTER TABLE posts ADD COLUMN created_at DATE;
SELECT * FROM payments WHERE app_trans_id = '250724_1753358533608';enum('BANK','MOMO','TIỀN_MẶT','VNPAY')


ALTER TABLE payments MODIFY COLUMN payment_method ENUM('TIỀN_MẶT', 'BANK', 'VNPAY', 'MOMO') NOT NULL;
SHOW COLUMNS FROM payments LIKE 'payment_method';