-- ============================================================
-- MIGRATION: Thêm hệ thống hướng dẫn sử dụng thuốc & hồ sơ thuốc người dùng
-- Date: 2026-02-04
-- Author: DoAnTotNghiep
-- Description: 
--   - dispense_instructions: Lưu hướng dẫn sử dụng khi hiệu thuốc bán thuốc
--   - app_users: Người dùng mobile app
--   - user_medication_records: Hồ sơ thuốc của người dùng app
--   - medication_reminders: Nhắc nhở uống thuốc
-- ============================================================

USE `BlockChain_DA`;

-- ============================================================
-- BẢNG 1: app_users - Người dùng mobile app
-- ============================================================
DROP TABLE IF EXISTS `app_users`;
CREATE TABLE `app_users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Thông tin đăng nhập (có thể dùng SĐT hoặc email)
    `phone` VARCHAR(20) UNIQUE,
    `email` VARCHAR(100) UNIQUE,
    `password_hash` VARCHAR(255),
    
    -- Thông tin cá nhân
    `full_name` VARCHAR(100),
    `date_of_birth` DATE,
    `gender` ENUM('MALE', 'FEMALE', 'OTHER'),
    `avatar_url` VARCHAR(500),
    
    -- Thông tin y tế (optional)
    `allergies` TEXT COMMENT 'Dị ứng thuốc (JSON array)',
    `medical_conditions` TEXT COMMENT 'Bệnh nền (JSON array)',
    `blood_type` VARCHAR(5),
    
    -- Cài đặt
    `notification_enabled` BOOLEAN DEFAULT TRUE,
    `reminder_sound` VARCHAR(50) DEFAULT 'default',
    `language` VARCHAR(10) DEFAULT 'vi',
    
    -- Trạng thái
    `is_active` BOOLEAN DEFAULT TRUE,
    `is_verified` BOOLEAN DEFAULT FALSE,
    `last_login_at` TIMESTAMP NULL,
    
    -- Metadata
    `fcm_token` VARCHAR(500) COMMENT 'Firebase Cloud Messaging token for push notifications',
    `device_info` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_phone (`phone`),
    INDEX idx_email (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Người dùng mobile app';

-- ============================================================
-- BẢNG 2: dispense_instructions - Hướng dẫn sử dụng thuốc khi bán
-- ============================================================
DROP TABLE IF EXISTS `dispense_instructions`;
CREATE TABLE `dispense_instructions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Liên kết với đơn bán
    `movement_id` BIGINT COMMENT 'FK -> product_item_movements (type=SALE)',
    `product_item_id` BIGINT NOT NULL COMMENT 'FK -> product_items',
    `pharmacy_id` BIGINT NOT NULL COMMENT 'FK -> pharma_companies (pharmacy)',
    
    -- Thông tin khách hàng (định danh)
    `customer_name` VARCHAR(100),
    `customer_phone` VARCHAR(20),
    `customer_app_user_id` BIGINT COMMENT 'FK -> app_users (nếu khách đã có tài khoản)',
    
    -- Thông tin thuốc (cache để truy vấn nhanh)
    `drug_name` VARCHAR(255) NOT NULL,
    `batch_number` VARCHAR(100),
    `item_code` VARCHAR(100),
    
    -- Hướng dẫn sử dụng
    `dosage` VARCHAR(50) DEFAULT '1 viên' COMMENT 'Liều lượng mỗi lần: 1 viên, 5ml, 1 gói...',
    `frequency` INT DEFAULT 3 COMMENT 'Số lần uống/ngày (1, 2, 3, 4)',
    `meal_relation` ENUM('BEFORE', 'AFTER', 'WITH', 'ANY') DEFAULT 'AFTER' 
        COMMENT 'Trước/Sau/Trong bữa ăn',
    `specific_times` VARCHAR(100) DEFAULT '08:00,12:00,20:00' 
        COMMENT 'Giờ uống cụ thể (comma separated)',
    `duration_days` INT DEFAULT 7 COMMENT 'Số ngày dùng thuốc',
    `special_notes` TEXT COMMENT 'Ghi chú đặc biệt từ dược sĩ',
    
    -- Cảnh báo
    `warnings` TEXT COMMENT 'Cảnh báo sử dụng (JSON array)',
    `contraindications` TEXT COMMENT 'Chống chỉ định (JSON array)',
    
    -- Thông tin dược sĩ
    `pharmacist_name` VARCHAR(100),
    `pharmacist_license` VARCHAR(50),
    
    -- Sale info
    `sale_price` DECIMAL(15,2) COMMENT 'Giá bán',
    `dispensed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm bán',
    
    -- Metadata
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_product_item (`product_item_id`),
    INDEX idx_customer_phone (`customer_phone`),
    INDEX idx_pharmacy (`pharmacy_id`),
    INDEX idx_app_user (`customer_app_user_id`),
    INDEX idx_item_code (`item_code`),
    INDEX idx_dispensed_at (`dispensed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Hướng dẫn sử dụng thuốc khi hiệu thuốc bán cho khách';

