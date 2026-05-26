const hre = require("hardhat");

async function main() {
    const contractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
    const DrugTraceability = await hre.ethers.getContractFactory("DrugTraceability");
    const contract = await DrugTraceability.attach(contractAddress);
    
    const wallets = [
        "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266", // Backend
        "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC", // NPP Demo
        "0x90F79bf6EB2c4f870365E785982E1f101E93b906", // HT Demo
        "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"  // NSX Other
    ];
    
    const MANUFACTURER_ROLE = await contract.MANUFACTURER_ROLE();
    const DISTRIBUTOR_ROLE = await contract.DISTRIBUTOR_ROLE();
    const PHARMACY_ROLE = await contract.PHARMACY_ROLE();
    
    console.log("=== ROLE CHECK ===");
    for (const w of wallets) {
        console.log(`Wallet: ${w}`);
        const nsx = await contract.hasRole(MANUFACTURER_ROLE, w);
        const npp = await contract.hasRole(DISTRIBUTOR_ROLE, w);
        const ht = await contract.hasRole(PHARMACY_ROLE, w);
        console.log(`  - NSX: ${nsx}`);
        console.log(`  - NPP: ${npp}`);
        console.log(`  - HT: ${ht}`);
    }
}

main().catch(console.error);
