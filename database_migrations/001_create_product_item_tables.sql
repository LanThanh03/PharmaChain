-- ============================================================
-- MIGRATION 001: Create Product Item Tracking Tables
-- Description: Thêm 3 bảng mới để tracking sản phẩm riêng lẻ
-- Date: 2025-10-03
-- ============================================================

-- --------------------------------------------------------
-- Bảng 1: product_items (Sản phẩm riêng lẻ)
-- --------------------------------------------------------
CREATE TABLE `product_items` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `item_code` varchar(100) NOT NULL COMMENT 'Mã sản phẩm duy nhất, VD: PARA-BATCH001-0001',
  `batch_id` bigint(20) NOT NULL COMMENT 'FK to drug_batches',
  `drug_product_id` bigint(20) NOT NULL COMMENT 'FK to drug_products',
  
  -- QR Code information
  `qr_code_data` varchar(500) DEFAULT NULL COMMENT 'Dữ liệu trong QR (URL hoặc JSON)',
  `qr_image_path` varchar(500) DEFAULT NULL COMMENT 'Đường dẫn file QR image (S3/MinIO)',
  `qr_generated_at` datetime(6) DEFAULT NULL,
  
  -- Status tracking
  `current_status` enum(
    'MANUFACTURED',
    'IN_WAREHOUSE',
    'IN_TRANSIT',
    'DELIVERED',
    'SOLD',
    'EXPIRED',
    'RECALLED',
    'DAMAGED'
  ) NOT NULL DEFAULT 'MANUFACTURED' COMMENT 'Trạng thái hiện tại của sản phẩm',
  
  -- Ownership tracking
  `current_owner_id` bigint(20) DEFAULT NULL COMMENT 'ID công ty đang sở hữu',
  `current_owner_type` enum(
    'MANUFACTURER',
    'DISTRIBUTOR',
    'PHARMACY',
    'CONSUMER'
  ) DEFAULT NULL COMMENT 'Loại chủ sở hữu',
  
  -- Product dates (denormalized từ batch để query nhanh)
  `manufacture_date` datetime(6) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  
  -- Blockchain tracking
  `blockchain_token_id` bigint(20) DEFAULT NULL COMMENT 'Token ID trên blockchain (nếu dùng NFT)',
  `blockchain_merkle_proof` text DEFAULT NULL COMMENT 'Merkle proof để verify',
  `is_blockchain_synced` bit(1) NOT NULL DEFAULT b'0',
  
  -- Metadata
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_code` (`item_code`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_drug_product_id` (`drug_product_id`),
  KEY `idx_current_status` (`current_status`),
  KEY `idx_qr_code_data` (`qr_code_data`),
  KEY `idx_batch_status` (`batch_id`, `current_status`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_owner` (`current_owner_id`, `current_owner_type`),
  
  CONSTRAINT `fk_product_items_batch` 
    FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) 
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_product_items_product` 
    FOREIGN KEY (`drug_product_id`) REFERENCES `drug_products` (`id`) 
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Bảng quản lý từng sản phẩm riêng lẻ với QR code tracking';

-- --------------------------------------------------------
-- Bảng 2: product_item_movements (Lịch sử di chuyển)
-- --------------------------------------------------------
CREATE TABLE `product_item_movements` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) NOT NULL COMMENT 'FK to product_items',
  `batch_id` bigint(20) NOT NULL COMMENT 'Denormalized để query nhanh theo lô',
  
  -- Movement details
  `movement_type` enum(
    'MANUFACTURE',
    'TRANSFER',
    'SHIP',
    'RECEIVE',
    'SALE',
    'RETURN',
    'RECALL',
    'DAMAGE',
    'EXPIRE'
  ) NOT NULL COMMENT 'Loại di chuyển/sự kiện',
  
  -- From location
  `from_company_id` bigint(20) DEFAULT NULL COMMENT 'Từ công ty nào (NULL nếu MANUFACTURE)',
  `from_company_type` enum('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY', 'CONSUMER') DEFAULT NULL,
  `from_company_name` varchar(255) DEFAULT NULL COMMENT 'Denormalized cho hiển thị',
  `from_address_detail` text DEFAULT NULL,
  
  -- To location
  `to_company_id` bigint(20) NOT NULL COMMENT 'Đến công ty nào',
  `to_company_type` enum('MANUFACTURER', 'DISTRIBUTOR', 'PHARMACY', 'CONSUMER') NOT NULL,
  `to_company_name` varchar(255) DEFAULT NULL COMMENT 'Denormalized cho hiển thị',
  `to_address_detail` text DEFAULT NULL,
  
  -- Related records
  `shipment_id` bigint(20) DEFAULT NULL COMMENT 'FK to drug_shipments (nếu có)',
  `related_transaction_id` varchar(100) DEFAULT NULL COMMENT 'Mã đơn hàng/giao dịch',
  
  -- Tracking
  `movement_timestamp` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `location_lat` decimal(10, 7) DEFAULT NULL COMMENT 'GPS latitude',
  `location_lng` decimal(10, 7) DEFAULT NULL COMMENT 'GPS longitude',
  
  -- Verification
  `verified_by` varchar(100) DEFAULT NULL COMMENT 'User thực hiện',
  `verification_method` enum('QR_SCAN', 'MANUAL', 'AUTO') DEFAULT 'AUTO',
  
  -- Blockchain
  `blockchain_tx_hash` varchar(66) DEFAULT NULL,
  `blockchain_block_number` bigint(20) DEFAULT NULL,
  `is_blockchain_synced` bit(1) NOT NULL DEFAULT b'0',
  
  -- Metadata
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_movement_timestamp` (`movement_timestamp`),
  KEY `idx_item_timestamp` (`item_id`, `movement_timestamp` DESC),
  KEY `idx_from_company` (`from_company_id`, `movement_type`),
  KEY `idx_to_company` (`to_company_id`, `movement_type`),
  KEY `idx_shipment` (`shipment_id`),
  KEY `idx_movement_type` (`movement_type`),
  
  CONSTRAINT `fk_movements_item` 
    FOREIGN KEY (`item_id`) REFERENCES `product_items` (`id`) 
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_movements_batch` 
    FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) 
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_movements_shipment` 
    FOREIGN KEY (`shipment_id`) REFERENCES `drug_shipments` (`id`) 
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Lịch sử di chuyển của từng sản phẩm (audit trail)';

-- --------------------------------------------------------
-- Bảng 3: product_item_verifications (Lịch sử quét QR)
-- --------------------------------------------------------
CREATE TABLE `product_item_verifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `item_id` bigint(20) NOT NULL COMMENT 'FK to product_items',
  
  -- Scanner information
  `scanner_type` enum(
    'MANUFACTURER',
    'DISTRIBUTOR',
    'PHARMACY',
    'CONSUMER',
    'INSPECTOR',
    'ANONYMOUS'
  ) NOT NULL COMMENT 'Ai quét QR',
  `scanner_id` varchar(100) DEFAULT NULL COMMENT 'User ID (nếu đăng nhập)',
  `scanner_name` varchar(255) DEFAULT NULL,
  
  -- Device & Location
  `scanner_device_info` json DEFAULT NULL COMMENT 'Device info: OS, browser, app version',
  `scanner_location` varchar(500) DEFAULT NULL COMMENT 'Địa điểm quét',
  `location_lat` decimal(10, 7) DEFAULT NULL,
  `location_lng` decimal(10, 7) DEFAULT NULL,
  
  -- Verification result
  `verification_result` enum(
    'AUTHENTIC',
    'SUSPICIOUS',
    'COUNTERFEIT',
    'EXPIRED',
    'RECALLED'
  ) NOT NULL DEFAULT 'AUTHENTIC',
  `verification_details` json DEFAULT NULL COMMENT 'Chi tiết xác thực',
  /*
  Example verification_details:
  {
    "blockchain_verified": true,
    "ownership_chain_valid": true,
    "expiry_status": "valid",
    "recall_status": "not_recalled",
    "scan_location_match": true,
    "suspicious_patterns": []
  }
  */
  
  -- Security tracking
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `scan_timestamp` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  
  -- Blockchain verification
  `blockchain_verified` bit(1) DEFAULT b'0',
  `blockchain_query_time_ms` int(11) DEFAULT NULL COMMENT 'Thời gian query blockchain',
  
  -- Metadata
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  
  PRIMARY KEY (`id`),
  KEY `idx_item_id` (`item_id`),
  KEY `idx_scan_timestamp` (`scan_timestamp`),
  KEY `idx_scanner_type_time` (`scanner_type`, `scan_timestamp`),
  KEY `idx_verification_result` (`verification_result`),
  KEY `idx_scanner` (`scanner_id`, `scanner_type`),
  KEY `idx_item_scanner` (`item_id`, `scanner_type`),
  
  CONSTRAINT `fk_verifications_item` 
    FOREIGN KEY (`item_id`) REFERENCES `product_items` (`id`) 
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
COMMENT='Lịch sử quét QR code (security & analytics)';