-- ============================================================
-- BẢNG 3: user_medication_records - Hồ sơ thuốc của người dùng App
-- ============================================================
DROP TABLE IF EXISTS `user_medication_records`;
CREATE TABLE `user_medication_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Liên kết với user app
    `user_id` BIGINT NOT NULL COMMENT 'FK -> app_users',
    
    -- Liên kết với thuốc đã mua
    `product_item_id` BIGINT COMMENT 'FK -> product_items',
    `dispense_instruction_id` BIGINT COMMENT 'FK -> dispense_instructions',
    
    -- Thông tin thuốc (cache để hiển thị nhanh)
    `drug_name` VARCHAR(255) NOT NULL,
    `batch_number` VARCHAR(50),
    `item_code` VARCHAR(100),
    `manufacturer` VARCHAR(255),
    `expiry_date` DATE,
    
    -- Lịch uống thuốc (copy từ dispense_instructions hoặc user tự nhập)
    `dosage` VARCHAR(50) DEFAULT '1 viên',
    `frequency` INT DEFAULT 3,
    `meal_relation` ENUM('BEFORE', 'AFTER', 'WITH', 'ANY') DEFAULT 'AFTER',
    `reminder_times` VARCHAR(100) DEFAULT '08:00,12:00,20:00',
    `start_date` DATE NOT NULL,
    `end_date` DATE,
    
    -- Tiến độ
    `total_doses` INT COMMENT 'Tổng số liều cần uống',
    `taken_doses` INT DEFAULT 0 COMMENT 'Số liều đã uống',
    `missed_doses` INT DEFAULT 0 COMMENT 'Số liều bỏ lỡ',
    `adherence_rate` DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE WHEN `total_doses` > 0 THEN (`taken_doses` / `total_doses`) * 100 ELSE 0 END
    ) STORED COMMENT 'Tỉ lệ tuân thủ (%)',
    
    -- Trạng thái
    `is_active` BOOLEAN DEFAULT TRUE COMMENT 'Đang dùng thuốc này',
    `is_completed` BOOLEAN DEFAULT FALSE COMMENT 'Đã hoàn thành liệu trình',
    `is_paused` BOOLEAN DEFAULT FALSE COMMENT 'Tạm dừng',
    `pause_reason` VARCHAR(255),
    
    -- Ghi chú
    `notes` TEXT,
    `special_instructions` TEXT COMMENT 'Hướng dẫn đặc biệt từ dược sĩ',
    
    -- Nguồn gốc
    `pharmacy_name` VARCHAR(100) COMMENT 'Hiệu thuốc đã bán',
    `pharmacy_id` BIGINT,
    `purchased_at` TIMESTAMP COMMENT 'Thời điểm mua',
    
    -- Metadata
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_user (`user_id`),
    INDEX idx_active (`user_id`, `is_active`),
    INDEX idx_product_item (`product_item_id`),
    INDEX idx_dispense (`dispense_instruction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Hồ sơ thuốc của người dùng mobile app';

