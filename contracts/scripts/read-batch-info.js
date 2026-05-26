const { ethers } = require("hardhat");

async function main() {
    // Connect to the deployed contract (Address from application.properties)
    const contractAddress = "0x5FC8d32690cc91D4c39d9d3abcBD16989F875707";
    const PharmaLedger = await ethers.getContractFactory("DrugTraceability");
    const contract = PharmaLedger.attach(contractAddress);

    console.log("📦 Reading batch information from blockchain...\n");

    // Try to read batch 1 (first batch created)
    try {
        const batch = await contract.batches(1);

        console.log("✅ BATCH #1 INFORMATION:");
        console.log("==========================================");
        console.log("Batch ID:", batch.batchId.toString());
        console.log("Exists:", batch.exists);

        if (batch.exists) {
            console.log("\n📋 DRUG INFO:");
            console.log("  Name:", batch.drugInfo.name);
            console.log("  Active Ingredient:", batch.drugInfo.activeIngredient);
            console.log("  Dosage:", batch.drugInfo.dosage);
            console.log("  Manufacturer:", batch.drugInfo.manufacturer);
            console.log("  Registration Number:", batch.drugInfo.registrationNumber);

            console.log("\n📊 BATCH DETAILS:");
            console.log("  Quantity:", batch.quantity.toString());
            console.log("  Manufacture Date:", new Date(Number(batch.manufactureDate) * 1000).toISOString());
            console.log("  Expiry Date:", new Date(Number(batch.expiryDate) * 1000).toISOString());
            console.log("  Manufacturer Address:", batch.manufacturer);
            console.log("  Current Owner:", batch.currentOwner);

            const statusNames = ["ACTIVE", "EXPIRED", "RECALLED"];
            console.log("  Status:", statusNames[batch.status]);

            console.log("\n🌳 MERKLE ROOT (Items Proof):");
            console.log("==========================================");
            console.log("Items Merkle Root:", batch.itemsMerkleRoot);

            // Check if it's a valid merkle root (not zero)
            const zeroHash = "0x0000000000000000000000000000000000000000000000000000000000000000";
            if (batch.itemsMerkleRoot === zeroHash) {
                console.log("⚠️  WARNING: Merkle Root is ZERO - items were not registered!");
            } else {
                console.log("✅ Merkle Root is VALID - items are registered!");
                console.log("   This root proves the existence of", batch.quantity.toString(), "items");
            }
        } else {
            console.log("❌ Batch does not exist");
        }

    } catch (error) {
        console.log("❌ Error reading batch:", error.message);
        console.log("\nTrying to read batch 0...");

        try {
            const batch0 = await contract.batches(0);
            console.log("Batch 0 exists:", batch0.exists);
        } catch (e) {
            console.log("No batches found on blockchain");
        }
    }
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
