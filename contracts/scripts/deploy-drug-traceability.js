const { ethers } = require("hardhat");
const fs = require('fs');
const path = require('path');

/**
 * Deploy DrugTraceability Contract
 * 
 * CONTRACT TRUY XUẤT NGUỒN GỐC THUỐC:
 * - 1 tx: Tạo batch + items merkle root
 * - 1 tx: Tạo và gửi shipment
 * - 1 tx: Nhận hàng + transfer ownership
 */

async function main() {
    console.log("\n" + "=".repeat(70));
    console.log("🚀 DEPLOYING DRUG TRACEABILITY CONTRACT");
    console.log("=".repeat(70) + "\n");

    // Get signers
    const [deployer] = await ethers.getSigners();
    console.log("📋 Deploying with account:", deployer.address);
    console.log("💰 Account balance:", ethers.formatEther(await ethers.provider.getBalance(deployer.address)), "ETH\n");

    // Deploy contract
    console.log("⏳ Deploying DrugTraceability...");
    const DrugTraceability = await ethers.getContractFactory("DrugTraceability");
    const drugTraceability = await DrugTraceability.deploy();

    await drugTraceability.waitForDeployment();

    const contractAddress = await drugTraceability.getAddress();
    const deployTx = drugTraceability.deploymentTransaction();

    console.log("✅ Contract deployed successfully!");
    console.log("📍 Contract Address:", contractAddress);
    console.log("🔗 Transaction Hash:", deployTx.hash);
    console.log("⛽ Gas Used:", deployTx.gasLimit.toString());

    // Setup test accounts with roles
    console.log("\n🔐 Setting up roles for test accounts...");

    const testManufacturer = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"; // Hardhat account #1
    const testDistributor = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";  // Hardhat account #2
    const testPharmacy = "0x90F79bf6EB2c4f870365E785982E1f101E93b906";     // Hardhat account #3

    try {
        // Add manufacturer
        const tx1 = await drugTraceability.addManufacturer(testManufacturer);
        await tx1.wait();
        console.log("✅ Manufacturer role granted to:", testManufacturer);

        // Add distributor
        const tx2 = await drugTraceability.addDistributor(testDistributor);
        await tx2.wait();
        console.log("✅ Distributor role granted to:", testDistributor);

        // Add pharmacy
        const tx3 = await drugTraceability.addPharmacy(testPharmacy);
        await tx3.wait();
        console.log("✅ Pharmacy role granted to:", testPharmacy);

    } catch (error) {
        console.log("⚠️  Role setup failed:", error.message);
    }

    // Verify contract info
    console.log("\n🔍 Verifying contract information...");

    const contractName = await drugTraceability.name();
    const contractSymbol = await drugTraceability.symbol();
    const hasAdminRole = await drugTraceability.hasRole(await drugTraceability.DEFAULT_ADMIN_ROLE(), deployer.address);

    console.log("📄 Contract Name:", contractName);
    console.log("🔤 Contract Symbol:", contractSymbol);
    console.log("👑 Deployer is Admin:", hasAdminRole);

    // Save deployment info
    const deploymentInfo = {
        contractName: "DrugTraceability",
        contractAddress: contractAddress,
        deployerAddress: deployer.address,
        transactionHash: deployTx.hash,
        blockNumber: deployTx.blockNumber,
        gasUsed: deployTx.gasLimit.toString(),
        timestamp: new Date().toISOString(),
        network: process.env.HARDHAT_NETWORK || "localhost",
        testAccounts: {
            manufacturer: testManufacturer,
            distributor: testDistributor,
            pharmacy: testPharmacy
        },
        contractABI: DrugTraceability.interface.format('json')
    };

    // Write to deployments folder
    const deploymentsDir = path.join(__dirname, '..', 'deployments');
    if (!fs.existsSync(deploymentsDir)) {
        fs.mkdirSync(deploymentsDir, { recursive: true });
    }

    const deploymentFile = path.join(deploymentsDir, 'DrugTraceability.json');
    fs.writeFileSync(deploymentFile, JSON.stringify(deploymentInfo, null, 2));
    console.log("\n💾 Deployment info saved to:", deploymentFile);

    // Write ABI separately
    const abiDir = path.join(__dirname, '..', 'abi');
    if (!fs.existsSync(abiDir)) {
        fs.mkdirSync(abiDir, { recursive: true });
    }
    const abiFile = path.join(abiDir, 'DrugTraceability.json');
    fs.writeFileSync(abiFile, JSON.stringify(JSON.parse(DrugTraceability.interface.formatJson()), null, 2));
    console.log("💾 ABI saved to:", abiFile);

    // Generate verification JSON
    const buildInfoDir = path.join(__dirname, '..', 'artifacts', 'build-info');
    const buildInfoFiles = fs.readdirSync(buildInfoDir).filter(f => f.endsWith('.json'));

    // Find newest build-info
    let newestFile = null;
    let newestTime = 0;
    for (const file of buildInfoFiles) {
        const stat = fs.statSync(path.join(buildInfoDir, file));
        if (stat.mtimeMs > newestTime) {
            newestTime = stat.mtimeMs;
            newestFile = file;
        }
    }

    if (newestFile) {
        const buildInfo = JSON.parse(fs.readFileSync(path.join(buildInfoDir, newestFile), 'utf8'));
        const verificationInput = {
            language: buildInfo.input.language,
            sources: buildInfo.input.sources,
            settings: {
                optimizer: buildInfo.input.settings.optimizer,
                evmVersion: buildInfo.input.settings.evmVersion || 'paris',
                outputSelection: {
                    "*": {
                        "*": ["abi", "evm.bytecode", "evm.deployedBytecode", "metadata"]
                    }
                }
            }
        };

        const verificationFile = path.join(__dirname, '..', `DrugTraceability-verification-${contractAddress.slice(0, 6)}.json`);
        fs.writeFileSync(verificationFile, JSON.stringify(verificationInput, null, 2));
        console.log("💾 Verification JSON saved to:", verificationFile);
        console.log("\n📋 VERIFICATION INFO:");
        console.log("   Compiler: v" + buildInfo.solcVersion);
        console.log("   Contract Name: contracts/DrugTraceability.sol:DrugTraceability");
    }

    // Print summary
    console.log("\n" + "=".repeat(70));
    console.log("🎉 DEPLOYMENT COMPLETED SUCCESSFULLY!");
    console.log("=".repeat(70));
    console.log("📍 Contract Address:", contractAddress);
    console.log("🌐 Network:", process.env.HARDHAT_NETWORK || "localhost");
    console.log("⏰ Timestamp:", new Date().toLocaleString());
    console.log("=".repeat(70));

    console.log("\n📝 Next Steps:");
    console.log("1. Update backend config with new contract address");
    console.log("2. Verify contract on Blockscout using the verification JSON");

    return contractAddress;
}

// Execute
main()
    .then((contractAddress) => {
        console.log("\n✅ Script completed successfully!");
        console.log("📍 Contract:", contractAddress);
        process.exit(0);
    })
    .catch((error) => {
        console.error("\n❌ Deployment failed:");
        console.error(error);
        process.exit(1);
    });
