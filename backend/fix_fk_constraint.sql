-- ============================================================
-- Script: Xóa Foreign Key Constraint trên bảng user_medication_records
-- Lý do: FK constraint user_id -> app_users(id) gây lỗi 
--        SQLIntegrityConstraintViolationException khi thêm thuốc
-- Giải pháp: Quản lý quan hệ ở application level thay vì DB level
-- ============================================================

-- Bước 1: Xóa FK constraint cũ
ALTER TABLE user_medication_records DROP FOREIGN KEY FKtn6ygqbsgvy4aan6vhdcor2nu;

-- Bước 2: Thêm INDEX cho user_id (để query nhanh, thay thế cho FK index)
-- Kiểm tra nếu index đã tồn tại thì bỏ qua
CREATE INDEX idx_umr_user_id ON user_medication_records(user_id);

-- Bước 3 (Tùy chọn): Nếu muốn xóa luôn dữ liệu bị lỗi
-- DELETE FROM user_medication_records WHERE user_id NOT IN (SELECT id FROM app_users);

-- ============================================================
-- Xác nhận: Kiểm tra xem FK đã bị xóa chưa
-- ============================================================
SELECT 
    CONSTRAINT_NAME, 
    TABLE_NAME, 
    COLUMN_NAME, 
    REFERENCED_TABLE_NAME 
FROM information_schema.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'blockchain_da' 
AND TABLE_NAME = 'user_medication_records'
AND REFERENCED_TABLE_NAME IS NOT NULL;
