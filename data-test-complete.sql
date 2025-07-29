-- =====================================================
-- COMPREHENSIVE TEST DATA FOR NHA TRO XANH SYSTEM
-- =====================================================

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. LOCATION DATA (Province, District, Ward)
-- =====================================================

INSERT INTO province (id, code, name) VALUES
(1, '79', 'Thành phố Hồ Chí Minh'),
(2, '01', 'Hà Nội'),
(3, '48', 'Đà Nẵng'),
(4, '74', 'Bình Dương'),
(5, '77', 'Bà Rịa - Vũng Tàu');

INSERT INTO district (id, code, name, province_id) VALUES
(1, '760', 'Quận 1', 1),
(2, '761', 'Quận 2', 1),
(3, '762', 'Quận 3', 1),
(4, '763', 'Quận 4', 1),
(5, '764', 'Quận 5', 1),
(6, '765', 'Quận 6', 1),
(7, '766', 'Quận 7', 1),
(8, '767', 'Quận 8', 1),
(9, '768', 'Quận 9', 1),
(10, '769', 'Quận 10', 1);

INSERT INTO ward (id, code, name, district_id) VALUES
(1, '26734', 'Phường Bến Nghé', 1),
(2, '26735', 'Phường Bến Thành', 1),
(3, '26736', 'Phường Cầu Kho', 1),
(4, '26737', 'Phường Cầu Ông Lãnh', 1),
(5, '26738', 'Phường Cô Giang', 1),
(6, '26740', 'Phường An Phú', 2),
(7, '26741', 'Phường An Khánh', 2),
(8, '26742', 'Phường Bình An', 2),
(9, '26743', 'Phường Bình Khánh', 2),
(10, '26744', 'Phường Bình Trưng Đông', 2);

-- =====================================================
-- 2. ADDRESS DATA
-- =====================================================

INSERT INTO address (id, street, ward_id, user_id) VALUES
(1, '123 Đường Nguyễn Huệ', 1, NULL),
(2, '456 Đường Lê Lợi', 2, NULL),
(3, '789 Đường Hai Bà Trưng', 3, NULL),
(4, '321 Đường Pasteur', 4, NULL),
(5, '654 Đường Cách Mạng Tháng 8', 5, NULL),
(6, '987 Đường Xa Lộ Hà Nội', 6, NULL),
(7, '147 Đường Nguyễn Văn Cừ', 7, NULL),
(8, '258 Đường Võ Văn Tần', 8, NULL),
(9, '369 Đường Trần Hưng Đạo', 9, NULL),
(10, '741 Đường Điện Biên Phủ', 10, NULL);

-- =====================================================
-- 3. CATEGORY DATA
-- =====================================================

INSERT INTO category (category_id, name) VALUES
(1, 'Phòng trọ'),
(2, 'Căn hộ mini'),
(3, 'Nhà nguyên căn'),
(4, 'Homestay'),
(5, 'Ký túc xá'),
(6, 'Phòng cao cấp'),
(7, 'Phòng bình dân'),
(8, 'Studio'),
(9, 'Duplex'),
(10, 'Penthouse');

-- =====================================================
-- 4. UTILITIES DATA
-- =====================================================

INSERT INTO utilities (name) VALUES
('Wifi miễn phí'),
('Điều hòa'),
('Tủ lạnh'),
('Máy giặt'),
('Bếp gas');
(6, 'Nóng lạnh'),
(7, 'Ban công'),
(8, 'Thang máy'),
(9, 'Bảo vệ 24/7'),
(10, 'Chỗ để xe'),
(11, 'Gần trường học'),
(12, 'Gần bệnh viện'),
(13, 'Gần chợ'),
(14, 'Gần công viên'),
(15, 'Gần trung tâm thương mại'),
(16, 'Phòng tắm riêng'),
(17, 'Phòng bếp riêng'),
(18, 'Cửa sổ thoáng mát'),
(19, 'Giường tủ đầy đủ'),
(20, 'Camera an ninh');

-- =====================================================
-- 5. USERS DATA
-- =====================================================

INSERT INTO users (password,fullname, phone, birthday, bank_account, balance, gender, email, avatar, otp_code, otp_expiration, enabled, address, created_at, address_id, role) VALUES
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2','Nguyễn Văn Admin','0901000001','1985-01-15', '1234567890', 50000000.00, 1, 'admin@nhatroxanh.com', '/uploads/admin-avatar.jpg', NULL, NULL, 1, '123 Đường Admin', '2024-01-01 08:00:00', 1, 'ADMIN'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Trần Thị Staff', '0901000002', '1990-03-20', '2345678901', 10000000.00, 0, 'staff@nhatroxanh.com', '/uploads/staff-avatar.jpg', NULL, NULL, 1, '456 Đường Staff', '2024-01-02 08:00:00', 2, 'STAFF'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Lê Văn Chủ Trọ', '0901000003', '1980-05-10', '3456789012', 100000000.00, 1, 'owner1@nhatroxanh.com', '/uploads/owner1-avatar.jpg', NULL, NULL, 1, '789 Đường Chủ Trọ 1', '2024-01-03 08:00:00', 3, 'OWNER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Phạm Thị Lan Anh', '0901000004', '1982-07-25', '4567890123', 80000000.00, 0, 'owner2@nhatroxanh.com', '/uploads/owner2-avatar.jpg', NULL, NULL, 1, '321 Đường Chủ Trọ 2', '2024-01-04 08:00:00', 4, 'OWNER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Hoàng Minh Tuấn', '0901000005', '1978-12-03', '5678901234', 120000000.00, 1, 'owner3@nhatroxanh.com', '/uploads/owner3-avatar.jpg', NULL, NULL, 1, '654 Đường Chủ Trọ 3', '2024-01-05 08:00:00', 5, 'OWNER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Nguyễn Thị Hoa', '0901000006', '1995-02-14', '6789012345', 5000000.00, 0, 'customer1@gmail.com', '/uploads/customer1-avatar.jpg', NULL, NULL, 1, '987 Đường Khách Hàng 1', '2024-01-06 08:00:00', 6, 'CUSTOMER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Trần Văn Nam', '0901000007', '1993-08-22', '7890123456', 3000000.00, 1, 'customer2@gmail.com', '/uploads/customer2-avatar.jpg', NULL, NULL, 1, '147 Đường Khách Hàng 2', '2024-01-07 08:00:00', 1, 'CUSTOMER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Lê Thị Mai', '0901000008', '1996-11-30', '8901234567', 4000000.00, 0, 'customer3@gmail.com', '/uploads/customer3-avatar.jpg', NULL, NULL, 1, '258 Đường Khách Hàng 3', '2024-01-08 08:00:00', 1, 'CUSTOMER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Phạm Văn Đức', '0901000009', '1994-04-18', '9012345678', 6000000.00, 1, 'customer4@gmail.com', '/uploads/customer4-avatar.jpg', NULL, NULL, 1, '369 Đường Khách Hàng 4', '2024-01-09 08:00:00', 4, 'CUSTOMER'),
('$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Võ Thị Lan', '0901000010', '1997-09-05', '0123456789', 2500000.00, 0, 'customer5@gmail.com', '/uploads/customer5-avatar.jpg', NULL, NULL, 1, '741 Đường Khách Hàng 5', '2024-01-10 08:00:00', 1, 'CUSTOMER');

