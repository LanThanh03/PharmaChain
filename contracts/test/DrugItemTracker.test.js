const { expect } = require("chai");
const { ethers } = require("hardhat");
const { MerkleTree } = require("merkletreejs");
const keccak256 = require("keccak256");

describe("DrugItemTracker", function () {
  let drugItemTracker;
  let admin, manufacturer, distributor, pharmacy, consumer;
  let MANUFACTURER_ROLE, DISTRIBUTOR_ROLE, PHARMACY_ROLE;

  // Helper function to create Merkle Tree
  function createMerkleTree(itemCodes) {
    const leaves = itemCodes.map((code) => keccak256(code));
    return new MerkleTree(leaves, keccak256, { sortPairs: true });
  }

  // Helper function to get Merkle proof
  function getMerkleProof(tree, itemCode) {
    const leaf = keccak256(itemCode);
    return tree.getHexProof(leaf);
  }

  beforeEach(async function () {
    [admin, manufacturer, distributor, pharmacy, consumer] = await ethers.getSigners();

    // Deploy contract
    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    drugItemTracker = await DrugItemTracker.deploy();
    await drugItemTracker.waitForDeployment();

    // Get role constants
    MANUFACTURER_ROLE = await drugItemTracker.MANUFACTURER_ROLE();
    DISTRIBUTOR_ROLE = await drugItemTracker.DISTRIBUTOR_ROLE();
    PHARMACY_ROLE = await drugItemTracker.PHARMACY_ROLE();

    // Grant roles
    await drugItemTracker.grantRoleToAddress(MANUFACTURER_ROLE, manufacturer.address);
    await drugItemTracker.grantRoleToAddress(DISTRIBUTOR_ROLE, distributor.address);
    await drugItemTracker.grantRoleToAddress(PHARMACY_ROLE, pharmacy.address);
  });

  describe("Deployment", function () {
    it("Should set the right admin", async function () {
      const DEFAULT_ADMIN_ROLE = await drugItemTracker.DEFAULT_ADMIN_ROLE();
      expect(await drugItemTracker.hasRole(DEFAULT_ADMIN_ROLE, admin.address)).to.be.true;
    });

    it("Should grant roles correctly", async function () {
      expect(await drugItemTracker.hasRole(MANUFACTURER_ROLE, manufacturer.address)).to.be.true;
      expect(await drugItemTracker.hasRole(DISTRIBUTOR_ROLE, distributor.address)).to.be.true;
      expect(await drugItemTracker.hasRole(PHARMACY_ROLE, pharmacy.address)).to.be.true;
    });
  });

  describe("Batch Creation with Merkle Tree", function () {
    let itemCodes;
    let merkleTree;
    let merkleRoot;

    beforeEach(function () {
      // Create 10 test items
      itemCodes = Array.from({ length: 10 }, (_, i) => 
        `PARA-BATCH001-${String(i + 1).padStart(4, "0")}`
      );
      merkleTree = createMerkleTree(itemCodes);
      merkleRoot = merkleTree.getHexRoot();
    });

    it("Should create batch with Merkle root", async function () {
      const batchId = 1;
      const batchNumber = "BATCH-2024-001";
      const drugName = "Paracetamol 500mg";
      const itemCount = itemCodes.length;
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60; // 1 year

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(
          batchId,
          batchNumber,
          drugName,
          itemCount,
          merkleRoot,
          expiryTimestamp
        );

      const batch = await drugItemTracker.getBatch(batchId);
      expect(batch.batchId).to.equal(batchId);
      expect(batch.batchNumber).to.equal(batchNumber);
      expect(batch.drugName).to.equal(drugName);
      expect(batch.itemCount).to.equal(itemCount);
      expect(batch.merkleRoot).to.equal(merkleRoot);
      expect(batch.manufacturer).to.equal(manufacturer.address);
      expect(batch.isActive).to.be.true;
    });

    it("Should fail if not manufacturer", async function () {
      const batchId = 1;
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      await expect(
        drugItemTracker
          .connect(consumer)
          .createBatchWithMerkleRoot(
            batchId,
            "BATCH-001",
            "Drug",
            10,
            merkleRoot,
            expiryTimestamp
          )
      ).to.be.revertedWith("Caller is not a manufacturer");
    });

    it("Should verify item in batch with Merkle proof", async function () {
      const batchId = 1;
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(
          batchId,
          "BATCH-001",
          "Drug",
          itemCodes.length,
          merkleRoot,
          expiryTimestamp
        );

      // Verify first item
      const itemCode = itemCodes[0];
      const proof = getMerkleProof(merkleTree, itemCode);
      const isValid = await drugItemTracker.verifyItemInBatch(itemCode, batchId, proof);
      
      expect(isValid).to.be.true;
    });

    it("Should fail verification with wrong proof", async function () {
      const batchId = 1;
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(
          batchId,
          "BATCH-001",
          "Drug",
          itemCodes.length,
          merkleRoot,
          expiryTimestamp
        );

      // Try to verify with wrong item code
      const fakeItemCode = "FAKE-ITEM-0001";
      const proof = getMerkleProof(merkleTree, itemCodes[0]); // Wrong proof
      const isValid = await drugItemTracker.verifyItemInBatch(fakeItemCode, batchId, proof);
      
      expect(isValid).to.be.false;
    });
  });

  describe("Item Registration", function () {
    let itemCodes;
    let merkleTree;
    let merkleRoot;
    const batchId = 1;

    beforeEach(async function () {
      itemCodes = Array.from({ length: 5 }, (_, i) => 
        `PARA-BATCH001-${String(i + 1).padStart(4, "0")}`
      );
      merkleTree = createMerkleTree(itemCodes);
      merkleRoot = merkleTree.getHexRoot();

      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(
          batchId,
          "BATCH-001",
          "Drug",
          itemCodes.length,
          merkleRoot,
          expiryTimestamp
        );
    });

    it("Should register items with valid proofs", async function () {
      const itemsToRegister = [itemCodes[0], itemCodes[1]];
      const proofs = itemsToRegister.map(code => getMerkleProof(merkleTree, code));

      await drugItemTracker
        .connect(manufacturer)
        .registerItems(batchId, itemsToRegister, proofs);

      // Check item status
      const item0 = await drugItemTracker.getItemStatus(itemsToRegister[0]);
      expect(item0.itemCode).to.equal(itemsToRegister[0]);
      expect(item0.batchId).to.equal(batchId);
      expect(item0.currentOwner).to.equal(manufacturer.address);
      expect(item0.status).to.equal("MANUFACTURED");
    });

    it("Should track total items", async function () {
      const itemsToRegister = [itemCodes[0], itemCodes[1]];
      const proofs = itemsToRegister.map(code => getMerkleProof(merkleTree, code));

      await drugItemTracker
        .connect(manufacturer)
        .registerItems(batchId, itemsToRegister, proofs);

      const totalTracked = await drugItemTracker.totalItemsTracked();
      expect(totalTracked).to.equal(2);
    });
  });

  describe("Item Transfer", function () {
    let itemCodes;
    let merkleTree;
    const batchId = 1;

    beforeEach(async function () {
      itemCodes = Array.from({ length: 5 }, (_, i) => 
        `PARA-BATCH001-${String(i + 1).padStart(4, "0")}`
      );
      merkleTree = createMerkleTree(itemCodes);
      const merkleRoot = merkleTree.getHexRoot();
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(
          batchId,
          "BATCH-001",
          "Drug",
          itemCodes.length,
          merkleRoot,
          expiryTimestamp
        );

      // Register first item
      const proof = getMerkleProof(merkleTree, itemCodes[0]);
      await drugItemTracker
        .connect(manufacturer)
        .registerItems(batchId, [itemCodes[0]], [proof]);
    });

    it("Should transfer item from manufacturer to distributor", async function () {
      const itemCode = itemCodes[0];

      await drugItemTracker
        .connect(manufacturer)
        .transferItem(
          itemCode,
          distributor.address,
          "SHIP",
          "Shipping to distributor",
          batchId,
          []
        );

      const item = await drugItemTracker.getItemStatus(itemCode);
      expect(item.currentOwner).to.equal(distributor.address);
      expect(item.status).to.equal("IN_TRANSIT");
    });

    it("Should auto-register item with valid Merkle proof", async function () {
      const itemCode = itemCodes[1]; // Not registered yet
      const proof = getMerkleProof(merkleTree, itemCode);

      await drugItemTracker
        .connect(manufacturer)
        .transferItem(
          itemCode,
          distributor.address,
          "SHIP",
          "First transfer",
          batchId,
          proof
        );

      const item = await drugItemTracker.getItemStatus(itemCode);
      expect(item.itemCode).to.equal(itemCode);
      expect(item.batchId).to.equal(batchId);
    });

    it("Should record transfer history", async function () {
      const itemCode = itemCodes[0];

      await drugItemTracker
        .connect(manufacturer)
        .transferItem(
          itemCode,
          distributor.address,
          "SHIP",
          "To distributor",
          batchId,
          []
        );

      const history = await drugItemTracker.getItemHistory(itemCode);
      expect(history.length).to.equal(2); // MANUFACTURE + SHIP
      expect(history[1].transferType).to.equal("SHIP");
      expect(history[1].toAddress).to.equal(distributor.address);
    });
  });

  describe("Item Sale", function () {
    let itemCode;
    const batchId = 1;

    beforeEach(async function () {
      const itemCodes = ["PARA-BATCH001-0001"];
      const merkleTree = createMerkleTree(itemCodes);
      const merkleRoot = merkleTree.getHexRoot();
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      itemCode = itemCodes[0];

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(batchId, "BATCH-001", "Drug", 1, merkleRoot, expiryTimestamp);

      const proof = getMerkleProof(merkleTree, itemCode);
      await drugItemTracker
        .connect(manufacturer)
        .registerItems(batchId, [itemCode], [proof]);

      // Transfer to pharmacy
      await drugItemTracker
        .connect(manufacturer)
        .transferItem(itemCode, pharmacy.address, "SHIP", "To pharmacy", batchId, []);
    });

    it("Should record item sale", async function () {
      const saleTime = Math.floor(Date.now() / 1000);

      await drugItemTracker
        .connect(pharmacy)
        .recordItemSale(itemCode, saleTime);

      const item = await drugItemTracker.getItemStatus(itemCode);
      expect(item.status).to.equal("SOLD");
    });
  });

  describe("Recall", function () {
    let itemCode;
    const batchId = 1;

    beforeEach(async function () {
      const itemCodes = ["PARA-BATCH001-0001"];
      const merkleTree = createMerkleTree(itemCodes);
      const merkleRoot = merkleTree.getHexRoot();
      const expiryTimestamp = Math.floor(Date.now() / 1000) + 365 * 24 * 60 * 60;

      itemCode = itemCodes[0];

      await drugItemTracker
        .connect(manufacturer)
        .createBatchWithMerkleRoot(batchId, "BATCH-001", "Drug", 1, merkleRoot, expiryTimestamp);

      const proof = getMerkleProof(merkleTree, itemCode);
      await drugItemTracker
        .connect(manufacturer)
        .registerItems(batchId, [itemCode], [proof]);
    });

    it("Should recall item", async function () {
      await drugItemTracker
        .connect(manufacturer)
        .recallItem(itemCode, "Quality issue");

      const isRecalled = await drugItemTracker.isItemRecalled(itemCode);
      expect(isRecalled).to.be.true;
    });

    it("Should recall entire batch", async function () {
      await drugItemTracker
        .connect(manufacturer)
        .recallBatch(batchId, "Batch contamination");

      const batch = await drugItemTracker.getBatch(batchId);
      expect(batch.isActive).to.be.false;

      const isRecalled = await drugItemTracker.isItemRecalled(itemCode);
      expect(isRecalled).to.be.true;
    });

    it("Should prevent transfer of recalled item", async function () {
      await drugItemTracker
        .connect(manufacturer)
        .recallItem(itemCode, "Quality issue");

      await expect(
        drugItemTracker
          .connect(manufacturer)
          .transferItem(itemCode, distributor.address, "SHIP", "Try to ship", batchId, [])
      ).to.be.revertedWith("Item is recalled");
    });
  });
});

