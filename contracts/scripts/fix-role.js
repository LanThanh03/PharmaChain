const { ethers } = require("hardhat");

async function main() {
    const contractAddress = "0xDDd3052fF0b0a39882260A07608d9053AdeDDB98";
    const DrugTraceability = await ethers.getContractFactory("DrugTraceability");
    const contract = DrugTraceability.attach(contractAddress);

    const [deployer] = await ethers.getSigners();
    console.log("Using account:", deployer.address);

    const manufacturerRole = await contract.MANUFACTURER_ROLE();
    const distributorRole = await contract.DISTRIBUTOR_ROLE();
    const pharmacyRole = await contract.PHARMACY_ROLE();

    console.log("Granting roles to Backend Relayer:", deployer.address);

    await (await contract.grantRole(manufacturerRole, deployer.address)).wait();
    console.log("✅ Granted MANUFACTURER_ROLE");

    await (await contract.grantRole(distributorRole, deployer.address)).wait();
    console.log("✅ Granted DISTRIBUTOR_ROLE");

    await (await contract.grantRole(pharmacyRole, deployer.address)).wait();
    console.log("✅ Granted PHARMACY_ROLE");

    console.log("All roles granted successfully!");
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
