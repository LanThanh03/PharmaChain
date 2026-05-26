const hre = require("hardhat");

async function main() {
    const contract = await (await hre.ethers.getContractFactory("DrugTraceability")).attach("0x5FbDB2315678afecb367f032d93F642f64180aa3");
    
    try {
        const batch = await contract.batches(102);
        console.log("Batch 102 Owner on-chain:", batch.currentOwner);
        console.log("Batch ID:", batch.batchId.toString());
        console.log("Exists:", batch.exists);
    } catch(e) {
        console.log("Error querying batch:", e.message);
    }
}

main().catch(console.error);