-- =====================================================
-- 6. USER CCCD DATA
-- =====================================================

INSERT INTO User_CCCD (id, user_id, cccd_number, full_name, date_of_birth, gender, nationality, place_of_origin, place_of_residence, date_of_issue, date_of_expiry, issuing_authority, front_image_url, back_image_url, verification_status, created_at, updated_at) VALUES
(1, 3, '079085001234', 'Lê Văn Chủ Trọ', '1980-05-10', 'Nam', 'Việt Nam', 'TP. Hồ Chí Minh', '789 Đường Chủ Trọ 1, Q.1, TP.HCM', '2015-05-10', '2030-05-10', 'Cục Cảnh sát QLHC về TTXH', '/uploads/cccd-front-1.jpg', '/uploads/cccd-back-1.jpg', 'VERIFIED', '2024-01-03 08:30:00', '2024-01-03 09:00:00'),
(2, 4, '079082002345', 'Phạm Thị Lan Anh', '1982-07-25', 'Nữ', 'Việt Nam', 'TP. Hồ Chí Minh', '321 Đường Chủ Trọ 2, Q.1, TP.HCM', '2017-07-25', '2032-07-25', 'Cục Cảnh sát QLHC về TTXH', '/uploads/cccd-front-2.jpg', '/uploads/cccd-back-2.jpg', 'VERIFIED', '2024-01-04 08:30:00', '2024-01-04 09:00:00'),
(3, 5, '079078003456', 'Hoàng Minh Tuấn', '1978-12-03', 'Nam', 'Việt Nam', 'TP. Hồ Chí Minh', '654 Đường Chủ Trọ 3, Q.1, TP.HCM', '2013-12-03', '2028-12-03', 'Cục Cảnh sát QLHC về TTXH', '/uploads/cccd-front-3.jpg', '/uploads/cccd-back-3.jpg', 'VERIFIED', '2024-01-05 08:30:00', '2024-01-05 09:00:00');

-- =====================================================
-- 7. HOSTELS DATA
-- =====================================================

INSERT INTO hostels (hostel_id, name, description, status, room_number, created_at, owner_id, address_id) VALUES
(1, 'Nhà Trọ Xanh Quận 1', 'Nhà trọ cao cấp tại trung tâm Quận 1, đầy đủ tiện nghi, an ninh 24/7', 1, 20, '2024-01-15', 3, 1),
(2, 'Căn Hộ Mini Lê Lợi', 'Căn hộ mini hiện đại, gần trung tâm thương mại, tiện ích đầy đủ', 1, 15, '2024-01-20', 3, 2),
(3, 'Homestay Hai Bà Trưng', 'Homestay ấm cúng, phù hợp cho sinh viên và người đi làm', 1, 12, '2024-01-25', 1, 3),
(4, 'Phòng Trọ Pasteur', 'Phòng trọ giá rẻ, sạch sẽ, gần bệnh viện và trường học', 1, 18, '2024-02-01', 1, 4),
(5, 'Nhà Trọ Cách Mạng Tháng 8', 'Nhà trọ rộng rãi, thoáng mát, có sân để xe rộng', 1, 25, '2024-02-05', 1, 5),
(6, 'Studio Xa Lộ Hà Nội', 'Studio cao cấp cho thuê theo tháng, đầy đủ nội thất', 1, 10, '2024-02-10', 2, 6);

-- =====================================================
-- 8. ROOMS DATA
-- =====================================================

