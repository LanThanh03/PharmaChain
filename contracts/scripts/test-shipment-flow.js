/**
 * Test Complete Shipment Flow với Dispatch Step
 * 
 * Flow mới:
 * 1. Manufacturer tạo batch
 * 2. Manufacturer tạo shipment (status: CREATED)
 * 3. ✅ Manufacturer DISPATCH shipment (status: IN_PROGRESS) - BƯỚC MỚI
 * 4. Distributor receive shipment (status: COMPLETED)
 * 5. Xem shipment history với tất cả checkpoints
 */

const { ethers } = require("hardhat");

async function main() {
  console.log("\n" + "=".repeat(80));
  console.log("🧪 TESTING COMPLETE SHIPMENT FLOW WITH DISPATCH");
  console.log("=".repeat(80) + "\n");

  // Get accounts - use Hardhat default accounts
  const signers = await ethers.getSigners();
  const deployer = signers[0];
  
  // Use hardcoded addresses from Hardhat config (these have roles already granted)
  const manufacturerAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
  const distributorAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC";
  
  // Create signers from private keys (Hardhat account #1 and #2)
  const manufacturer = new ethers.Wallet(
    "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d",
    ethers.provider
  );
  
  const distributor = new ethers.Wallet(
    "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a",
    ethers.provider
  );
  
  console.log("👥 Test Accounts:");
  console.log("   Deployer/Admin:", deployer.address);
  console.log("   Manufacturer:", manufacturer.address);
  console.log("   Distributor:", distributor.address);
  console.log();

  // Load deployed contract
  const contractAddress = "0x5FC8d32690cc91D4c39d9d3abcBD16989F875707"; // Updated with new deployment
  const PharmaLedger = await ethers.getContractFactory("PharmaLedger");
  const pharmaLedger = PharmaLedger.attach(contractAddress);

  console.log("📦 Contract Address:", contractAddress);
  console.log();

  // ========================================================================
  // STEP 0: Grant Roles
  // ========================================================================
  console.log("─".repeat(80));
  console.log("🔐 STEP 0: Granting Roles");
  console.log("─".repeat(80));
  
  try {
    const tx1 = await pharmaLedger.connect(deployer).addManufacturer(manufacturer.address);
    await tx1.wait();
    console.log("✅ Manufacturer role granted to:", manufacturer.address);
    
    const tx2 = await pharmaLedger.connect(deployer).addDistributor(distributor.address);
    await tx2.wait();
    console.log("✅ Distributor role granted to:", distributor.address);
  } catch (error) {
    console.log("⚠️  Roles may already be granted");
  }
  console.log();

  // ========================================================================
  // STEP 1: Create Batch
  // ========================================================================
  console.log("─".repeat(80));
  console.log("📦 STEP 1: Manufacturer Creates Batch");
  console.log("─".repeat(80));

  const drugInfo = {
    name: "Paracetamol 500mg",
    activeIngredient: "Paracetamol",
    dosage: "500mg",
    manufacturer: "ABC Pharma",
    registrationNumber: "VD-12345-21"
  };

  const quantity = 1000;
  const manufactureDate = Math.floor(Date.now() / 1000);
  const expiryDate = manufactureDate + (365 * 24 * 60 * 60); // +1 year
  const qrCode = `QR-BATCH-${Date.now()}`;

  const tx1 = await pharmaLedger.connect(manufacturer).issueBatch(
    drugInfo,
    quantity,
    manufactureDate,
    expiryDate,
    qrCode
  );
  
  const receipt1 = await tx1.wait();
  console.log("✅ Batch created! Transaction:", receipt1.hash);

  // Get batch ID from event
  const batchIssuedEvent = receipt1.logs.find(log => {
    try {
      const parsed = pharmaLedger.interface.parseLog(log);
      return parsed.name === "BatchIssued";
    } catch (e) {
      return false;
    }
  });

  const parsedEvent = pharmaLedger.interface.parseLog(batchIssuedEvent);
  const batchId = parsedEvent.args[0];
  
  console.log("   Batch ID:", batchId.toString());
  console.log("   Drug Name:", drugInfo.name);
  console.log("   Quantity:", quantity);
  console.log();

  // ========================================================================
  // STEP 2: Create Shipment
  // ========================================================================
  console.log("─".repeat(80));
  console.log("📤 STEP 2: Manufacturer Creates Shipment (Status: CREATED)");
  console.log("─".repeat(80));

  const trackingNumber = `TRK-${batchId}-${Date.now()}`;
  
  const tx2 = await pharmaLedger.connect(manufacturer).createShipment(
    batchId,
    distributor.address,
    500,  // Ship 500 units
    trackingNumber
  );
  
  const receipt2 = await tx2.wait();
  console.log("✅ Shipment created! Transaction:", receipt2.hash);

  // Get shipment ID from event
  const shipmentCreatedEvent = receipt2.logs.find(log => {
    try {
      const parsed = pharmaLedger.interface.parseLog(log);
      return parsed.name === "ShipmentCreated";
    } catch (e) {
      return false;
    }
  });

  const shipmentParsedEvent = pharmaLedger.interface.parseLog(shipmentCreatedEvent);
  const shipmentId = shipmentParsedEvent.args[0];
  
  console.log("   Shipment ID:", shipmentId.toString());
  console.log("   From:", manufacturer.address);
  console.log("   To:", distributor.address);
  console.log("   Quantity:", 500);
  console.log("   Tracking Number:", trackingNumber);
  console.log("   📊 Status: CREATED (chỉ tạo đơn hàng, chưa gửi)");
  console.log();

  // Get initial shipment history
  console.log("📜 Initial Shipment History:");
  const history1 = await pharmaLedger.getShipmentHistory(shipmentId);
  console.log(`   Found ${history1.length} checkpoint(s):`);
  for (let i = 0; i < history1.length; i++) {
    const checkpoint = history1[i];
    const date = new Date(Number(checkpoint.timestamp) * 1000);
    console.log(`   ${i + 1}. ${checkpoint.location} - ${getStatusName(checkpoint.status)}`);
    console.log(`      Time: ${date.toLocaleString()}`);
    console.log(`      Actor: ${checkpoint.actor}`);
    console.log(`      Notes: ${checkpoint.notes}`);
  }
  console.log();

  // ========================================================================
  // STEP 3: ✅ DISPATCH SHIPMENT (NEW!)
  // ========================================================================
  console.log("─".repeat(80));
  console.log("🚚 STEP 3: ✅ Manufacturer DISPATCHES Shipment (Status: IN_PROGRESS)");
  console.log("─".repeat(80));

  const dispatchLocation = "ABC Pharma Warehouse - Hanoi";
  const dispatchNotes = "Carrier: Express Logistics, Vehicle: VN-29A-12345, Driver: Nguyen Van A";

  const tx3 = await pharmaLedger.connect(manufacturer).dispatchShipment(
    shipmentId,
    dispatchLocation,
    dispatchNotes
  );
  
  const receipt3 = await tx3.wait();
  console.log("✅ Shipment dispatched! Transaction:", receipt3.hash);
  console.log("   Location:", dispatchLocation);
  console.log("   Notes:", dispatchNotes);
  console.log("   📊 Status: IN_PROGRESS (đã gửi hàng thực tế)");
  console.log();

  // Get updated shipment history after dispatch
  console.log("📜 Shipment History After Dispatch:");
  const history2 = await pharmaLedger.getShipmentHistory(shipmentId);
  console.log(`   Found ${history2.length} checkpoint(s):`);
  for (let i = 0; i < history2.length; i++) {
    const checkpoint = history2[i];
    const date = new Date(Number(checkpoint.timestamp) * 1000);
    console.log(`   ${i + 1}. ${checkpoint.location} - ${getStatusName(checkpoint.status)}`);
    console.log(`      Time: ${date.toLocaleString()}`);
    console.log(`      Actor: ${checkpoint.actor}`);
    console.log(`      Notes: ${checkpoint.notes}`);
  }
  console.log();

  // Optional: Add more checkpoints during transit
  console.log("─".repeat(80));
  console.log("📍 OPTIONAL: Adding Transit Checkpoints");
  console.log("─".repeat(80));

  // Checkpoint 1: Arrived at sorting center
  const tx4 = await pharmaLedger.connect(manufacturer).updateShipmentStatus(
    shipmentId,
    1, // IN_PROGRESS
    "Hanoi Sorting Center",
    "Package arrived at sorting center, temperature: 25°C"
  );
  await tx4.wait();
  console.log("✅ Checkpoint added: Hanoi Sorting Center");

  // Checkpoint 2: In transit to destination
  const tx5 = await pharmaLedger.connect(manufacturer).updateShipmentStatus(
    shipmentId,
    1, // IN_PROGRESS
    "Highway Route 1A - KM 150",
    "In transit to Ho Chi Minh City, ETA: 4 hours"
  );
  await tx5.wait();
  console.log("✅ Checkpoint added: Highway Route 1A");
  console.log();

  // Get full history with all checkpoints
  console.log("📜 Complete Shipment History (All Checkpoints):");
  const history3 = await pharmaLedger.getShipmentHistory(shipmentId);
  console.log(`   Found ${history3.length} checkpoint(s):`);
  for (let i = 0; i < history3.length; i++) {
    const checkpoint = history3[i];
    const date = new Date(Number(checkpoint.timestamp) * 1000);
    console.log(`   ${i + 1}. ${checkpoint.location} - ${getStatusName(checkpoint.status)}`);
    console.log(`      Time: ${date.toLocaleString()}`);
    console.log(`      Actor: ${checkpoint.actor}`);
    console.log(`      Notes: ${checkpoint.notes}`);
  }
  console.log();

  // ========================================================================
  // STEP 4: Receive Shipment
  // ========================================================================
  console.log("─".repeat(80));
  console.log("📥 STEP 4: Distributor Receives Shipment (Status: COMPLETED)");
  console.log("─".repeat(80));

  const tx6 = await pharmaLedger.connect(distributor).receiveShipment(shipmentId);
  const receipt6 = await tx6.wait();
  console.log("✅ Shipment received! Transaction:", receipt6.hash);
  console.log("   Receiver:", distributor.address);
  console.log("   📊 Status: COMPLETED");
  console.log();

  // ========================================================================
  // STEP 5: View Final History
  // ========================================================================
  console.log("─".repeat(80));
  console.log("📜 FINAL SHIPMENT HISTORY");
  console.log("─".repeat(80));

  const finalHistory = await pharmaLedger.getShipmentHistory(shipmentId);
  console.log(`✅ Complete tracking with ${finalHistory.length} checkpoints:\n`);
  
  for (let i = 0; i < finalHistory.length; i++) {
    const checkpoint = finalHistory[i];
    const date = new Date(Number(checkpoint.timestamp) * 1000);
    const statusEmoji = getStatusEmoji(checkpoint.status);
    
    console.log(`${statusEmoji} Checkpoint ${i + 1}: ${checkpoint.location}`);
    console.log(`   Status: ${getStatusName(checkpoint.status)}`);
    console.log(`   Time: ${date.toLocaleString()}`);
    console.log(`   Actor: ${checkpoint.actor}`);
    console.log(`   Notes: ${checkpoint.notes}`);
    console.log();
  }

  // Get shipment details summary
  const shipmentDetails = await pharmaLedger.getShipmentDetails(shipmentId);
  console.log("─".repeat(80));
  console.log("📊 SHIPMENT SUMMARY");
  console.log("─".repeat(80));
  console.log("Shipment ID:", shipmentDetails[0].shipmentId.toString());
  console.log("Batch ID:", shipmentDetails[0].batchId.toString());
  console.log("From:", shipmentDetails[0].from);
  console.log("To:", shipmentDetails[0].to);
  console.log("Quantity:", shipmentDetails[0].quantity.toString());
  console.log("Final Status:", getStatusName(shipmentDetails[0].status));
  console.log("Total Checkpoints:", shipmentDetails[1].toString());
  console.log();

  console.log("=".repeat(80));
  console.log("✅ TEST COMPLETED SUCCESSFULLY!");
  console.log("=".repeat(80));
  console.log("\n🎉 NEW FLOW với DISPATCH step đã hoạt động:");
  console.log("   1. ✅ Create Batch");
  console.log("   2. ✅ Create Shipment (CREATED)");
  console.log("   3. ✅ Dispatch Shipment (IN_PROGRESS) - BƯỚC MỚI!");
  console.log("   4. ✅ Add Transit Checkpoints");
  console.log("   5. ✅ Receive Shipment (COMPLETED)");
  console.log("   6. ✅ View Complete History\n");
}

// Helper functions
function getStatusName(status) {
  const statusMap = {
    0: "CREATED",
    1: "IN_PROGRESS",
    2: "COMPLETED",
    3: "CANCELLED"
  };
  return statusMap[status] || "UNKNOWN";
}

function getStatusEmoji(status) {
  const emojiMap = {
    0: "📝",
    1: "🚚",
    2: "✅",
    3: "❌"
  };
  return emojiMap[status] || "❓";
}

// Execute
main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("❌ Error:", error);
    process.exit(1);
  });

