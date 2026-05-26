-- ============================================================
-- MIGRATION: Thêm thông tin chi tiết thuốc cho AI chatbot
-- Date: 2026-02-27
-- Description: Thêm các cột chi tiết y khoa vào drug_products
--   để AI chatbot có context trả lời chính xác hơn
-- ============================================================

USE `BlockChain_DA`;

-- Thêm các cột thông tin chi tiết thuốc
ALTER TABLE `drug_products`
    ADD COLUMN IF NOT EXISTS `indications` TEXT COMMENT 'Chỉ định (lí do uống thuốc)' AFTER `description`,
    ADD COLUMN IF NOT EXISTS `contraindications` TEXT COMMENT 'Chống chỉ định' AFTER `indications`,
    ADD COLUMN IF NOT EXISTS `side_effects` TEXT COMMENT 'Tác dụng phụ' AFTER `contraindications`,
    ADD COLUMN IF NOT EXISTS `precautions` TEXT COMMENT 'Thận trọng khi sử dụng' AFTER `side_effects`,
    ADD COLUMN IF NOT EXISTS `drug_interactions` TEXT COMMENT 'Tương tác thuốc' AFTER `precautions`,
    ADD COLUMN IF NOT EXISTS `usage_instructions` TEXT COMMENT 'Hướng dẫn sử dụng chi tiết' AFTER `drug_interactions`;

SELECT 'Migration 003 completed: Added drug detail columns!' AS status;