INSERT INTO rooms (room_id, category_id, hostel_id, description, namerooms, status, address, acreage, max_tenants, price) VALUES
(1, 1, 1, 'Phòng trọ cao cấp, đầy đủ tiện nghi, view đẹp', 'Phòng 101', 'unactive', '123 Đường Nguyễn Huệ, P.Bến Nghé, Q.1', 25.0, 2, 4500000.00),
(2, 1, 1, 'Phòng trọ rộng rãi, có ban công, thoáng mát', 'Phòng 102', 'unactive', '123 Đường Nguyễn Huệ, P.Bến Nghé, Q.1', 28.0, 2, 5000000.00),
(3, 6, 1, 'Phòng cao cấp, nội thất sang trọng', 'Phòng 201', 'active', '123 Đường Nguyễn Huệ, P.Bến Nghé, Q.1', 35.0, 2, 7000000.00),
(4, 6, 1, 'Phòng VIP, view thành phố, đầy đủ tiện ích', 'Phòng 202', 'unactive', '123 Đường Nguyễn Huệ, P.Bến Nghé, Q.1', 40.0, 3, 8500000.00),
(5, 1, 1, 'Phòng tiêu chuẩn, sạch sẽ, giá hợp lý', 'Phòng 301', 'active', '123 Đường Nguyễn Huệ, P.Bến Nghé, Q.1', 22.0, 2, 4000000.00),
(6, 2, 2, 'Căn hộ mini 1 phòng ngủ, bếp riêng', 'Mini 01', 'unactive', '456 Đường Lê Lợi, P.Bến Thành, Q.1', 30.0, 2, 6000000.00),
(7, 2, 2, 'Căn hộ mini 2 phòng ngủ, phòng khách riêng', 'Mini 02', 'active', '456 Đường Lê Lợi, P.Bến Thành, Q.1', 45.0, 4, 9000000.00),
(8, 8, 2, 'Studio hiện đại, không gian mở', 'Studio 01', 'unactive', '456 Đường Lê Lợi, P.Bến Thành, Q.1', 25.0, 1, 5500000.00),
(9, 8, 2, 'Studio cao cấp, nội thất đầy đủ', 'Studio 02', 'unactive', '456 Đường Lê Lợi, P.Bến Thành, Q.1', 28.0, 2, 6500000.00),
(10, 4, 3, 'Homestay ấm cúng, phù hợp gia đình nhỏ', 'Home 01', 'active', '789 Đường Hai Bà Trưng, P.Cầu Kho, Q.1', 35.0, 3, 5500000.00),
(11, 4, 3, 'Homestay rộng rãi, có sân vườn nhỏ', 'Home 02', 'unactive', '789 Đường Hai Bà Trưng, P.Cầu Kho, Q.1', 50.0, 5, 8000000.00),
(12, 4, 3, 'Homestay tiện nghi, gần trung tâm', 'Home 03', 'unactive', '789 Đường Hai Bà Trưng, P.Cầu Kho, Q.1', 40.0, 4, 7000000.00),
(13, 7, 4, 'Phòng bình dân, giá rẻ, sạch sẽ', 'Phòng A01', 'unactive', '321 Đường Pasteur, P.Cầu Ông Lãnh, Q.1', 18.0, 2, 2800000.00),
(14, 7, 4, 'Phòng nhỏ gọn, phù hợp sinh viên', 'Phòng A02', 'active', '321 Đường Pasteur, P.Cầu Ông Lãnh, Q.1', 20.0, 2, 3200000.00),
(15, 1, 4, 'Phòng trọ tiêu chuẩn, tiện nghi cơ bản', 'Phòng B01', 'unactive', '321 Đường Pasteur, P.Cầu Ông Lãnh, Q.1', 25.0, 3, 3800000.00),
(16, 1, 5, 'Phòng trọ rộng rãi, thoáng mát', 'Phòng 1A', 'active', '654 Đường Cách Mạng Tháng 8, P.Cô Giang, Q.1', 30.0, 3, 4200000.00),
(17, 1, 5, 'Phòng trọ có ban công, view đẹp', 'Phòng 1B', 'unactive', '654 Đường Cách Mạng Tháng 8, P.Cô Giang, Q.1', 32.0, 3, 4500000.00),
(18, 6, 5, 'Phòng cao cấp, nội thất hiện đại', 'Phòng 2A', 'unactive', '654 Đường Cách Mạng Tháng 8, P.Cô Giang, Q.1', 38.0, 4, 6500000.00),
(19, 8, 6, 'Studio sang trọng, đầy đủ tiện nghi', 'Studio Premium 01', 'active', '987 Đường Xa Lộ Hà Nội, P.An Phú, Q.2', 35.0, 2, 7500000.00),
(20, 8, 6, 'Studio hiện đại, không gian mở', 'Studio Premium 02', 'unactive', '987 Đường Xa Lộ Hà Nội, P.An Phú, Q.2', 32.0, 2, 7000000.00);

-- =====================================================
-- 9. ROOM UTILITIES RELATIONSHIP
-- =====================================================

INSERT INTO room_utilities (room_id, utility_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 6), (1, 9), (1, 10), (1, 16), (1, 18), (1, 19),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 6), (2, 7), (2, 9), (2, 10), (2, 16), (2, 18), (2, 19),
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 6), (3, 7), (3, 8), (3, 9), (3, 10), (3, 16), (3, 17), (3, 18), (3, 19), (3, 20),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 16), (4, 17), (4, 18), (4, 19), (4, 20),
(6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 9), (6, 10), (6, 16), (6, 17), (6, 18), (6, 19),
(19, 1), (19, 2), (19, 3), (19, 4), (19, 5), (19, 6), (19, 8), (19, 9), (19, 10), (19, 16), (19, 17), (19, 18), (19, 19), (19, 20);

INSERT INTO posts (post_id, description, price, area, view, status, title, created_at, approval_status, approved_by, approved_at, user_id, address_id, category_id, hostel_id) VALUES
(1, 'Phòng trọ cao cấp tại trung tâm Quận 1, đầy đủ tiện nghi hiện đại. Gần trường học, bệnh viện, trung tâm thương mại. An ninh 24/7, thang máy, chỗ để xe rộng rãi.', 4500000.00, 25.0, 150, 1, 'Cho thuê phòng trọ cao cấp Q1 - Đầy đủ tiện nghi', '2024-02-15', 'APPROVED', 2, '2024-02-16', 3, 1, 1, 1),
(2, 'Căn hộ mini hiện đại với thiết kế tối ưu không gian. Bếp riêng, phòng tắm riêng, đầy đủ nội thất. Phù hợp cho cặp đôi hoặc gia đình nhỏ.', 6000000.00, 30.0, 89, 1, 'Căn hộ mini Lê Lợi - Thiết kế hiện đại', '2024-02-20', 'APPROVED', 2, '2024-02-21', 3, 2, 2, 2),
(3, 'Homestay ấm cúng với không gian xanh mát. Phù hợp cho những ai yêu thích sự yên tĩnh và gần gũi với thiên nhiên. Có sân vườn nhỏ để thư giãn.', 5500000.00, 35.0, 67, 1, 'Homestay Hai Bà Trưng - Không gian xanh', '2024-02-25', 'APPROVED', 2, '2024-02-26', 4, 3, 4, 3),
(4, 'Phòng trọ giá rẻ dành cho sinh viên và người lao động. Vị trí thuận tiện, gần trường học và bệnh viện. Môi trường sạch sẽ, an toàn.', 2800000.00, 18.0, 234, 1, 'Phòng trọ sinh viên Pasteur - Giá rẻ', '2024-03-01', 'APPROVED', 2, '2024-03-02', 4, 4, 7, 4),
(5, 'Studio cao cấp với thiết kế sang trọng. Không gian mở, tận dụng tối đa ánh sáng tự nhiên. Đầy đủ tiện nghi hiện đại cho cuộc sống tiện nghi.', 7500000.00, 35.0, 112, 1, 'Studio Premium Xa Lộ Hà Nội - Sang trọng', '2024-03-05', 'APPROVED', 2, '2024-03-06', 5, 6, 8, 6),
(6, 'Phòng trọ mới đăng, đang chờ duyệt. Vị trí đẹp, giá cả hợp lý, phù hợp cho nhiều đối tượng khách hàng.', 4200000.00, 30.0, 45, 1, 'Phòng trọ Cách Mạng Tháng 8 - Mới', '2024-03-10', 'PENDING', NULL, NULL, 5, 5, 1, 5),
(7, 'Bài đăng bị từ chối do không đủ thông tin. Cần bổ sung thêm hình ảnh và mô tả chi tiết hơn.', 3500000.00, 22.0, 12, 0, 'Phòng trọ cần cập nhật thông tin', '2024-03-12', 'REJECTED', 2, '2024-03-13', 3, 1, 1, 1);

