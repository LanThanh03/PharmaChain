-- Fix: Insert pharmacy_inventory for delivered shipments
-- Run this SQL to add inventory for shipments that were DELIVERED before the fix

-- Check current state
SELECT 'Current pharmacy_inventory count:' AS info, COUNT(*) AS count FROM pharmacy_inventory;

-- Insert inventory for pharmacy (id=5) from delivered shipment (id=51)
INSERT INTO pharmacy_inventory (
    pharmacy_id, 
    batch_id, 
    drug_name, 
    manufacturer, 
    batch_number, 
    quantity, 
    received_quantity, 
    is_verified, 
    blockchain_batch_id,
    current_owner_address,
    received_shipment_id,
    received_from_distributor_id,
    received_date,
    created_at, 
    updated_at
) 
SELECT 
    5,  -- pharmacy_id: Hiệu thuốc An Khang
    db.id,
    db.drug_name,
    db.manufacturer,
    db.batch_number,
    ds.quantity,
    ds.quantity,
    1,  -- is_verified
    db.batch_id,  -- blockchain_batch_id
    (SELECT wallet_address FROM pharma_companies WHERE id = 5),
    ds.id,  -- shipment_id
    ds.from_company_id,
    ds.actual_delivery_date,
    NOW(),
    NOW()
FROM drug_shipments ds
JOIN drug_batches db ON ds.batch_id = db.id
WHERE ds.to_company_id = 5 
  AND ds.shipment_status = 'DELIVERED'
  AND NOT EXISTS (
      SELECT 1 FROM pharmacy_inventory pi 
      WHERE pi.pharmacy_id = 5 AND pi.batch_id = db.id
  );

-- Verify insertion
SELECT 'After fix - pharmacy_inventory count:' AS info, COUNT(*) AS count FROM pharmacy_inventory;
SELECT id, pharmacy_id, drug_name, batch_number, quantity FROM pharmacy_inventory;
