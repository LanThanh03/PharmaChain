/**
 * Record Sale Script: Pharmacy → Consumer
 * 
 * Mục đích: Ghi nhận bán hàng cho khách hàng cuối
 * Kết thúc chuỗi cung ứng trên blockchain
 */

const { ethers } = require("hardhat");

// Configuration
const CONFIG = {
    CONTRACT_ADDRESS: process.env.CONTRACT_ADDRESS || ""
};

/**
 * Ghi nhận bán hàng
 */
async function recordSale(itemCode, saleTimestamp = null) {
    const [pharmacy] = await ethers.getSigners();
    
    // Attach to deployed contract
    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);
    
    console.log("\n💰 Ghi nhận bán hàng cho khách hàng");
    console.log("─".repeat(60));
    console.log(`Item Code: ${itemCode}`);
    console.log(`Pharmacy: ${pharmacy.address}`);
    console.log(`Sale Time: ${saleTimestamp ? new Date(saleTimestamp * 1000).toLocaleString() : "Now"}\n`);
    
    // Gọi recordItemSale() trên blockchain
    const timestamp = saleTimestamp || Math.floor(Date.now() / 1000);
    
    const tx = await contract.connect(pharmacy).recordItemSale(
        itemCode,
        timestamp
    );
    
    console.log("⏳ Đang chờ transaction được confirm...");
    const receipt = await tx.wait();
    
    console.log("\n✅ Ghi nhận thành công!");
    console.log(`   Tx Hash: ${receipt.hash}`);
    console.log(`   Block: ${receipt.blockNumber}`);
    console.log(`   Gas Used: ${receipt.gasUsed.toString()}`);
    
    // Verify status
    const itemStatus = await contract.getItemStatus(itemCode);
    console.log("\n📊 Trạng thái cuối cùng:");
    console.log(`   Status: ${itemStatus.status}`);
    console.log(`   Owner: ${itemStatus.currentOwner}`);
    
    // Show complete history
    const history = await contract.getItemHistory(itemCode);
    console.log(`\n📜 Lịch sử hoàn chỉnh (${history.length} transfers):`);
    history.forEach((transfer, idx) => {
        console.log(`   [${idx + 1}] ${transfer.transferType}: ${transfer.notes}`);
    });
    
    return receipt;
}

// Export functions
module.exports = {
    recordSale
};

// CLI execution
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length < 1) {
        console.log("\n❌ Usage:");
        console.log("   npx hardhat run scripts/record-sale.js --network localhost ITEM_CODE [TIMESTAMP]\n");
        process.exit(1);
    }
    
    const [itemCode, timestamp] = args;
    
    recordSale(itemCode, timestamp ? parseInt(timestamp) : null)
        .then(() => process.exit(0))
        .catch(error => {
            console.error("❌ Error:", error);
            process.exit(1);
        });
}

