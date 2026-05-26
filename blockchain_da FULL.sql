-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Máy chủ: 127.0.0.1
-- Thời gian đã tạo: Th3 29, 2026 lúc 08:11 PM
-- Phiên bản máy phục vụ: 10.4.32-MariaDB
-- Phiên bản PHP: 8.1.23

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Cơ sở dữ liệu: `blockchain_da`
--
CREATE DATABASE BlockChain_DA;
USE BlockChain_DA;

DELIMITER $$
--
-- Thủ tục
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_distributor_receive_shipment` (IN `p_shipment_id` BIGINT, IN `p_distributor_id` BIGINT)   BEGIN
    DECLARE v_batch_id BIGINT;
    DECLARE v_quantity INT;
    DECLARE v_from_company_id BIGINT;
    DECLARE v_exists INT;

    -- Get shipment details
    SELECT batch_id, quantity, from_company_id
    INTO v_batch_id, v_quantity, v_from_company_id
    FROM drug_shipments
    WHERE id = p_shipment_id AND to_company_id = p_distributor_id;

    -- Check if inventory record exists
    SELECT COUNT(*) INTO v_exists
    FROM distributor_inventory
    WHERE distributor_id = p_distributor_id AND batch_id = v_batch_id;

    IF v_exists > 0 THEN
        -- Update existing record
        UPDATE distributor_inventory
        SET quantity = quantity + v_quantity,
            received_shipment_id = p_shipment_id,
            received_date = NOW(),
            updated_at = NOW()
        WHERE distributor_id = p_distributor_id AND batch_id = v_batch_id;
    ELSE
        -- Insert new record
        INSERT INTO distributor_inventory (
            distributor_id, batch_id, drug_name, manufacturer, batch_number,
            quantity, manufacture_date, expiry_date, qr_code,
            received_from_company_id, received_shipment_id, received_date, received_quantity
        )
        SELECT
            p_distributor_id, db.id, db.drug_name, db.manufacturer, db.batch_number,
            v_quantity, db.manufacture_timestamp, db.expiry_date, db.qr_code,
            v_from_company_id, p_shipment_id, NOW(), v_quantity
        FROM drug_batches db
        WHERE db.id = v_batch_id;
    END IF;

    -- Update shipment status
    UPDATE drug_shipments
    SET shipment_status = 'DELIVERED',
        actual_delivery_date = NOW()
    WHERE id = p_shipment_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_generate_items_for_batch` (IN `p_batch_id` BIGINT, IN `p_quantity` INT, IN `p_prefix` VARCHAR(50))   BEGIN
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

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_get_item_journey` (IN `p_item_code` VARCHAR(100))   BEGIN
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

CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_pharmacy_receive_shipment` (IN `p_shipment_id` BIGINT, IN `p_pharmacy_id` BIGINT)   BEGIN
    DECLARE v_batch_id BIGINT;
    DECLARE v_quantity INT;
    DECLARE v_from_company_id BIGINT;
    DECLARE v_exists INT;

    -- Get shipment details
    SELECT batch_id, quantity, from_company_id
    INTO v_batch_id, v_quantity, v_from_company_id
    FROM drug_shipments
    WHERE id = p_shipment_id AND to_company_id = p_pharmacy_id;

    -- Check if inventory record exists
    SELECT COUNT(*) INTO v_exists
    FROM pharmacy_inventory
    WHERE pharmacy_id = p_pharmacy_id AND batch_id = v_batch_id;

    IF v_exists > 0 THEN
        -- Update existing record
        UPDATE pharmacy_inventory
        SET quantity = quantity + v_quantity,
            received_shipment_id = p_shipment_id,
            received_date = NOW(),
            updated_at = NOW()
        WHERE pharmacy_id = p_pharmacy_id AND batch_id = v_batch_id;
    ELSE
        -- Insert new record
        INSERT INTO pharmacy_inventory (
            pharmacy_id, batch_id, drug_name, manufacturer, batch_number,
            quantity, manufacture_date, expiry_date, qr_code,
            received_from_distributor_id, received_shipment_id, received_date, received_quantity
        )
        SELECT
            p_pharmacy_id, db.id, db.drug_name, db.manufacturer, db.batch_number,
            v_quantity, db.manufacture_timestamp, db.expiry_date, db.qr_code,
            v_from_company_id, p_shipment_id, NOW(), v_quantity
        FROM drug_batches db
        WHERE db.id = v_batch_id;
    END IF;

    -- Update shipment status
    UPDATE drug_shipments
    SET shipment_status = 'DELIVERED',
        actual_delivery_date = NOW()
    WHERE id = p_shipment_id;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `VerifyDrugByQR` (IN `qr_code_input` VARCHAR(1000))   BEGIN
    DECLARE batch_count INT DEFAULT 0;
    DECLARE is_expired BOOLEAN DEFAULT FALSE;
    DECLARE batch_status VARCHAR(50);
    DECLARE expiry_date_val TIMESTAMP;
    DECLARE batch_id_val BIGINT;

    -- Check if batch exists
    SELECT COUNT(*), status, expiry_date, id
    INTO batch_count, batch_status, expiry_date_val, batch_id_val
    FROM drug_batches
    WHERE qr_code = qr_code_input
    LIMIT 1;

    -- Check if expired
    IF expiry_date_val < NOW() THEN
        SET is_expired = TRUE;
    END IF;

    -- Return verification result with batch details
    IF batch_count > 0 THEN
        SELECT
            db.*,
            CASE
                WHEN is_expired THEN 'EXPIRED'
                WHEN batch_status = 'SOLD' THEN 'ALREADY_SOLD'
                ELSE 'VALID'
            END as verification_status,
            is_expired,
            (SELECT COUNT(*) FROM drug_shipments WHERE batch_id = batch_id_val) as shipment_count
        FROM drug_batches db
        WHERE db.id = batch_id_val;
    ELSE
        SELECT
            qr_code_input as qr_code,
            'INVALID' as verification_status,
            FALSE as batch_exists,
            NULL as drug_name,
            NULL as manufacturer;
    END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `allergen_categories`
--

