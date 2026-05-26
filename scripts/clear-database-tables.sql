-- Clear blockchain-related tables for fresh start
-- Run this in MySQL: mysql -u root -p BlockChain_DA < scripts/clear-database-tables.sql

USE BlockChain_DA;

-- Truncate blockchain tables
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE blockchain_transactions;
TRUNCATE TABLE blockchain_events;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Blockchain tables cleared successfully!' AS Status;