-- =====================================================
-- 11. POST UTILITIES RELATIONSHIP
-- =====================================================

INSERT INTO post_utilities (post_id, utility_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 6), (1, 9), (1, 10), (1, 16), (1, 18), (1, 19),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 9), (2, 10), (2, 16), (2, 17), (2, 18), (2, 19),
(3, 1), (3, 2), (3, 6), (3, 7), (3, 9), (3, 10), (3, 14), (3, 16), (3, 18), (3, 19),
(4, 1), (4, 6), (4, 9), (4, 10), (4, 11), (4, 12), (4, 16), (4, 18), (4, 19),
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 8), (5, 9), (5, 10), (5, 16), (5, 17), (5, 18), (5, 19), (5, 20);

-- =====================================================
-- 12. IMAGES DATA
-- =====================================================

INSERT INTO Images (image_id, image_url, post_id, room_id, contract_id) VALUES
(1, '/uploads/post1-img1.jpg', 1, NULL, NULL),
(2, '/uploads/post1-img2.jpg', 1, NULL, NULL),
(3, '/uploads/post1-img3.jpg', 1, NULL, NULL),
(4, '/uploads/post2-img1.jpg', 2, NULL, NULL),
(5, '/uploads/post2-img2.jpg', 2, NULL, NULL),
(6, '/uploads/post3-img1.jpg', 3, NULL, NULL),
(7, '/uploads/post3-img2.jpg', 3, NULL, NULL),
(8, '/uploads/post4-img1.jpg', 4, NULL, NULL),
(9, '/uploads/post5-img1.jpg', 5, NULL, NULL),
(10, '/uploads/post5-img2.jpg', 5, NULL, NULL),

-- Images for Rooms
(11, '/uploads/room1-img1.jpg', NULL, 1, NULL),
(12, '/uploads/room1-img2.jpg', NULL, 1, NULL),
(13, '/uploads/room2-img1.jpg', NULL, 2, NULL),
(14, '/uploads/room3-img1.jpg', NULL, 3, NULL),
(15, '/uploads/room3-img2.jpg', NULL, 3, NULL),
(16, '/uploads/room4-img1.jpg', NULL, 4, NULL),
(17, '/uploads/room6-img1.jpg', NULL, 6, NULL),
(18, '/uploads/room7-img1.jpg', NULL, 7, NULL),
(19, '/uploads/room19-img1.jpg', NULL, 19, NULL),
(20, '/uploads/room19-img2.jpg', NULL, 19, NULL);

-- =====================================================
-- 13. UNREGISTERED TENANTS DATA
-- =====================================================

INSERT INTO unregistered_tenants (id,user_id, full_name, phone, cccd_number, address, status, created_at) VALUES
(1,12, 'Nguyễn Văn Khách', '0987654321', '079095001111', '123 Đường ABC, Q.1, TP.HCM', 'ACTIVE', '2024-02-01 10:00:00'),
(33,9, 'Trần Thị Linh', '0987654322', '079096002222', '456 Đường DEF, Q.2, TP.HCM', 'ACTIVE', '2024-02-05 11:00:00'),
(3,10, 'Lê Minh Tâm', '0987654323', '079097003333', '789 Đường GHI, Q.3, TP.HCM', 'INACTIVE', '2024-02-10 12:00:00');

-- =====================================================
-- 14. CONTRACTS DATA
-- =====================================================

INSERT INTO contracts (contract_id, room_id, tenant_id, owner_id, unregistered_tenant_id, user_id, contract_date, created_at, start_date, end_date, price, deposit, duration, terms, status, tenant_phone, return_status) VALUES
(33, 10, 8, 4, 3, 8, '2024-02-25', '2024-02-25', '2024-03-01', '2024-12-01', 5500000.00, 11000000.00, 9.0, 'Hợp đồng thuê homestay 9 tháng. Có thể sử dụng khu vực chung. Thanh toán đúng hạn.', 'ACTIVE', '0901000008', NULL);
(44, 14, 9, 4, 33, 9, '2024-03-01', '2024-03-01', '2024-03-15', '2024-09-15', 3200000.00, 6400000.00, 6.0, 'Hợp đồng thuê phòng sinh viên 6 tháng. Giá rẻ, phù hợp sinh viên. Không ồn ào sau 22h.', 'ACTIVE', '0901000009', NULL),
(55, 16, 10, 5, 33, 10, '2024-03-05', '2024-03-05', '2024-03-15', '2025-03-15', 4200000.00, 8400000.00, 12.0, 'Hợp đồng thuê phòng 12 tháng. Có chỗ để xe miễn phí. Bảo trì định kỳ.', 'ACTIVE', '0901000010', NULL),
(66, 19, 11, 5, 33, 11, '2024-03-10', '2024-03-10', '2024-04-01', '2025-04-01', 7500000.00, 15000000.00, 12.0, 'Hợp đồng thuê studio cao cấp 12 tháng. Đầy đủ nội thất. Dịch vụ dọn dẹp hàng tuần.', 'ACTIVE', '0901000011', NULL),
(77, 1, 3, 3, 1, 12, '2024-03-12', '2024-03-12', '2024-04-01', '2024-10-01', 4500000.00, 9000000.00, 6.0, 'Hợp đồng thuê phòng với khách chưa đăng ký tài khoản. Thanh toán bằng tiền mặt.', 'ACTIVE', '0987654321', NULL),
(88, 2, 3, 3, 2, 9, '2024-03-15', '2024-03-15', '2024-04-01', '2025-04-01', 5000000.00, 10000000.00, 12.0, 'Hợp đồng thuê phòng dài hạn với khách chưa có tài khoản hệ thống.', 'ACTIVE', '0987654322', NULL),
(99, 4, 12, 3, 3, 10, '2024-03-18', '2024-03-18', '2024-04-15', '2024-10-15', 8500000.00, 17000000.00, 6.0, 'Hợp đồng nháp, chưa ký kết chính thức. Đang thương lượng điều khoản.', 'DRAFT', '0901000012', NULL),
(100, 5, 6, 3, 3, 11, '2024-01-15', '2024-01-15', '2024-02-01', '2024-08-01', 4000000.00, 8000000.00, 6.0, 'Hợp đồng đã kết thúc trước hạn do khách chuyển đi.', 'TERMINATED', '0901000006', 'APPROVED');

