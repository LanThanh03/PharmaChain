const hre = require("hardhat");

async function main() {
    const contractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
    const backendWallet = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
    
    console.log("Granting ALL ROLES to Backend Wallet:", backendWallet);
    
    const contract = await (await hre.ethers.getContractFactory("DrugTraceability")).attach(contractAddress);
    
    const roles = [
        await contract.MANUFACTURER_ROLE(),
        await contract.DISTRIBUTOR_ROLE(),
        await contract.PHARMACY_ROLE(),
        await contract.ADMIN_ROLE()
    ];
    
    for (const role of roles) {
        if (!(await contract.hasRole(role, backendWallet))) {
            const tx = await contract.grantRole(role, backendWallet);
            await tx.wait();
            console.log(`✅ Granted role ${role} to ${backendWallet}`);
        } else {
            console.log(`ℹ️ Wallet already has role ${role}`);
        }
    }
    
    console.log("\n🚀 ALL ROLES GRANTED SUCCESSFULLY!");
}

main().catch(console.error);
