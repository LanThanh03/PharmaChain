/**
 * Transfer Script: Distributor → Pharmacy
 * 
 * Mục đích: Chuyển hàng từ nhà phân phối sang nhà thuốc
 * Ghi nhận transaction trên blockchain
 */

const { ethers } = require("hardhat");
const { MerkleTree } = require("merkletreejs");
const keccak256 = require("keccak256");

// Configuration
const CONFIG = {
    CONTRACT_ADDRESS: process.env.CONTRACT_ADDRESS || "",
    BATCH_ID: process.env.BATCH_ID || 1,
    PHARMACY_ADDRESS: process.env.PHARMACY_ADDRESS || ""
};

/**
 * Tạo Merkle Tree từ danh sách item codes
 */
function createMerkleTree(itemCodes) {
    const leaves = itemCodes.map(code => keccak256(code));
    return new MerkleTree(leaves, keccak256, { sortPairs: true });
}

/**
 * Lấy Merkle proof cho item code
 */
function getMerkleProof(merkleTree, itemCode) {
    const leaf = keccak256(itemCode);
    return merkleTree.getHexProof(leaf);
}

/**
 * Main transfer function
 */
async function transferToPharmacy(itemCode, pharmacyAddress, notes, batchId, merkleProof) {
    const [distributor] = await ethers.getSigners();
    
    // Attach to deployed contract
    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);
    
    console.log("\n📦 Chuyển hàng từ Distributor → Pharmacy");
    console.log("─".repeat(60));
    console.log(`Item Code: ${itemCode}`);
    console.log(`From: ${distributor.address}`);
    console.log(`To: ${pharmacyAddress}`);
    console.log(`Batch ID: ${batchId}`);
    console.log(`Notes: ${notes}\n`);
    
    // Gọi transferItem() trên blockchain
    const tx = await contract.connect(distributor).transferItem(
        itemCode,
        pharmacyAddress,
        "SHIP",
        notes,
        batchId,
        merkleProof
    );
    
    console.log("⏳ Đang chờ transaction được confirm...");
    const receipt = await tx.wait();
    
    console.log("\n✅ Transaction thành công!");
    console.log(`   Tx Hash: ${receipt.hash}`);
    console.log(`   Block: ${receipt.blockNumber}`);
    console.log(`   Gas Used: ${receipt.gasUsed.toString()}`);
    
    // Verify status
    const itemStatus = await contract.getItemStatus(itemCode);
    console.log("\n📊 Trạng thái sau khi chuyển:");
    console.log(`   Owner: ${itemStatus.currentOwner}`);
    console.log(`   Status: ${itemStatus.status}`);
    
    return receipt;
}

/**
 * Xác nhận nhận hàng
 */
async function confirmReceive(itemCode, batchId, merkleProof) {
    const [distributor] = await ethers.getSigners();
    
    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);
    
    console.log("\n✅ Xác nhận nhận hàng");
    console.log("─".repeat(60));
    
    const tx = await contract.connect(distributor).transferItem(
        itemCode,
        distributor.address,  // Transfer cho chính mình
        "RECEIVE",
        "Received and stored in warehouse",
        batchId,
        merkleProof
    );
    
    const receipt = await tx.wait();
    
    console.log(`✅ Confirmed! Tx: ${receipt.hash}`);
    
    return receipt;
}

// Export functions
module.exports = {
    transferToPharmacy,
    confirmReceive,
    createMerkleTree,
    getMerkleProof
};

// CLI execution
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length < 2) {
        console.log("\n❌ Usage:");
        console.log("   npx hardhat run scripts/transfer-distributor-to-pharmacy.js --network localhost ITEM_CODE PHARMACY_ADDRESS\n");
        process.exit(1);
    }
    
    const [itemCode, pharmacyAddress] = args;
    
    const itemCodes = [itemCode];
    const merkleTree = createMerkleTree(itemCodes);
    const proof = getMerkleProof(merkleTree, itemCode);
    
    transferToPharmacy(
        itemCode,
        pharmacyAddress,
        "Delivered to pharmacy",
        CONFIG.BATCH_ID,
        proof
    )
    .then(() => process.exit(0))
    .catch(error => {
        console.error("❌ Error:", error);
        process.exit(1);
    });
}