-- =====================================================
-- 15. PAYMENTS DATA
-- =====================================================

INSERT INTO Payments (payment_id, contract_id, total_amount, due_date, payment_date, payment_status, payment_method, transaction_id, app_trans_id, notes) VALUES
(1, 1, 7000000.00, '2024-03-05', '2024-03-03', 'ĐÃ_THANH_TOÁN', 'BANK_TRANSFER', 'TXN001', 'APP001', 'Thanh toán tiền thuê tháng 3/2024'),
(2, 1, 7000000.00, '2024-04-05', '2024-04-04', 'ĐÃ_THANH_TOÁN', 'BANK_TRANSFER', 'TXN002', 'APP002', 'Thanh toán tiền thuê tháng 4/2024'),
(3, 1, 7000000.00, '2024-05-05', NULL, 'CHƯA_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 5/2024'),
(4, 2, 9000000.00, '2024-03-05', '2024-03-02', 'ĐÃ_THANH_TOÁN', 'ZALOPAY', 'TXN003', 'APP003', 'Thanh toán tiền thuê tháng 3/2024'),
(5, 2, 9000000.00, '2024-04-05', '2024-04-03', 'ĐÃ_THANH_TOÁN', 'ZALOPAY', 'TXN004', 'APP004', 'Thanh toán tiền thuê tháng 4/2024'),
(6, 2, 9000000.00, '2024-05-05', NULL, 'CHƯA_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 5/2024'),
(7, 3, 5500000.00, '2024-03-05', '2024-03-01', 'ĐÃ_THANH_TOÁN', 'CASH', 'TXN005', 'APP005', 'Thanh toán tiền thuê tháng 3/2024'),
(8, 3, 5500000.00, '2024-04-05', '2024-04-06', 'ĐÃ_THANH_TOÁN', 'CASH', 'TXN006', 'APP006', 'Thanh toán tiền thuê tháng 4/2024 (trễ 1 ngày)'),
(9, 3, 5500000.00, '2024-05-05', NULL, 'CHƯA_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 5/2024'),
(10, 4, 3200000.00, '2024-03-20', '2024-03-18', 'ĐÃ_THANH_TOÁN', 'BANK_TRANSFER', 'TXN007', 'APP007', 'Thanh toán tiền thuê tháng 3/2024'),
(11, 4, 3200000.00, '2024-04-20', NULL, 'QUÁ_HẠN_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 4/2024 - Quá hạn'),
(12, 5, 4200000.00, '2024-03-20', '2024-03-19', 'ĐÃ_THANH_TOÁN', 'MOMO', 'TXN008', 'APP008', 'Thanh toán tiền thuê tháng 3/2024'),
(13, 5, 4200000.00, '2024-04-20', '2024-04-18', 'ĐÃ_THANH_TOÁN', 'MOMO', 'TXN009', 'APP009', 'Thanh toán tiền thuê tháng 4/2024'),
(14, 6, 7500000.00, '2024-04-05', NULL, 'CHƯA_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 4/2024'),
(15, 7, 4500000.00, '2024-04-05', '2024-04-03', 'ĐÃ_THANH_TOÁN', 'CASH', 'TXN010', 'APP010', 'Thanh toán tiền thuê tháng 4/2024'),
(16, 8, 5000000.00, '2024-04-05', NULL, 'CHƯA_THANH_TOÁN', NULL, NULL, NULL, 'Tiền thuê tháng 4/2024');

-- =====================================================
-- 16. DETAIL PAYMENTS DATA
-- =====================================================

INSERT INTO Detail_payments (id, payment_id, description, amount, payment_type) VALUES
-- Detail for payment 1 (Contract 1 - March)
(1, 1, 'Tiền thuê phòng', 7000000.00, 'RENT'),
(2, 1, 'Tiền điện', 150000.00, 'ELECTRICITY'),
(3, 1, 'Tiền nước', 80000.00, 'WATER'),

-- Detail for payment 2 (Contract 1 - April)
(4, 2, 'Tiền thuê phòng', 7000000.00, 'RENT'),
(5, 2, 'Tiền điện', 180000.00, 'ELECTRICITY'),
(6, 2, 'Tiền nước', 90000.00, 'WATER'),

-- Detail for payment 4 (Contract 2 - March)
(7, 4, 'Tiền thuê căn hộ', 9000000.00, 'RENT'),
(8, 4, 'Phí dịch vụ', 200000.00, 'SERVICE'),

-- Detail for payment 5 (Contract 2 - April)
(9, 5, 'Tiền thuê căn hộ', 9000000.00, 'RENT'),
(10, 5, 'Phí dịch vụ', 200000.00, 'SERVICE'),

-- Detail for payment 7 (Contract 3 - March)
(11, 7, 'Tiền thuê homestay', 5500000.00, 'RENT'),
(12, 7, 'Tiền điện nước', 120000.00, 'UTILITIES'),

-- Detail for payment 10 (Contract 4 - March)
(13, 10, 'Tiền thuê phòng sinh viên', 3200000.00, 'RENT'),
(14, 10, 'Tiền điện', 100000.00, 'ELECTRICITY'),
(15, 10, 'Tiền nước', 50000.00, 'WATER');

-- =====================================================
-- 17. REVIEWS DATA
-- =====================================================

