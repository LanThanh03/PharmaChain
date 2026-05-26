const { ethers } = require("hardhat");

async function main() {
  console.log("🔐 Granting MANUFACTURER_ROLE...\n");

  // Get contract address from deployment
  const fs = require('fs');
  const path = require('path');
  const deploymentFile = path.join(__dirname, '..', 'deployments', 'PharmaLedger.json');
  
  if (!fs.existsSync(deploymentFile)) {
    console.error("❌ Deployment file not found. Please deploy contract first.");
    process.exit(1);
  }

  const deployment = JSON.parse(fs.readFileSync(deploymentFile, 'utf8'));
  const contractAddress = deployment.contractAddress;

  console.log("📍 Contract Address:", contractAddress);

  // Get signers
  const [deployer, manufacturer1] = await ethers.getSigners();
  console.log("👤 Deployer:", deployer.address);
  if (manufacturer1) {
    console.log("👤 Manufacturer 1:", manufacturer1.address);
  }

  // Get contract instance
  const PharmaLedger = await ethers.getContractFactory("PharmaLedger");
  const pharmaLedger = PharmaLedger.attach(contractAddress);

  // Calculate role hash
  const MANUFACTURER_ROLE = await pharmaLedger.MANUFACTURER_ROLE();
  console.log("\n🔑 MANUFACTURER_ROLE hash:", MANUFACTURER_ROLE);

  // Grant role to deployer (for testing)
  console.log("\n📝 Granting MANUFACTURER_ROLE to deployer...");
  const tx1 = await pharmaLedger.addManufacturer(deployer.address);
  await tx1.wait();
  console.log("✅ Deployer now has MANUFACTURER_ROLE");
  console.log("   TX:", tx1.hash);

  // Grant role to manufacturer1 if available
  if (manufacturer1) {
    console.log("\n📝 Granting MANUFACTURER_ROLE to manufacturer1...");
    const tx2 = await pharmaLedger.addManufacturer(manufacturer1.address);
    await tx2.wait();
    console.log("✅ Manufacturer1 now has MANUFACTURER_ROLE");
    console.log("   TX:", tx2.hash);
  }

  // Verify roles
  console.log("\n🔍 Verifying roles...");
  const hasRole1 = await pharmaLedger.hasRole(MANUFACTURER_ROLE, deployer.address);
  console.log("✅ Deployer has MANUFACTURER_ROLE:", hasRole1);
  
  if (manufacturer1) {
    const hasRole2 = await pharmaLedger.hasRole(MANUFACTURER_ROLE, manufacturer1.address);
    console.log("✅ Manufacturer1 has MANUFACTURER_ROLE:", hasRole2);
  }

  console.log("\n" + "=".repeat(60));
  console.log("🎉 MANUFACTURER_ROLE granted successfully!");
  console.log("=".repeat(60));
  console.log("\n📋 Accounts with MANUFACTURER_ROLE:");
  console.log("- " + deployer.address);
  if (manufacturer1) {
    console.log("- " + manufacturer1.address);
  }
  console.log("\nYou can now call issueBatch() with these accounts!");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("❌ Error:", error);
    process.exit(1);
  });