-- --------------------------------------------------------
-- Cập nhật bảng drug_shipments
-- --------------------------------------------------------
ALTER TABLE `drug_shipments`
  ADD COLUMN `shipping_method` enum('FULL_BATCH', 'PARTIAL_BATCH', 'ITEM_LEVEL') 
    DEFAULT 'FULL_BATCH' 
    COMMENT 'Phương thức giao hàng' AFTER `shipment_status`,
  ADD COLUMN `items_json` json DEFAULT NULL 
    COMMENT 'Danh sách item_id trong shipment này' AFTER `shipping_method`,
  ADD COLUMN `actual_items_count` int(11) DEFAULT 0 
    COMMENT 'Số lượng items thực tế' AFTER `items_json`,
  ADD COLUMN `blockchain_merkle_root` varchar(66) DEFAULT NULL 
    COMMENT 'Merkle root của danh sách items' AFTER `receive_tx_hash`;

-- --------------------------------------------------------
-- Thêm index cho drug_shipments
-- --------------------------------------------------------
ALTER TABLE `drug_shipments`
  ADD KEY `idx_shipping_method` (`shipping_method`),
  ADD KEY `idx_actual_items_count` (`actual_items_count`);

-- --------------------------------------------------------
-- Tạo Views để query dễ dàng hơn
-- --------------------------------------------------------