-- ============================================================
-- BẢNG 4: medication_reminders - Lịch sử nhắc nhở & uống thuốc
-- ============================================================
DROP TABLE IF EXISTS `medication_reminders`;
CREATE TABLE `medication_reminders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    `record_id` BIGINT NOT NULL COMMENT 'FK -> user_medication_records',
    
    -- Thời gian lên lịch
    `scheduled_date` DATE NOT NULL,
    `scheduled_time` TIME NOT NULL,
    
    -- Trạng thái
    `status` ENUM('PENDING', 'NOTIFIED', 'TAKEN', 'MISSED', 'SKIPPED') DEFAULT 'PENDING',
    
    -- Khi đã uống
    `taken_at` TIMESTAMP NULL,
    `response_time_minutes` INT COMMENT 'Thời gian phản hồi (phút) sau khi nhắc',
    
    -- Ghi chú
    `notes` TEXT COMMENT 'Ghi chú khi uống (triệu chứng, phản ứng...)',
    `skip_reason` VARCHAR(255) COMMENT 'Lý do bỏ qua',
    
    -- Push notification
    `notification_sent` BOOLEAN DEFAULT FALSE,
    `notification_sent_at` TIMESTAMP NULL,
    
    -- Metadata
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_record (`record_id`),
    INDEX idx_scheduled (`scheduled_date`, `scheduled_time`),
    INDEX idx_user_date (`record_id`, `scheduled_date`),
    INDEX idx_status (`status`),
    INDEX idx_pending (`status`, `scheduled_date`, `scheduled_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Lịch sử nhắc nhở uống thuốc';

-- ============================================================
-- VIEW: Thống kê tuân thủ uống thuốc của user
-- ============================================================
DROP VIEW IF EXISTS `v_user_medication_stats`;
CREATE VIEW `v_user_medication_stats` AS
SELECT 
    umr.user_id,
    au.full_name AS user_name,
    au.phone AS user_phone,
    COUNT(DISTINCT umr.id) AS total_medications,
    SUM(CASE WHEN umr.is_active = TRUE THEN 1 ELSE 0 END) AS active_medications,
    SUM(CASE WHEN umr.is_completed = TRUE THEN 1 ELSE 0 END) AS completed_medications,
    SUM(umr.taken_doses) AS total_taken_doses,
    SUM(umr.missed_doses) AS total_missed_doses,
    ROUND(AVG(umr.adherence_rate), 2) AS avg_adherence_rate
FROM user_medication_records umr
JOIN app_users au ON umr.user_id = au.id
GROUP BY umr.user_id, au.full_name, au.phone;

-- ============================================================
-- VIEW: Nhắc nhở hôm nay
-- ============================================================
DROP VIEW IF EXISTS `v_today_reminders`;
CREATE VIEW `v_today_reminders` AS
SELECT 
    mr.id AS reminder_id,
    mr.record_id,
    mr.scheduled_date,
    mr.scheduled_time,
    mr.status,
    umr.user_id,
    umr.drug_name,
    umr.dosage,
    umr.meal_relation,
    au.full_name AS user_name,
    au.fcm_token
FROM medication_reminders mr
JOIN user_medication_records umr ON mr.record_id = umr.id
JOIN app_users au ON umr.user_id = au.id
WHERE mr.scheduled_date = CURDATE()
  AND umr.is_active = TRUE
ORDER BY mr.scheduled_time;

-- ============================================================
-- TRIGGER: Tự động cập nhật thống kê khi đánh dấu đã uống
-- ============================================================
DROP TRIGGER IF EXISTS `trg_update_medication_stats`;
DELIMITER //
CREATE TRIGGER `trg_update_medication_stats`
AFTER UPDATE ON `medication_reminders`
FOR EACH ROW
BEGIN
    -- Khi status chuyển thành TAKEN
    IF NEW.status = 'TAKEN' AND OLD.status != 'TAKEN' THEN
        UPDATE user_medication_records
        SET taken_doses = taken_doses + 1,
            updated_at = NOW()
        WHERE id = NEW.record_id;
    END IF;
    
    -- Khi status chuyển thành MISSED
    IF NEW.status = 'MISSED' AND OLD.status != 'MISSED' THEN
        UPDATE user_medication_records
        SET missed_doses = missed_doses + 1,
            updated_at = NOW()
        WHERE id = NEW.record_id;
    END IF;
END//
DELIMITER ;

