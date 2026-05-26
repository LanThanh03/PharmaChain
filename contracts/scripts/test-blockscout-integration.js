const { ethers } = require("hardhat");
const fs = require('fs');
const path = require('path');
const http = require('http');

/**
 * Blockscout Integration Test
 * Tests end-to-end transaction visibility on Blockscout
 */

const BLOCKSCOUT_API = "http://localhost:3000/api";
const WAIT_TIME = 10000; // 10 seconds

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function checkTransactionOnBlockscout(txHash, maxRetries = 6) {
  console.log(`\n🔍 Checking transaction on Blockscout: ${txHash}`);
  
  for (let i = 0; i < maxRetries; i++) {
    if (i > 0) {
      console.log(`   Retry ${i}/${maxRetries}...`);
      await sleep(5000);
    }
    
    const result = await new Promise((resolve) => {
      const req = http.get(`${BLOCKSCOUT_API}/v2/transactions/${txHash}`, (res) => {
        let data = '';
        
        res.on('data', chunk => { data += chunk; });
        res.on('end', () => {
          try {
            if (res.statusCode === 200) {
              const txInfo = JSON.parse(data);
              resolve({
                found: true,
                method: txInfo.method || null,
                decoded: txInfo.method && !txInfo.method.includes('unrecognized'),
                status: txInfo.status,
                blockNumber: txInfo.block
              });
            } else {
              resolve({ found: false });
            }
          } catch (error) {
            resolve({ found: false });
          }
        });
      });
      
      req.on('error', () => resolve({ found: false }));
      req.setTimeout(5000, () => {
        req.destroy();
        resolve({ found: false });
      });
    });
    
    if (result.found) {
      console.log("   ✅ Transaction found on Blockscout!");
      console.log("   Method:", result.method || "<unrecognized-selector>");
      console.log("   Decoded:", result.decoded ? "YES" : "NO");
      console.log("   Status:", result.status);
      console.log("   Block:", result.blockNumber);
      return result;
    }
  }
  
  console.log("   ❌ Transaction not found after", maxRetries, "retries");
  return { found: false };
}