-- View: Full product item information
CREATE OR REPLACE VIEW `v_product_items_full` AS
SELECT 
  pi.id AS item_id,
  pi.item_code,
  pi.current_status,
  pi.current_owner_id,
  pi.current_owner_type,
  pi.qr_code_data,
  pi.manufacture_date AS item_manufacture_date,
  pi.expiry_date AS item_expiry_date,
  pi.is_blockchain_synced,
  pi.created_at AS item_created_at,
  
  -- Batch info
  db.id AS batch_id,
  db.batch_number,
  db.batch_id AS blockchain_batch_id,
  db.quantity AS batch_quantity,
  db.status AS batch_status,
  db.manufacturer AS batch_manufacturer,
  db.storage_conditions,
  
  -- Product info
  dp.id AS product_id,
  dp.name AS product_name,
  dp.active_ingredient,
  dp.dosage,
  dp.dosage_form,
  dp.category,
  dp.manufacturer_id,
  dp.registration_number,
  dp.description AS product_description,
  
  -- Calculated fields
  CASE 
    WHEN pi.expiry_date < NOW() THEN 'EXPIRED'
    WHEN pi.expiry_date < DATE_ADD(NOW(), INTERVAL 3 MONTH) THEN 'EXPIRING_SOON'
    ELSE 'VALID'
  END AS expiry_status,
  
  DATEDIFF(pi.expiry_date, NOW()) AS days_until_expiry
  
FROM product_items pi
JOIN drug_batches db ON pi.batch_id = db.id
JOIN drug_products dp ON pi.drug_product_id = dp.id;

-- View: Item movement history summary
CREATE OR REPLACE VIEW `v_item_movement_summary` AS
SELECT 
  pi.id AS item_id,
  pi.item_code,
  pi.current_status,
  COUNT(pim.id) AS total_movements,
  MIN(pim.movement_timestamp) AS first_movement,
  MAX(pim.movement_timestamp) AS last_movement,
  
  -- Count by movement type
  SUM(CASE WHEN pim.movement_type = 'SHIP' THEN 1 ELSE 0 END) AS ship_count,
  SUM(CASE WHEN pim.movement_type = 'RECEIVE' THEN 1 ELSE 0 END) AS receive_count,
  SUM(CASE WHEN pim.movement_type = 'SALE' THEN 1 ELSE 0 END) AS sale_count,
  
  -- Blockchain status
  SUM(CASE WHEN pim.is_blockchain_synced = b'1' THEN 1 ELSE 0 END) AS blockchain_synced_count
  
FROM product_items pi
LEFT JOIN product_item_movements pim ON pi.id = pim.item_id
GROUP BY pi.id, pi.item_code, pi.current_status;

-- View: Item verification summary
CREATE OR REPLACE VIEW `v_item_verification_summary` AS
SELECT 
  pi.id AS item_id,
  pi.item_code,
  pi.current_status,
  COUNT(piv.id) AS total_scans,
  
  -- Count by scanner type
  SUM(CASE WHEN piv.scanner_type = 'CONSUMER' THEN 1 ELSE 0 END) AS consumer_scans,
  SUM(CASE WHEN piv.scanner_type = 'PHARMACY' THEN 1 ELSE 0 END) AS pharmacy_scans,
  SUM(CASE WHEN piv.scanner_type = 'DISTRIBUTOR' THEN 1 ELSE 0 END) AS distributor_scans,
  
  -- Count by result
  SUM(CASE WHEN piv.verification_result = 'AUTHENTIC' THEN 1 ELSE 0 END) AS authentic_scans,
  SUM(CASE WHEN piv.verification_result = 'SUSPICIOUS' THEN 1 ELSE 0 END) AS suspicious_scans,
  
  MIN(piv.scan_timestamp) AS first_scan,
  MAX(piv.scan_timestamp) AS last_scan
  