CREATE TABLE `allergen_categories` (
  `id` int(11) NOT NULL,
  `category` varchar(50) NOT NULL,
  `common_symptoms` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `cross_reactions` text DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `severity_levels` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `app_users`
--

CREATE TABLE `app_users` (
  `id` bigint(20) NOT NULL,
  `allergies` text DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `blood_type` varchar(5) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_of_birth` date DEFAULT NULL,
  `device_info` varchar(500) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `fcm_token` varchar(500) DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_verified` bit(1) DEFAULT NULL,
  `language` varchar(10) DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `medical_conditions` text DEFAULT NULL,
  `notification_enabled` bit(1) DEFAULT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `reminder_sound` varchar(50) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `app_users`
--

INSERT INTO `app_users` (`id`, `allergies`, `avatar_url`, `blood_type`, `created_at`, `date_of_birth`, `device_info`, `email`, `fcm_token`, `full_name`, `gender`, `is_active`, `is_verified`, `language`, `last_login_at`, `medical_conditions`, `notification_enabled`, `password_hash`, `phone`, `reminder_sound`, `updated_at`) VALUES
(1, NULL, NULL, NULL, '2026-02-25 12:31:38.000000', NULL, NULL, 'user1@demo.com', NULL, 'Demo User 1', NULL, b'1', b'1', NULL, NULL, NULL, NULL, NULL, '0987654321', NULL, '2026-02-25 12:31:38.000000'),
(4, NULL, NULL, NULL, '2026-02-25 12:48:38.000000', NULL, NULL, 'user_mobile_0@demo.com', NULL, 'Người dùng 0', NULL, b'1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-25 12:48:38.000000'),
(155468011, NULL, NULL, NULL, '2026-02-27 00:57:06.000000', NULL, NULL, 'user_mobile_155468011@demo.com', NULL, 'Người dùng 155468011', NULL, b'1', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-02-27 00:57:06.000000');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `blockchain_config`
--

CREATE TABLE `blockchain_config` (
  `id` int(11) NOT NULL,
  `config_key` varchar(100) NOT NULL,
  `config_value` text NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `updated_by` varchar(36) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `blockchain_config`
--

INSERT INTO `blockchain_config` (`id`, `config_key`, `config_value`, `description`, `is_active`, `updated_by`, `created_at`, `updated_at`) VALUES
(1, 'contract_address', '0x5FbDB2315678afecb367f032d93F642f64180aa3', 'PharmaLedger smart contract address', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(2, 'network_url', 'http://localhost:8545', 'Blockchain network RPC URL', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(3, 'chain_id', '1337', 'Blockchain network chain ID', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(4, 'last_synced_block', '0', 'Last synced block number for event indexing', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(5, 'indexer_enabled', 'true', 'Enable/disable blockchain event indexing', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(6, 'gas_price', '20000000000', 'Default gas price in wei', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49'),
(7, 'gas_limit', '6721975', 'Default gas limit for transactions', 1, NULL, '2025-09-18 15:38:49', '2025-09-18 15:38:49');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `blockchain_events`
--

CREATE TABLE `blockchain_events` (
  `id` bigint(20) NOT NULL,
  `batch_id` decimal(38,0) DEFAULT NULL,
  `block_number` decimal(38,0) NOT NULL,
  `contract_address` varchar(255) NOT NULL,
  `event_data` text DEFAULT NULL,
  `event_type` varchar(255) NOT NULL,
  `from_address` varchar(255) DEFAULT NULL,
  `indexed_at` datetime(6) NOT NULL,
  `log_index` decimal(38,0) NOT NULL,
  `processed` bit(1) NOT NULL,
  `shipment_id` decimal(38,0) DEFAULT NULL,
  `to_address` varchar(255) DEFAULT NULL,
  `transaction_hash` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `blockchain_events`
--

INSERT INTO `blockchain_events` (`id`, `batch_id`, `block_number`, `contract_address`, `event_data`, `event_type`, `from_address`, `indexed_at`, `log_index`, `processed`, `shipment_id`, `to_address`, `transaction_hash`) VALUES
(1, 0, 8, '0xCf7Ed3AccA5a467e9e704C703E8D87F634fB0Fc9', '{\"quantity\":\"\",\"qrCode\":\"\",\"drugName\":\"\"}', 'BatchIssued', '', '2025-10-22 00:36:42.000000', 1, b'0', NULL, NULL, '0xec2e2d7d8077e5ca84ff859bfa4a3c11e20c1d1197469138459e29f4313d1edf'),
(2, 0, 16, '0x5FC8d32690cc91D4c39d9d3abcBD16989F875707', '{\"quantity\":\"\",\"qrCode\":\"\",\"drugName\":\"\"}', 'BatchIssued', '', '2025-10-28 12:27:46.000000', 1, b'0', NULL, NULL, '0xe7983b5b3ce2b96a900f5bfe6420ed40690a823d7235d6ddcfd134d3e0686527'),
(3, 0, 17, '0x5FC8d32690cc91D4c39d9d3abcBD16989F875707', '{\"quantity\":\"\"}', 'ShipmentCreated', '', '2025-10-28 12:27:46.000000', 2, b'0', 0, '', '0xc9270e94870eb4d99ad5a0935db12486f05daa9d42ecacd71319ca32801d1629');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `blockchain_transactions`
--

CREATE TABLE `blockchain_transactions` (
  `id` bigint(20) NOT NULL,
  `block_number` decimal(38,0) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  `event_logs` tinytext DEFAULT NULL,
  `from_address` varchar(42) NOT NULL,
  `function_name` varchar(100) NOT NULL,
  `gas_price` decimal(38,0) DEFAULT NULL,
  `gas_used` decimal(38,0) DEFAULT NULL,
  `input_data` tinytext DEFAULT NULL,
  `status` enum('FAILED','PENDING','REVERTED','SUCCESS') NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `to_address` varchar(42) NOT NULL,
  `transaction_hash` varchar(66) NOT NULL,
  `batch_id` bigint(20) DEFAULT NULL,
  `shipment_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `blockchain_transactions`
--

INSERT INTO `blockchain_transactions` (`id`, `block_number`, `created_at`, `error_message`, `event_logs`, `from_address`, `function_name`, `gas_price`, `gas_used`, `input_data`, `status`, `timestamp`, `to_address`, `transaction_hash`, `batch_id`, `shipment_id`) VALUES
(140, 8, '2026-02-26 21:28:37.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'createShipment', NULL, 31170, NULL, 'SUCCESS', '2026-02-26 21:28:37.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xf8460752b6755dab28b2ca36a9dde34c8ccc9b6cb41674e7609b5bc5d4e89f84', 96, 62),
(141, 9, '2026-02-26 21:28:45.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'receiveShipment', NULL, 23400, NULL, 'SUCCESS', '2026-02-26 21:28:45.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xa12f4f17ffeb10fd96254ab7582e37e5899d2fdaf024ceaaf43adb65b0d2613d', 96, 62),
(142, 11, '2026-02-26 21:29:03.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'receiveShipment', NULL, 23490, NULL, 'SUCCESS', '2026-02-26 21:29:03.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xffda611663d742abc71c3f7265c821edf52e83bad90a5b07df9215adfb1b9bfd', 96, 63),
(143, 2, '2026-03-09 07:55:49.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'createShipment', NULL, 31170, NULL, 'SUCCESS', '2026-03-09 07:55:49.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xbcd13fecbc2ba15820db1263fc89c88cc18224130f5a848c70e2a5e62730215f', 97, 64),
(144, 3, '2026-03-25 15:03:36.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'createShipment', NULL, 31170, NULL, 'SUCCESS', '2026-03-25 15:03:36.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xb7921efc508a368910d0a58e8a2034992ad70df1a928f9926b590139b4983184', 98, 65),
(145, 4, '2026-03-25 15:03:50.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'receiveShipment', NULL, 23400, NULL, 'SUCCESS', '2026-03-25 15:03:50.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0x82cb0babaf5910aeb9a6d5d4380a00755b37def1a24dbff9e0b3d1fc51cf62e6', 98, 65),
(146, 6, '2026-03-25 15:04:33.000000', NULL, NULL, '0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266', 'receiveShipment', NULL, 23490, NULL, 'SUCCESS', '2026-03-25 15:04:33.000000', '0xe7f1725e7734ce288f8367e1bb143e90bb3f0512', '0xf55077780de5b185dde52166ea5953c42c20c0e05da78f77a146e528811f687f', 98, 66);

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `disease_categories`
--

CREATE TABLE `disease_categories` (
  `id` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text DEFAULT NULL,
  `icd10_code` varchar(20) DEFAULT NULL,
  `is_chronic` bit(1) NOT NULL,
  `is_contagious` bit(1) NOT NULL,
  `is_hereditary` bit(1) NOT NULL,
  `name` varchar(200) NOT NULL,
  `severity_level` enum('critical','mild','moderate','severe') DEFAULT NULL,
  `specialty_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `dispense_instructions`
--

CREATE TABLE `dispense_instructions` (
  `id` bigint(20) NOT NULL,
  `batch_number` varchar(100) DEFAULT NULL,
  `contraindications` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `customer_app_user_id` bigint(20) DEFAULT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `customer_phone` varchar(20) DEFAULT NULL,
  `dispensed_at` datetime(6) DEFAULT NULL,
  `dosage` varchar(50) DEFAULT NULL,
  `drug_name` varchar(255) NOT NULL,
  `duration_days` int(11) DEFAULT NULL,
  `frequency` int(11) DEFAULT NULL,
  `item_code` varchar(100) DEFAULT NULL,
  `meal_relation` enum('AFTER','ANY','BEFORE','WITH') DEFAULT NULL,
  `movement_id` bigint(20) DEFAULT NULL,
  `pharmacist_license` varchar(50) DEFAULT NULL,
  `pharmacist_name` varchar(100) DEFAULT NULL,
  `pharmacy_id` bigint(20) NOT NULL,
  `product_item_id` bigint(20) NOT NULL,
  `sale_price` decimal(15,2) DEFAULT NULL,
  `special_notes` text DEFAULT NULL,
  `specific_times` varchar(100) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `warnings` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `dispense_instructions`
--

INSERT INTO `dispense_instructions` (`id`, `batch_number`, `contraindications`, `created_at`, `customer_app_user_id`, `customer_name`, `customer_phone`, `dispensed_at`, `dosage`, `drug_name`, `duration_days`, `frequency`, `item_code`, `meal_relation`, `movement_id`, `pharmacist_license`, `pharmacist_name`, `pharmacy_id`, `product_item_id`, `sale_price`, `special_notes`, `specific_times`, `updated_at`, `warnings`) VALUES
(3, 'BT202602262049', NULL, '2026-02-27 09:33:15.000000', NULL, NULL, NULL, '2026-02-27 09:33:15.000000', '1 viên', 'Azithromycin 500mg', 3, 2, 'AN500-7030250', 'AFTER', 491, NULL, 'Hoa', 1, 501, NULL, 'Uống từ từ', '012:00,19:00', '2026-02-27 09:33:15.000000', NULL);

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `distributors`
--

CREATE TABLE `distributors` (
  `id` bigint(20) NOT NULL,
  `address` varchar(500) NOT NULL,
  `contact_person` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `license_number` varchar(50) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE','SUSPENDED') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `distributor_inventory`
--

CREATE TABLE `distributor_inventory` (
  `id` bigint(20) NOT NULL,
  `distributor_id` bigint(20) NOT NULL COMMENT 'FK to pharma_companies (DISTRIBUTOR)',
  `batch_id` bigint(20) NOT NULL COMMENT 'FK to drug_batches',
  `drug_name` varchar(255) NOT NULL,
  `manufacturer` varchar(255) NOT NULL,
  `batch_number` varchar(100) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0 COMMENT 'Số lượng hiện có trong kho',
  `reserved_quantity` int(11) NOT NULL DEFAULT 0 COMMENT 'Số lượng đã được đặt trước/đang chờ xuất',
  `available_quantity` int(11) GENERATED ALWAYS AS (`quantity` - `reserved_quantity`) STORED COMMENT 'Số lượng có thể xuất',
  `manufacture_date` timestamp NULL DEFAULT NULL,
  `expiry_date` timestamp NULL DEFAULT NULL,
  `qr_code` varchar(1000) DEFAULT NULL,
  `warehouse_location` varchar(100) DEFAULT 'Kho chính' COMMENT 'Vị trí trong kho: Kho A, Kệ B1, v.v.',
  `storage_conditions` varchar(500) DEFAULT NULL,
  `storage_temperature` varchar(50) DEFAULT NULL COMMENT 'Nhiệt độ bảo quản',
  `unit_price` decimal(15,2) DEFAULT 0.00 COMMENT 'Giá nhập (từ NSX)',
  `selling_price` decimal(15,2) DEFAULT 0.00 COMMENT 'Giá bán (cho hiệu thuốc)',
  `total_value` decimal(15,2) GENERATED ALWAYS AS (`quantity` * `unit_price`) STORED,
  `status` enum('GOOD','LOW_STOCK','EXPIRING_SOON','EXPIRED','QUARANTINE') DEFAULT 'GOOD',
  `min_stock_level` int(11) DEFAULT 100 COMMENT 'Ngưỡng tồn kho tối thiểu',
  `max_stock_level` int(11) DEFAULT 10000 COMMENT 'Ngưỡng tồn kho tối đa',
  `blockchain_batch_id` decimal(38,0) DEFAULT NULL COMMENT 'ID lô trên blockchain',
  `receive_tx_hash` varchar(66) DEFAULT NULL COMMENT 'TX hash khi nhận hàng',
  `current_owner_address` varchar(42) DEFAULT NULL COMMENT 'Địa chỉ ví blockchain',
  `received_from_company_id` bigint(20) DEFAULT NULL COMMENT 'Nhận từ công ty nào (thường là NSX)',
  `received_shipment_id` bigint(20) DEFAULT NULL COMMENT 'FK to drug_shipments',
  `received_date` timestamp NULL DEFAULT NULL,
  `received_quantity` int(11) DEFAULT 0 COMMENT 'Số lượng nhập ban đầu',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(36) DEFAULT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Quản lý kho của nhà phân phối';

--
-- Đang đổ dữ liệu cho bảng `distributor_inventory`
--

INSERT INTO `distributor_inventory` (`id`, `distributor_id`, `batch_id`, `drug_name`, `manufacturer`, `batch_number`, `quantity`, `reserved_quantity`, `manufacture_date`, `expiry_date`, `qr_code`, `warehouse_location`, `storage_conditions`, `storage_temperature`, `unit_price`, `selling_price`, `status`, `min_stock_level`, `max_stock_level`, `blockchain_batch_id`, `receive_tx_hash`, `current_owner_address`, `received_from_company_id`, `received_shipment_id`, `received_date`, `received_quantity`, `created_at`, `updated_at`, `created_by`, `updated_by`, `notes`) VALUES
(8, 3, 96, 'Azithromycin 500mg', 'Dược Hậu Giang', 'BT202602262049', 5, 0, '2026-02-26 13:49:58', '2028-02-26 16:59:59', 'NCKH-PHARMA-3EF547F65715D3-BT202602262049', 'Kho chính', NULL, NULL, 0.00, 0.00, 'GOOD', 100, 10000, 17721137980446163, NULL, '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC', 1, 62, '2026-02-26 14:28:45', 5, '2026-02-26 14:28:45', '2026-02-26 14:28:45', NULL, NULL, NULL),
(9, 3, 98, 'Klamentin 1g', 'Dược Hậu Giang', 'BT202603091219', 5, 0, '2026-03-09 05:19:01', '2028-03-09 16:59:59', 'NCKH-PHARMA-3EFDA568234FBC-BT202603091219', 'Kho chính', NULL, NULL, 0.00, 0.00, 'GOOD', 100, 10000, 17730335414636476, NULL, '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC', 1, 65, '2026-03-25 08:03:50', 5, '2026-03-25 08:03:50', '2026-03-25 08:03:50', NULL, NULL, NULL);

--
-- Bẫy `distributor_inventory`
--
DELIMITER $$
CREATE TRIGGER `trg_distributor_inventory_movement_log` AFTER UPDATE ON `distributor_inventory` FOR EACH ROW BEGIN
    IF OLD.quantity != NEW.quantity THEN
        INSERT INTO `inventory_movements` (
            `inventory_type`,
            `inventory_id`,
            `movement_type`,
            `quantity_before`,
            `quantity_change`,
            `quantity_after`,
            `reason`,
            `movement_date`
        ) VALUES (
            'DISTRIBUTOR',
            NEW.id,
            CASE
                WHEN NEW.quantity > OLD.quantity THEN 'RECEIVE'
                WHEN NEW.quantity < OLD.quantity THEN 'SHIP_OUT'
                ELSE 'ADJUSTMENT'
            END,
            OLD.quantity,
            NEW.quantity - OLD.quantity,
            NEW.quantity,
            'Auto-logged by trigger',
            NOW()
        );
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_distributor_inventory_status_update` BEFORE UPDATE ON `distributor_inventory` FOR EACH ROW BEGIN
    DECLARE days_to_expiry INT;
    SET days_to_expiry = DATEDIFF(NEW.expiry_date, NOW());

    -- Kiểm tra hết hạn
    IF days_to_expiry < 0 THEN
        SET NEW.status = 'EXPIRED';
    -- Sắp hết hạn (30 ngày)
    ELSEIF days_to_expiry <= 30 THEN
        SET NEW.status = 'EXPIRING_SOON';
    -- Tồn kho thấp
    ELSEIF NEW.available_quantity <= NEW.min_stock_level THEN
        SET NEW.status = 'LOW_STOCK';
    ELSE
        SET NEW.status = 'GOOD';
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `distributor_users`
--

CREATE TABLE `distributor_users` (
  `id` varchar(36) NOT NULL,
  `company_address` varchar(500) DEFAULT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `is_active` bit(1) NOT NULL,
  `is_profile_complete` bit(1) NOT NULL,
  `is_verified` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `license_expiry_date` datetime(6) DEFAULT NULL,
  `license_number` varchar(50) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `role` enum('DISTRIBUTOR','MANUFACTURER','PHARMACY') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `distributor_users`
--

INSERT INTO `distributor_users` (`id`, `company_address`, `company_name`, `created_at`, `email`, `is_active`, `is_profile_complete`, `is_verified`, `last_login_at`, `license_expiry_date`, `license_number`, `name`, `password_hash`, `phone_number`, `role`, `updated_at`, `wallet_address`) VALUES
('dist-001-uuid', '789 Đường GHI, Quận 3, TP.HCM', 'Nhà phân phối CPC1 Hà Nội', '2025-09-29 19:00:12.000000', 'distributor@demo.com', b'1', b'1', b'1', '2026-03-25 14:43:49.000000', NULL, NULL, 'Nguyễn Văn A', '$2a$10$jPOq3CIOWI89C2gqoZRypeZLe93ZcmKjJXE1qQQ1jk9Shvw/jJ3Cm', '0903456789', 'DISTRIBUTOR', '2026-03-25 14:43:49.000000', '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC'),
('dist-002-uuid', '321 Đường JKL, Quận 7, TP.HCM', 'Zuellig Pharma Vietnam', '2025-09-29 19:00:12.000000', 'zuellig@demo.com', b'1', b'1', b'1', NULL, NULL, NULL, 'Trần Thị Zuellig', '$2a$10$LYes.J77LJsTVkcrK.Eea.CzbAosv/MhaWbPx5KZoIGwriBg3rIfq', '0904567890', 'DISTRIBUTOR', NULL, '0x90F79bf6EB2c4f870365E785982E1f101E93b906');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `drug_batches`
--

CREATE TABLE `drug_batches` (
  `id` bigint(20) NOT NULL,
  `batch_id` decimal(38,0) NOT NULL,
  `batch_number` varchar(100) NOT NULL,
  `block_number` decimal(38,0) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_owner` varchar(42) NOT NULL,
  `drug_name` varchar(255) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `is_synced` bit(1) NOT NULL,
  `manufacture_timestamp` datetime(6) NOT NULL,
  `manufacturer` varchar(255) NOT NULL,
  `manufacturer_address` varchar(42) NOT NULL,
  `qr_code` varchar(1000) DEFAULT NULL,
  `quantity` bigint(20) NOT NULL,
  `status` enum('DELIVERED','IN_TRANSIT','MANUFACTURED','SOLD') NOT NULL,
  `storage_conditions` varchar(500) DEFAULT NULL,
  `transaction_hash` varchar(66) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `items_merkle_root` varchar(66) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `drug_batches`
--

INSERT INTO `drug_batches` (`id`, `batch_id`, `batch_number`, `block_number`, `created_at`, `current_owner`, `drug_name`, `expiry_date`, `is_synced`, `manufacture_timestamp`, `manufacturer`, `manufacturer_address`, `qr_code`, `quantity`, `status`, `storage_conditions`, `transaction_hash`, `updated_at`, `items_merkle_root`) VALUES
(96, 17721137980446163, 'BT202602262049', 7, '2026-02-26 20:49:58.000000', '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 'Azithromycin 500mg', '2028-02-26 23:59:59.000000', b'1', '2026-02-26 20:49:58.000000', 'Dược Hậu Giang', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NCKH-PHARMA-3EF547F65715D3-BT202602262049', 0, 'DELIVERED', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', '0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0', '2026-02-26 21:29:03.000000', '0xccf3a6e1d907991ba8e7c5a850d36a2d236c35f17103912ff20250b541320590'),
(97, 17730177372305144, 'BT202603090755', 1, '2026-03-09 07:55:37.000000', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'Augmentin 1g', '2028-03-09 23:59:59.000000', b'1', '2026-03-09 07:55:37.000000', 'Dược Hậu Giang', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NCKH-PHARMA-3EFD809C14B6F8-BT202603090755', 5, 'IN_TRANSIT', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', '0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb', '2026-03-09 07:55:49.000000', '0x9691cc0249f0151a64e1e664156e84a73bead64680d15e9200160571ae58bbcd'),
(98, 17730335414636476, 'BT202603091219', 3, '2026-03-09 12:19:01.000000', '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 'Klamentin 1g', '2028-03-09 23:59:59.000000', b'1', '2026-03-09 12:19:01.000000', 'Dược Hậu Giang', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NCKH-PHARMA-3EFDA568234FBC-BT202603091219', 0, 'DELIVERED', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', '0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f', '2026-03-25 15:04:33.000000', '0xd0a2b12a3259777ffb2b519f00f92e3be16e57e09fdce30ff4b0df9e1f4b0c7b'),
(99, 17744248278126801, 'BT202603251447', 1, '2026-03-25 14:47:07.000000', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'Augmentin 1g', '2028-03-31 23:59:59.000000', b'1', '2026-03-25 14:47:07.000000', 'Dược Hậu Giang', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NCKH-PHARMA-3F0A4CBF6C90D1-BT202603251447', 1, 'MANUFACTURED', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', '0x2c7b4ea024aea85978db5c3f4de83b53319caed672c62e975db150827bba5941', '2026-03-25 14:47:08.000000', '0x4fba614783c5b81c23f75fab564fb1ad5cc59c8e7ef1135767f0c79096fb85ee'),
(100, 17744256371854763, 'BT202603251500', 2, '2026-03-25 15:00:37.000000', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'Klamentin 625', '2028-03-25 23:59:59.000000', b'1', '2026-03-25 15:00:37.000000', 'Dược Hậu Giang', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NCKH-PHARMA-3F0A4EA1D90DAB-BT202603251500', 5, 'MANUFACTURED', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', '0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf', '2026-03-25 15:00:37.000000', '0xad056a280d29104668aa9ca53bc8587cf00ac3fa3904038834712ec821f0fa5b');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `drug_products`
--

CREATE TABLE `drug_products` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `active_ingredient` varchar(255) DEFAULT NULL,
  `dosage` varchar(100) DEFAULT NULL,
  `dosage_form` enum('tablet','capsule','syrup','injection','cream','drops','other') DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `manufacturer_id` bigint(20) NOT NULL,
  `description` text DEFAULT NULL,
  `storage_conditions` varchar(500) DEFAULT NULL,
  `shelf_life_months` int(11) DEFAULT NULL,
  `registration_number` varchar(100) DEFAULT NULL,
  `approval_date` date DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `shelf_life` varchar(100) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  `unit` varchar(50) DEFAULT NULL,
  `contraindications` text DEFAULT NULL,
  `drug_interactions` text DEFAULT NULL,
  `indications` text DEFAULT NULL,
  `precautions` text DEFAULT NULL,
  `side_effects` text DEFAULT NULL,
  `usage_instructions` text DEFAULT NULL,
  `article_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `drug_products`
--

INSERT INTO `drug_products` (`id`, `name`, `active_ingredient`, `dosage`, `dosage_form`, `category`, `image_url`, `manufacturer_id`, `description`, `storage_conditions`, `shelf_life_months`, `registration_number`, `approval_date`, `is_active`, `created_at`, `updated_at`, `shelf_life`, `status`, `unit`, `contraindications`, `drug_interactions`, `indications`, `precautions`, `side_effects`, `usage_instructions`, `article_url`) VALUES
(19, 'Augmentin 1g', 'Amoxicillin + Acid Clavulanic', '875mg/125mg', NULL, 'Kháng sinh – thuốc kê đơn', 'http://localhost:8080/uploads/7d85649e-22ae-4997-87bf-39f7e2efe8f6.jpeg', 1, 'Kháng sinh phổ rộng điều trị nhiễm khuẩn đường hô hấp, tiết niệu, da và mô mềm.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Dị ứng với amoxicillin, acid clavulanic hoặc kháng sinh beta-lactam; tiền sử vàng da/viêm gan do thuốc.', 'Probenecid làm tăng nồng độ amoxicillin; giảm hiệu quả thuốc tránh thai đường uống; tương tác với allopurinol có thể tăng nguy cơ phát ban.', 'Điều trị các nhiễm khuẩn do vi khuẩn nhạy cảm như: viêm amidan, viêm xoang, viêm tai giữa, viêm phế quản và viêm phổi; nhiễm khuẩn tiết niệu và nhiễm khuẩn da và mô mềm.', 'Theo dõi chức năng gan, thận khi dùng kéo dài; thận trọng ở bệnh nhân dị ứng penicillin trước đó.', 'Tiêu chảy, buồn nôn, đau bụng, phát ban; hiếm gặp: phản vệ, viêm đại tràng giả mạc.', 'Uống trong bữa ăn để giảm kích ứng dạ dày; dùng đủ liệu trình theo chỉ định bác sĩ.', 'https://medlatec.vn/tin-tuc/augmentin-1g-tac-dung-lieu-dung-va-nhung-luu-y-khi-su-dung-s195-n18198'),
(20, 'Klamentin 625', 'Amoxicillin + Acid Clavulanic', '500mg/125mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/dsc_0528_403c4f796d.jpg', 1, 'Kháng sinh phối hợp điều trị nhiễm khuẩn nhạy cảm với Amoxicillin.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Quá mẫn với penicillin, cephalosporin hoặc thành phần thuốc.', 'Giống Augmentin; probenecid làm tăng nồng độ thuốc.', 'Điều trị nhiễm khuẩn hô hấp trên, viêm tai giữa, nhiễm khuẩn tiết niệu và da.', 'Thận trọng khi suy gan, suy thận; theo dõi men gan.', 'Buồn nôn, tiêu chảy, phát ban da.', 'Uống đầu bữa ăn, theo lịch triều bác sĩ.', 'https://www.vinmec.com/vie/bai-viet/cong-dung-cua-thuoc-klamentin-625-mg-vi'),
(21, 'Klamentin 1g', 'Amoxicillin + Acid Clavulanic', '875mg/125mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00018596_klamentin_1g_duoc_hau_giang_2x7_5475_6207_1637731778.jpg', 1, 'Kháng sinh hàm lượng cao cho các nhiễm khuẩn nặng đường hô hấp, tiết niệu.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Quá mẫn với amoxicillin/clavulanate; rối loạn chức năng gan nghiêm trọng.', 'Tương tự Augmentin.', 'Điều trị nhiễm khuẩn nặng đường hô hấp, mô mềm và viêm phổi.', 'Điều chỉnh liều ở người suy thận; theo dõi chức năng gan.', 'Đau bụng, tiêu chảy, phát ban.', 'Uống theo liều bác sĩ khuyến cáo.', 'https://www.vinmec.com/vie/bai-viet/klamentin-1g-la-thuoc-gi-vi'),
(22, 'Zinnat 500mg', 'Cefuroxim axetil', '500mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00003460_zinnat_500mg_hang_nhap_khau_1v_6380_6339_1633512301.jpg', 1, 'Kháng sinh nhóm Cephalosporin thế hệ 2 điều trị nhiễm khuẩn hiệu quả.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Dị ứng cephalosporin; tiền sử phản ứng nặng với penicillin.', 'Probenecid làm tăng nồng độ thuốc.', 'Điều trị nhiễm khuẩn hô hấp, nhiễm khuẩn da và đường tiết niệu nhạy cảm với cefuroxim.', 'Thận trọng ở bệnh nhân dị ứng beta-lactam.', 'Tiêu chảy, buồn nôn, đau bụng.', 'Uống sau ăn để cải thiện hấp thu.', 'http://vinmec.com/vie/bai-viet/zinnat-cap-nhat-thong-tin-su-dung-vi'),
(23, 'Medoclav 1g', 'Amoxicillin + Acid Clavulanic', '875mg/125mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00000002_medoclav_1g_medochemie_2x7_5568_6142_1634629452.jpg', 1, 'Kháng sinh phối hợp giúp chống lại vi khuẩn kháng thuốc.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Quá mẫn với penicillin hoặc beta-lactam.', 'Tương tác probenecid.', 'Điều trị nhiễm khuẩn do vi khuẩn nhạy cảm như viêm tai giữa, viêm xoang, nhiễm khuẩn da và mô mềm.', 'Theo dõi chức năng gan; thận trọng ở người có tiền sử dị ứng penicillin.', 'Buồn nôn, tiêu chảy, đau đầu.', 'Uống theo hướng dẫn, không bỏ liều giữa chừng.', 'https://www.vinmec.com/vie/bai-viet/cong-dung-thuoc-medoclav-vi'),
(24, 'Cefixim 200mg', 'Cefixim', '200mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00001037_cefixim_200mg_3x10_cuu_long_5177_6118_1633514757.jpg', 1, 'Kháng sinh Cephalosporin thế hệ 3, dùng cho nhiễm khuẩn hô hấp và tiết niệu.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Dị ứng cephalosporin.', 'Probenecid làm tăng nồng độ thuốc.', 'Điều trị nhiễm khuẩn như viêm phổi, viêm họng, nhiễm khuẩn tiết niệu.', 'Theo dõi chức năng thận ở bệnh nhân suy thận.', 'Tiêu chảy, đau bụng.', 'Uống 1–2 lần/ngày theo chỉ định.', 'https://medlatec.vn/tin-tuc/cefixime-thuoc-khang-sinh-dieu-tri-nhiem-trung-nhung-can-can-trong-khi-dung'),
(25, 'Cefuroxim 500mg', 'Cefuroxim', '500mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00000300_cefuroxim_500mg_vidipha_2x5_5548_6097_1634542284.jpg', 1, 'Điều trị các nhiễm khuẩn do vi khuẩn nhạy cảm gây ra.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Quá mẫn cephalosporin.', 'Probenecid làm tăng thuốc trong máu.', 'Điều trị viêm xoang, viêm phổi, nhiễm khuẩn tiết niệu.', 'Theo dõi chức năng thận.', 'Buồn nôn, tiêu chảy.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/bai-viet/thuoc-cefuroxim-500mg-tri-benh-gi-vi'),
(26, 'Levofloxacin 500mg', 'Levofloxacin', '500mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00007887_levofloxacin_500mg_pymepharco_2x10_8147_6151_1637731777.jpg', 1, 'Kháng sinh nhóm Fluoroquinolone cho nhiễm khuẩn nặng hô hấp và da.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Dị ứng quinolone; trẻ em và người mang thai trừ chỉ định đặc biệt.', 'Thuốc kháng acid làm giảm hấp thu.', 'Điều trị nhiễm khuẩn phổi, tiết niệu và da do vi khuẩn nhạy cảm.', 'Nguy cơ viêm gân ở người già; thận trọng suy thận.', 'Chóng mặt, buồn nôn, đau đầu.', 'Uống nhiều nước, tránh dùng cùng các chất chứa kim loại.', 'https://www.vinmec.com/vie/bai-viet/cong-dung-thuoc-levofloxacin-stada-500-mg-vi'),
(27, 'Moxifloxacin 400mg', 'Moxifloxacin', '400mg', NULL, 'Kháng sinh – thuốc kê đơn', 'https://cms-prod.s3-sgn09.fptcloud.com/00007843_moxifloxacin_400mg_pymepharco_1x10_3735_6169_1637731779.jpg', 1, 'Kháng sinh phổ rộng điều trị viêm phổi và các nhiễm khuẩn phức tạp.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Dị ứng quinolone; tiền sử rối loạn nhịp.', 'Thuốc kháng acid làm giảm hấp thu.', 'Điều trị viêm phổi cộng đồng và các nhiễm khuẩn phức tạp.', 'Thận trọng ở bệnh nhân bệnh tim và nhịp QT kéo dài.', 'Buồn nôn, đau đầu, tiêu chảy.', 'Uống theo chỉ định, tránh dùng cùng sữa.', 'https://nhathuoclongchau.com.vn/thanh-phan/moxifloxacin'),
(28, 'Azithromycin 500mg', 'Azithromycin', '500mg', NULL, 'Kháng sinh – thuốc kê đơn', 'http://localhost:8080/uploads/a64de0be-0d46-4dea-bfb9-ae7ec4ce0d6f.webp', 1, 'Kháng sinh nhóm Macrolide điều trị nhiễm khuẩn hô hấp, liều ngắn ngày.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Dị ứng azithromycin hoặc macrolid.', 'Tương tác với thuốc chống đông.', 'Điều trị nhiễm khuẩn đường hô hấp, viêm họng và nhiễm khuẩn da.', 'Thận trọng ở bệnh nhân bệnh gan nặng.', 'Tiêu chảy, buồn nôn, đau bụng.', 'Uống 1 lần/ngày theo liệu trình.', 'https://www.vinmec.com/vie/bai-viet/thuoc-azithromycin-cong-dung-chi-dinh-va-luu-y-khi-dung-vi'),
(29, 'Nexium 40mg', 'Esomeprazol', '40mg', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00000052_nexium_40mg_astrazeneca_2x7_5365.jpg', 1, 'Thuốc ức chế bơm proton điều trị trào ngược dạ dày (GERD) và viêm loét.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Quá mẫn với esomeprazole hoặc PPI.', 'Giảm hấp thu ketoconazole và thuốc HIV; tương tác với clopidogrel.', 'Điều trị trào ngược dạ dày thực quản (GERD) và loét dạ dày tá tràng.', 'Dùng lâu có thể giảm hấp thu vitamin B12.', 'Đau đầu, buồn nôn, tiêu chảy.', 'Uống trước bữa ăn 30–60 phút.', 'https://medlatec.vn/tin-tuc/thuoc-da-day-nexium-40mg-va-6-thong-tin-can-biet-truoc-khi-su-dung'),
(30, 'Esomeprazol STADA 40mg', 'Esomeprazol', '40mg', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00021615_esomeprazol_stada_40mg_stada_4x7_5474_6010_1633513303.jpg', 1, 'Giảm tiết acid dạ dày, hỗ trợ điều trị viêm loét và HP.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Viên', 'Dị ứng PPI.', 'Tương tác thuốc hấp thu acid dạ dày.', 'Điều trị GERD, loét dạ dày tá tràng và hội chứng tăng acid.', 'Theo dõi khi dùng dài ngày.', 'Đau đầu, tiêu chảy.', 'Uống trước ăn để hiệu quả tối ưu.', 'https://nhathuoclongchau.com.vn/thuoc/esomeprazole-stada-40mg-2x10-33212.html'),
(31, 'Pantoprazol 40mg', 'Pantoprazol', '40mg', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00009653_pantoprazol_stada_40mg_stada_4x7_8984_6168.jpg', 1, 'Thuốc ức chế bơm proton thế hệ mới, điều trị loét dạ dày tá tràng.', 'Nơi khô, dưới 30 độ C, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Viên', 'Quá mẫn với pantoprazole hoặc thuốc ức chế bơm proton.', 'Giảm hấp thu thuốc phụ thuộc pH dạ dày như ketoconazole.', 'Điều trị trào ngược dạ dày thực quản (GERD), viêm thực quản do acid, loét dạ dày tá tràng.', 'Dùng dài ngày có thể giảm hấp thu vitamin B12 và magnesi.', 'Đau đầu, tiêu chảy, đau bụng, buồn nôn.', 'Uống trước bữa ăn khoảng 30 phút.', 'https://www.vinmec.com/vie/bai-viet/cong-dung-thuoc-pantoprazol-40mg-vi'),
(32, 'Gaviscon hộp', 'Natri alginate, Natri bicarbonat, Calci carbonat', 'Hỗn dịch', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00010903_gaviscon_dual_action_r_10ml_24v_7366.jpg', 1, 'Tạo lớp màng bảo vệ ngăn chặn trào ngược acid dạ dày, giảm ợ nóng.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Hộp', 'Quá mẫn với alginate hoặc bicarbonate.', 'Có thể ảnh hưởng hấp thu thuốc khác nếu dùng cùng lúc.', 'Điều trị trào ngược dạ dày, ợ nóng và khó tiêu do acid.', 'Thận trọng ở bệnh nhân suy thận hoặc hạn chế natri.', 'Táo bón nhẹ, đầy hơi.', 'Uống sau bữa ăn và trước khi ngủ.', 'https://www.vinmec.com/vie/bai-viet/thuoc-gaviscon-tac-dung-lieu-dung-va-luu-y-su-dung-vi'),
(33, 'Enterogermina', 'Bacillus clausii', '2 tỷ bào tử', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00004944_enterogermina_2ty_5ml_20_ong_8223.jpg', 1, 'Men vi sinh bổ sung lợi khuẩn, phòng và điều trị rối loạn tiêu hóa.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Ống', 'Quá mẫn với Bacillus clausii.', 'Kháng sinh có thể làm giảm hiệu quả.', 'Phòng và điều trị rối loạn hệ vi sinh đường ruột, tiêu chảy do kháng sinh.', 'Không pha với nước nóng.', 'Ít gặp: đầy hơi, khó chịu nhẹ.', 'Uống trực tiếp hoặc pha với nước.', 'https://nhathuoclongchau.com.vn/thuoc/enterogemina-5ml-sanofi-20-ong-17315.html'),
(34, 'BioGaia Probiotic', 'Lactobacillus reuteri Protectis', '5ml', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00501192_biogaia_protectis_baby_drops_5ml_1863.jpg', 1, 'Men vi sinh từ Thụy Điển giúp cải thiện hệ tiêu hóa cho trẻ em.', 'Bảo quản dưới 25 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Chai', 'Dị ứng với Lactobacillus reuteri.', 'Không đáng kể.', 'Bổ sung lợi khuẩn giúp cải thiện tiêu hóa ở trẻ.', 'Bảo quản đúng nhiệt độ.', 'Ít gặp: đầy hơi.', 'Nhỏ trực tiếp vào miệng hoặc pha với sữa.', 'https://medlatec.vn/tin-tuc/tim-hieu-men-tieu-hoa-biogaia-va-cach-su-dung-an-toan-hieu-qua'),
(35, 'Smecta hộp', 'Diosmectite', '3g', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00014059_smecta_v_cam_chanh_30g_7718.jpg', 1, 'Thuốc hấp phụ bảo vệ niêm mạc đường tiêu hóa, điều trị tiêu chảy.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Hộp', 'Quá mẫn với diosmectite.', 'Có thể giảm hấp thu thuốc khác.', 'Điều trị tiêu chảy cấp và mạn, bảo vệ niêm mạc tiêu hóa.', 'Thận trọng khi dùng cho trẻ nhỏ.', 'Táo bón nhẹ.', 'Pha gói thuốc với nước trước khi uống.', 'https://www.vinmec.com/vie/bai-viet/thuoc-smecta-chu-y-lieu-dung-va-huong-dan-su-dung-dung-cach-vi'),
(36, 'Biosubtyl-II', 'Bacillus subtilis', '1 tỷ bào tử', NULL, 'Thuốc dạ dày – tiêu hóa', 'https://cms-prod.s3-sgn09.fptcloud.com/00000405_biosubtyl_ii_dalat_25_goi_x_1g_1411_6345.jpg', 1, 'Vi khuẩn sống có lợi cho đường ruột, hỗ trợ tiêu hóa.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '24 tháng', 'active', 'Gói', 'Dị ứng với Bacillus subtilis.', 'Kháng sinh có thể làm giảm hiệu quả.', 'Bổ sung vi khuẩn có lợi cho đường ruột.', 'Không dùng với nước nóng.', 'Đầy hơi nhẹ.', 'Uống sau ăn.', 'https://tpvnpharma.com/biosubtyl-ii'),
(37, 'Prospan Siro', 'Cao khô lá thường xuân', '100ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/00000941_siro_ho_prospan_100ml_3756.jpg', 1, 'Siro ho thảo dược giúp long đờm, giảm ho cho trẻ em và người lớn.', 'Nơi khô, dưới 25 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:03:34', '36 tháng', 'active', 'Chai', 'Dị ứng với cao lá thường xuân.', 'Không đáng kể.', 'Giảm ho, long đờm trong viêm phế quản.', 'Thận trọng ở trẻ dưới 2 tuổi.', 'Rối loạn tiêu hóa nhẹ.', 'Uống theo liều khuyến cáo.', 'https://www.vinmec.com/vie/bai-viet/huong-dan-su-dung-thuoc-ho-prospan-dang-siro-vien-ngam-cho-tre-vi'),
(38, 'Muhi Siro trẻ em', 'Chiết xuất thảo dược', '120ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/wellbaby_multi_vitamin_liquid_vitabiotics_ho_tro_suc_khoe_cho_be_150ml_7909_6372_1625907409.jpg', 1, 'Siro ho Nhật Bản với các hương vị dễ uống dành cho trẻ em.', 'Nơi khô, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:45', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Không đáng kể.', 'Giảm ho và long đờm cho trẻ em.', 'Thận trọng ở trẻ nhỏ.', 'Buồn ngủ nhẹ.', 'Dùng theo liều khuyến cáo.', 'https://www.vinmec.com/vie/bai-viet/tac-dung-phu-co-gap-khi-su-dung-khang-sinh-azithromycin-vi'),
(39, 'Astex Siro', 'Húng chanh, Núc nác, Cineol', '90ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/00000490_siro_ho_astex_90ml_4091_6339.jpg', 1, 'Siro ho thảo dược bệnh viện Nhi Đồng 1 sản xuất.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:45', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần thảo dược.', 'Không đáng kể.', 'Điều trị ho do viêm họng hoặc cảm lạnh.', 'Thận trọng với trẻ nhỏ.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau ăn.', 'https://www.vinmec.com/vie/bai-viet/tac-dung-phu-co-gap-khi-su-dung-khang-sinh-azithromycin-vi'),
(40, 'Bổ phế Nam Hà chai', 'Cát cánh, Tỳ bà diệp, Mơ muối...', '125ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/00001099_thuoc_ho_bo_phe_nam_ha_chi_khai_lo_125ml_4366.jpg', 1, 'Thuốc ho đông dược truyền thống giúp bổ phế, chỉ khái.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Dị ứng với thành phần đông dược.', 'Chưa ghi nhận.', 'Giảm ho, long đờm, hỗ trợ điều trị viêm phế quản.', 'Không dùng quá liều.', 'Ít gặp.', 'Uống theo liều hướng dẫn.', 'https://www.vinmec.com/vie/bai-viet/tac-dung-phu-co-gap-khi-su-dung-khang-sinh-azithromycin-vi'),
(41, 'Eugica Siro', 'Eucalyptol, Gừng, Tần, Menthol', '100ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/00000412_siro_ho_eugica_100ml_1752.jpg', 1, 'Siro ho thảo dược giúp sát trùng đường hô hấp, làm dịu cơn ho.', 'Nơi khô, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với tinh dầu.', 'Có thể tương tác thuốc an thần.', 'Giảm ho, sát trùng đường hô hấp.', 'Không dùng quá liều.', 'Buồn nôn nhẹ.', 'Uống sau ăn.', 'https://www.vinmec.com/vie/bai-viet/tac-dung-phu-co-gap-khi-su-dung-khang-sinh-azithromycin-vi'),
(42, 'Toplexil Siro', 'Oxomemazine, Guaifenesin', '125ml', NULL, 'Thuốc ho – hô hấp – siro', 'https://cms-prod.s3-sgn09.fptcloud.com/00024479_siro_toplexil_sanofi_125ml_8700.jpg', 1, 'Thuốc trị ho có đờm và ho khan do kích ứng.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Trẻ nhỏ dưới 2 tuổi.', 'Tăng tác dụng khi dùng cùng thuốc an thần.', 'Điều trị ho khan và ho có đờm.', 'Không lái xe khi dùng.', 'Buồn ngủ, chóng mặt.', 'Uống theo chỉ định bác sĩ.', 'https://www.vinmec.com/vie/bai-viet/tac-dung-phu-co-gap-khi-su-dung-khang-sinh-azithromycin-vi'),
(43, 'Plavix 75mg', 'Clopidogrel', '75mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00001859_plavix_75mg_28v_6221.jpg', 1, 'Thuốc chống kết tập tiểu cầu, phòng ngừa đột quỵ và nhồi máu cơ tim.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Xuất huyết đang tiến triển.', 'Tương tác thuốc chống đông.', 'Phòng ngừa nhồi máu cơ tim và đột quỵ.', 'Thận trọng ở bệnh nhân nguy cơ chảy máu.', 'Chảy máu, bầm tím.', 'Uống mỗi ngày một lần.', 'https://www.vinmec.com/vie/bai-viet/thuoc-rosuvastatin-cong-dung-lieu-dung-va-luu-y-tac-dung-phu-vi'),
(44, 'Crestor 10mg', 'Rosuvastatin', '10mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00000055_crestor_10mg_astrazeneca_2x14_9495.jpg', 1, 'Thuốc hạ mỡ máu nhóm Statin hiệu quả cao.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Bệnh gan hoạt động.', 'Antacid có thể giảm hấp thu.', 'Giảm cholesterol và phòng bệnh tim mạch.', 'Theo dõi chức năng gan.', 'Đau cơ, đau đầu.', 'Uống mỗi ngày một lần.', 'https://www.vinmec.com/vie/bai-viet/thuoc-crestor-cong-dung-chi-dinh-va-luu-y-khi-dung-vi'),
(45, 'Crestor 20mg', 'Rosuvastatin', '20mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00005597_crestor_20mg_astrazeneca_2x14_6380.jpg', 1, 'Mức liều 20mg cho bệnh nhân mỡ máu cao nặng.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Bệnh gan nặng.', 'Tương tác antacid.', 'Điều trị tăng cholesterol máu nặng.', 'Theo dõi men gan.', 'Đau cơ, rối loạn tiêu hóa.', 'Uống 1 lần/ngày.', 'https://www.vinmec.com/vie/bai-viet/thuoc-crestor-cong-dung-chi-dinh-va-luu-y-khi-dung-vi'),
(46, 'Coveram', 'Perindopril + Amlodipin', '5mg/5mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00021319_coveram_5mg_5mg_servier_1x30_8932.jpg', 1, 'Thuốc phối hợp điều trị tăng huyết áp vô căn.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Viên', 'Hạ huyết áp nặng.', 'Thuốc lợi tiểu tăng tác dụng hạ áp.', 'Điều trị tăng huyết áp.', 'Theo dõi huyết áp.', 'Chóng mặt, đau đầu.', 'Uống mỗi ngày một lần.', 'https://www.vinmec.com/vie/bai-viet/thuoc-dieu-tri-roi-loan-lipid-mau-vi'),
(47, 'Concor 5mg', 'Bisoprolol fumarate', '5mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00003023_concor_5mg_merck_3x10_8698.jpg', 1, 'Thuốc chẹn beta điều trị huyết áp và suy tim.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Nhịp tim chậm.', 'Thuốc hạ áp khác tăng tác dụng.', 'Điều trị tăng huyết áp và suy tim.', 'Không ngừng thuốc đột ngột.', 'Mệt mỏi, chóng mặt.', 'Uống buổi sáng.', 'https://www.vinmec.com/vie/bai-viet/thuoc-dieu-tri-roi-loan-lipid-mau-vi'),
(48, 'Lipitor 20mg', 'Atorvastatin', '20mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00005598_lipitor_20mg_pfizer_3x10_4509.jpg', 1, 'Thuốc ức chế HMG-CoA reductase hạ cholesterol toàn phần và LDL.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Bệnh gan nặng.', 'Tương tác thuốc chống nấm.', 'Giảm cholesterol LDL và phòng bệnh tim.', 'Theo dõi men gan.', 'Đau cơ, buồn nôn.', 'Uống 1 lần/ngày.', 'https://www.vinmec.com/vie/bai-viet/thuoc-dieu-tri-roi-loan-lipid-mau-vi'),
(49, 'Vastarel MR 35mg', 'Trimetazidine', '35mg', NULL, 'Thuốc tim mạch – huyết áp – mỡ máu', 'https://cms-prod.s3-sgn09.fptcloud.com/00001097_vastarel_mr_35mg_servier_2x30_3136.jpg', 1, 'Phòng ngừa cơn đau thắt ngực ổn định.', 'Nơi khô, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Viên', 'Bệnh Parkinson.', 'Chưa ghi nhận.', 'Điều trị đau thắt ngực ổn định.', 'Thận trọng người cao tuổi.', 'Chóng mặt, đau đầu.', 'Uống sau ăn.', 'https://www.vinmec.com/vie/bai-viet/thuoc-dieu-tri-roi-loan-lipid-mau-vi'),
(50, 'Blackmores Multivitamin', 'Vitamin & Khoáng chất', 'Nhiều loại', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00502127_vien_uong_blackmores_mens_performance_multi_bo_sung_vitamin_cho_nam_gioi_50_vien_8350.jpg', 1, 'Thực phẩm bảo vệ sức khỏe bổ sung vitamin tổng hợp.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin'),
(51, 'Doppelherz Aktiv Omega 3', 'Dầu cá (EPA, DHA), Vitamin E', '2000mg dầu cá', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00021669_doppelherz_omega_3_seefischol_1000mg_30_vien_2041_6339.jpg', 1, 'Hỗ trợ sức khỏe tim mạch, não bộ và thị lực.', 'Nơi khô ráo, dưới 25 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/omega-3'),
(52, 'Nature Made Vitamin C', 'Vitamin C', '500mg', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00021626_nature_made_vitamin_c_500mg_100_vien_9984_6339.jpg', 1, 'Tăng khả năng miễn dịch và chống oxy hóa cho cơ thể.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-c'),
(53, 'DHC Collagen', 'Collagen peptide từ cá', '2050mg', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00024484_vien_uong_collagen_dhg_30_ngay_3277.jpg', 1, 'Bổ sung collagen hỗ trợ làm đẹp da, tăng đàn hồi.', 'Nơi khô ráo, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Gói', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/collagen'),
(54, 'Centrum A-Z', 'Vitamin & Khoáng chất', 'Đa vitamin', NULL, 'Vitamin – thực phẩm chức năng', 'https://image.uniqlo.com/UQ/ST3/vn/imagesgoods/451433/item/vngoods_12_451433.jpg', 1, 'Bổ sung đầy đủ vi chất dinh dưỡng hàng ngày.', 'Nơi khô ráo, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin'),
(55, 'Pharmaton', 'G115, Vitamin & Khoáng chất', 'Viên nang', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00001098_pharmaton_vitality_60_vien_7190.jpg', 1, 'Tăng cường sức khỏe khi mệt mỏi, kén ăn, suy nhược.', 'Nơi khô ráo,ưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin'),
(56, 'NattoEnzym 670FU', 'Nattokinase', '670FU', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00021634_nattoenzym_670fu_duoc_hau_giang_3x10_8698_6339.jpg', 1, 'Hỗ trợ tuần hoàn máu, giải tán cục máu đông, phòng đột quỵ.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/nattokinase'),
(57, 'Bocalex Multi', 'Vitamin B + C', 'Viên sủi', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00018597_bocalex_multi_20_vien_sui_7553.jpg', 1, 'Viên sủi bổ sung vitamin nhanh chóng phục hồi thể lực.', 'Nơi khô ráo, tránh ẩm.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Tuýp', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-b'),
(58, 'Daily-Vits', 'Vitamin & Khoáng chất', 'Đa vitamin', NULL, 'Vitamin – thực phẩm chức năng', 'https://image.uniqlo.com/UQ/ST3/vn/imagesgoods/451433/item/vngoods_12_451433.jpg', 1, 'Bổ sung dinh dưỡng thiết yếu duy trì sức khỏe tổng thể.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin'),
(59, 'Apisatone', 'Sữa ong chúa, mật ong, vitamin', '10ml', NULL, 'Vitamin – thực phẩm chức năng', 'https://cms-prod.s3-sgn09.fptcloud.com/00000490_siro_ho_astex_90ml_4091_6339.jpg', 1, 'Thuốc bổ chứa sữa ong chúa giúp bồi bổ cơ thể, tăng sức đề kháng.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Hộp', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/sua-ong-chua'),
(60, 'Pediakid Vitamin', '22 Vitamin & Oligo-elements', '125ml', NULL, 'Sản phẩm trẻ em – men – bổ sung', 'https://cms-prod.s3-sgn09.fptcloud.com/00502127_vien_uong_blackmores_mens_performance_multi_bo_sung_vitamin_cho_nam_gioi_50_vien_8350.jpg', 1, 'Bổ sung vitamin và khoáng chất cho trẻ từ 6 tháng tuổi.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-d3'),
(61, 'Fitobimbi Siro', 'Chiết xuất thảo dược Italy', '200ml', NULL, 'Sản phẩm trẻ em – men – bổ sung', 'https://cms-prod.s3-sgn09.fptcloud.com/00021634_nattoenzym_670fu_duoc_hau_giang_3x10_8698_6339.jpg', 1, 'Nhóm siro thảo dược Italy hỗ trợ bé ăn ngon, ngủ ngon.', 'Nơi khô ráo, dưới 30 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/tre-em-va-dinh-duong'),
(62, 'BioAmicus Vitamin D3', 'Vitamin D3 nguyên chất', '10ml', NULL, 'Sản phẩm trẻ em – men – bổ sung', 'https://cms-prod.s3-sgn09.fptcloud.com/00501192_biogaia_protectis_baby_drops_5ml_1863.jpg', 1, 'Vitamin D3 tinh khiết không mùi không vị cho trẻ sơ sinh.', 'Nơi khô ráo, dưới 25 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-d3'),
(63, 'ChildLife Multivitamin', 'Vitamin & Khoáng chất dạng lỏng', '237ml', NULL, 'Sản phẩm trẻ em – men – bổ sung', 'https://cms-prod.s3-sgn09.fptcloud.com/00001037_cefixim_200mg_3x10_cuu_long_5177_6118_1633514757.jpg', 1, 'Cung cấp 23 loại vitamin và khoáng chất cần thiết nhất cho trẻ.', 'Nơi khô ráo, tránh ánh sáng.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '24 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-tre-em'),
(64, 'Wellbaby Syrup', '14 Vitamin & Khoáng chất, Mạch nha', '150ml', NULL, 'Sản phẩm trẻ em – men – bổ sung', 'https://cms-prod.s3-sgn09.fptcloud.com/wellbaby_multi_vitamin_liquid_vitabiotics_ho_tro_suc_khoe_cho_be_150ml_7909_6372_1625907409.jpg', 1, 'Siro bổ sung kẽm, sắt và vitamin cho sự phát triển của bé.', 'Bảo quản nơi khô ráo, dưới 25 độ C.', NULL, NULL, NULL, 1, '2026-02-26 09:49:48', '2026-02-27 11:04:46', '36 tháng', 'active', 'Chai', 'Quá mẫn với thành phần.', 'Có thể tương tác với thuốc khác.', 'Bổ sung vitamin và khoáng chất cho cơ thể.', 'Không dùng quá liều khuyến cáo.', 'Rối loạn tiêu hóa nhẹ.', 'Uống sau bữa ăn.', 'https://www.vinmec.com/vie/chu-de/vitamin-tre-em');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `drug_shipments`
--

CREATE TABLE `drug_shipments` (
  `id` bigint(20) NOT NULL,
  `shipment_code` varchar(100) NOT NULL,
  `batch_id` bigint(20) NOT NULL,
  `from_company_id` bigint(20) NOT NULL,
  `to_company_id` bigint(20) NOT NULL,
  `quantity` int(11) NOT NULL,
  `shipment_date` timestamp NULL DEFAULT NULL,
  `expected_delivery_date` timestamp NULL DEFAULT NULL,
  `actual_delivery_date` timestamp NULL DEFAULT NULL,
  `shipment_status` enum('PENDING','IN_TRANSIT','DELIVERED','CANCELLED') DEFAULT 'PENDING',
  `shipping_method` enum('FULL_BATCH','PARTIAL_BATCH','ITEM_LEVEL') DEFAULT 'FULL_BATCH' COMMENT 'Phương thức giao hàng',
  `items_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Danh sách item_id trong shipment này' CHECK (json_valid(`items_json`)),
  `actual_items_count` int(11) DEFAULT 0 COMMENT 'Số lượng items thực tế',
  `create_tx_hash` varchar(66) DEFAULT NULL,
  `receive_tx_hash` varchar(66) DEFAULT NULL,
  `blockchain_merkle_root` varchar(66) DEFAULT NULL COMMENT 'Merkle root của danh sách items',
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `drug_shipments`
--

INSERT INTO `drug_shipments` (`id`, `shipment_code`, `batch_id`, `from_company_id`, `to_company_id`, `quantity`, `shipment_date`, `expected_delivery_date`, `actual_delivery_date`, `shipment_status`, `shipping_method`, `items_json`, `actual_items_count`, `create_tx_hash`, `receive_tx_hash`, `blockchain_merkle_root`, `notes`, `created_at`, `updated_at`) VALUES
(62, 'SHIP-17721161169867612', 96, 1, 3, 5, '2026-02-26 14:28:37', '2026-03-01 14:28:37', '2026-02-26 14:28:45', 'DELIVERED', 'FULL_BATCH', NULL, 0, '0xf8460752b6755dab28b2ca36a9dde34c8ccc9b6cb41674e7609b5bc5d4e89f84', '0xa12f4f17ffeb10fd96254ab7582e37e5899d2fdaf024ceaaf43adb65b0d2613d', NULL, '{\"block_number\":\"8\",\"to_address\":\"0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC\",\"original_shipment_id\":\"17721161169867612\",\"from_address\":\"0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266\",\"tracking_info\":\"Shipment to CPC1 Hà Nội\",\"is_synced\":true}', '2026-02-26 14:28:37', '2026-02-26 14:28:45'),
(63, 'SHIP-1772116135807', 96, 3, 5, 5, '2026-02-26 14:28:55', '2026-02-28 14:28:55', '2026-02-26 14:29:03', 'DELIVERED', 'FULL_BATCH', NULL, 0, '0xce726f3d6f397e9c1512d406e2c2d72ae340e970ca5b3eb1e99f748628bd67ab', '0xffda611663d742abc71c3f7265c821edf52e83bad90a5b07df9215adfb1b9bfd', NULL, '{\"block_number\":\"10\",\"to_address\":\"0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65\",\"original_shipment_id\":\"1772116135807\",\"from_address\":\"0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC\",\"tracking_info\":\"SH135001BFZ\",\"is_synced\":true, \"shipment_info\": {\"driver_name\": \"\", \"pharmacy_address\": \"Phú Diễn, Hà Nội\", \"user_notes\": \"\", \"pharmacy_name\": \"Hiệu thuốc Long Châu\", \"transport_method\": \"Xe tải\", \"tracking_number\": \"SH135001BFZ\", \"driver_phone\": \"\"}}', '2026-02-26 14:28:55', '2026-02-26 14:29:03'),
(64, 'SHIP-17730177493633103', 97, 1, 3, 5, '2026-03-09 00:55:49', '2026-03-12 00:55:49', NULL, 'PENDING', 'FULL_BATCH', NULL, 0, '0xbcd13fecbc2ba15820db1263fc89c88cc18224130f5a848c70e2a5e62730215f', NULL, NULL, '{\"block_number\":\"2\",\"to_address\":\"0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC\",\"original_shipment_id\":\"17730177493633103\",\"from_address\":\"0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266\",\"tracking_info\":\"Shipment to CPC1 Hà Nội\",\"is_synced\":true}', '2026-03-09 00:55:49', '2026-03-09 00:55:49'),
(65, 'SHIP-17744258165544474', 98, 1, 3, 5, '2026-03-25 08:03:36', '2026-03-28 08:03:36', '2026-03-25 08:03:50', 'DELIVERED', 'FULL_BATCH', NULL, 0, '0xb7921efc508a368910d0a58e8a2034992ad70df1a928f9926b590139b4983184', '0x82cb0babaf5910aeb9a6d5d4380a00755b37def1a24dbff9e0b3d1fc51cf62e6', NULL, '{\"block_number\":\"3\",\"to_address\":\"0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC\",\"original_shipment_id\":\"17744258165544474\",\"from_address\":\"0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266\",\"tracking_info\":\"Shipment to CPC1 Hà Nội\",\"is_synced\":true}', '2026-03-25 08:03:36', '2026-03-25 08:03:50'),
(66, 'SHIP-1774425856897', 98, 3, 5, 5, '2026-03-25 08:04:16', '2026-03-27 08:04:16', '2026-03-25 08:04:33', 'DELIVERED', 'FULL_BATCH', NULL, 0, '0xb67e75873e6bb280e16f137a2d837727289ec86d914e767b32a9e2f0a4604b6b', '0xf55077780de5b185dde52166ea5953c42c20c0e05da78f77a146e528811f687f', NULL, '{\"block_number\":\"5\",\"to_address\":\"0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65\",\"original_shipment_id\":\"1774425856897\",\"from_address\":\"0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC\",\"tracking_info\":\"SH8532363W6\",\"is_synced\":true, \"shipment_info\": {\"driver_name\": \"\", \"pharmacy_address\": \"Phú Diễn, Hà Nội\", \"user_notes\": \"\", \"pharmacy_name\": \"Hiệu thuốc Long Châu\", \"transport_method\": \"Xe tải\", \"tracking_number\": \"SH8532363W6\", \"driver_phone\": \"\"}}', '2026-03-25 08:04:16', '2026-03-25 08:04:33');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `drug_verification_records`
--

CREATE TABLE `drug_verification_records` (
  `id` bigint(20) NOT NULL,
  `batch_id` bigint(20) NOT NULL,
  `verified_by_company_id` bigint(20) DEFAULT NULL,
  `verification_type` enum('QR_SCAN','MANUAL_CHECK','COUNTER_VERIFICATION','CONSUMER_SCAN') NOT NULL,
  `verification_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `verification_location` varchar(255) DEFAULT NULL,
  `is_authentic` tinyint(1) NOT NULL,
  `verification_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`verification_result`)),
  `scanner_device` varchar(100) DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `blockchain_verified` tinyint(1) DEFAULT 0,
  `blockchain_query_time_ms` int(11) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `inventory_movements`
--

CREATE TABLE `inventory_movements` (
  `id` bigint(20) NOT NULL,
  `inventory_type` enum('DISTRIBUTOR','PHARMACY') NOT NULL,
  `inventory_id` bigint(20) NOT NULL COMMENT 'ID của distributor_inventory hoặc pharmacy_inventory',
  `movement_type` enum('RECEIVE','SHIP_OUT','SALE','RETURN','ADJUSTMENT','DAMAGE','EXPIRED','RECALL','TRANSFER') NOT NULL,
  `quantity_before` int(11) NOT NULL,
  `quantity_change` int(11) NOT NULL COMMENT 'Số dương = tăng, số âm = giảm',
  `quantity_after` int(11) NOT NULL,
  `related_shipment_id` bigint(20) DEFAULT NULL,
  `related_sale_id` bigint(20) DEFAULT NULL COMMENT 'FK to sales/orders (if exists)',
  `performed_by` varchar(36) DEFAULT NULL COMMENT 'User thực hiện',
  `reason` varchar(500) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `movement_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Lịch sử biến động kho (nhập/xuất/bán/điều chỉnh)';

--
-- Đang đổ dữ liệu cho bảng `inventory_movements`
--

INSERT INTO `inventory_movements` (`id`, `inventory_type`, `inventory_id`, `movement_type`, `quantity_before`, `quantity_change`, `quantity_after`, `related_shipment_id`, `related_sale_id`, `performed_by`, `reason`, `notes`, `movement_date`, `created_at`) VALUES
(1, 'DISTRIBUTOR', 1, 'SHIP_OUT', 500, -500, 0, NULL, NULL, NULL, 'Auto-logged by trigger', NULL, '2025-09-30 09:38:30', '2025-09-30 09:38:30'),
(2, 'DISTRIBUTOR', 1, 'RECEIVE', 0, 500, 500, NULL, NULL, NULL, 'Auto-logged by trigger', NULL, '2025-09-30 10:05:02', '2025-09-30 10:05:02'),
(3, 'DISTRIBUTOR', 1, 'SHIP_OUT', 500, -499, 1, NULL, NULL, NULL, 'Auto-logged by trigger', NULL, '2025-09-30 10:07:59', '2025-09-30 10:07:59');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `manufacturer_users`
--

CREATE TABLE `manufacturer_users` (
  `id` varchar(36) NOT NULL,
  `company_address` varchar(500) DEFAULT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `is_verified` bit(1) NOT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `license_number` varchar(50) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','DISTRIBUTOR','MANUFACTURER','PHARMACY','USER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `manufacturer_users`
--

INSERT INTO `manufacturer_users` (`id`, `company_address`, `company_name`, `created_at`, `email`, `is_verified`, `last_login_at`, `license_number`, `name`, `password_hash`, `role`, `updated_at`, `wallet_address`) VALUES
('manu-001-uuid', '123 Đường ABC, Quận 1, TP.HCM', 'Công ty Cổ phần Dược Hậu Giang\r\n', '2025-09-29 19:00:12.000000', 'manufacturer@demo.com', b'1', '2026-03-25 14:43:46.000000', NULL, 'Lê Văn Hậu Giang', '$2a$10$3alAj9C1NKgRuow/bkcqpOLNtuEQq9RoYnFArbg3UuuQqNAROw32a', 'MANUFACTURER', '2026-03-25 14:43:46.000000', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266'),
('manu-002-uuid', '456 Đường DEF, Hà Nội', 'Traphaco JSC', '2025-09-29 19:00:12.000000', 'traphaco@demo.com', b'1', NULL, NULL, 'Phạm Thị Traphaco', '$2a$10$tyE/oab7/nH7Al93GIpA3eGPL1xmxn0xTNq5RFKCn7Wbv2Q1zSayW', 'MANUFACTURER', NULL, '0x70997970C51812dc3A010C7d01b50e0d17dc79C8');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `medical_specialties`
--

CREATE TABLE `medical_specialties` (
  `id` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `parent_specialty_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `medications`
--

CREATE TABLE `medications` (
  `id` int(11) NOT NULL,
  `brand_names` text DEFAULT NULL,
  `common_dosages` text DEFAULT NULL,
  `contraindications` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `dosage_forms` text DEFAULT NULL,
  `drug_class` varchar(100) DEFAULT NULL,
  `generic_name` varchar(200) DEFAULT NULL,
  `interactions` text DEFAULT NULL,
  `is_controlled_substance` bit(1) NOT NULL,
  `name` varchar(200) NOT NULL,
  `pregnancy_category` enum('A','B','C','D','X','unknown') DEFAULT NULL,
  `requires_prescription` bit(1) NOT NULL,
  `side_effects` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `medication_reminders`
--

CREATE TABLE `medication_reminders` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `notes` text DEFAULT NULL,
  `notification_sent` bit(1) DEFAULT NULL,
  `notification_sent_at` datetime(6) DEFAULT NULL,
  `record_id` bigint(20) NOT NULL,
  `response_time_minutes` int(11) DEFAULT NULL,
  `scheduled_date` date NOT NULL,
  `scheduled_time` time(6) NOT NULL,
  `skip_reason` varchar(255) DEFAULT NULL,
  `status` enum('MISSED','NOTIFIED','PENDING','SKIPPED','TAKEN') NOT NULL,
  `taken_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `medication_reminders`
--

INSERT INTO `medication_reminders` (`id`, `created_at`, `notes`, `notification_sent`, `notification_sent_at`, `record_id`, `response_time_minutes`, `scheduled_date`, `scheduled_time`, `skip_reason`, `status`, `taken_at`, `updated_at`) VALUES
(412, '2026-02-27 15:36:25.000000', 'Đã uống qua thông báo', b'0', NULL, 28, 1, '2026-02-27', '16:37:00.000000', NULL, 'TAKEN', '2026-02-27 15:38:06.000000', '2026-02-27 15:38:06.000000'),
(413, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-02-27', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(414, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-02-28', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(415, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-02-28', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(416, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-02-28', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(417, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-01', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(418, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-01', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(419, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-01', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(420, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-02', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(421, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-02', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(422, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-02', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(423, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-03', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(424, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-03', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(425, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-03', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(426, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-04', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(427, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-04', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(428, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-04', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(429, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-05', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(430, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-05', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(431, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-05', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(432, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-06', '09:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(433, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-06', '16:37:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(434, '2026-02-27 15:36:25.000000', NULL, b'0', NULL, 28, NULL, '2026-03-06', '21:00:00.000000', NULL, 'PENDING', NULL, '2026-02-27 15:36:25.000000'),
(435, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-25', '13:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(436, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-25', '20:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(437, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-26', '13:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(438, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-26', '20:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(439, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-27', '13:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(440, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-27', '20:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(441, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-28', '13:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000'),
(442, '2026-03-25 14:52:32.000000', NULL, b'0', NULL, 29, NULL, '2026-03-28', '20:00:00.000000', NULL, 'PENDING', NULL, '2026-03-25 14:52:32.000000');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `ml_model_performance`
--

CREATE TABLE `ml_model_performance` (
  `id` int(11) NOT NULL,
  `accuracy_rate` decimal(5,2) DEFAULT NULL,
  `correct_predictions` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `evaluation_date` date NOT NULL,
  `f1_score` decimal(5,2) DEFAULT NULL,
  `false_negatives` int(11) NOT NULL,
  `false_positives` int(11) NOT NULL,
  `model_name` varchar(100) NOT NULL,
  `model_version` varchar(50) NOT NULL,
  `notes` text DEFAULT NULL,
  `performance_by_severity` text DEFAULT NULL,
  `performance_by_specialty` text DEFAULT NULL,
  `precision_score` decimal(5,2) DEFAULT NULL,
  `recall_score` decimal(5,2) DEFAULT NULL,
  `total_predictions` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `news_articles`
--

CREATE TABLE `news_articles` (
  `id` int(11) NOT NULL,
  `author` varchar(200) DEFAULT NULL,
  `bookmark_count` int(11) NOT NULL,
  `content` longtext DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `fact_checked` bit(1) NOT NULL,
  `image_url` varchar(1000) DEFAULT NULL,
  `last_updated` datetime(6) DEFAULT NULL,
  `medical_accuracy_score` decimal(3,2) DEFAULT NULL,
  `publication_date` datetime(6) DEFAULT NULL,
  `reading_level` enum('advanced','basic','intermediate') DEFAULT NULL,
  `related_diseases` text DEFAULT NULL,
  `related_symptoms` text DEFAULT NULL,
  `share_count` int(11) NOT NULL,
  `source_name` varchar(100) DEFAULT NULL,
  `summary` text DEFAULT NULL,
  `target_audience` enum('general_public','patients','professionals') DEFAULT NULL,
  `title` varchar(500) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `url` varchar(1000) DEFAULT NULL,
  `view_count` int(11) NOT NULL,
  `primary_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `ownership_history`
--

CREATE TABLE `ownership_history` (
  `id` bigint(20) NOT NULL,
  `batch_id` bigint(20) NOT NULL,
  `from_company_id` bigint(20) DEFAULT NULL,
  `to_company_id` bigint(20) NOT NULL,
  `transfer_date` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `transfer_type` enum('MANUFACTURE','SHIPMENT','DELIVERY','SALE') NOT NULL,
  `quantity_transferred` int(11) DEFAULT NULL,
  `blockchain_tx_hash` varchar(66) NOT NULL,
  `block_number` bigint(20) DEFAULT NULL,
  `gas_used` bigint(20) DEFAULT NULL,
  `event_log_index` int(11) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `pharmacy_inventory`
--

CREATE TABLE `pharmacy_inventory` (
  `id` bigint(20) NOT NULL,
  `pharmacy_id` bigint(20) NOT NULL COMMENT 'FK to pharma_companies (PHARMACY)',
  `batch_id` bigint(20) NOT NULL COMMENT 'FK to drug_batches',
  `drug_name` varchar(255) NOT NULL,
  `manufacturer` varchar(255) NOT NULL,
  `batch_number` varchar(100) NOT NULL,
  `quantity` int(11) NOT NULL DEFAULT 0 COMMENT 'Số lượng hiện có trong kho',
  `reserved_quantity` int(11) NOT NULL DEFAULT 0 COMMENT 'Số lượng đã được đặt trước (đơn hàng online)',
  `available_quantity` int(11) GENERATED ALWAYS AS (`quantity` - `reserved_quantity`) STORED COMMENT 'Số lượng có thể bán',
  `sold_quantity` int(11) NOT NULL DEFAULT 0 COMMENT 'Tổng số lượng đã bán',
  `manufacture_date` timestamp NULL DEFAULT NULL,
  `expiry_date` timestamp NULL DEFAULT NULL,
  `qr_code` varchar(1000) DEFAULT NULL,
  `shelf_location` varchar(100) DEFAULT 'Kệ chính' COMMENT 'Vị trí: Kệ A, Tủ B, Quầy 1, v.v.',
  `storage_conditions` varchar(500) DEFAULT NULL,
  `storage_temperature` varchar(50) DEFAULT NULL,
  `cost_price` decimal(15,2) DEFAULT 0.00 COMMENT 'Giá vốn (mua từ NPP)',
  `retail_price` decimal(15,2) DEFAULT 0.00 COMMENT 'Giá bán lẻ cho khách hàng',
  `discount_price` decimal(15,2) DEFAULT NULL COMMENT 'Giá khuyến mãi (nếu có)',
  `total_value` decimal(15,2) GENERATED ALWAYS AS (`quantity` * `cost_price`) STORED,
  `profit_margin` decimal(5,2) GENERATED ALWAYS AS (case when `cost_price` > 0 then (`retail_price` - `cost_price`) / `cost_price` * 100 else 0 end) STORED COMMENT 'Lợi nhuận %',
  `status` enum('IN_STOCK','LOW_STOCK','OUT_OF_STOCK','EXPIRING_SOON','EXPIRED','RECALL') DEFAULT 'IN_STOCK',
  `min_stock_level` int(11) DEFAULT 20 COMMENT 'Ngưỡng cảnh báo hết hàng',
  `max_stock_level` int(11) DEFAULT 500 COMMENT 'Ngưỡng tồn kho tối đa',
  `reorder_point` int(11) DEFAULT 30 COMMENT 'Điểm đặt hàng lại',
  `blockchain_batch_id` decimal(38,0) DEFAULT NULL,
  `receive_tx_hash` varchar(66) DEFAULT NULL,
  `current_owner_address` varchar(42) DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT 0 COMMENT 'Đã xác thực trên blockchain',
  `received_from_distributor_id` bigint(20) DEFAULT NULL COMMENT 'Nhận từ NPP nào',
  `received_shipment_id` bigint(20) DEFAULT NULL COMMENT 'FK to drug_shipments',
  `received_date` timestamp NULL DEFAULT NULL,
  `received_quantity` int(11) DEFAULT 0 COMMENT 'Số lượng nhập ban đầu',
  `first_sale_date` timestamp NULL DEFAULT NULL COMMENT 'Ngày bán đầu tiên',
  `last_sale_date` timestamp NULL DEFAULT NULL COMMENT 'Ngày bán gần nhất',
  `average_daily_sales` decimal(10,2) DEFAULT 0.00 COMMENT 'Trung bình bán/ngày',
  `days_of_supply` int(11) GENERATED ALWAYS AS (case when `average_daily_sales` > 0 then floor(`available_quantity` / `average_daily_sales`) else 999 end) STORED COMMENT 'Số ngày đủ bán',
  `requires_prescription` tinyint(1) DEFAULT 0 COMMENT 'Cần đơn thuốc',
  `controlled_substance` tinyint(1) DEFAULT 0 COMMENT 'Thuốc kiểm soát đặc biệt',
  `is_featured` tinyint(1) DEFAULT 0 COMMENT 'Sản phẩm nổi bật',
  `is_on_sale` tinyint(1) DEFAULT 0 COMMENT 'Đang khuyến mãi',
  `display_order` int(11) DEFAULT 999 COMMENT 'Thứ tự hiển thị',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(36) DEFAULT NULL,
  `updated_by` varchar(36) DEFAULT NULL,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Quản lý kho của hiệu thuốc';

--
-- Đang đổ dữ liệu cho bảng `pharmacy_inventory`
--

INSERT INTO `pharmacy_inventory` (`id`, `pharmacy_id`, `batch_id`, `drug_name`, `manufacturer`, `batch_number`, `quantity`, `reserved_quantity`, `sold_quantity`, `manufacture_date`, `expiry_date`, `qr_code`, `shelf_location`, `storage_conditions`, `storage_temperature`, `cost_price`, `retail_price`, `discount_price`, `status`, `min_stock_level`, `max_stock_level`, `reorder_point`, `blockchain_batch_id`, `receive_tx_hash`, `current_owner_address`, `is_verified`, `received_from_distributor_id`, `received_shipment_id`, `received_date`, `received_quantity`, `first_sale_date`, `last_sale_date`, `average_daily_sales`, `requires_prescription`, `controlled_substance`, `is_featured`, `is_on_sale`, `display_order`, `created_at`, `updated_at`, `created_by`, `updated_by`, `notes`) VALUES
(8, 5, 96, 'Azithromycin 500mg', 'Dược Hậu Giang', 'BT202602262049', 5, 0, 0, '2026-02-26 13:49:58', '2028-02-26 16:59:59', 'NCKH-PHARMA-3EF547F65715D3-BT202602262049', 'Kệ chính', NULL, NULL, 0.00, 0.00, NULL, 'IN_STOCK', 20, 500, 30, 17721137980446163, NULL, '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 1, 3, 63, '2026-02-26 14:29:03', 5, NULL, NULL, 0.00, 0, 0, 0, 0, 999, '2026-02-26 14:29:03', '2026-02-26 14:29:03', NULL, NULL, NULL),
(9, 5, 98, 'Klamentin 1g', 'Dược Hậu Giang', 'BT202603091219', 5, 0, 0, '2026-03-09 05:19:01', '2028-03-09 16:59:59', 'NCKH-PHARMA-3EFDA568234FBC-BT202603091219', 'Kệ chính', NULL, NULL, 0.00, 0.00, NULL, 'IN_STOCK', 20, 500, 30, 17730335414636476, NULL, '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 1, 3, 66, '2026-03-25 08:04:33', 5, NULL, NULL, 0.00, 0, 0, 0, 0, 999, '2026-03-25 08:04:33', '2026-03-25 08:04:33', NULL, NULL, NULL);

--
-- Bẫy `pharmacy_inventory`
--
DELIMITER $$
CREATE TRIGGER `trg_pharmacy_inventory_movement_log` AFTER UPDATE ON `pharmacy_inventory` FOR EACH ROW BEGIN
    IF OLD.quantity != NEW.quantity THEN
        INSERT INTO `inventory_movements` (
            `inventory_type`,
            `inventory_id`,
            `movement_type`,
            `quantity_before`,
            `quantity_change`,
            `quantity_after`,
            `reason`,
            `movement_date`
        ) VALUES (
            'PHARMACY',
            NEW.id,
            CASE
                WHEN NEW.quantity > OLD.quantity THEN 'RECEIVE'
                WHEN NEW.quantity < OLD.quantity AND NEW.sold_quantity > OLD.sold_quantity THEN 'SALE'
                WHEN NEW.quantity < OLD.quantity THEN 'ADJUSTMENT'
                ELSE 'ADJUSTMENT'
            END,
            OLD.quantity,
            NEW.quantity - OLD.quantity,
            NEW.quantity,
            'Auto-logged by trigger',
            NOW()
        );
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_pharmacy_inventory_status_update` BEFORE UPDATE ON `pharmacy_inventory` FOR EACH ROW BEGIN
    DECLARE days_to_expiry INT;
    SET days_to_expiry = DATEDIFF(NEW.expiry_date, NOW());

    -- Kiểm tra hết hạn
    IF days_to_expiry < 0 THEN
        SET NEW.status = 'EXPIRED';
    -- Sắp hết hạn (30 ngày)
    ELSEIF days_to_expiry <= 30 THEN
        SET NEW.status = 'EXPIRING_SOON';
    -- Hết hàng
    ELSEIF NEW.available_quantity <= 0 THEN
        SET NEW.status = 'OUT_OF_STOCK';
    -- Tồn kho thấp
    ELSEIF NEW.available_quantity <= NEW.min_stock_level THEN
        SET NEW.status = 'LOW_STOCK';
    ELSE
        SET NEW.status = 'IN_STOCK';
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `pharmacy_users`
--

CREATE TABLE `pharmacy_users` (
  `id` bigint(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `pharmacy_name` varchar(255) NOT NULL,
  `pharmacy_code` varchar(255) DEFAULT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `is_profile_complete` tinyint(1) NOT NULL DEFAULT 0,
  `last_login_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `pharmacy_users`
--

INSERT INTO `pharmacy_users` (`id`, `email`, `password`, `pharmacy_name`, `pharmacy_code`, `wallet_address`, `address`, `phone`, `is_active`, `is_profile_complete`, `last_login_at`, `created_at`, `updated_at`) VALUES
(1, 'pharmacy@ankhang.com', '$2a$10$MDxKhLpYxBs0uadHKywfa.HweyWMBVmyEpahToXZOCWHje1X5nY9u', 'Hiệu thuốc Long Châu', 'HT-2024-001', '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 'Phú diễn, Hà Nội', '0905678901', 1, 1, '2025-12-13 11:01:25', '2025-09-30 07:45:42', '2026-02-15 02:48:06'),
(2, 'pharmacy@pharmacity.vn', '$2a$10$MDxKhLpYxBs0uadHKywfa.HweyWMBVmyEpahToXZOCWHje1X5nY9u', 'Pharmacity Bình Thạnh', 'HT-2024-002', '0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc', '789 Đường PQR, Quận Bình Thạnh, TP.HCM', '0906789012', 1, 1, NULL, '2025-09-30 07:45:42', '2025-09-30 07:45:42');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `pharma_companies`
--

CREATE TABLE `pharma_companies` (
  `id` bigint(20) NOT NULL,
  `name` varchar(255) NOT NULL,
  `company_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY') NOT NULL,
  `wallet_address` varchar(42) DEFAULT NULL,
  `license_number` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `contact_person` varchar(255) DEFAULT NULL,
  `registration_number` varchar(255) DEFAULT NULL,
  `tax_code` varchar(255) DEFAULT NULL,
  `establishment_date` datetime(6) DEFAULT NULL,
  `blockchain_verified` tinyint(1) DEFAULT 0,
  `verification_date` timestamp NULL DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `manufacturer_user_id` varchar(36) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `pharma_companies`
--

INSERT INTO `pharma_companies` (`id`, `name`, `company_type`, `wallet_address`, `license_number`, `address`, `phone`, `email`, `contact_person`, `registration_number`, `tax_code`, `establishment_date`, `blockchain_verified`, `verification_date`, `is_active`, `created_at`, `updated_at`, `manufacturer_user_id`, `status`) VALUES
(1, 'Công ty Dược phẩm ABC', 'MANUFACTURER', '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266', 'NSX-2024-001', '123 Đường ABC, Quận 1, TP.HCM', '0901234567', 'nsx@demo.com', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2025-09-29 05:54:41', 'manu-001-uuid', 'ACTIVE'),
(2, 'Traphaco JSC', 'MANUFACTURER', '0x70997970C51812dc3A010C7d01b50e0d17dc79C8', 'NSX-2024-002', '456 Đường DEF, Hà Nội', '0902345678', 'info@traphaco.com.vn', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2025-09-29 12:00:12', NULL, 'ACTIVE'),
(3, 'Nhà phân phối XYZ', 'DISTRIBUTOR', '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC', 'NPP-2024-001', '789 Đường GHI, Quận 3, TP.HCM', '0903456789', 'npp@xyz.com', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2025-09-29 12:00:12', NULL, 'ACTIVE'),
(4, 'Zuellig Pharma Vietnam', 'DISTRIBUTOR', '0x90F79bf6EB2c4f870365E785982E1f101E93b906', 'NPP-2024-002', '321 Đường JKL, Quận 7, TP.HCM', '0904567890', 'vietnam@zuelligpharma.com', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2025-09-29 12:00:12', NULL, 'ACTIVE'),
(5, 'Hiệu thuốc Long Châu', 'PHARMACY', '0x15d34AAf54267DB7D7c367839AAf71A00a2C6A65', 'HT-2024-001', 'Phú Diễn, Hà Nội', '0905678901', 'info@ankhang.com', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2026-02-15 02:48:49', NULL, 'ACTIVE'),
(6, 'Pharmacity Bình Thạnh', 'PHARMACY', '0x9965507D1a55bcC2695C58ba16FB37d819B0A4dc', 'HT-2024-002', '789 Đường PQR, Quận Bình Thạnh, TP.HCM', '0906789012', 'binhthahn@pharmacity.vn', NULL, NULL, NULL, NULL, 0, NULL, 1, '2025-09-29 12:00:12', '2025-09-29 12:00:12', NULL, 'ACTIVE');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `product_items`
--

CREATE TABLE `product_items` (
  `id` bigint(20) NOT NULL,
  `item_code` varchar(100) NOT NULL COMMENT 'Mã sản phẩm duy nhất, VD: PARA-BATCH001-0001',
  `batch_id` bigint(20) NOT NULL COMMENT 'FK to drug_batches',
  `drug_product_id` bigint(20) NOT NULL COMMENT 'FK to drug_products',
  `qr_code_data` varchar(500) DEFAULT NULL COMMENT 'Dữ liệu trong QR (URL hoặc JSON)',
  `qr_image_path` varchar(500) DEFAULT NULL COMMENT 'Đường dẫn file QR image (S3/MinIO)',
  `qr_generated_at` datetime(6) DEFAULT NULL,
  `current_status` enum('MANUFACTURED','IN_WAREHOUSE','IN_TRANSIT','DELIVERED','SOLD','EXPIRED','RECALLED','DAMAGED') NOT NULL DEFAULT 'MANUFACTURED' COMMENT 'Trạng thái hiện tại của sản phẩm',
  `current_owner_id` bigint(20) DEFAULT NULL COMMENT 'ID công ty đang sở hữu',
  `current_owner_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER') DEFAULT NULL COMMENT 'Loại chủ sở hữu',
  `manufacture_date` datetime(6) NOT NULL,
  `expiry_date` datetime(6) NOT NULL,
  `blockchain_token_id` bigint(20) DEFAULT NULL COMMENT 'Token ID trên blockchain (nếu dùng NFT)',
  `blockchain_merkle_proof` text DEFAULT NULL COMMENT 'Merkle proof để verify',
  `is_blockchain_synced` bit(1) NOT NULL DEFAULT b'0',
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `updated_at` datetime(6) DEFAULT NULL ON UPDATE current_timestamp(6),
  `sold_at` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Bảng quản lý từng sản phẩm riêng lẻ với QR code tracking';

--
-- Đang đổ dữ liệu cho bảng `product_items`
--

INSERT INTO `product_items` (`id`, `item_code`, `batch_id`, `drug_product_id`, `qr_code_data`, `qr_image_path`, `qr_generated_at`, `current_status`, `current_owner_id`, `current_owner_type`, `manufacture_date`, `expiry_date`, `blockchain_token_id`, `blockchain_merkle_proof`, `is_blockchain_synced`, `notes`, `created_at`, `updated_at`, `sold_at`) VALUES
(500, 'AN500-2555689', 96, 28, 'AN500-2555689', NULL, '2026-02-26 20:49:58.000000', 'SOLD', NULL, 'CONSUMER', '2026-02-26 20:49:58.000000', '2028-02-26 23:59:59.000000', NULL, '0x8fbe1c4dfe0925696911aea0eb117daefcaf74bc52dc7f8b2261a8b8352649c1,0x9bdeab5c4ad4ba2a5d215486ca9d600d74c90edd678ac2f69c4aa179ecc65829,0xcb9970f7f7b3110d0b5b99c707679ad42f70ccb9fcd9e18e594e8cfe035fd41e', b'1', '\n[Blockchain] Registered in TX: 0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0 at block 2026-02-26T20:49:58.360016200\n[SALE] Sold at Hiệu thuốc An Khang on 2026-02-26T21:29:22.814278100. Buyer: Walk-in customer', '2026-02-26 20:49:58.000000', '2026-02-26 21:29:22.000000', '2026-02-26 21:29:22.000000'),
(501, 'AN500-7030250', 96, 28, 'AN500-7030250', NULL, '2026-02-26 20:49:58.000000', 'SOLD', NULL, 'CONSUMER', '2026-02-26 20:49:58.000000', '2028-02-26 23:59:59.000000', NULL, '0xc396ffb08fbd0319314ec99616f4ee6437723ff3b4d278bc1d08709bb3686f1e,0xe0d489b28daee7873ec8f8800cc1d9b74abb222d47edc180be7b4f5d015f92c9,0xcb9970f7f7b3110d0b5b99c707679ad42f70ccb9fcd9e18e594e8cfe035fd41e', b'1', '\n[Blockchain] Registered in TX: 0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0 at block 2026-02-26T20:49:58.360016200', '2026-02-26 20:49:58.000000', '2026-02-27 09:33:15.000000', NULL),
(502, 'AN500-7941538', 96, 28, 'AN500-7941538', NULL, '2026-02-26 20:49:58.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-02-26 20:49:58.000000', '2028-02-26 23:59:59.000000', NULL, '0x2c03a3ab52610f2dc03ccdf8a1c7a985fdcb745ff7ee408fd46dafb639e375d5', b'1', '\n[Blockchain] Registered in TX: 0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0 at block 2026-02-26T20:49:58.361015400', '2026-02-26 20:49:58.000000', '2026-02-26 20:49:58.000000', NULL),
(503, 'AN500-2017645', 96, 28, 'AN500-2017645', NULL, '2026-02-26 20:49:58.000000', 'DAMAGED', NULL, 'MANUFACTURER', '2026-02-26 20:49:58.000000', '2028-02-26 23:59:59.000000', NULL, '0x8c5a45fa6d7aad3d4a8b9f4e6c03c68deeda7bbe71d729f23890cc68a08767d0,0x9bdeab5c4ad4ba2a5d215486ca9d600d74c90edd678ac2f69c4aa179ecc65829,0xcb9970f7f7b3110d0b5b99c707679ad42f70ccb9fcd9e18e594e8cfe035fd41e', b'1', '\n[Blockchain] Registered in TX: 0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0 at block 2026-02-26T20:49:58.361015400\n[DAMAGED REPORT] Time: 2026-03-01T22:12:47.758564800. Reason: Bị hỏng vỡ hộp. Image: http://10.10.33.186:8080/uploads/40c03469-4470-470d-a23a-023c8767a96c.jpg', '2026-02-26 20:49:58.000000', '2026-03-01 22:12:47.000000', NULL),
(504, 'AN500-9051289', 96, 28, 'AN500-9051289', NULL, '2026-02-26 20:49:58.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-02-26 20:49:58.000000', '2028-02-26 23:59:59.000000', NULL, '0x9a69dd310964c57c8c4b6ed856b405f1c96fb80e1d52c4e4db16f141d823de38,0xe0d489b28daee7873ec8f8800cc1d9b74abb222d47edc180be7b4f5d015f92c9,0xcb9970f7f7b3110d0b5b99c707679ad42f70ccb9fcd9e18e594e8cfe035fd41e', b'1', '\n[Blockchain] Registered in TX: 0x90b92a774d4708b355abece126d2efc7740ea72030fd118953d22d1f2428e6a0 at block 2026-02-26T20:49:58.361015400', '2026-02-26 20:49:58.000000', '2026-02-26 20:49:58.000000', NULL),
(505, 'AN-9329264', 97, 19, 'AN-9329264', NULL, '2026-03-09 07:55:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 07:55:37.000000', '2028-03-09 23:59:59.000000', NULL, '0x85eb7f9158b0b3b4d69eba076796519ee6533c9d5afc91f758dd28059c7a279e,0xc8d16799b2d633c5474a8ecf4bafde57fd752d57954347061fe021dfef3e944c,0xe5ec00b771c5ecf1c83fa5c63b663a54a8c2f0dc9069bf809d283ccbcf30d273', b'1', '\n[Blockchain] Registered in TX: 0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb at block 2026-03-09T07:55:37.448524300', '2026-03-09 07:55:37.000000', '2026-03-09 07:55:37.000000', NULL),
(506, 'AN-9775788', 97, 19, 'AN-9775788', NULL, '2026-03-09 07:55:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 07:55:37.000000', '2028-03-09 23:59:59.000000', NULL, '0xb1b4e6fa73ba34160708e5b60730fe93651564016fa04d5fb3c2f5ee92a43e10,0x08338aaf2ae52c18bbd2ea93a8936c09c226ece02033802d43342724471b31b6,0xe5ec00b771c5ecf1c83fa5c63b663a54a8c2f0dc9069bf809d283ccbcf30d273', b'1', '\n[Blockchain] Registered in TX: 0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb at block 2026-03-09T07:55:37.449072200', '2026-03-09 07:55:37.000000', '2026-03-09 07:55:37.000000', NULL),
(507, 'AN-8263104', 97, 19, 'AN-8263104', NULL, '2026-03-09 07:55:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 07:55:37.000000', '2028-03-09 23:59:59.000000', NULL, '0xeafb007c2365650619b8d51f3e33197be089a4feb8165b96fe58dd9b6bc5ad57', b'1', '\n[Blockchain] Registered in TX: 0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb at block 2026-03-09T07:55:37.449072200', '2026-03-09 07:55:37.000000', '2026-03-09 07:55:37.000000', NULL),
(508, 'AN-2982607', 97, 19, 'AN-2982607', NULL, '2026-03-09 07:55:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 07:55:37.000000', '2028-03-09 23:59:59.000000', NULL, '0x7188e3bbfdbe8c3a0e20d45e09094e60e0c4d31ee01a958f86abfb094a66829c,0xc8d16799b2d633c5474a8ecf4bafde57fd752d57954347061fe021dfef3e944c,0xe5ec00b771c5ecf1c83fa5c63b663a54a8c2f0dc9069bf809d283ccbcf30d273', b'1', '\n[Blockchain] Registered in TX: 0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb at block 2026-03-09T07:55:37.449613', '2026-03-09 07:55:37.000000', '2026-03-09 07:55:37.000000', NULL),
(509, 'AN-0816565', 97, 19, 'AN-0816565', NULL, '2026-03-09 07:55:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 07:55:37.000000', '2028-03-09 23:59:59.000000', NULL, '0xcfddb438f6a0bc424093673a0d954ace41f8804f1b7ef5aeb936fc1a729ea6b5,0x08338aaf2ae52c18bbd2ea93a8936c09c226ece02033802d43342724471b31b6,0xe5ec00b771c5ecf1c83fa5c63b663a54a8c2f0dc9069bf809d283ccbcf30d273', b'1', '\n[Blockchain] Registered in TX: 0x019b0a0d063e8528fd7895449a3502b795152907fe5b64ed05c5f8e4955d58fb at block 2026-03-09T07:55:37.450185500', '2026-03-09 07:55:37.000000', '2026-03-09 07:55:37.000000', NULL),
(510, 'KN-6279230', 98, 21, 'KN-6279230', NULL, '2026-03-09 12:19:01.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 12:19:01.000000', '2028-03-09 23:59:59.000000', NULL, '0xb90c5dba7e7143a9ed2fa084f88285e6e50fbfa0d163f18efde82c48f04b3f57', b'1', '\n[Blockchain] Registered in TX: 0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f at block 2026-03-09T12:19:01.675155300', '2026-03-09 12:19:01.000000', '2026-03-09 12:19:01.000000', NULL),
(511, 'KN-8261669', 98, 21, 'KN-8261669', NULL, '2026-03-09 12:19:01.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 12:19:01.000000', '2028-03-09 23:59:59.000000', NULL, '0xdfd2a35fad3040593862c3ccf6df4a56f4df7c2153486b175c79999b191d279a,0xdeb11c0f6ed65372636b6746417d7abb6d870809ed73de1bb44610d0f6b5697f,0xf7d1e745d5376ee82ff4274d5c79cf32c471959ab7842911b4fab7ffff90c8d4', b'1', '\n[Blockchain] Registered in TX: 0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f at block 2026-03-09T12:19:01.676967600', '2026-03-09 12:19:01.000000', '2026-03-09 12:19:01.000000', NULL),
(512, 'KN-0766684', 98, 21, 'KN-0766684', NULL, '2026-03-09 12:19:01.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 12:19:01.000000', '2028-03-09 23:59:59.000000', NULL, '0x3cb3c477014eb0a4422de83babf00459d482e39a393a15262e7b0f50ced28949,0x6f31fa4ddab869571b47495ab735fe961899ee0566ad70623211739de4ea7b79,0xf7d1e745d5376ee82ff4274d5c79cf32c471959ab7842911b4fab7ffff90c8d4', b'1', '\n[Blockchain] Registered in TX: 0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f at block 2026-03-09T12:19:01.677664400', '2026-03-09 12:19:01.000000', '2026-03-09 12:19:01.000000', NULL),
(513, 'KN-8848334', 98, 21, 'KN-8848334', NULL, '2026-03-09 12:19:01.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 12:19:01.000000', '2028-03-09 23:59:59.000000', NULL, '0x691df68224da92e4e090bd03b65c08cdd8f7f38de1ff2b03a001047a1119783b,0x6f31fa4ddab869571b47495ab735fe961899ee0566ad70623211739de4ea7b79,0xf7d1e745d5376ee82ff4274d5c79cf32c471959ab7842911b4fab7ffff90c8d4', b'1', '\n[Blockchain] Registered in TX: 0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f at block 2026-03-09T12:19:01.678590200', '2026-03-09 12:19:01.000000', '2026-03-09 12:19:01.000000', NULL),
(514, 'KN-1098805', 98, 21, 'KN-1098805', NULL, '2026-03-09 12:19:01.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-09 12:19:01.000000', '2028-03-09 23:59:59.000000', NULL, '0xbf2366d4d3269665b0d50f7c2445580065c41945b3f87d4452cce1753b16cbf9,0xdeb11c0f6ed65372636b6746417d7abb6d870809ed73de1bb44610d0f6b5697f,0xf7d1e745d5376ee82ff4274d5c79cf32c471959ab7842911b4fab7ffff90c8d4', b'1', '\n[Blockchain] Registered in TX: 0x7118b536bc89a29fdeb40f6548fd9fdb90120cbc4348ac91fc9e8a7a87749d2f at block 2026-03-09T12:19:01.678590200', '2026-03-09 12:19:01.000000', '2026-03-09 12:19:01.000000', NULL),
(515, 'AN-9625992', 99, 19, 'AN-9625992', NULL, '2026-03-25 14:47:07.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 14:47:07.000000', '2028-03-31 23:59:59.000000', NULL, '', b'1', '\n[Blockchain] Registered in TX: 0x2c7b4ea024aea85978db5c3f4de83b53319caed672c62e975db150827bba5941 at block 2026-03-25T14:47:07.993820600', '2026-03-25 14:47:07.000000', '2026-03-25 14:47:08.000000', NULL),
(516, 'KN-4076148', 100, 20, 'KN-4076148', NULL, '2026-03-25 15:00:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 15:00:37.000000', '2028-03-25 23:59:59.000000', NULL, '0x2cd5ce27dd50d082d5dd585927209d136c0171dbee38d14fc48e22812a87bd08', b'1', '\n[Blockchain] Registered in TX: 0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf at block 2026-03-25T15:00:37.285254900', '2026-03-25 15:00:37.000000', '2026-03-25 15:00:37.000000', NULL),
(517, 'KN-7745200', 100, 20, 'KN-7745200', NULL, '2026-03-25 15:00:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 15:00:37.000000', '2028-03-25 23:59:59.000000', NULL, '0x6752479b00da6aac9112e3b03dc5b56c5404850fb32463d410372cc68dd6253b,0x39a0a10b3e74b73008bfd1e4b6b3cdfec49a11e7d1be6417143fe75cba97cc9d,0xfa893afbae4354a0f100de6dfea769c4272a98d5dab1a86d12df4566cf24a8f7', b'1', '\n[Blockchain] Registered in TX: 0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf at block 2026-03-25T15:00:37.285254900', '2026-03-25 15:00:37.000000', '2026-03-25 15:00:37.000000', NULL),
(518, 'KN-2730902', 100, 20, 'KN-2730902', NULL, '2026-03-25 15:00:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 15:00:37.000000', '2028-03-25 23:59:59.000000', NULL, '0x8733a96459c71fba4233eacfb25fa27b3249684521d6c3ec554d343c6cfa8d24,0x39a0a10b3e74b73008bfd1e4b6b3cdfec49a11e7d1be6417143fe75cba97cc9d,0xfa893afbae4354a0f100de6dfea769c4272a98d5dab1a86d12df4566cf24a8f7', b'1', '\n[Blockchain] Registered in TX: 0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf at block 2026-03-25T15:00:37.285254900', '2026-03-25 15:00:37.000000', '2026-03-25 15:00:37.000000', NULL),
(519, 'KN-7423970', 100, 20, 'KN-7423970', NULL, '2026-03-25 15:00:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 15:00:37.000000', '2028-03-25 23:59:59.000000', NULL, '0x3802c7ed14eb133ef4732e77b0aab14787d0e8b84a90d82ec4bc21453c417748,0x12f15b4fbbeb55d5acbc9894ae010c815e5b9460c1e07677ca2814b7c2764f71,0xfa893afbae4354a0f100de6dfea769c4272a98d5dab1a86d12df4566cf24a8f7', b'1', '\n[Blockchain] Registered in TX: 0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf at block 2026-03-25T15:00:37.290260400', '2026-03-25 15:00:37.000000', '2026-03-25 15:00:37.000000', NULL),
(520, 'KN-6141035', 100, 20, 'KN-6141035', NULL, '2026-03-25 15:00:37.000000', 'MANUFACTURED', NULL, 'MANUFACTURER', '2026-03-25 15:00:37.000000', '2028-03-25 23:59:59.000000', NULL, '0x4a298a21cb086b5f63481a9f4d20f5d6a9b203af3c26cb7446b401b3b9e93890,0x12f15b4fbbeb55d5acbc9894ae010c815e5b9460c1e07677ca2814b7c2764f71,0xfa893afbae4354a0f100de6dfea769c4272a98d5dab1a86d12df4566cf24a8f7', b'1', '\n[Blockchain] Registered in TX: 0xb42abf0586a4f518a733dd89a58750e4c040aec4892eea5b6be70ea83d9e7ddf at block 2026-03-25T15:00:37.290260400', '2026-03-25 15:00:37.000000', '2026-03-25 15:00:37.000000', NULL);

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `product_item_movements`
--

CREATE TABLE `product_item_movements` (
  `id` bigint(20) NOT NULL,
  `item_id` bigint(20) NOT NULL COMMENT 'FK to product_items',
  `batch_id` bigint(20) NOT NULL COMMENT 'Denormalized để query nhanh theo lô',
  `movement_type` enum('MANUFACTURE','TRANSFER','SHIP','RECEIVE','SALE','RETURN','RECALL','DAMAGE','EXPIRE') NOT NULL COMMENT 'Loại di chuyển/sự kiện',
  `from_company_id` bigint(20) DEFAULT NULL COMMENT 'Từ công ty nào (NULL nếu MANUFACTURE)',
  `from_company_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER') DEFAULT NULL,
  `from_company_name` varchar(255) DEFAULT NULL COMMENT 'Denormalized cho hiển thị',
  `from_address_detail` text DEFAULT NULL,
  `to_company_id` bigint(20) NOT NULL COMMENT 'Đến công ty nào',
  `to_company_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER') NOT NULL,
  `to_company_name` varchar(255) DEFAULT NULL COMMENT 'Denormalized cho hiển thị',
  `to_address_detail` text DEFAULT NULL,
  `shipment_id` bigint(20) DEFAULT NULL COMMENT 'FK to drug_shipments (nếu có)',
  `related_transaction_id` varchar(100) DEFAULT NULL COMMENT 'Mã đơn hàng/giao dịch',
  `movement_timestamp` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `location_lat` decimal(10,7) DEFAULT NULL COMMENT 'GPS latitude',
  `location_lng` decimal(10,7) DEFAULT NULL COMMENT 'GPS longitude',
  `verified_by` varchar(100) DEFAULT NULL COMMENT 'User thực hiện',
  `verification_method` enum('QR_SCAN','MANUAL','AUTO') DEFAULT 'AUTO',
  `blockchain_tx_hash` varchar(66) DEFAULT NULL,
  `blockchain_block_number` bigint(20) DEFAULT NULL,
  `is_blockchain_synced` bit(1) NOT NULL DEFAULT b'0',
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Lịch sử di chuyển của từng sản phẩm (audit trail)';

--
-- Đang đổ dữ liệu cho bảng `product_item_movements`
--

INSERT INTO `product_item_movements` (`id`, `item_id`, `batch_id`, `movement_type`, `from_company_id`, `from_company_type`, `from_company_name`, `from_address_detail`, `to_company_id`, `to_company_type`, `to_company_name`, `to_address_detail`, `shipment_id`, `related_transaction_id`, `movement_timestamp`, `location_lat`, `location_lng`, `verified_by`, `verification_method`, `blockchain_tx_hash`, `blockchain_block_number`, `is_blockchain_synced`, `notes`, `created_at`) VALUES
(485, 500, 96, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-02-26 20:49:58.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-02-26 20:49:58.000000'),
(486, 501, 96, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-02-26 20:49:58.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-02-26 20:49:58.000000'),
(487, 502, 96, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-02-26 20:49:58.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-02-26 20:49:58.000000'),
(488, 503, 96, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-02-26 20:49:58.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-02-26 20:49:58.000000'),
(489, 504, 96, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-02-26 20:49:58.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-02-26 20:49:58.000000'),
(490, 500, 96, 'SALE', 1, 'PHARMACY', NULL, NULL, 1, 'CONSUMER', 'Khach hang', NULL, NULL, NULL, '2026-02-26 21:29:22.000000', NULL, NULL, NULL, 'QR_SCAN', NULL, NULL, b'0', 'Counter sale at Hiệu thuốc An Khang', '2026-02-26 21:29:22.000000'),
(491, 501, 96, 'SALE', NULL, 'MANUFACTURER', 'MANUFACTURER', NULL, 1, 'CONSUMER', 'Nguoi tieu dung', NULL, NULL, NULL, '2026-02-27 09:33:15.000000', NULL, NULL, NULL, 'QR_SCAN', NULL, NULL, b'0', 'Dispensed to consumer', '2026-02-27 09:33:15.000000'),
(492, 503, 96, 'DAMAGE', 1, 'PHARMACY', NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-01 22:12:47.000000', NULL, NULL, NULL, 'QR_SCAN', NULL, NULL, b'0', 'Damaged at Pharmacy. Reason: Bị hỏng vỡ hộp. Image: http://10.10.33.186:8080/uploads/40c03469-4470-470d-a23a-023c8767a96c.jpg', '2026-03-01 22:12:47.000000'),
(493, 505, 97, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 07:55:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 07:55:37.000000'),
(494, 506, 97, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 07:55:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 07:55:37.000000'),
(495, 507, 97, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 07:55:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 07:55:37.000000'),
(496, 508, 97, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 07:55:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 07:55:37.000000'),
(497, 509, 97, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 07:55:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 07:55:37.000000'),
(498, 510, 98, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 12:19:01.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 12:19:01.000000'),
(499, 511, 98, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 12:19:01.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 12:19:01.000000'),
(500, 512, 98, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 12:19:01.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 12:19:01.000000'),
(501, 513, 98, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 12:19:01.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 12:19:01.000000'),
(502, 514, 98, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-09 12:19:01.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-09 12:19:01.000000'),
(503, 515, 99, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 14:47:07.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 14:47:07.000000'),
(504, 516, 100, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 15:00:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 15:00:37.000000'),
(505, 517, 100, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 15:00:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 15:00:37.000000'),
(506, 518, 100, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 15:00:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 15:00:37.000000'),
(507, 519, 100, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 15:00:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 15:00:37.000000'),
(508, 520, 100, 'MANUFACTURE', NULL, NULL, NULL, NULL, 1, 'MANUFACTURER', 'Dược Hậu Giang', NULL, NULL, NULL, '2026-03-25 15:00:37.000000', NULL, NULL, NULL, 'AUTO', NULL, NULL, b'0', 'Item manufactured', '2026-03-25 15:00:37.000000');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `product_item_verifications`
--

CREATE TABLE `product_item_verifications` (
  `id` bigint(20) NOT NULL,
  `item_id` bigint(20) NOT NULL COMMENT 'FK to product_items',
  `scanner_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER','INSPECTOR','ANONYMOUS') NOT NULL COMMENT 'Ai quét QR',
  `scanner_id` varchar(100) DEFAULT NULL COMMENT 'User ID (nếu đăng nhập)',
  `scanner_name` varchar(255) DEFAULT NULL,
  `scanner_device_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Device info: OS, browser, app version' CHECK (json_valid(`scanner_device_info`)),
  `scanner_location` varchar(500) DEFAULT NULL COMMENT 'Địa điểm quét',
  `location_lat` decimal(10,7) DEFAULT NULL,
  `location_lng` decimal(10,7) DEFAULT NULL,
  `verification_result` enum('AUTHENTIC','SUSPICIOUS','COUNTERFEIT','EXPIRED','RECALLED') NOT NULL DEFAULT 'AUTHENTIC',
  `verification_details` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Chi tiết xác thực' CHECK (json_valid(`verification_details`)),
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `scan_timestamp` datetime(6) NOT NULL DEFAULT current_timestamp(6),
  `blockchain_verified` bit(1) DEFAULT b'0',
  `blockchain_query_time_ms` int(11) DEFAULT NULL COMMENT 'Thời gian query blockchain',
  `notes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT current_timestamp(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Lịch sử quét QR code (security & analytics)';

--
-- Đang đổ dữ liệu cho bảng `product_item_verifications`
--

INSERT INTO `product_item_verifications` (`id`, `item_id`, `scanner_type`, `scanner_id`, `scanner_name`, `scanner_device_info`, `scanner_location`, `location_lat`, `location_lng`, `verification_result`, `verification_details`, `ip_address`, `user_agent`, `scan_timestamp`, `blockchain_verified`, `blockchain_query_time_ms`, `notes`, `created_at`) VALUES
(112, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 20:50:31.000000', b'1', NULL, NULL, '2026-02-26 20:50:31.000000'),
(113, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 21:29:15.000000', b'1', NULL, NULL, '2026-02-26 21:29:15.000000'),
(114, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 21:29:26.000000', b'1', NULL, NULL, '2026-02-26 21:29:26.000000'),
(115, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 21:29:31.000000', b'1', NULL, NULL, '2026-02-26 21:29:31.000000'),
(116, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 22:31:27.000000', b'1', NULL, NULL, '2026-02-26 22:31:27.000000'),
(117, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 22:32:25.000000', b'1', NULL, NULL, '2026-02-26 22:32:25.000000'),
(118, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 22:33:25.000000', b'1', NULL, NULL, '2026-02-26 22:33:25.000000'),
(119, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-26 22:33:32.000000', b'1', NULL, NULL, '2026-02-26 22:33:32.000000'),
(120, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:19:01.000000', b'1', NULL, NULL, '2026-02-27 00:19:01.000000'),
(121, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:23:12.000000', b'1', NULL, NULL, '2026-02-27 00:23:12.000000'),
(122, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:25:23.000000', b'1', NULL, NULL, '2026-02-27 00:25:23.000000'),
(123, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:26:29.000000', b'1', NULL, NULL, '2026-02-27 00:26:29.000000'),
(124, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:26:39.000000', b'1', NULL, NULL, '2026-02-27 00:26:39.000000'),
(125, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:27:48.000000', b'1', NULL, NULL, '2026-02-27 00:27:48.000000'),
(126, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:03.000000', b'1', NULL, NULL, '2026-02-27 00:28:03.000000'),
(127, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:25.000000', b'1', NULL, NULL, '2026-02-27 00:28:25.000000'),
(128, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:26.000000', b'1', NULL, NULL, '2026-02-27 00:28:26.000000'),
(129, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:27.000000', b'1', NULL, NULL, '2026-02-27 00:28:27.000000'),
(130, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:35.000000', b'1', NULL, NULL, '2026-02-27 00:28:35.000000'),
(131, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:35.000000', b'1', NULL, NULL, '2026-02-27 00:28:35.000000'),
(132, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:39.000000', b'1', NULL, NULL, '2026-02-27 00:28:39.000000'),
(133, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:28:53.000000', b'1', NULL, NULL, '2026-02-27 00:28:53.000000'),
(134, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:29:02.000000', b'1', NULL, NULL, '2026-02-27 00:29:02.000000'),
(135, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:29:03.000000', b'1', NULL, NULL, '2026-02-27 00:29:03.000000'),
(136, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:29:07.000000', b'1', NULL, NULL, '2026-02-27 00:29:07.000000'),
(137, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:31:18.000000', b'1', NULL, NULL, '2026-02-27 00:31:18.000000'),
(138, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:44:17.000000', b'1', NULL, NULL, '2026-02-27 00:44:17.000000'),
(139, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:44:29.000000', b'1', NULL, NULL, '2026-02-27 00:44:29.000000'),
(140, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:44:39.000000', b'1', NULL, NULL, '2026-02-27 00:44:39.000000'),
(141, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:44:42.000000', b'1', NULL, NULL, '2026-02-27 00:44:42.000000'),
(142, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:45:17.000000', b'1', NULL, NULL, '2026-02-27 00:45:17.000000'),
(143, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:57:02.000000', b'1', NULL, NULL, '2026-02-27 00:57:02.000000'),
(144, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:07.000000', b'1', NULL, NULL, '2026-02-27 00:58:07.000000'),
(145, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:14.000000', b'1', NULL, NULL, '2026-02-27 00:58:14.000000'),
(146, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:19.000000', b'1', NULL, NULL, '2026-02-27 00:58:19.000000'),
(147, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:20.000000', b'1', NULL, NULL, '2026-02-27 00:58:20.000000'),
(148, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:42.000000', b'1', NULL, NULL, '2026-02-27 00:58:42.000000'),
(149, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 00:58:47.000000', b'1', NULL, NULL, '2026-02-27 00:58:47.000000'),
(150, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 01:00:34.000000', b'1', NULL, NULL, '2026-02-27 01:00:34.000000'),
(151, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 01:00:37.000000', b'1', NULL, NULL, '2026-02-27 01:00:37.000000'),
(152, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 09:31:22.000000', b'1', NULL, NULL, '2026-02-27 09:31:22.000000'),
(153, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 09:31:34.000000', b'1', NULL, NULL, '2026-02-27 09:31:34.000000'),
(154, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 09:33:19.000000', b'1', NULL, NULL, '2026-02-27 09:33:19.000000'),
(155, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 09:33:30.000000', b'1', NULL, NULL, '2026-02-27 09:33:30.000000'),
(156, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:07:06.000000', b'1', NULL, NULL, '2026-02-27 10:07:06.000000'),
(157, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:09:04.000000', b'1', NULL, NULL, '2026-02-27 10:09:04.000000'),
(158, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:14:47.000000', b'1', NULL, NULL, '2026-02-27 10:14:47.000000'),
(159, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:18:35.000000', b'1', NULL, NULL, '2026-02-27 10:18:35.000000'),
(160, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:20:33.000000', b'1', NULL, NULL, '2026-02-27 10:20:33.000000'),
(161, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:20:35.000000', b'1', NULL, NULL, '2026-02-27 10:20:35.000000'),
(162, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:20:39.000000', b'1', NULL, NULL, '2026-02-27 10:20:39.000000'),
(163, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:20:41.000000', b'1', NULL, NULL, '2026-02-27 10:20:41.000000'),
(164, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:20:55.000000', b'1', NULL, NULL, '2026-02-27 10:20:55.000000'),
(165, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:21:31.000000', b'1', NULL, NULL, '2026-02-27 10:21:31.000000'),
(166, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 10:49:12.000000', b'1', NULL, NULL, '2026-02-27 10:49:12.000000'),
(167, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:04:05.000000', b'1', NULL, NULL, '2026-02-27 11:04:05.000000'),
(168, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:04:12.000000', b'1', NULL, NULL, '2026-02-27 11:04:12.000000'),
(169, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:30:41.000000', b'1', NULL, NULL, '2026-02-27 11:30:41.000000'),
(170, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:30:52.000000', b'1', NULL, NULL, '2026-02-27 11:30:52.000000'),
(171, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:31:14.000000', b'1', NULL, NULL, '2026-02-27 11:31:14.000000'),
(172, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:31:22.000000', b'1', NULL, NULL, '2026-02-27 11:31:22.000000'),
(173, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:32:58.000000', b'1', NULL, NULL, '2026-02-27 11:32:58.000000'),
(174, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:37:59.000000', b'1', NULL, NULL, '2026-02-27 11:37:59.000000'),
(175, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:39:58.000000', b'1', NULL, NULL, '2026-02-27 11:39:58.000000'),
(176, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:40:02.000000', b'1', NULL, NULL, '2026-02-27 11:40:02.000000'),
(177, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:40:11.000000', b'1', NULL, NULL, '2026-02-27 11:40:11.000000'),
(178, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:40:19.000000', b'1', NULL, NULL, '2026-02-27 11:40:19.000000'),
(179, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:40:23.000000', b'1', NULL, NULL, '2026-02-27 11:40:23.000000'),
(180, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:40:53.000000', b'1', NULL, NULL, '2026-02-27 11:40:53.000000'),
(181, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 11:45:41.000000', b'1', NULL, NULL, '2026-02-27 11:45:41.000000'),
(182, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 13:11:49.000000', b'1', NULL, NULL, '2026-02-27 13:11:49.000000'),
(183, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 15:07:14.000000', b'1', NULL, NULL, '2026-02-27 15:07:14.000000'),
(184, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-27 15:07:19.000000', b'1', NULL, NULL, '2026-02-27 15:07:19.000000'),
(185, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-02-28 00:35:19.000000', b'1', NULL, NULL, '2026-02-28 00:35:19.000000'),
(186, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 21:14:41.000000', b'1', NULL, NULL, '2026-03-01 21:14:41.000000'),
(187, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 22:12:51.000000', b'1', NULL, NULL, '2026-03-01 22:12:51.000000'),
(188, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 22:12:56.000000', b'1', NULL, NULL, '2026-03-01 22:12:56.000000'),
(189, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 22:12:56.000000', b'1', NULL, NULL, '2026-03-01 22:12:56.000000'),
(190, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 22:19:58.000000', b'1', NULL, NULL, '2026-03-01 22:19:58.000000'),
(191, 503, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-01 22:23:22.000000', b'1', NULL, NULL, '2026-03-01 22:23:22.000000'),
(192, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:52:12.000000', b'1', NULL, NULL, '2026-03-25 14:52:12.000000'),
(193, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:53:17.000000', b'1', NULL, NULL, '2026-03-25 14:53:17.000000'),
(194, 502, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:53:24.000000', b'1', NULL, NULL, '2026-03-25 14:53:24.000000'),
(195, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:53:28.000000', b'1', NULL, NULL, '2026-03-25 14:53:28.000000'),
(196, 501, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:53:34.000000', b'1', NULL, NULL, '2026-03-25 14:53:34.000000'),
(197, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:53:40.000000', b'1', NULL, NULL, '2026-03-25 14:53:40.000000'),
(198, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 14:56:06.000000', b'1', NULL, NULL, '2026-03-25 14:56:06.000000'),
(199, 500, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:02:05.000000', b'1', NULL, NULL, '2026-03-25 15:02:05.000000'),
(200, 516, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:06:49.000000', b'1', NULL, NULL, '2026-03-25 15:06:49.000000'),
(201, 515, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:06:55.000000', b'1', NULL, NULL, '2026-03-25 15:06:55.000000'),
(202, 516, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:07:21.000000', b'1', NULL, NULL, '2026-03-25 15:07:21.000000'),
(203, 510, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:07:46.000000', b'1', NULL, NULL, '2026-03-25 15:07:46.000000'),
(204, 510, 'CONSUMER', NULL, NULL, NULL, NULL, NULL, NULL, 'AUTHENTIC', NULL, NULL, NULL, '2026-03-25 15:08:44.000000', b'1', NULL, NULL, '2026-03-25 15:08:44.000000');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `provinces`
--

CREATE TABLE `provinces` (
  `id` int(11) NOT NULL,
  `climate` enum('subtropical','temperate','tropical') DEFAULT NULL,
  `code` varchar(10) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `endemic_diseases` text DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `region` enum('central','north','south') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `symptoms`
--

CREATE TABLE `symptoms` (
  `id` int(11) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `common_causes` text DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `measurement_unit` varchar(50) DEFAULT NULL,
  `name` varchar(200) NOT NULL,
  `red_flag_indicators` text DEFAULT NULL,
  `related_body_systems` text DEFAULT NULL,
  `severity_scale` enum('ABSENT_PRESENT','MILD_SEVERE','ONE_TO_TEN') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `users`
--

CREATE TABLE `users` (
  `id` varchar(36) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_login_at` timestamp NULL DEFAULT NULL,
  `is_profile_complete` tinyint(1) DEFAULT 0,
  `is_active` tinyint(1) DEFAULT 1,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `users`
--

INSERT INTO `users` (`id`, `email`, `password_hash`, `name`, `created_at`, `last_login_at`, `is_profile_complete`, `is_active`, `updated_at`) VALUES
('076e2d56-8c6e-40f0-b07f-b997442cb66e', 'lebadaomac@gmail.com', '$2a$10$uX7xbqFgg/cx8AfoDfiSde625P/TZb/jvw0T6Rp0WIpUSha8zD5qq', 'quyen', '2025-09-09 10:05:56', '2026-02-24 16:17:56', 1, 1, '2026-02-24 16:17:56'),
('c95e23bb-7a92-47af-a2b3-48d5e9d8a3f7', 'quyen@gmail.com', '$2a$10$/nvz1ezBv0JZs8gZE/cU6e/EUvk2qBLlGJQG3IbEdihr9liBVfTgq', 'quyen', '2025-09-09 20:38:33', NULL, 0, 1, '2025-09-09 20:38:33');

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_allergies`
--

CREATE TABLE `user_allergies` (
  `id` int(11) NOT NULL,
  `confirmed_by_test` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `first_reaction_date` date DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `last_reaction_date` date DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `severity` enum('anaphylaxis','mild','moderate','severe') DEFAULT NULL,
  `symptoms_experienced` text DEFAULT NULL,
  `test_type` varchar(100) DEFAULT NULL,
  `treatment_required` text DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `allergen_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_analytics`
--

CREATE TABLE `user_analytics` (
  `id` int(11) NOT NULL,
  `articles_read` int(11) NOT NULL,
  `average_satisfaction` decimal(3,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `diagnoses_received` int(11) NOT NULL,
  `features_used` text DEFAULT NULL,
  `helpful_responses` int(11) NOT NULL,
  `recommendations_followed` int(11) NOT NULL,
  `session_date` date NOT NULL,
  `symptoms_reported` int(11) NOT NULL,
  `total_responses` int(11) NOT NULL,
  `total_sessions` int(11) NOT NULL,
  `total_time_minutes` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_blockchain_addresses`
--

CREATE TABLE `user_blockchain_addresses` (
  `id` bigint(20) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `blockchain_address` varchar(42) NOT NULL,
  `address_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER') NOT NULL,
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `private_key_encrypted` text DEFAULT NULL COMMENT 'Encrypted private key (optional)',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_chronic_diseases`
--

CREATE TABLE `user_chronic_diseases` (
  `id` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_status` enum('active','cured','managed','remission') DEFAULT NULL,
  `diagnosed_by` varchar(200) DEFAULT NULL,
  `diagnosed_date` date DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `severity_current` enum('mild','moderate','severe') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `disease_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_demographics`
--

CREATE TABLE `user_demographics` (
  `id` int(11) NOT NULL,
  `birth_month` int(11) DEFAULT NULL,
  `birth_year` int(11) NOT NULL,
  `blood_type` enum('AB_NEGATIVE','AB_POSITIVE','A_NEGATIVE','A_POSITIVE','B_NEGATIVE','B_POSITIVE','O_NEGATIVE','O_POSITIVE','unknown') DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `education_level` enum('bachelor','high_school','master','other','phd','primary','secondary') DEFAULT NULL,
  `gender` enum('female','male','other') NOT NULL,
  `height_cm` int(11) DEFAULT NULL,
  `occupation` varchar(100) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `weight_kg` decimal(5,2) DEFAULT NULL,
  `province_id` int(11) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_family_history`
--

CREATE TABLE `user_family_history` (
  `id` int(11) NOT NULL,
  `age_of_onset` int(11) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_deceased` bit(1) NOT NULL,
  `notes` text DEFAULT NULL,
  `relationship` enum('aunt_uncle','cousin','father','grandparent','mother','other','sibling') DEFAULT NULL,
  `cause_of_death` int(11) DEFAULT NULL,
  `disease_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_lifestyle`
--

CREATE TABLE `user_lifestyle` (
  `id` int(11) NOT NULL,
  `alcohol_frequency` enum('daily','never','rarely','weekly') DEFAULT NULL,
  `alcohol_type_preference` text DEFAULT NULL,
  `alcohol_units_per_week` decimal(4,1) DEFAULT NULL,
  `chemical_exposure` bit(1) NOT NULL,
  `cigarettes_per_day` int(11) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `diet_type` enum('keto','omnivore','other','paleo','vegan','vegetarian') DEFAULT NULL,
  `exercise_duration_minutes` int(11) DEFAULT NULL,
  `exercise_frequency` enum('daily','none','rare','weekly') DEFAULT NULL,
  `exercise_intensity` enum('light','moderate','vigorous') DEFAULT NULL,
  `exercise_types` text DEFAULT NULL,
  `meals_per_day` int(11) DEFAULT NULL,
  `mental_health_status` enum('excellent','fair','good','poor') DEFAULT NULL,
  `physical_demands` enum('heavy','light','moderate','sedentary') DEFAULT NULL,
  `sleep_disorders` text DEFAULT NULL,
  `sleep_hours_average` decimal(3,1) DEFAULT NULL,
  `sleep_quality` enum('excellent','fair','good','poor') DEFAULT NULL,
  `smoking_quit_age` int(11) DEFAULT NULL,
  `smoking_start_age` int(11) DEFAULT NULL,
  `smoking_status` enum('current','former','never') NOT NULL,
  `stress_level` enum('high','low','moderate','severe') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `water_intake_liters` decimal(3,1) DEFAULT NULL,
  `work_environment` enum('industrial','medical','office','other','outdoor') DEFAULT NULL,
  `years_smoked` int(11) DEFAULT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_medications`
--

CREATE TABLE `user_medications` (
  `id` int(11) NOT NULL,
  `adherence_level` enum('excellent','fair','good','poor') DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `dosage` varchar(50) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `frequency` varchar(50) DEFAULT NULL,
  `indication` varchar(200) DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `notes` text DEFAULT NULL,
  `prescribed_by` varchar(200) DEFAULT NULL,
  `route` enum('inhaled','injection','oral','other','topical') DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `medication_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_medication_records`
--

CREATE TABLE `user_medication_records` (
  `id` bigint(20) NOT NULL,
  `adherence_rate` double DEFAULT NULL,
  `batch_number` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `dispense_instruction_id` bigint(20) DEFAULT NULL,
  `dosage` varchar(50) DEFAULT NULL,
  `drug_name` varchar(255) NOT NULL,
  `end_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `frequency` int(11) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_completed` bit(1) DEFAULT NULL,
  `is_paused` bit(1) DEFAULT NULL,
  `item_code` varchar(100) DEFAULT NULL,
  `manufacturer` varchar(255) DEFAULT NULL,
  `meal_relation` enum('AFTER','ANY','BEFORE','WITH') DEFAULT NULL,
  `missed_doses` int(11) DEFAULT NULL,
  `notes` text DEFAULT NULL,
  `pause_reason` varchar(255) DEFAULT NULL,
  `pharmacy_id` bigint(20) DEFAULT NULL,
  `pharmacy_name` varchar(100) DEFAULT NULL,
  `product_item_id` bigint(20) DEFAULT NULL,
  `purchased_at` datetime(6) DEFAULT NULL,
  `reminder_times` varchar(100) DEFAULT NULL,
  `special_instructions` text DEFAULT NULL,
  `start_date` date NOT NULL,
  `taken_doses` int(11) DEFAULT NULL,
  `total_doses` int(11) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Đang đổ dữ liệu cho bảng `user_medication_records`
--

INSERT INTO `user_medication_records` (`id`, `adherence_rate`, `batch_number`, `created_at`, `dispense_instruction_id`, `dosage`, `drug_name`, `end_date`, `expiry_date`, `frequency`, `is_active`, `is_completed`, `is_paused`, `item_code`, `manufacturer`, `meal_relation`, `missed_doses`, `notes`, `pause_reason`, `pharmacy_id`, `pharmacy_name`, `product_item_id`, `purchased_at`, `reminder_times`, `special_instructions`, `start_date`, `taken_doses`, `total_doses`, `updated_at`, `user_id`) VALUES
(28, NULL, NULL, '2026-02-27 15:07:24.000000', NULL, '500mg', 'Azithromycin 500mg', '2026-03-06', '2028-02-26', 3, b'1', b'0', b'0', NULL, NULL, 'AFTER', 0, NULL, NULL, NULL, NULL, NULL, NULL, '08:00,15:37,20:00', NULL, '2026-02-27', 0, 23, '2026-02-27 15:36:25.000000', 155468011),
(29, NULL, NULL, '2026-03-25 14:52:32.000000', NULL, '1 viên', 'Azithromycin 500mg', '2026-03-28', '2028-02-26', 2, b'1', b'0', b'0', NULL, NULL, 'AFTER', 0, NULL, NULL, NULL, NULL, NULL, NULL, '12:00,19:00', NULL, '2026-03-25', 0, 8, '2026-03-25 14:52:32.000000', 155468011);

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_news_interactions`
--

CREATE TABLE `user_news_interactions` (
  `id` int(11) NOT NULL,
  `interaction_timestamp` datetime(6) NOT NULL,
  `interaction_type` enum('bookmark','like','report','share','view') DEFAULT NULL,
  `reading_time_seconds` int(11) DEFAULT NULL,
  `article_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc bảng cho bảng `user_symptom_reports`
--

CREATE TABLE `user_symptom_reports` (
  `id` int(11) NOT NULL,
  `associated_symptoms` text DEFAULT NULL,
  `duration_hours` int(11) DEFAULT NULL,
  `frequency` enum('constant','intermittent','occasional') DEFAULT NULL,
  `location_body_part` varchar(100) DEFAULT NULL,
  `onset_type` enum('gradual','sudden') DEFAULT NULL,
  `quality_description` text DEFAULT NULL,
  `reported_at` datetime(6) NOT NULL,
  `session_id` varchar(36) DEFAULT NULL,
  `severity` int(11) DEFAULT NULL,
  `triggers` text DEFAULT NULL,
  `symptom_id` int(11) NOT NULL,
  `user_id` varchar(36) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_batch_status`
-- (See below for the actual view)
--
CREATE TABLE `v_batch_status` (
`batch_id` decimal(38,0)
,`drug_name` varchar(255)
,`batch_number` varchar(100)
,`manufacturer` varchar(255)
,`quantity` bigint(20)
,`status` enum('DELIVERED','IN_TRANSIT','MANUFACTURED','SOLD')
,`expiry_date` datetime(6)
,`validity_status` varchar(13)
,`current_owner_name` varchar(255)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_blockchain_overview`
-- (See below for the actual view)
--
CREATE TABLE `v_blockchain_overview` (
`transaction_hash` varchar(66)
,`function_name` varchar(100)
,`from_address` varchar(42)
,`to_address` varchar(42)
,`status` enum('FAILED','PENDING','REVERTED','SUCCESS')
,`gas_used` decimal(38,0)
,`timestamp` datetime(6)
,`batch_drug_name` varchar(255)
,`shipment_code` varchar(100)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_company_users`
-- (See below for the actual view)
--
CREATE TABLE `v_company_users` (
`company_id` bigint(20)
,`company_name` varchar(255)
,`company_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY')
,`wallet_address` varchar(42)
,`license_number` varchar(255)
,`user_email` varchar(255)
,`user_name` varchar(255)
,`user_type` varchar(12)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_distributor_inventory_full`
-- (See below for the actual view)
--
CREATE TABLE `v_distributor_inventory_full` (
`id` bigint(20)
,`distributor_id` bigint(20)
,`batch_id` bigint(20)
,`drug_name` varchar(255)
,`manufacturer` varchar(255)
,`batch_number` varchar(100)
,`quantity` int(11)
,`reserved_quantity` int(11)
,`available_quantity` int(11)
,`manufacture_date` timestamp
,`expiry_date` timestamp
,`qr_code` varchar(1000)
,`warehouse_location` varchar(100)
,`storage_conditions` varchar(500)
,`storage_temperature` varchar(50)
,`unit_price` decimal(15,2)
,`selling_price` decimal(15,2)
,`total_value` decimal(15,2)
,`status` enum('GOOD','LOW_STOCK','EXPIRING_SOON','EXPIRED','QUARANTINE')
,`min_stock_level` int(11)
,`max_stock_level` int(11)
,`blockchain_batch_id` decimal(38,0)
,`receive_tx_hash` varchar(66)
,`current_owner_address` varchar(42)
,`received_from_company_id` bigint(20)
,`received_shipment_id` bigint(20)
,`received_date` timestamp
,`received_quantity` int(11)
,`created_at` timestamp
,`updated_at` timestamp
,`created_by` varchar(36)
,`updated_by` varchar(36)
,`notes` text
,`distributor_name` varchar(255)
,`distributor_wallet` varchar(42)
,`batch_status` enum('DELIVERED','IN_TRANSIT','MANUFACTURED','SOLD')
,`manufacturer_address` varchar(42)
,`days_to_expiry` int(7)
,`alert_status` varchar(13)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_item_movement_summary`
-- (See below for the actual view)
--
CREATE TABLE `v_item_movement_summary` (
`item_id` bigint(20)
,`item_code` varchar(100)
,`current_status` enum('MANUFACTURED','IN_WAREHOUSE','IN_TRANSIT','DELIVERED','SOLD','EXPIRED','RECALLED','DAMAGED')
,`total_movements` bigint(21)
,`first_movement` datetime(6)
,`last_movement` datetime(6)
,`ship_count` decimal(22,0)
,`receive_count` decimal(22,0)
,`sale_count` decimal(22,0)
,`blockchain_synced_count` decimal(22,0)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_item_verification_summary`
-- (See below for the actual view)
--
CREATE TABLE `v_item_verification_summary` (
`item_id` bigint(20)
,`item_code` varchar(100)
,`current_status` enum('MANUFACTURED','IN_WAREHOUSE','IN_TRANSIT','DELIVERED','SOLD','EXPIRED','RECALLED','DAMAGED')
,`total_scans` bigint(21)
,`consumer_scans` decimal(22,0)
,`pharmacy_scans` decimal(22,0)
,`distributor_scans` decimal(22,0)
,`authentic_scans` decimal(22,0)
,`suspicious_scans` decimal(22,0)
,`first_scan` datetime(6)
,`last_scan` datetime(6)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_pharmacy_inventory_full`
-- (See below for the actual view)
--
CREATE TABLE `v_pharmacy_inventory_full` (
`id` bigint(20)
,`pharmacy_id` bigint(20)
,`batch_id` bigint(20)
,`drug_name` varchar(255)
,`manufacturer` varchar(255)
,`batch_number` varchar(100)
,`quantity` int(11)
,`reserved_quantity` int(11)
,`available_quantity` int(11)
,`sold_quantity` int(11)
,`manufacture_date` timestamp
,`expiry_date` timestamp
,`qr_code` varchar(1000)
,`shelf_location` varchar(100)
,`storage_conditions` varchar(500)
,`storage_temperature` varchar(50)
,`cost_price` decimal(15,2)
,`retail_price` decimal(15,2)
,`discount_price` decimal(15,2)
,`total_value` decimal(15,2)
,`profit_margin` decimal(5,2)
,`status` enum('IN_STOCK','LOW_STOCK','OUT_OF_STOCK','EXPIRING_SOON','EXPIRED','RECALL')
,`min_stock_level` int(11)
,`max_stock_level` int(11)
,`reorder_point` int(11)
,`blockchain_batch_id` decimal(38,0)
,`receive_tx_hash` varchar(66)
,`current_owner_address` varchar(42)
,`is_verified` tinyint(1)
,`received_from_distributor_id` bigint(20)
,`received_shipment_id` bigint(20)
,`received_date` timestamp
,`received_quantity` int(11)
,`first_sale_date` timestamp
,`last_sale_date` timestamp
,`average_daily_sales` decimal(10,2)
,`days_of_supply` int(11)
,`requires_prescription` tinyint(1)
,`controlled_substance` tinyint(1)
,`is_featured` tinyint(1)
,`is_on_sale` tinyint(1)
,`display_order` int(11)
,`created_at` timestamp
,`updated_at` timestamp
,`created_by` varchar(36)
,`updated_by` varchar(36)
,`notes` text
,`pharmacy_name` varchar(255)
,`pharmacy_wallet` varchar(42)
,`pharmacy_address` varchar(255)
,`batch_status` enum('DELIVERED','IN_TRANSIT','MANUFACTURED','SOLD')
,`manufacturer_address` varchar(42)
,`days_to_expiry` int(7)
,`alert_status` varchar(13)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_product_items_full`
-- (See below for the actual view)
--
CREATE TABLE `v_product_items_full` (
`item_id` bigint(20)
,`item_code` varchar(100)
,`current_status` enum('MANUFACTURED','IN_WAREHOUSE','IN_TRANSIT','DELIVERED','SOLD','EXPIRED','RECALLED','DAMAGED')
,`current_owner_id` bigint(20)
,`current_owner_type` enum('MANUFACTURER','DISTRIBUTOR','PHARMACY','CONSUMER')
,`qr_code_data` varchar(500)
,`item_manufacture_date` datetime(6)
,`item_expiry_date` datetime(6)
,`is_blockchain_synced` bit(1)
,`item_created_at` datetime(6)
,`batch_id` bigint(20)
,`batch_number` varchar(100)
,`blockchain_batch_id` decimal(38,0)
,`batch_quantity` bigint(20)
,`batch_status` enum('DELIVERED','IN_TRANSIT','MANUFACTURED','SOLD')
,`batch_manufacturer` varchar(255)
,`storage_conditions` varchar(500)
,`product_id` bigint(20)
,`product_name` varchar(255)
,`active_ingredient` varchar(255)
,`dosage` varchar(100)
,`dosage_form` enum('tablet','capsule','syrup','injection','cream','drops','other')
,`category` varchar(100)
,`manufacturer_id` bigint(20)
,`registration_number` varchar(100)
,`product_description` text
,`expiry_status` varchar(13)
,`days_until_expiry` int(7)
);

-- --------------------------------------------------------

--
-- Cấu trúc đóng vai cho view `v_shipment_history`
-- (See below for the actual view)
--
CREATE TABLE `v_shipment_history` (
`shipment_code` varchar(100)
,`batch_id` decimal(38,0)
,`drug_name` varchar(255)
,`batch_number` varchar(100)
,`from_company` varchar(255)
,`to_company` varchar(255)
,`quantity` int(11)
,`shipment_status` enum('PENDING','IN_TRANSIT','DELIVERED','CANCELLED')
,`shipment_date` timestamp
,`expected_delivery_date` timestamp
,`actual_delivery_date` timestamp
);

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_batch_status`
--
DROP TABLE IF EXISTS `v_batch_status`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_batch_status`  AS SELECT `db`.`batch_id` AS `batch_id`, `db`.`drug_name` AS `drug_name`, `db`.`batch_number` AS `batch_number`, `db`.`manufacturer` AS `manufacturer`, `db`.`quantity` AS `quantity`, `db`.`status` AS `status`, `db`.`expiry_date` AS `expiry_date`, CASE WHEN `db`.`expiry_date` < current_timestamp() THEN 'EXPIRED' WHEN to_days(`db`.`expiry_date`) - to_days(current_timestamp()) <= 90 THEN 'EXPIRING_SOON' ELSE 'VALID' END AS `validity_status`, `pc`.`name` AS `current_owner_name` FROM (`drug_batches` `db` left join `pharma_companies` `pc` on(`db`.`current_owner` = `pc`.`wallet_address`)) ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_blockchain_overview`
--
DROP TABLE IF EXISTS `v_blockchain_overview`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_blockchain_overview`  AS SELECT `bt`.`transaction_hash` AS `transaction_hash`, `bt`.`function_name` AS `function_name`, `bt`.`from_address` AS `from_address`, `bt`.`to_address` AS `to_address`, `bt`.`status` AS `status`, `bt`.`gas_used` AS `gas_used`, `bt`.`timestamp` AS `timestamp`, `db`.`drug_name` AS `batch_drug_name`, `ds`.`shipment_code` AS `shipment_code` FROM ((`blockchain_transactions` `bt` left join `drug_batches` `db` on(`bt`.`batch_id` = `db`.`id`)) left join `drug_shipments` `ds` on(`bt`.`shipment_id` = `ds`.`id`)) ORDER BY `bt`.`timestamp` DESC ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_company_users`
--
DROP TABLE IF EXISTS `v_company_users`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_company_users`  AS SELECT `pc`.`id` AS `company_id`, `pc`.`name` AS `company_name`, `pc`.`company_type` AS `company_type`, `pc`.`wallet_address` AS `wallet_address`, `pc`.`license_number` AS `license_number`, coalesce(`du`.`email`,`mu`.`email`) AS `user_email`, coalesce(`du`.`name`,`mu`.`name`) AS `user_name`, CASE WHEN `du`.`id` is not null THEN 'DISTRIBUTOR' WHEN `mu`.`id` is not null THEN 'MANUFACTURER' ELSE NULL END AS `user_type` FROM ((`pharma_companies` `pc` left join `distributor_users` `du` on(`pc`.`wallet_address` = `du`.`wallet_address`)) left join `manufacturer_users` `mu` on(`pc`.`wallet_address` = `mu`.`wallet_address`)) ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_distributor_inventory_full`
--
DROP TABLE IF EXISTS `v_distributor_inventory_full`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_distributor_inventory_full`  AS SELECT `di`.`id` AS `id`, `di`.`distributor_id` AS `distributor_id`, `di`.`batch_id` AS `batch_id`, `di`.`drug_name` AS `drug_name`, `di`.`manufacturer` AS `manufacturer`, `di`.`batch_number` AS `batch_number`, `di`.`quantity` AS `quantity`, `di`.`reserved_quantity` AS `reserved_quantity`, `di`.`available_quantity` AS `available_quantity`, `di`.`manufacture_date` AS `manufacture_date`, `di`.`expiry_date` AS `expiry_date`, `di`.`qr_code` AS `qr_code`, `di`.`warehouse_location` AS `warehouse_location`, `di`.`storage_conditions` AS `storage_conditions`, `di`.`storage_temperature` AS `storage_temperature`, `di`.`unit_price` AS `unit_price`, `di`.`selling_price` AS `selling_price`, `di`.`total_value` AS `total_value`, `di`.`status` AS `status`, `di`.`min_stock_level` AS `min_stock_level`, `di`.`max_stock_level` AS `max_stock_level`, `di`.`blockchain_batch_id` AS `blockchain_batch_id`, `di`.`receive_tx_hash` AS `receive_tx_hash`, `di`.`current_owner_address` AS `current_owner_address`, `di`.`received_from_company_id` AS `received_from_company_id`, `di`.`received_shipment_id` AS `received_shipment_id`, `di`.`received_date` AS `received_date`, `di`.`received_quantity` AS `received_quantity`, `di`.`created_at` AS `created_at`, `di`.`updated_at` AS `updated_at`, `di`.`created_by` AS `created_by`, `di`.`updated_by` AS `updated_by`, `di`.`notes` AS `notes`, `pc`.`name` AS `distributor_name`, `pc`.`wallet_address` AS `distributor_wallet`, `db`.`status` AS `batch_status`, `db`.`manufacturer_address` AS `manufacturer_address`, to_days(`di`.`expiry_date`) - to_days(current_timestamp()) AS `days_to_expiry`, CASE WHEN `di`.`available_quantity` <= `di`.`min_stock_level` THEN 'NEED_REORDER' WHEN to_days(`di`.`expiry_date`) - to_days(current_timestamp()) <= 30 THEN 'EXPIRING_SOON' ELSE 'NORMAL' END AS `alert_status` FROM ((`distributor_inventory` `di` join `pharma_companies` `pc` on(`di`.`distributor_id` = `pc`.`id`)) join `drug_batches` `db` on(`di`.`batch_id` = `db`.`id`)) WHERE `pc`.`company_type` = 'DISTRIBUTOR' ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_item_movement_summary`
--
DROP TABLE IF EXISTS `v_item_movement_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_item_movement_summary`  AS SELECT `pi`.`id` AS `item_id`, `pi`.`item_code` AS `item_code`, `pi`.`current_status` AS `current_status`, count(`pim`.`id`) AS `total_movements`, min(`pim`.`movement_timestamp`) AS `first_movement`, max(`pim`.`movement_timestamp`) AS `last_movement`, sum(case when `pim`.`movement_type` = 'SHIP' then 1 else 0 end) AS `ship_count`, sum(case when `pim`.`movement_type` = 'RECEIVE' then 1 else 0 end) AS `receive_count`, sum(case when `pim`.`movement_type` = 'SALE' then 1 else 0 end) AS `sale_count`, sum(case when `pim`.`is_blockchain_synced` = 0x01 then 1 else 0 end) AS `blockchain_synced_count` FROM (`product_items` `pi` left join `product_item_movements` `pim` on(`pi`.`id` = `pim`.`item_id`)) GROUP BY `pi`.`id`, `pi`.`item_code`, `pi`.`current_status` ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_item_verification_summary`
--
DROP TABLE IF EXISTS `v_item_verification_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_item_verification_summary`  AS SELECT `pi`.`id` AS `item_id`, `pi`.`item_code` AS `item_code`, `pi`.`current_status` AS `current_status`, count(`piv`.`id`) AS `total_scans`, sum(case when `piv`.`scanner_type` = 'CONSUMER' then 1 else 0 end) AS `consumer_scans`, sum(case when `piv`.`scanner_type` = 'PHARMACY' then 1 else 0 end) AS `pharmacy_scans`, sum(case when `piv`.`scanner_type` = 'DISTRIBUTOR' then 1 else 0 end) AS `distributor_scans`, sum(case when `piv`.`verification_result` = 'AUTHENTIC' then 1 else 0 end) AS `authentic_scans`, sum(case when `piv`.`verification_result` = 'SUSPICIOUS' then 1 else 0 end) AS `suspicious_scans`, min(`piv`.`scan_timestamp`) AS `first_scan`, max(`piv`.`scan_timestamp`) AS `last_scan` FROM (`product_items` `pi` left join `product_item_verifications` `piv` on(`pi`.`id` = `piv`.`item_id`)) GROUP BY `pi`.`id`, `pi`.`item_code`, `pi`.`current_status` ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_pharmacy_inventory_full`
--
DROP TABLE IF EXISTS `v_pharmacy_inventory_full`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_pharmacy_inventory_full`  AS SELECT `pi`.`id` AS `id`, `pi`.`pharmacy_id` AS `pharmacy_id`, `pi`.`batch_id` AS `batch_id`, `pi`.`drug_name` AS `drug_name`, `pi`.`manufacturer` AS `manufacturer`, `pi`.`batch_number` AS `batch_number`, `pi`.`quantity` AS `quantity`, `pi`.`reserved_quantity` AS `reserved_quantity`, `pi`.`available_quantity` AS `available_quantity`, `pi`.`sold_quantity` AS `sold_quantity`, `pi`.`manufacture_date` AS `manufacture_date`, `pi`.`expiry_date` AS `expiry_date`, `pi`.`qr_code` AS `qr_code`, `pi`.`shelf_location` AS `shelf_location`, `pi`.`storage_conditions` AS `storage_conditions`, `pi`.`storage_temperature` AS `storage_temperature`, `pi`.`cost_price` AS `cost_price`, `pi`.`retail_price` AS `retail_price`, `pi`.`discount_price` AS `discount_price`, `pi`.`total_value` AS `total_value`, `pi`.`profit_margin` AS `profit_margin`, `pi`.`status` AS `status`, `pi`.`min_stock_level` AS `min_stock_level`, `pi`.`max_stock_level` AS `max_stock_level`, `pi`.`reorder_point` AS `reorder_point`, `pi`.`blockchain_batch_id` AS `blockchain_batch_id`, `pi`.`receive_tx_hash` AS `receive_tx_hash`, `pi`.`current_owner_address` AS `current_owner_address`, `pi`.`is_verified` AS `is_verified`, `pi`.`received_from_distributor_id` AS `received_from_distributor_id`, `pi`.`received_shipment_id` AS `received_shipment_id`, `pi`.`received_date` AS `received_date`, `pi`.`received_quantity` AS `received_quantity`, `pi`.`first_sale_date` AS `first_sale_date`, `pi`.`last_sale_date` AS `last_sale_date`, `pi`.`average_daily_sales` AS `average_daily_sales`, `pi`.`days_of_supply` AS `days_of_supply`, `pi`.`requires_prescription` AS `requires_prescription`, `pi`.`controlled_substance` AS `controlled_substance`, `pi`.`is_featured` AS `is_featured`, `pi`.`is_on_sale` AS `is_on_sale`, `pi`.`display_order` AS `display_order`, `pi`.`created_at` AS `created_at`, `pi`.`updated_at` AS `updated_at`, `pi`.`created_by` AS `created_by`, `pi`.`updated_by` AS `updated_by`, `pi`.`notes` AS `notes`, `pc`.`name` AS `pharmacy_name`, `pc`.`wallet_address` AS `pharmacy_wallet`, `pc`.`address` AS `pharmacy_address`, `db`.`status` AS `batch_status`, `db`.`manufacturer_address` AS `manufacturer_address`, to_days(`pi`.`expiry_date`) - to_days(current_timestamp()) AS `days_to_expiry`, CASE WHEN `pi`.`available_quantity` <= 0 THEN 'OUT_OF_STOCK' WHEN `pi`.`available_quantity` <= `pi`.`reorder_point` THEN 'NEED_REORDER' WHEN to_days(`pi`.`expiry_date`) - to_days(current_timestamp()) <= 30 THEN 'EXPIRING_SOON' ELSE 'NORMAL' END AS `alert_status` FROM ((`pharmacy_inventory` `pi` join `pharma_companies` `pc` on(`pi`.`pharmacy_id` = `pc`.`id`)) join `drug_batches` `db` on(`pi`.`batch_id` = `db`.`id`)) WHERE `pc`.`company_type` = 'PHARMACY' ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_product_items_full`
--
DROP TABLE IF EXISTS `v_product_items_full`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_product_items_full`  AS SELECT `pi`.`id` AS `item_id`, `pi`.`item_code` AS `item_code`, `pi`.`current_status` AS `current_status`, `pi`.`current_owner_id` AS `current_owner_id`, `pi`.`current_owner_type` AS `current_owner_type`, `pi`.`qr_code_data` AS `qr_code_data`, `pi`.`manufacture_date` AS `item_manufacture_date`, `pi`.`expiry_date` AS `item_expiry_date`, `pi`.`is_blockchain_synced` AS `is_blockchain_synced`, `pi`.`created_at` AS `item_created_at`, `db`.`id` AS `batch_id`, `db`.`batch_number` AS `batch_number`, `db`.`batch_id` AS `blockchain_batch_id`, `db`.`quantity` AS `batch_quantity`, `db`.`status` AS `batch_status`, `db`.`manufacturer` AS `batch_manufacturer`, `db`.`storage_conditions` AS `storage_conditions`, `dp`.`id` AS `product_id`, `dp`.`name` AS `product_name`, `dp`.`active_ingredient` AS `active_ingredient`, `dp`.`dosage` AS `dosage`, `dp`.`dosage_form` AS `dosage_form`, `dp`.`category` AS `category`, `dp`.`manufacturer_id` AS `manufacturer_id`, `dp`.`registration_number` AS `registration_number`, `dp`.`description` AS `product_description`, CASE WHEN `pi`.`expiry_date` < current_timestamp() THEN 'EXPIRED' WHEN `pi`.`expiry_date` < current_timestamp() + interval 3 month THEN 'EXPIRING_SOON' ELSE 'VALID' END AS `expiry_status`, to_days(`pi`.`expiry_date`) - to_days(current_timestamp()) AS `days_until_expiry` FROM ((`product_items` `pi` join `drug_batches` `db` on(`pi`.`batch_id` = `db`.`id`)) join `drug_products` `dp` on(`pi`.`drug_product_id` = `dp`.`id`)) ;

-- --------------------------------------------------------

--
-- Cấu trúc cho view `v_shipment_history`
--
DROP TABLE IF EXISTS `v_shipment_history`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_shipment_history`  AS SELECT `ds`.`shipment_code` AS `shipment_code`, `db`.`batch_id` AS `batch_id`, `db`.`drug_name` AS `drug_name`, `db`.`batch_number` AS `batch_number`, `fc`.`name` AS `from_company`, `tc`.`name` AS `to_company`, `ds`.`quantity` AS `quantity`, `ds`.`shipment_status` AS `shipment_status`, `ds`.`shipment_date` AS `shipment_date`, `ds`.`expected_delivery_date` AS `expected_delivery_date`, `ds`.`actual_delivery_date` AS `actual_delivery_date` FROM (((`drug_shipments` `ds` join `drug_batches` `db` on(`ds`.`batch_id` = `db`.`id`)) join `pharma_companies` `fc` on(`ds`.`from_company_id` = `fc`.`id`)) join `pharma_companies` `tc` on(`ds`.`to_company_id` = `tc`.`id`)) ;

--
-- Chỉ mục cho các bảng đã đổ
--

--
-- Chỉ mục cho bảng `allergen_categories`
--
ALTER TABLE `allergen_categories`
  ADD PRIMARY KEY (`id`);

--
-- Chỉ mục cho bảng `app_users`
--
ALTER TABLE `app_users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK4vj92ux8a2eehds1mdvmks473` (`email`),
  ADD UNIQUE KEY `UKr5r4rpieqrjfj44jh43gabif1` (`phone`);

--
-- Chỉ mục cho bảng `blockchain_config`
--
ALTER TABLE `blockchain_config`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `config_key` (`config_key`),
  ADD KEY `idx_config_key` (`config_key`),
  ADD KEY `idx_is_active` (`is_active`);

--
-- Chỉ mục cho bảng `blockchain_events`
--
ALTER TABLE `blockchain_events`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKmy081fpj6u877vqk9jv516sr4` (`transaction_hash`),
  ADD KEY `idx_block_number` (`block_number`),
  ADD KEY `idx_transaction_hash` (`transaction_hash`),
  ADD KEY `idx_event_type` (`event_type`),
  ADD KEY `idx_transaction_hash_be` (`transaction_hash`);

--
-- Chỉ mục cho bảng `blockchain_transactions`
--
ALTER TABLE `blockchain_transactions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKp634ahad8pjht0rqxixrykeu4` (`transaction_hash`),
  ADD UNIQUE KEY `idx_transaction_hash` (`transaction_hash`),
  ADD KEY `idx_from_address` (`from_address`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_shipment_id` (`shipment_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_composite_status_timestamp` (`status`,`timestamp`),
  ADD KEY `idx_from_address_bt` (`from_address`),
  ADD KEY `idx_batch_id_bt` (`batch_id`),
  ADD KEY `idx_shipment_id_bt` (`shipment_id`),
  ADD KEY `idx_status_bt` (`status`);

--
-- Chỉ mục cho bảng `disease_categories`
--
ALTER TABLE `disease_categories`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKs57ajxig1nh76dtsynjj424yn` (`specialty_id`);

--
-- Chỉ mục cho bảng `dispense_instructions`
--
ALTER TABLE `dispense_instructions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK71iej3cfeq289mji8fodpx7r7` (`customer_app_user_id`),
  ADD KEY `FKcv89b3tglirm3lxbdrevl79qx` (`product_item_id`);

--
-- Chỉ mục cho bảng `distributors`
--
ALTER TABLE `distributors`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKck47qvoigd7gh59moffh0kub0` (`wallet_address`);

--
-- Chỉ mục cho bảng `distributor_inventory`
--
ALTER TABLE `distributor_inventory`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_distributor_batch` (`distributor_id`,`batch_id`),
  ADD KEY `idx_distributor_id` (`distributor_id`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_expiry_date` (`expiry_date`),
  ADD KEY `idx_available_quantity` (`available_quantity`),
  ADD KEY `fk_dist_inv_received_from` (`received_from_company_id`),
  ADD KEY `fk_dist_inv_shipment` (`received_shipment_id`);

--
-- Chỉ mục cho bảng `distributor_users`
--
ALTER TABLE `distributor_users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKmpvt4be3vwqcoed0ovvdh7jil` (`email`),
  ADD UNIQUE KEY `idx_email` (`email`),
  ADD UNIQUE KEY `idx_email_dist` (`email`),
  ADD UNIQUE KEY `unique_distributor_wallet` (`wallet_address`),
  ADD KEY `idx_wallet_address` (`wallet_address`),
  ADD KEY `idx_wallet_address_dist` (`wallet_address`);

--
-- Chỉ mục cho bảng `drug_batches`
--
ALTER TABLE `drug_batches`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKt9md736g9fj8cosy0i1ewwaop` (`batch_id`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_qr_code` (`qr_code`(100)),
  ADD KEY `idx_manufacturer_address` (`manufacturer_address`),
  ADD KEY `idx_current_owner` (`current_owner`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_composite_status_expiry` (`status`,`expiry_date`);

--
-- Chỉ mục cho bảng `drug_products`
--
ALTER TABLE `drug_products`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_drug_products_manufacturer` (`manufacturer_id`),
  ADD KEY `idx_drug_products_name` (`name`),
  ADD KEY `idx_drug_products_category` (`category`),
  ADD KEY `idx_drug_products_active` (`is_active`),
  ADD KEY `idx_drug_products_manufacturer_id` (`manufacturer_id`),
  ADD KEY `idx_manufacturer_id` (`manufacturer_id`),
  ADD KEY `idx_registration_number` (`registration_number`);
ALTER TABLE `drug_products` ADD FULLTEXT KEY `name` (`name`,`active_ingredient`,`description`);
ALTER TABLE `drug_products` ADD FULLTEXT KEY `name_2` (`name`,`active_ingredient`,`description`);

--
-- Chỉ mục cho bảng `drug_shipments`
--
ALTER TABLE `drug_shipments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `shipment_code` (`shipment_code`),
  ADD UNIQUE KEY `idx_shipment_code` (`shipment_code`),
  ADD KEY `idx_drug_shipments_batch` (`batch_id`),
  ADD KEY `idx_drug_shipments_from` (`from_company_id`),
  ADD KEY `idx_drug_shipments_to` (`to_company_id`),
  ADD KEY `idx_drug_shipments_status` (`shipment_status`),
  ADD KEY `idx_drug_shipments_batch_id` (`batch_id`),
  ADD KEY `idx_drug_shipments_from_company` (`from_company_id`),
  ADD KEY `idx_drug_shipments_to_company` (`to_company_id`),
  ADD KEY `idx_drug_shipments_shipment_date` (`shipment_date`),
  ADD KEY `idx_drug_shipments_shipment_code` (`shipment_code`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_from_company` (`from_company_id`),
  ADD KEY `idx_to_company` (`to_company_id`),
  ADD KEY `idx_shipment_status` (`shipment_status`),
  ADD KEY `idx_composite_status_date` (`shipment_status`,`shipment_date`),
  ADD KEY `idx_batch_id_shipments` (`batch_id`),
  ADD KEY `idx_shipping_method` (`shipping_method`),
  ADD KEY `idx_actual_items_count` (`actual_items_count`);

--
-- Chỉ mục cho bảng `drug_verification_records`
--
ALTER TABLE `drug_verification_records`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_verification_records_batch` (`batch_id`),
  ADD KEY `idx_verification_records_company` (`verified_by_company_id`),
  ADD KEY `idx_verification_records_date` (`verification_date`),
  ADD KEY `idx_verification_records_type` (`verification_type`),
  ADD KEY `idx_verification_records_authentic` (`is_authentic`),
  ADD KEY `idx_verification_records_composite` (`batch_id`,`verification_date`,`is_authentic`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_verification_date` (`verification_date`),
  ADD KEY `idx_is_authentic` (`is_authentic`),
  ADD KEY `idx_batch_id_dvr` (`batch_id`);

--
-- Chỉ mục cho bảng `inventory_movements`
--
ALTER TABLE `inventory_movements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_inventory_type_id` (`inventory_type`,`inventory_id`),
  ADD KEY `idx_movement_type` (`movement_type`),
  ADD KEY `idx_movement_date` (`movement_date`),
  ADD KEY `idx_related_shipment` (`related_shipment_id`);

--
-- Chỉ mục cho bảng `manufacturer_users`
--
ALTER TABLE `manufacturer_users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK95h07q846p7dv5v8l5epve0vs` (`email`),
  ADD UNIQUE KEY `idx_email` (`email`),
  ADD UNIQUE KEY `idx_email_manu` (`email`),
  ADD UNIQUE KEY `unique_manufacturer_wallet` (`wallet_address`),
  ADD KEY `idx_manufacturer_user_email` (`email`),
  ADD KEY `idx_manufacturer_user_wallet_address` (`wallet_address`),
  ADD KEY `idx_manufacturer_user_role` (`role`),
  ADD KEY `idx_wallet_address` (`wallet_address`),
  ADD KEY `idx_wallet_address_manu` (`wallet_address`);

--
-- Chỉ mục cho bảng `medical_specialties`
--
ALTER TABLE `medical_specialties`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKlhtvpg16rimsy4mkpmj5qhx2x` (`name`),
  ADD KEY `FKopvnsmewctm3u4tb8y86mnwss` (`parent_specialty_id`);

--
-- Chỉ mục cho bảng `medications`
--
ALTER TABLE `medications`
  ADD PRIMARY KEY (`id`);

--
-- Chỉ mục cho bảng `medication_reminders`
--
ALTER TABLE `medication_reminders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK75mil2vo3q2yxwo8jpnya2wjy` (`record_id`);

--
-- Chỉ mục cho bảng `ml_model_performance`
--
ALTER TABLE `ml_model_performance`
  ADD PRIMARY KEY (`id`);

--
-- Chỉ mục cho bảng `news_articles`
--
ALTER TABLE `news_articles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKke29j5hkg9i08d4a8lhb48q6p` (`primary_category_id`);

--
-- Chỉ mục cho bảng `ownership_history`
--
ALTER TABLE `ownership_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_ownership_history_batch` (`batch_id`),
  ADD KEY `idx_ownership_history_tx_hash` (`blockchain_tx_hash`),
  ADD KEY `idx_ownership_history_transfer_date` (`transfer_date`),
  ADD KEY `idx_ownership_history_from` (`from_company_id`),
  ADD KEY `idx_ownership_history_to` (`to_company_id`),
  ADD KEY `idx_ownership_history_composite` (`batch_id`,`transfer_date`,`transfer_type`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_transfer_date` (`transfer_date`),
  ADD KEY `idx_blockchain_tx_hash` (`blockchain_tx_hash`),
  ADD KEY `idx_batch_id_oh` (`batch_id`);

--
-- Chỉ mục cho bảng `pharmacy_inventory`
--
ALTER TABLE `pharmacy_inventory`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_pharmacy_batch` (`pharmacy_id`,`batch_id`),
  ADD KEY `idx_pharmacy_id` (`pharmacy_id`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_expiry_date` (`expiry_date`),
  ADD KEY `idx_available_quantity` (`available_quantity`),
  ADD KEY `idx_drug_name` (`drug_name`),
  ADD KEY `idx_is_featured` (`is_featured`),
  ADD KEY `fk_pharm_inv_distributor` (`received_from_distributor_id`),
  ADD KEY `fk_pharm_inv_shipment` (`received_shipment_id`);

--
-- Chỉ mục cho bảng `pharmacy_users`
--
ALTER TABLE `pharmacy_users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `pharmacy_code` (`pharmacy_code`),
  ADD UNIQUE KEY `wallet_address` (`wallet_address`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_wallet_address` (`wallet_address`),
  ADD KEY `idx_pharmacy_code` (`pharmacy_code`);

--
-- Chỉ mục cho bảng `pharma_companies`
--
ALTER TABLE `pharma_companies`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `license_number` (`license_number`),
  ADD UNIQUE KEY `wallet_address` (`wallet_address`),
  ADD UNIQUE KEY `wallet_address_2` (`wallet_address`),
  ADD UNIQUE KEY `wallet_address_3` (`wallet_address`),
  ADD KEY `idx_pharma_companies_type` (`company_type`),
  ADD KEY `idx_pharma_companies_wallet` (`wallet_address`),
  ADD KEY `idx_pharma_companies_license` (`license_number`),
  ADD KEY `idx_pharma_companies_active` (`is_active`),
  ADD KEY `idx_company_type` (`company_type`),
  ADD KEY `idx_wallet_address` (`wallet_address`);
ALTER TABLE `pharma_companies` ADD FULLTEXT KEY `name` (`name`,`address`);
ALTER TABLE `pharma_companies` ADD FULLTEXT KEY `name_2` (`name`,`address`);

--
-- Chỉ mục cho bảng `product_items`
--
ALTER TABLE `product_items`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_item_code` (`item_code`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_drug_product_id` (`drug_product_id`),
  ADD KEY `idx_current_status` (`current_status`),
  ADD KEY `idx_qr_code_data` (`qr_code_data`),
  ADD KEY `idx_batch_status` (`batch_id`,`current_status`),
  ADD KEY `idx_expiry_date` (`expiry_date`),
  ADD KEY `idx_owner` (`current_owner_id`,`current_owner_type`);

--
-- Chỉ mục cho bảng `product_item_movements`
--
ALTER TABLE `product_item_movements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_item_id` (`item_id`),
  ADD KEY `idx_batch_id` (`batch_id`),
  ADD KEY `idx_movement_timestamp` (`movement_timestamp`),
  ADD KEY `idx_item_timestamp` (`item_id`,`movement_timestamp`),
  ADD KEY `idx_from_company` (`from_company_id`,`movement_type`),
  ADD KEY `idx_to_company` (`to_company_id`,`movement_type`),
  ADD KEY `idx_shipment` (`shipment_id`),
  ADD KEY `idx_movement_type` (`movement_type`);

--
-- Chỉ mục cho bảng `product_item_verifications`
--
ALTER TABLE `product_item_verifications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_item_id` (`item_id`),
  ADD KEY `idx_scan_timestamp` (`scan_timestamp`),
  ADD KEY `idx_scanner_type_time` (`scanner_type`,`scan_timestamp`),
  ADD KEY `idx_verification_result` (`verification_result`),
  ADD KEY `idx_scanner` (`scanner_id`,`scanner_type`),
  ADD KEY `idx_item_scanner` (`item_id`,`scanner_type`);

--
-- Chỉ mục cho bảng `provinces`
--
ALTER TABLE `provinces`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKl256wnwfdobq071mcq7rckr9y` (`name`);

--
-- Chỉ mục cho bảng `symptoms`
--
ALTER TABLE `symptoms`
  ADD PRIMARY KEY (`id`);

--
-- Chỉ mục cho bảng `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Chỉ mục cho bảng `user_allergies`
--
ALTER TABLE `user_allergies`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKlpfcepd1quxrwrg1iso3rbtol` (`user_id`,`allergen_id`),
  ADD KEY `FKe9l0g417v7f5a0x7mlgd8nef6` (`allergen_id`);

--
-- Chỉ mục cho bảng `user_analytics`
--
ALTER TABLE `user_analytics`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK7mdo9lpckgwptue7g6mcadpbt` (`user_id`,`session_date`);

--
-- Chỉ mục cho bảng `user_blockchain_addresses`
--
ALTER TABLE `user_blockchain_addresses`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_user_address_type` (`user_id`,`address_type`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_blockchain_address` (`blockchain_address`),
  ADD KEY `idx_address_type` (`address_type`),
  ADD KEY `idx_is_verified` (`is_verified`);

--
-- Chỉ mục cho bảng `user_chronic_diseases`
--
ALTER TABLE `user_chronic_diseases`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK8sygvsr1aj77insmjtoxe1slp` (`user_id`,`disease_id`),
  ADD KEY `FK8vrsx37q69dypyauw2ofhdglk` (`disease_id`);

--
-- Chỉ mục cho bảng `user_demographics`
--
ALTER TABLE `user_demographics`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKf6vynx60dpd6wgq0vqsbjw73s` (`user_id`),
  ADD KEY `FKgtdim7lin9k4ksae3rlai3423` (`province_id`);

--
-- Chỉ mục cho bảng `user_family_history`
--
ALTER TABLE `user_family_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK5j07xbkwg6ar194tpu0pnjc1u` (`cause_of_death`),
  ADD KEY `FK5kfo8kst0cfl6bn3jnqnkaya3` (`disease_id`),
  ADD KEY `FKo72n0klmcu5t03ubdrqg1erlg` (`user_id`);

--
-- Chỉ mục cho bảng `user_lifestyle`
--
ALTER TABLE `user_lifestyle`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKyep64m0od2ba5hj03pp6hao4` (`user_id`);

--
-- Chỉ mục cho bảng `user_medications`
--
ALTER TABLE `user_medications`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKqdnk12od7cjg3denr5poygql8` (`medication_id`),
  ADD KEY `FK19lmjhtt7n8cqs1csr37kxe3t` (`user_id`);

--
-- Chỉ mục cho bảng `user_medication_records`
--
ALTER TABLE `user_medication_records`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKpb1evaoa70h08n62li4tp41xd` (`dispense_instruction_id`),
  ADD KEY `FKtn6ygqbsgvy4aan6vhdcor2nu` (`user_id`),
  ADD KEY `FKf7v60s8xthtuqxw9nnud4wfnf` (`product_item_id`);

--
-- Chỉ mục cho bảng `user_news_interactions`
--
ALTER TABLE `user_news_interactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKnwx3trc1fnawwofdugamsshbl` (`article_id`),
  ADD KEY `FKnnnrc539o3ogkp37dg6b4acs8` (`user_id`);

--
-- Chỉ mục cho bảng `user_symptom_reports`
--
ALTER TABLE `user_symptom_reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKc1ofdoqfa5gfjr24gv7u13csj` (`symptom_id`),
  ADD KEY `FKljga2nkpr5nevb46jgm5v6620` (`user_id`);

--
-- AUTO_INCREMENT cho các bảng đã đổ
--

--
-- AUTO_INCREMENT cho bảng `allergen_categories`
--
ALTER TABLE `allergen_categories`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `app_users`
--
ALTER TABLE `app_users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=155468012;

--
-- AUTO_INCREMENT cho bảng `blockchain_config`
--
ALTER TABLE `blockchain_config`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT cho bảng `blockchain_events`
--
ALTER TABLE `blockchain_events`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT cho bảng `blockchain_transactions`
--
ALTER TABLE `blockchain_transactions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=147;

--
-- AUTO_INCREMENT cho bảng `disease_categories`
--
ALTER TABLE `disease_categories`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `dispense_instructions`
--
ALTER TABLE `dispense_instructions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT cho bảng `distributors`
--
ALTER TABLE `distributors`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `distributor_inventory`
--
ALTER TABLE `distributor_inventory`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT cho bảng `drug_batches`
--
ALTER TABLE `drug_batches`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT cho bảng `drug_products`
--
ALTER TABLE `drug_products`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=65;

--
-- AUTO_INCREMENT cho bảng `drug_shipments`
--
ALTER TABLE `drug_shipments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=67;

--
-- AUTO_INCREMENT cho bảng `drug_verification_records`
--
ALTER TABLE `drug_verification_records`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `inventory_movements`
--
ALTER TABLE `inventory_movements`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT cho bảng `medical_specialties`
--
ALTER TABLE `medical_specialties`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `medications`
--
ALTER TABLE `medications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `medication_reminders`
--
ALTER TABLE `medication_reminders`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=443;

--
-- AUTO_INCREMENT cho bảng `ml_model_performance`
--
ALTER TABLE `ml_model_performance`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `news_articles`
--
ALTER TABLE `news_articles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `ownership_history`
--
ALTER TABLE `ownership_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT cho bảng `pharmacy_inventory`
--
ALTER TABLE `pharmacy_inventory`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT cho bảng `pharmacy_users`
--
ALTER TABLE `pharmacy_users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT cho bảng `pharma_companies`
--
ALTER TABLE `pharma_companies`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT cho bảng `product_items`
--
ALTER TABLE `product_items`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=521;

--
-- AUTO_INCREMENT cho bảng `product_item_movements`
--
ALTER TABLE `product_item_movements`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=509;

--
-- AUTO_INCREMENT cho bảng `product_item_verifications`
--
ALTER TABLE `product_item_verifications`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=205;

--
-- AUTO_INCREMENT cho bảng `provinces`
--
ALTER TABLE `provinces`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `symptoms`
--
ALTER TABLE `symptoms`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_allergies`
--
ALTER TABLE `user_allergies`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_analytics`
--
ALTER TABLE `user_analytics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_blockchain_addresses`
--
ALTER TABLE `user_blockchain_addresses`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_chronic_diseases`
--
ALTER TABLE `user_chronic_diseases`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_demographics`
--
ALTER TABLE `user_demographics`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_family_history`
--
ALTER TABLE `user_family_history`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_lifestyle`
--
ALTER TABLE `user_lifestyle`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_medications`
--
ALTER TABLE `user_medications`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_medication_records`
--
ALTER TABLE `user_medication_records`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT cho bảng `user_news_interactions`
--
ALTER TABLE `user_news_interactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT cho bảng `user_symptom_reports`
--
ALTER TABLE `user_symptom_reports`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Các ràng buộc cho các bảng đã đổ
--

--
-- Các ràng buộc cho bảng `blockchain_transactions`
--
ALTER TABLE `blockchain_transactions`
  ADD CONSTRAINT `FK1xwa92xkjqy4m4w86qt9frq0p` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`),
  ADD CONSTRAINT `FKthjbaitcky21e87g7k64ixmyu` FOREIGN KEY (`shipment_id`) REFERENCES `drug_shipments` (`id`);

--
-- Các ràng buộc cho bảng `disease_categories`
--
ALTER TABLE `disease_categories`
  ADD CONSTRAINT `FKs57ajxig1nh76dtsynjj424yn` FOREIGN KEY (`specialty_id`) REFERENCES `medical_specialties` (`id`);

--
-- Các ràng buộc cho bảng `dispense_instructions`
--
ALTER TABLE `dispense_instructions`
  ADD CONSTRAINT `FK71iej3cfeq289mji8fodpx7r7` FOREIGN KEY (`customer_app_user_id`) REFERENCES `app_users` (`id`),
  ADD CONSTRAINT `FKcv89b3tglirm3lxbdrevl79qx` FOREIGN KEY (`product_item_id`) REFERENCES `product_items` (`id`);

--
-- Các ràng buộc cho bảng `distributor_inventory`
--
ALTER TABLE `distributor_inventory`
  ADD CONSTRAINT `fk_dist_inv_batch` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_dist_inv_distributor` FOREIGN KEY (`distributor_id`) REFERENCES `pharma_companies` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_dist_inv_received_from` FOREIGN KEY (`received_from_company_id`) REFERENCES `pharma_companies` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_dist_inv_shipment` FOREIGN KEY (`received_shipment_id`) REFERENCES `drug_shipments` (`id`) ON DELETE SET NULL;

--
-- Các ràng buộc cho bảng `drug_products`
--
ALTER TABLE `drug_products`
  ADD CONSTRAINT `drug_products_ibfk_1` FOREIGN KEY (`manufacturer_id`) REFERENCES `pharma_companies` (`id`);

--
-- Các ràng buộc cho bảng `drug_shipments`
--
ALTER TABLE `drug_shipments`
  ADD CONSTRAINT `FK62vjipmxnoneo3nstux5d7492` FOREIGN KEY (`from_company_id`) REFERENCES `pharma_companies` (`id`),
  ADD CONSTRAINT `FKg2b8lwl3cf30c3yumc4bm2jo0` FOREIGN KEY (`to_company_id`) REFERENCES `pharma_companies` (`id`),
  ADD CONSTRAINT `FKijrigymbgnkf344fc6dv25swr` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`),
  ADD CONSTRAINT `fk_drug_shipments_batch` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) ON UPDATE CASCADE;

--
-- Các ràng buộc cho bảng `drug_verification_records`
--
ALTER TABLE `drug_verification_records`
  ADD CONSTRAINT `drug_verification_records_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`),
  ADD CONSTRAINT `drug_verification_records_ibfk_2` FOREIGN KEY (`verified_by_company_id`) REFERENCES `pharma_companies` (`id`);

--
-- Các ràng buộc cho bảng `inventory_movements`
--
ALTER TABLE `inventory_movements`
  ADD CONSTRAINT `fk_inv_mov_shipment` FOREIGN KEY (`related_shipment_id`) REFERENCES `drug_shipments` (`id`) ON DELETE SET NULL;

--
-- Các ràng buộc cho bảng `medical_specialties`
--
ALTER TABLE `medical_specialties`
  ADD CONSTRAINT `FKopvnsmewctm3u4tb8y86mnwss` FOREIGN KEY (`parent_specialty_id`) REFERENCES `medical_specialties` (`id`);

--
-- Các ràng buộc cho bảng `medication_reminders`
--
ALTER TABLE `medication_reminders`
  ADD CONSTRAINT `FK75mil2vo3q2yxwo8jpnya2wjy` FOREIGN KEY (`record_id`) REFERENCES `user_medication_records` (`id`);

--
-- Các ràng buộc cho bảng `news_articles`
--
ALTER TABLE `news_articles`
  ADD CONSTRAINT `FKke29j5hkg9i08d4a8lhb48q6p` FOREIGN KEY (`primary_category_id`) REFERENCES `medical_specialties` (`id`);

--
-- Các ràng buộc cho bảng `ownership_history`
--
ALTER TABLE `ownership_history`
  ADD CONSTRAINT `ownership_history_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`),
  ADD CONSTRAINT `ownership_history_ibfk_2` FOREIGN KEY (`from_company_id`) REFERENCES `pharma_companies` (`id`),
  ADD CONSTRAINT `ownership_history_ibfk_3` FOREIGN KEY (`to_company_id`) REFERENCES `pharma_companies` (`id`);

--
-- Các ràng buộc cho bảng `pharmacy_inventory`
--
ALTER TABLE `pharmacy_inventory`
  ADD CONSTRAINT `fk_pharm_inv_batch` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_pharm_inv_distributor` FOREIGN KEY (`received_from_distributor_id`) REFERENCES `pharma_companies` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_pharm_inv_pharmacy` FOREIGN KEY (`pharmacy_id`) REFERENCES `pharma_companies` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_pharm_inv_shipment` FOREIGN KEY (`received_shipment_id`) REFERENCES `drug_shipments` (`id`) ON DELETE SET NULL;

--
-- Các ràng buộc cho bảng `product_items`
--
ALTER TABLE `product_items`
  ADD CONSTRAINT `fk_product_items_batch` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_product_items_product` FOREIGN KEY (`drug_product_id`) REFERENCES `drug_products` (`id`) ON UPDATE CASCADE;

--
-- Các ràng buộc cho bảng `product_item_movements`
--
ALTER TABLE `product_item_movements`
  ADD CONSTRAINT `fk_movements_batch` FOREIGN KEY (`batch_id`) REFERENCES `drug_batches` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_movements_item` FOREIGN KEY (`item_id`) REFERENCES `product_items` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_movements_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `drug_shipments` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Các ràng buộc cho bảng `product_item_verifications`
--
ALTER TABLE `product_item_verifications`
  ADD CONSTRAINT `fk_verifications_item` FOREIGN KEY (`item_id`) REFERENCES `product_items` (`id`) ON UPDATE CASCADE;

--
-- Các ràng buộc cho bảng `user_allergies`
--
ALTER TABLE `user_allergies`
  ADD CONSTRAINT `FK4mclrynvl1em11jh8sxt5s74k` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKe9l0g417v7f5a0x7mlgd8nef6` FOREIGN KEY (`allergen_id`) REFERENCES `allergen_categories` (`id`);

--
-- Các ràng buộc cho bảng `user_analytics`
--
ALTER TABLE `user_analytics`
  ADD CONSTRAINT `FKs410q6f29i1omwuogxe0jf8cp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Các ràng buộc cho bảng `user_chronic_diseases`
--
ALTER TABLE `user_chronic_diseases`
  ADD CONSTRAINT `FK7bbk7fm509ihkwc721bigrhc2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FK8vrsx37q69dypyauw2ofhdglk` FOREIGN KEY (`disease_id`) REFERENCES `disease_categories` (`id`);

--
-- Các ràng buộc cho bảng `user_demographics`
--
ALTER TABLE `user_demographics`
  ADD CONSTRAINT `FK6oi5c2vx6uwx787h2dh42tds0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKgtdim7lin9k4ksae3rlai3423` FOREIGN KEY (`province_id`) REFERENCES `provinces` (`id`);

--
-- Các ràng buộc cho bảng `user_family_history`
--
ALTER TABLE `user_family_history`
  ADD CONSTRAINT `FK5j07xbkwg6ar194tpu0pnjc1u` FOREIGN KEY (`cause_of_death`) REFERENCES `disease_categories` (`id`),
  ADD CONSTRAINT `FK5kfo8kst0cfl6bn3jnqnkaya3` FOREIGN KEY (`disease_id`) REFERENCES `disease_categories` (`id`),
  ADD CONSTRAINT `FKo72n0klmcu5t03ubdrqg1erlg` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Các ràng buộc cho bảng `user_lifestyle`
--
ALTER TABLE `user_lifestyle`
  ADD CONSTRAINT `FK1oq3d9ckvuvqhvcwqat4yt96q` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Các ràng buộc cho bảng `user_medications`
--
ALTER TABLE `user_medications`
  ADD CONSTRAINT `FK19lmjhtt7n8cqs1csr37kxe3t` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKqdnk12od7cjg3denr5poygql8` FOREIGN KEY (`medication_id`) REFERENCES `medications` (`id`);

--
-- Các ràng buộc cho bảng `user_medication_records`
--
ALTER TABLE `user_medication_records`
  ADD CONSTRAINT `FKf7v60s8xthtuqxw9nnud4wfnf` FOREIGN KEY (`product_item_id`) REFERENCES `product_items` (`id`),
  ADD CONSTRAINT `FKpb1evaoa70h08n62li4tp41xd` FOREIGN KEY (`dispense_instruction_id`) REFERENCES `dispense_instructions` (`id`);

--
-- Các ràng buộc cho bảng `user_news_interactions`
--
ALTER TABLE `user_news_interactions`
  ADD CONSTRAINT `FKnnnrc539o3ogkp37dg6b4acs8` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKnwx3trc1fnawwofdugamsshbl` FOREIGN KEY (`article_id`) REFERENCES `news_articles` (`id`);

--
-- Các ràng buộc cho bảng `user_symptom_reports`
--
ALTER TABLE `user_symptom_reports`
  ADD CONSTRAINT `FKc1ofdoqfa5gfjr24gv7u13csj` FOREIGN KEY (`symptom_id`) REFERENCES `symptoms` (`id`),
  ADD CONSTRAINT `FKljga2nkpr5nevb46jgm5v6620` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