async function main() {
  console.log("\n" + "=".repeat(70));
  console.log("🧪 BLOCKSCOUT INTEGRATION TEST");
  console.log("=".repeat(70) + "\n");

  // Load deployment info
  const deploymentFile = path.join(__dirname, '..', 'deployments', `deployment-${hre.network.name}.json`);
  
  if (!fs.existsSync(deploymentFile)) {
    console.log("❌ Deployment file not found. Deploy contracts first:");
    console.log("   npx hardhat run scripts/deploy-all-contracts.js --network localhost");
    process.exit(1);
  }

  const deployment = JSON.parse(fs.readFileSync(deploymentFile, 'utf8'));
  const pharmaLedgerAddress = deployment.contracts.pharmaLedger.address;
  const drugItemTrackerAddress = deployment.contracts.drugItemTracker.address;

  console.log("📋 Test Configuration:");
  console.log("   Network:", hre.network.name);
  console.log("   PharmaLedger:", pharmaLedgerAddress);
  console.log("   DrugItemTracker:", drugItemTrackerAddress);

  // Get signer
  const [signer] = await ethers.getSigners();
  console.log("\n👤 Test Account:", signer.address);

  // ========================================================================
  // TEST 1: Create test transaction on PharmaLedger
  // ========================================================================
  console.log("\n" + "─".repeat(70));
  console.log("🧪 TEST 1: PharmaLedger - Issue Batch");
  console.log("─".repeat(70));

  const PharmaLedger = await ethers.getContractFactory("PharmaLedger");
  const pharmaLedger = PharmaLedger.attach(pharmaLedgerAddress);

  // Grant manufacturer role to signer
  const MANUFACTURER_ROLE = await pharmaLedger.MANUFACTURER_ROLE();
  const hasRole = await pharmaLedger.hasRole(MANUFACTURER_ROLE, signer.address);
  
  if (!hasRole) {
    console.log("🔐 Granting manufacturer role...");
    const grantTx = await pharmaLedger.grantRole(MANUFACTURER_ROLE, signer.address);
    await grantTx.wait();
    console.log("   ✅ Role granted");
  }

  // Create test batch
  console.log("\n📦 Creating test batch...");
  const drugInfo = {
    name: "Test Drug " + Date.now(),
    activeIngredient: "Test Ingredient",
    dosage: "500mg",
    manufacturer: "Test Manufacturer",
    registrationNumber: "TEST-" + Date.now()
  };

  const quantity = 100;
  const manufactureDate = Math.floor(Date.now() / 1000);
  const expiryDate = manufactureDate + (365 * 24 * 60 * 60); // 1 year
  const qrCode = "TEST_QR_" + Date.now();

  const tx1 = await pharmaLedger.issueBatch(
    drugInfo,
    quantity,
    manufactureDate,
    expiryDate,
    qrCode
  );

  console.log("   Transaction submitted:", tx1.hash);
  const receipt1 = await tx1.wait();
  console.log("   ✅ Transaction confirmed in block:", receipt1.blockNumber);

  // Wait for Blockscout to index
  console.log(`\n⏳ Waiting ${WAIT_TIME/1000}s for Blockscout to index...`);
  await sleep(WAIT_TIME);

  // Check on Blockscout
  const result1 = await checkTransactionOnBlockscout(tx1.hash);

  // ========================================================================
  // TEST 2: Create test transaction on DrugItemTracker
  // ========================================================================
  console.log("\n" + "─".repeat(70));
  console.log("🧪 TEST 2: DrugItemTracker - Register Batch");
  console.log("─".repeat(70));

  const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
  const drugItemTracker = DrugItemTracker.attach(drugItemTrackerAddress);

  // Grant manufacturer role
  const MANUFACTURER_ROLE_2 = await drugItemTracker.MANUFACTURER_ROLE();
  const hasRole2 = await drugItemTracker.hasRole(MANUFACTURER_ROLE_2, signer.address);
  
  if (!hasRole2) {
    console.log("🔐 Granting manufacturer role...");
    const grantTx = await drugItemTracker.grantRole(MANUFACTURER_ROLE_2, signer.address);
    await grantTx.wait();
    console.log("   ✅ Role granted");
  }

  console.log("\n📦 Registering test batch...");
  const batchNumber = "BATCH-" + Date.now();
  const drugName = "Test Item " + Date.now();
  const itemCount = 50;
  const merkleRoot = ethers.keccak256(ethers.toUtf8Bytes("test_root"));

  const tx2 = await drugItemTracker.registerBatch(
    batchNumber,
    drugName,
    itemCount,
    merkleRoot,
    expiryDate
  );

  console.log("   Transaction submitted:", tx2.hash);
  const receipt2 = await tx2.wait();
  console.log("   ✅ Transaction confirmed in block:", receipt2.blockNumber);

  // Wait for Blockscout to index
  console.log(`\n⏳ Waiting ${WAIT_TIME/1000}s for Blockscout to index...`);
  await sleep(WAIT_TIME);

  // Check on Blockscout
  const result2 = await checkTransactionOnBlockscout(tx2.hash);

  // ========================================================================
  // RESULTS SUMMARY
  // ========================================================================
  console.log("\n" + "=".repeat(70));
  console.log("📊 TEST RESULTS");
  console.log("=".repeat(70));

  const test1Pass = result1.found && result1.decoded;
  const test2Pass = result2.found && result2.decoded;

  console.log("\nTest 1 - PharmaLedger.issueBatch():");
  console.log("   Found on Blockscout:", result1.found ? "✅ YES" : "❌ NO");
  console.log("   Transaction decoded:", result1.decoded ? "✅ YES" : "❌ NO");
  console.log("   Status:", test1Pass ? "✅ PASS" : "❌ FAIL");
  console.log("   Link:", `http://localhost:3000/tx/${tx1.hash}`);

  console.log("\nTest 2 - DrugItemTracker.registerBatch():");
  console.log("   Found on Blockscout:", result2.found ? "✅ YES" : "❌ NO");
  console.log("   Transaction decoded:", result2.decoded ? "✅ YES" : "❌ NO");
  console.log("   Status:", test2Pass ? "✅ PASS" : "❌ FAIL");
  console.log("   Link:", `http://localhost:3000/tx/${tx2.hash}`);

  console.log("\n" + "─".repeat(70));
  const allTestsPass = test1Pass && test2Pass;
  
  if (allTestsPass) {
    console.log("🎉 ALL TESTS PASSED!");
    console.log("   Blockchain integration is working correctly");
    console.log("   Transactions are visible and decoded on Blockscout");
  } else {
    console.log("❌ SOME TESTS FAILED");
    
    if (!result1.found || !result2.found) {
      console.log("\n💡 Transactions not found on Blockscout:");
      console.log("   - Check Blockscout is running: docker-compose ps");
      console.log("   - Check Blockscout logs: docker-compose logs blockscout");
      console.log("   - Verify network connectivity");
    }
    
    if ((result1.found && !result1.decoded) || (result2.found && !result2.decoded)) {
      console.log("\n💡 Transactions not decoded:");
      console.log("   - Contracts may not be verified on Blockscout");
      console.log("   - Run: npx hardhat run scripts/verify-contracts-blockscout.js --network localhost");
      console.log("   - Or verify manually via Blockscout UI");
    }
  }

  console.log("=".repeat(70) + "\n");

  return allTestsPass ? 0 : 1;
}

main()
  .then((exitCode) => {
    process.exit(exitCode);
  })
  .catch((error) => {
    console.error("\n❌ Test execution failed:");
    console.error(error);
    process.exit(1);
  });

