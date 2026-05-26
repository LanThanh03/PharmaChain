/**
 * Transfer Script: Manufacturer → Distributor
 * 
 * Mục đích: Chuyển hàng từ nhà sản xuất sang nhà phân phối
 * Ghi nhận transaction trên blockchain
 */

const { ethers } = require("hardhat");
const { MerkleTree } = require("merkletreejs");
const keccak256 = require("keccak256");

// Configuration
const CONFIG = {
    CONTRACT_ADDRESS: process.env.CONTRACT_ADDRESS || "",
    BATCH_ID: process.env.BATCH_ID || 1,
    DISTRIBUTOR_ADDRESS: process.env.DISTRIBUTOR_ADDRESS || ""
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
async function transferToDistributor(itemCode, distributorAddress, notes, batchId, merkleProof) {
    const [manufacturer] = await ethers.getSigners();
    
    // Attach to deployed contract
    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);
    
    console.log("\n📦 Chuyển hàng từ Manufacturer → Distributor");
    console.log("─".repeat(60));
    console.log(`Item Code: ${itemCode}`);
    console.log(`From: ${manufacturer.address}`);
    console.log(`To: ${distributorAddress}`);
    console.log(`Batch ID: ${batchId}`);
    console.log(`Notes: ${notes}\n`);
    
    // Gọi transferItem() trên blockchain
    const tx = await contract.connect(manufacturer).transferItem(
        itemCode,
        distributorAddress,
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
 * Batch transfer nhiều items cùng lúc
 */
async function batchTransfer(itemCodes, distributorAddress, notes, batchId, merkleTree) {
    console.log(`\n📦 Batch Transfer: ${itemCodes.length} items`);
    
    for (let i = 0; i < itemCodes.length; i++) {
        const itemCode = itemCodes[i];
        const proof = getMerkleProof(merkleTree, itemCode);
        
        console.log(`\n[${i + 1}/${itemCodes.length}] Transferring ${itemCode}...`);
        await transferToDistributor(itemCode, distributorAddress, notes, batchId, proof);
    }
}

// Export functions
module.exports = {
    transferToDistributor,
    batchTransfer,
    createMerkleTree,
    getMerkleProof
};

// CLI execution
if (require.main === module) {
    const args = process.argv.slice(2);
    
    if (args.length < 2) {
        console.log("\n❌ Usage:");
        console.log("   npx hardhat run scripts/transfer-manufacturer-to-distributor.js --network localhost ITEM_CODE DISTRIBUTOR_ADDRESS\n");
        process.exit(1);
    }
    
    const [itemCode, distributorAddress] = args;
    
    // Load batch data và merkle tree từ file hoặc database
    // Đây chỉ là demo, thực tế bạn cần load từ storage
    const itemCodes = [itemCode]; // Load từ database
    const merkleTree = createMerkleTree(itemCodes);
    const proof = getMerkleProof(merkleTree, itemCode);
    
    transferToDistributor(
        itemCode,
        distributorAddress,
        "Shipment via logistics partner",
        CONFIG.BATCH_ID,
        proof
    )
    .then(() => process.exit(0))
    .catch(error => {
        console.error("❌ Error:", error);
        process.exit(1);
    });
}