FROM product_items pi
LEFT JOIN product_item_verifications piv ON pi.id = piv.item_id
GROUP BY pi.id, pi.item_code, pi.current_status;

-- --------------------------------------------------------
-- Tạo stored procedures hỗ trợ
-- --------------------------------------------------------

DELIMITER $$

-- Procedure: Generate items for a batch
CREATE PROCEDURE `sp_generate_items_for_batch`(
  IN p_batch_id BIGINT,
  IN p_quantity INT,
  IN p_prefix VARCHAR(50)
)
BEGIN
  DECLARE v_counter INT DEFAULT 1;
  DECLARE v_drug_product_id BIGINT;
  DECLARE v_manufacture_date DATETIME(6);
  DECLARE v_expiry_date DATETIME(6);
  DECLARE v_item_code VARCHAR(100);
  
  -- Get batch info
  SELECT 
    db.drug_name, 
    db.manufacture_timestamp, 
    db.expiry_date
  INTO 
    @drug_name,
    v_manufacture_date,
    v_expiry_date
  FROM drug_batches db
  WHERE db.id = p_batch_id;
  
  -- Get drug_product_id (assuming it exists)
  SELECT dp.id INTO v_drug_product_id
  FROM drug_products dp
  WHERE dp.name = @drug_name
  LIMIT 1;
  
  -- Generate items
  WHILE v_counter <= p_quantity DO
    SET v_item_code = CONCAT(
      p_prefix, 
      '-BATCH', LPAD(p_batch_id, 6, '0'),
      '-', LPAD(v_counter, 4, '0')
    );
    
    INSERT INTO product_items (
      item_code,
      batch_id,
      drug_product_id,
      current_status,
      current_owner_type,
      manufacture_date,
      expiry_date,
      created_at
    ) VALUES (
      v_item_code,
      p_batch_id,
      v_drug_product_id,
      'MANUFACTURED',
      'MANUFACTURER',
      v_manufacture_date,
      v_expiry_date,
      NOW(6)
    );
    
    SET v_counter = v_counter + 1;
  END WHILE;
  
  SELECT CONCAT('Generated ', p_quantity, ' items for batch ', p_batch_id) AS result;
END$$

-- Procedure: Get item journey (full history)
CREATE PROCEDURE `sp_get_item_journey`(
  IN p_item_code VARCHAR(100)
)
BEGIN
  -- Get item info
  SELECT * FROM v_product_items_full
  WHERE item_code = p_item_code;
  
  -- Get movement history
  SELECT 
    pim.*,
    CONCAT(pim.from_company_name, ' → ', pim.to_company_name) AS movement_description
  FROM product_item_movements pim
  JOIN product_items pi ON pim.item_id = pi.id
  WHERE pi.item_code = p_item_code
  ORDER BY pim.movement_timestamp ASC;
  
  -- Get verification history
  SELECT * FROM product_item_verifications piv
  JOIN product_items pi ON piv.item_id = pi.id
  WHERE pi.item_code = p_item_code
  ORDER BY piv.scan_timestamp DESC
  LIMIT 20;
END$$

DELIMITER ;

-- --------------------------------------------------------
-- Insert sample data (optional, for testing)
-- --------------------------------------------------------

-- Uncomment below to insert test data
/*
-- Generate 10 items for batch 1
CALL sp_generate_items_for_batch(1, 10, 'PARA');

-- Create a movement for item 1
INSERT INTO product_item_movements (
  item_id, batch_id, movement_type,
  from_company_id, from_company_type, from_company_name,
  to_company_id, to_company_type, to_company_name,
  movement_timestamp, verification_method
) VALUES (
  1, 1, 'MANUFACTURE',
  NULL, NULL, NULL,
  1, 'MANUFACTURER', 'Công ty Dược phẩm ABC',
  NOW(6), 'AUTO'
);

-- Create a verification
INSERT INTO product_item_verifications (
  item_id, scanner_type, scanner_location,
  verification_result, scan_timestamp
) VALUES (
  1, 'MANUFACTURER', 'Nhà máy TP.HCM',
  'AUTHENTIC', NOW(6)
);
*/

-- ============================================================
-- END OF MIGRATION
-- ============================================================

