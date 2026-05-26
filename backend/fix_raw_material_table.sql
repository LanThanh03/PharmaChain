-- Chạy file này trong MySQL Workbench hoặc TablePlus
-- Kết nối tới database: BlockChain_DA

-- Xóa bảng cũ (tạo lại sạch từ entity mới)
DROP TABLE IF EXISTS `raw_material_batches`;

-- Hibernate sẽ tự tạo lại bảng đúng schema khi backend restart