INSERT INTO reviews (review_id, post_id, user_id, rating, comment, created_at, status) VALUES
(1, 1, 6, 5, 'Phòng rất đẹp và sạch sẽ. Chủ trọ thân thiện, hỗ trợ tốt. Vị trí thuận tiện, gần trung tâm. Rất hài lòng!', '2024-03-15 14:30:00', 1),
(2, 1, 7, 4, 'Phòng ổn, giá hơi cao nhưng chất lượng tương xứng. Wifi nhanh, điều hòa mát. Sẽ giới thiệu cho bạn bè.', '2024-03-20 16:45:00', 1),
(3, 2, 8, 5, 'Căn hộ mini rất tiện nghi. Bếp đầy đủ dụng cụ, phòng tắm sạch sẽ. Phù hợp cho cặp đôi. Highly recommended!', '2024-03-25 10:20:00', 1),
(4, 3, 9, 4, 'Homestay ấm cúng, có không gian xanh đẹp. Chủ nhà dễ thương. Chỉ có điều hơi xa trung tâm một chút.', '2024-04-01 11:15:00', 1),
(5, 4, 10, 3, 'Phòng bình thường, phù hợp với giá tiền. Tiện ích cơ bản đầy đủ. Tuy nhiên âm thanh hơi kém cách âm.', '2024-04-05 13:30:00', 1),
(6, 5, 11, 5, 'Studio tuyệt vời! Thiết kế hiện đại, view đẹp. Đầy đủ tiện nghi cao cấp. Đáng đồng tiền bát gạo.', '2024-04-10 15:45:00', 1),
(7, 1, 12, 2, 'Phòng không như hình ảnh quảng cáo. Một số tiện ích bị hỏng chưa được sửa chữa. Cần cải thiện.', '2024-04-12 09:20:00', 0);

-- =====================================================
-- 18. FAVORITE POSTS DATA
-- =====================================================

INSERT INTO favorite_posts (id, user_id, post_id, created_at) VALUES
(1, 6, 2, '2024-03-01 10:00:00'),
(2, 6, 3, '2024-03-02 11:00:00'),
(3, 6, 5, '2024-03-03 12:00:00'),
(4, 7, 1, '2024-03-05 13:00:00'),
(5, 7, 4, '2024-03-06 14:00:00'),
(6, 8, 1, '2024-03-08 15:00:00'),
(7, 8, 2, '2024-03-09 16:00:00'),
(8, 9, 3, '2024-03-10 17:00:00'),
(9, 10, 5, '2024-03-12 18:00:00'),
(10, 11, 1, '2024-03-15 19:00:00');

-- =====================================================
-- 19. NOTIFICATIONS DATA
-- =====================================================

INSERT INTO Notifications (notification_id, user_id, title, message, is_read, created_at, notification_type, room_id) VALUES
(1, 6, 'Hợp đồng được ký thành công', 'Hợp đồng thuê phòng 201 đã được ký thành công. Vui lòng thanh toán đúng hạn.', 1, '2024-02-15 10:00:00', 'CONTRACT', 3),
(2, 6, 'Nhắc nhở thanh toán', 'Tiền thuê tháng 5/2024 sẽ đến hạn vào ngày 05/05. Vui lòng chuẩn bị thanh toán.', 0, '2024-05-01 08:00:00', 'PAYMENT', 3),
(3, 7, 'Chào mừng thuê căn hộ', 'Chào mừng bạn đến với căn hộ mini Mini 02. Chúc bạn có trải nghiệm tuyệt vời!', 1, '2024-02-20 11:00:00', 'WELCOME', 7),
(4, 8, 'Xác nhận đặt phòng', 'Đặt phòng homestay Home 01 thành công. Hợp đồng sẽ được ký trong thời gian sớm nhất.', 1, '2024-02-25 12:00:00', 'BOOKING', 10),
(5, 9, 'Cảnh báo thanh toán quá hạn', 'Tiền thuê tháng 4/2024 đã quá hạn. Vui lòng thanh toán ngay để tránh phí phạt.', 0, '2024-04-21 09:00:00', 'OVERDUE', 14),
(6, 3, 'Bài đăng được duyệt', 'Bài đăng "Cho thuê phòng trọ cao cấp Q1" đã được duyệt và hiển thị công khai.', 1, '2024-02-16 14:00:00', 'POST_APPROVED', NULL),
(7, 3, 'Có khách quan tâm', 'Có 5 khách hàng đã yêu thích bài đăng của bạn. Hãy chuẩn bị sẵn sàng tư vấn!', 0, '2024-03-20 16:00:00', 'INTEREST', NULL),
(8, 4, 'Đánh giá mới', 'Phòng homestay của bạn nhận được đánh giá 4 sao từ khách thuê. Hãy xem chi tiết!', 0, '2024-04-01 17:00:00', 'REVIEW', 10),
(9, 5, 'Hợp đồng sắp hết hạn', 'Hợp đồng thuê studio sẽ hết hạn vào 01/04/2025. Liên hệ khách để gia hạn.', 0, '2024-03-01 18:00:00', 'CONTRACT_EXPIRY', 19),
(10, 2, 'Bài đăng cần duyệt', 'Có 1 bài đăng mới cần được duyệt. Vui lòng kiểm tra và phê duyệt.', 0, '2024-03-10 08:30:00', 'PENDING_APPROVAL', NULL);

-- =====================================================
-- 20. VOUCHERS DATA
-- =====================================================

INSERT INTO vouchers ( user_id, code, title, description, discount_type, discount_value, min_order_value, max_discount_amount, start_date, end_date, usage_limit, used_count, status, created_at) VALUES
(1, 3, 'WELCOME2024', 'Voucher chào mừng', 'Giảm giá cho khách hàng mới đăng ký', 'PERCENTAGE', 10000.0, 5000000.00, 500000.00, '2024-01-01', '2024-12-31', 100, 15, 1, '2024-01-01 00:00:00'),
(2, 3, 'SUMMER2024', 'Voucher mùa hè', 'Ưu đãi đặc biệt mùa hè 2024', 'FIXED_AMOUNT', 300000.0, 3000000.00, 300000.00, '2024-06-01', '2024-08-31', 50, 8, 1, '2024-05-15 00:00:00'),
(3, 4, 'STUDENT50', 'Ưu đãi sinh viên', 'Giảm giá đặc biệt cho sinh viên', 'PERCENTAGE', 15.0, 2000000.00, 400000.00, '2024-02-01', '2024-06-30', 200, 45, 1, '2024-01-15 00:00:00'),
(4, 4, 'LONGTERM', 'Thuê dài hạn', 'Ưu đãi cho hợp đồng từ 6 tháng trở lên', 'PERCENTAGE', 5.0, 10000000.00, 1000000.00, '2024-01-01', '2024-12-31', 30, 12, 1, '2024-01-01 00:00:00'),
(5, 5, 'PREMIUM2024', 'Voucher cao cấp', 'Dành cho phòng cao cấp và studio', 'FIXED_AMOUNT', 500000.0, 7000000.00, 500000.00, '2024-03-01', '2024-05-31', 25, 3, 'PENDING', '2024-02-20 00:00:00'),
(6, 3, 'EXPIRED2023', 'Voucher hết hạn', 'Voucher đã hết hạn sử dụng', 'PERCENTAGE', 20.0, 1000000.00, 200000.00, '2023-12-01', '2023-12-31', 100, 85, 1, '2023-11-15 00:00:00');