-- ============================================================
-- STORED PROCEDURE: Tạo reminders cho một medication record
-- ============================================================
DROP PROCEDURE IF EXISTS `sp_generate_medication_reminders`;
DELIMITER //
CREATE PROCEDURE `sp_generate_medication_reminders`(
    IN p_record_id BIGINT
)
BEGIN
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;
    DECLARE v_reminder_times VARCHAR(100);
    DECLARE v_current_date DATE;
    DECLARE v_time_str VARCHAR(10);
    DECLARE v_pos INT;
    DECLARE v_total_doses INT DEFAULT 0;
    
    -- Get record info
    SELECT start_date, end_date, reminder_times
    INTO v_start_date, v_end_date, v_reminder_times
    FROM user_medication_records
    WHERE id = p_record_id;
    
    -- Loop through each day
    SET v_current_date = v_start_date;
    
    WHILE v_current_date <= v_end_date DO
        -- Parse reminder times and create reminders
        SET v_pos = 1;
        
        -- Split by comma and insert reminders
        -- Simple approach: handle up to 4 times per day
        IF LOCATE(',', v_reminder_times) > 0 THEN
            -- Time 1
            SET v_time_str = TRIM(SUBSTRING_INDEX(v_reminder_times, ',', 1));
            INSERT INTO medication_reminders (record_id, scheduled_date, scheduled_time, status)
            VALUES (p_record_id, v_current_date, STR_TO_DATE(v_time_str, '%H:%i'), 'PENDING');
            SET v_total_doses = v_total_doses + 1;
            
            -- Time 2
            SET v_time_str = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(v_reminder_times, ',', 2), ',', -1));
            IF v_time_str != '' THEN
                INSERT INTO medication_reminders (record_id, scheduled_date, scheduled_time, status)
                VALUES (p_record_id, v_current_date, STR_TO_DATE(v_time_str, '%H:%i'), 'PENDING');
                SET v_total_doses = v_total_doses + 1;
            END IF;
            
            -- Time 3
            SET v_time_str = TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(v_reminder_times, ',', 3), ',', -1));
            IF v_time_str != '' AND LOCATE(',', v_reminder_times, LOCATE(',', v_reminder_times) + 1) > 0 THEN
                INSERT INTO medication_reminders (record_id, scheduled_date, scheduled_time, status)
                VALUES (p_record_id, v_current_date, STR_TO_DATE(v_time_str, '%H:%i'), 'PENDING');
                SET v_total_doses = v_total_doses + 1;
            END IF;
            
            -- Time 4
            SET v_time_str = TRIM(SUBSTRING_INDEX(v_reminder_times, ',', -1));
            IF LENGTH(v_reminder_times) - LENGTH(REPLACE(v_reminder_times, ',', '')) >= 3 THEN
                INSERT INTO medication_reminders (record_id, scheduled_date, scheduled_time, status)
                VALUES (p_record_id, v_current_date, STR_TO_DATE(v_time_str, '%H:%i'), 'PENDING');
                SET v_total_doses = v_total_doses + 1;
            END IF;
        ELSE
            -- Single time
            INSERT INTO medication_reminders (record_id, scheduled_date, scheduled_time, status)
            VALUES (p_record_id, v_current_date, STR_TO_DATE(v_reminder_times, '%H:%i'), 'PENDING');
            SET v_total_doses = v_total_doses + 1;
        END IF;
        
        SET v_current_date = DATE_ADD(v_current_date, INTERVAL 1 DAY);
    END WHILE;
    
    -- Update total_doses in record
    UPDATE user_medication_records
    SET total_doses = v_total_doses
    WHERE id = p_record_id;
    
    SELECT CONCAT('Generated ', v_total_doses, ' reminders for record ', p_record_id) AS result;
END//
DELIMITER ;

-- ============================================================
-- INSERT SAMPLE DATA (Optional - for testing)
-- ============================================================

-- Sample app user
INSERT INTO `app_users` (`id`, `phone`, `full_name`, `is_active`, `notification_enabled`)
VALUES (1, '0901234567', 'Nguyễn Văn Test', TRUE, TRUE);

-- Note: dispense_instructions will be created when pharmacy sells items

SELECT 'Migration completed successfully!' AS status;
