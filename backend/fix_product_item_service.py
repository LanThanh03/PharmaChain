import re

# Read the corrupted file
with open(r'c:\Users\Admin\Documents\Workspace\NCKH_AI_Med\backend\src\main\java\com\nckh\dia5\service\ProductItemService.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Find the corrupted section and replace it
# The section starts around line 300 with "// STEP 1: Create Merkle Tree"
# and should end with the complete blockchain registration code

corrupted_pattern = r'(// STEP 1: Create Merkle Tree.*?)(// STEP 2: Create batch on PharmaLedgerOptimized contract with Merkle root.*?BigInteger expiryTimestamp = BigInteger\.valueOf\(\s*batch\.getExpiryDate\(\)\.toEpochSecond\(java\.time\.ZoneOffset\.UTC\)\s*\)\s*\*/\s*public TransactionReceipt registerItemsToBlockchain)'

replacement = r'''\1// STEP 2: Create batch on PharmaLedgerOptimized contract with Merkle root
            // This SINGLE transaction replaces the old multi-step process
            log.info("📦 Creating batch on blockchain with Merkle root...");
            BigInteger expiryTimestamp = BigInteger.valueOf(
                batch.getExpiryDate().toEpochSecond(java.time.ZoneOffset.UTC)
            );
            BigInteger manufactureTimestamp = BigInteger.valueOf(
                batch.getManufactureTimestamp().toEpochSecond(java.time.ZoneOffset.UTC)
            );
            
            TransactionReceipt batchReceipt = blockchainService.createBatchWithItems(
                    batch.getDrugName(),
                    batch.getManufacturer(),
                    BigInteger.valueOf(quantity),
                    manufactureTimestamp,
                    expiryTimestamp,
                    merkleRoot
            ).get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            if (batchReceipt != null && "0x1".equals(batchReceipt.getStatus())) {
                log.info("✅ Batch created on blockchain successfully!");
                log.info("📝 Batch TX Hash: {}", batchReceipt.getTransactionHash());
                
                // Update batch with blockchain info
                batch.setTransactionHash(batchReceipt.getTransactionHash());
                batch.setBlockNumber(batchReceipt.getBlockNumber() != null ? batchReceipt.getBlockNumber() : BigInteger.ONE);
                batch.setIsSynced(true);
                drugBatchRepository.save(batch);
                log.info("✅ Updated batch with blockchain TX: {}", batchReceipt.getTransactionHash());
                
                // Update items with blockchain info AND Merkle Proofs
                updateItemsBlockchainStatus(savedItems, batch.getBatchId(), batchReceipt.getTransactionHash());
                
            } else {
                log.error("❌ Batch creation failed on blockchain");
                throw new RuntimeException("Batch creation failed");
            }
            
        } catch (java.util.concurrent.TimeoutException te) {
            log.warn("⏱️ Blockchain registration timed out after 30 seconds. Items saved in database but not synced to blockchain yet.");
            log.warn("💡 You can retry blockchain sync later using the batch sync endpoint.");
        } catch (Exception e) {
            log.error("❌ Failed to register items on blockchain: {}", e.getMessage(), e);
            log.warn("📊 Items are saved in database but not synced to blockchain.");
            log.warn("💡 You can retry blockchain sync later.");
        }
        
        return savedItems;
    }
    
    /**
     * Register items to blockchain (public method for retry)
     * NOTE: In the optimized contract, we don't register items individually anymore.
     * We just ensure the batch exists. This method might be deprecated or used to re-verify.
     */
    public TransactionReceipt registerItemsToBlockchain'''

# Apply the fix
fixed_content = re.sub(corrupted_pattern, replacement, content, flags=re.DOTALL)

# Write back
with open(r'c:\Users\Admin\Documents\Workspace\NCKH_AI_Med\backend\src\main\java\com\nckh\dia5\service\ProductItemService.java', 'w', encoding='utf-8') as f:
    f.write(fixed_content)

print("File fixed!")