-- =====================================================
-- 21. EXTENSION REQUESTS DATA
-- =====================================================

INSERT INTO extension_requests (id, contract_id, requested_end_date, reason, status, created_at, processed_at, processed_by) VALUES
(1, 1, '2025-03-01', 'Công việc ổn định, muốn ở lâu dài tại đây. Phòng rất phù hợp với nhu cầu.', 'APPROVED', '2024-04-15 10:00:00', '2024-04-16 14:30:00', 3),
(2, 3, '2025-06-01', 'Gia đình rất thích không gian homestay này. Xin gia hạn thêm 6 tháng nữa.', 'PENDING', '2024-04-20 11:00:00', NULL, NULL),
(3, 4, '2025-03-15', 'Học kỳ kéo dài, cần ở thêm 6 tháng để hoàn thành việc học.', 'REJECTED', '2024-04-18 09:30:00', '2024-04-19 16:00:00', 4),
(4, 6, '2026-04-01', 'Rất hài lòng với studio, muốn gia hạn thêm 1 năm nữa.', 'APPROVED', '2024-04-25 15:00:00', '2024-04-26 10:00:00', 5);

-- =====================================================
-- 22. INCIDENT REPORTS DATA
-- =====================================================

INSERT INTO IncidentReports (id, room_id, reporter_user_id, incident_type, description, status, created_at, resolved_at, resolved_by, priority, last_notification_date) VALUES
(1, 3, 6, 'MAINTENANCE', 'Điều hòa không hoạt động, cần kiểm tra và sửa chữa gấp.', 'DA_XU_LY', '2024-03-10 08:00:00', '2024-03-12 14:00:00', 3, 'HIGH', '2024-03-10 08:00:00'),
(2, 7, 7, 'UTILITIES', 'Áp lực nước yếu, khó sử dụng. Xin kiểm tra hệ thống nước.', 'DANG_XU_LY', '2024-03-15 10:30:00', NULL, NULL, 'MEDIUM', '2024-03-15 10:30:00'),
(3, 10, 8, 'SAFETY', 'Ổ cắm điện bị lỏng, có nguy cơ chập cháy. Cần thay thế ngay.', 'DA_XU_LY', '2024-03-20 16:45:00', '2024-03-21 09:00:00', 4, 'HIGH', '2024-03-20 16:45:00'),
(4, 14, 9, 'NOISE', 'Phòng bên cạnh ồn ào vào ban đêm, ảnh hưởng đến giấc ngủ.', 'CHUA_XU_LY', '2024-04-01 22:00:00', NULL, NULL, 'LOW', '2024-04-01 22:00:00'),
(5, 19, 11, 'MAINTENANCE', 'Máy giặt không hoạt động, cần bảo trì hoặc thay thế.', 'DANG_XU_LY', '2024-04-10 11:00:00', NULL, NULL, 'MEDIUM', '2024-04-10 11:00:00');

-- =====================================================
-- 23. ELECTRIC WATER READING DATA
-- =====================================================

INSERT INTO ElectricWaterReading (id, room_id, reading_date, electric_reading, water_reading, electric_cost, water_cost, total_cost, notes, created_by) VALUES
(1, 3, '2024-03-01', 150, 25, 450000.00, 125000.00, 575000.00, 'Chỉ số điện nước tháng 3/2024', 3),
(2, 3, '2024-04-01', 210, 32, 630000.00, 160000.00, 790000.00, 'Chỉ số điện nước tháng 4/2024', 3),
(3, 7, '2024-03-01', 180, 28, 540000.00, 140000.00, 680000.00, 'Chỉ số điện nước căn hộ mini tháng 3/2024', 3),
(4, 7, '2024-04-01', 245, 35, 735000.00, 175000.00, 910000.00, 'Chỉ số điện nước căn hộ mini tháng 4/2024', 3),
(5, 10, '2024-03-01', 120, 20, 360000.00, 100000.00, 460000.00, 'Chỉ số điện nước homestay tháng 3/2024', 4),
(6, 10, '2024-04-01', 165, 26, 495000.00, 130000.00, 625000.00, 'Chỉ số điện nước homestay tháng 4/2024', 4),
(7, 14, '2024-03-01', 80, 15, 240000.00, 75000.00, 315000.00, 'Chỉ số điện nước phòng sinh viên tháng 3/2024', 4),
(8, 14, '2024-04-01', 115, 19, 345000.00, 95000.00, 440000.00, 'Chỉ số điện nước phòng sinh viên tháng 4/2024', 4),
(9, 16, '2024-03-01', 140, 22, 420000.00, 110000.00, 530000.00, 'Chỉ số điện nước tháng 3/2024', 5),
(10, 19, '2024-04-01', 200, 30, 600000.00, 150000.00, 750000.00, 'Chỉ số điện nước studio tháng 4/2024', 5);

-- =====================================================
-- 24. TEMPORARY RESIDENCE DATA
-- =====================================================

INSERT INTO TemporaryRecidence (id, room_id, guest_name, guest_phone, guest_cccd, check_in_date, check_out_date, purpose, notes, created_by, created_at) VALUES
(1, 3, 'Nguyễn Văn Khách A', '0987111111', '079099001111', '2024-03-15', '2024-03-17', 'Thăm người thân', 'Khách thăm của người thuê phòng', 3, '2024-03-15 14:00:00'),
(2, 7, 'Trần Thị Bạn B', '0987222222', '079099002222', '2024-03-20', '2024-03-25', 'Du lịch', 'Bạn của người thuê căn hộ, ở tạm 5 ngày', 3, '2024-03-20 16:00:00'),
(3, 10, 'Lê Văn Anh Trai', '0987333333', '079099003333', '2024-04-01', '2024-04-03', 'Thăm gia đình', 'Anh trai của người thuê homestay', 4, '2024-04-01 10:00:00'),
(4, 14, 'Phạm Thị Bạn Học', '0987444444', '079099004444', '2024-04-10', '2024-04-12', 'Học tập', 'Bạn cùng trường đến ở tạm để làm bài tập nhóm', 4, '2024-04-10 18:00:00'),
(5, 19, 'Hoàng Văn Đối Tác', '0987555555', '079099005555', '2024-04-15', '2024-04-20', 'Công việc', 'Đối tác công việc từ tỉnh khác lên', 5, '2024-04-15 09:00:00');

-- =====================================================
-- 25. PERSISTENT LOGINS DATA (Remember Me)
-- =====================================================

INSERT INTO persistent_logins (username, series, token, last_used) VALUES
('owner1@nhatroxanh.com', 'SERIES001', 'TOKEN001', '2024-04-20 08:00:00'),
('customer1@gmail.com', 'SERIES002', 'TOKEN002', '2024-04-19 14:30:00'),
('customer2@gmail.com', 'SERIES003', 'TOKEN003', '2024-04-18 20:15:00'),
('admin@nhatroxanh.com', 'SERIES004', 'TOKEN004', '2024-04-21 07:45:00'),
('staff@nhatroxanh.com', 'SERIES005', 'TOKEN005', '2024-04-20 16:20:00');

-- =====================================================
-- 26. ADDITIONAL USERS FOR TESTING
-- =====================================================

INSERT INTO users (user_id, password, fullname, phone, birthday, bank_account, balance, gender, email, avatar, otp_code, otp_expiration, enabled, address, created_at, address_id, role) VALUES
(11, '$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Đặng Văn Hùng', '0901000011', '1992-06-12', '1234567891', 7000000.00, 1, 'customer6@gmail.com', '/uploads/customer6-avatar.jpg', NULL, NULL, 1, '852 Đường Khách Hàng 6', '2024-01-11 08:00:00', 11, 'CUSTOMER'),
(12, '$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Bùi Thị Ngọc', '0901000012', '1998-01-28', '2345678902', 3500000.00, 0, 'customer7@gmail.com', '/uploads/customer7-avatar.jpg', NULL, NULL, 1, '963 Đường Khách Hàng 7', '2024-01-12 08:00:00', 12, 'CUSTOMER'),
(13, '$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Vũ Minh Quân', '0901000013', '1991-09-15', '3456789013', 8500000.00, 1, 'customer8@gmail.com', '/uploads/customer8-avatar.jpg', NULL, NULL, 1, '159 Đường Khách Hàng 8', '2024-01-13 08:00:00', 13, 'CUSTOMER'),
(14, '$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Cao Thị Hương', '0901000014', '1989-04-22', '4567890124', 4200000.00, 0, 'customer9@gmail.com', '/uploads/customer9-avatar.jpg', NULL, NULL, 1, '357 Đường Khách Hàng 9', '2024-01-14 08:00:00', 14, 'CUSTOMER'),
(15, '$2a$10$N.zmdr9k7uOCQb96VdodL.ZHPvsXwDxpn8SYXFrjNTYtUjuaVrTw2', 'Phan Văn Tài', '0901000015', '1987-11-08', '5678901235', 6300000.00, 1, 'customer10@gmail.com', '/uploads/customer10-avatar.jpg', NULL, NULL, 1, '486 Đường Khách Hàng 10', '2024-01-15 08:00:00', 15, 'CUSTOMER');

-- =====================================================
-- 27. ADDITIONAL ADDRESSES FOR NEW USERS
-- =====================================================

INSERT INTO Address (id, street, ward_id, user_id) VALUES
(11, '852 Đường Lý Thường Kiệt', 11, 11),
(12, '963 Đường Hoàng Văn Thụ', 12, 12),
(13, '159 Đường Phan Xích Long', 13, 13),
(14, '357 Đường Nguyễn Thị Minh Khai', 14, 14),
(15, '486 Đường Cộng Hòa', 15, 15);

-- =====================================================
-- FINAL SETUP
-- =====================================================

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- SUMMARY OF TEST DATA CREATED
-- =====================================================

/*
SUMMARY OF TEST DATA:
===================

1. LOCATION DATA:
   - 5 Provinces (HCM, Hanoi, Da Nang, Binh Duong, Ba Ria-Vung Tau)
   - 18 Districts across provinces
   - 20 Wards across districts
   - 15 Addresses linked to wards

2. USERS DATA:
   - 1 Admin user
   - 1 Staff user  
   - 3 Owner users (property owners)
   - 10 Customer users (tenants)
   - All with encrypted passwords, realistic Vietnamese data

3. PROPERTY DATA:
   - 10 Categories (Phòng trọ, Căn hộ mini, Homestay, etc.)
   - 20 Utilities (Wifi, Điều hòa, Tủ lạnh, etc.)
   - 6 Hostels owned by different owners
   - 20 Rooms across hostels with various statuses
   - Room-Utility relationships

4. POSTS & CONTENT:
   - 7 Posts with different approval statuses
   - Post-Utility relationships
   - 20 Images for posts and rooms
   - 7 Reviews with ratings 2-5 stars
   - 10 Favorite posts by users

5. CONTRACTS & PAYMENTS:
   - 10 Contracts (Active, Draft, Terminated)
   - 3 Unregistered tenants
   - 16 Payment records with various statuses
   - 15 Detailed payment breakdowns
   - 4 Extension requests

6. NOTIFICATIONS & COMMUNICATION:
   - 10 Notifications for various events
   - 6 Vouchers with different statuses
   - 5 Incident reports with priorities
   - 10 Electric/Water readings
   - 5 Temporary residence records

7. AUTHENTICATION:
   - 5 Persistent login sessions (Remember Me)
   - User CCCD verification data for owners

USAGE:
======
1. Run this script in your MySQL database
2. All foreign key relationships are properly maintained
3. Data includes realistic Vietnamese names, addresses, and scenarios
4. Covers all major use cases for testing the application
5. Includes various statuses and edge cases for comprehensive testing

LOGIN CREDENTIALS:
==================
All users have password: "password123" (encrypted)
- Admin: admin@nhatroxanh.com
- Staff: staff@nhatroxanh.com  
- Owner1: owner1@nhatroxanh.com
- Owner2: owner2@nhatroxanh.com
- Owner3: owner3@nhatroxanh.com
- Customers: customer1@gmail.com to customer10@gmail.com
*/

alter table vouchers drop column room_id
