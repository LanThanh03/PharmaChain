const { ethers } = require("hardhat");

async function main() {
  const errorData = "0xe2517d3f000000000000000000000000f39fd6e51aad88f6f4ce6ab8827279cfffb92266eefb95e842a3287179d933b4460be539a1d5af11aa8b325bb45c5c8dc92de4ed";
  
  // Error signature
  const errorSelector = errorData.slice(0, 10);
  console.log("Error selector:", errorSelector);
  
  // This is AccessControl's AccessControlUnauthorizedAccount error
  // error AccessControlUnauthorizedAccount(address account, bytes32 neededRole)
  
  // Decode the data
  const iface = new ethers.Interface([
    "error AccessControlUnauthorizedAccount(address account, bytes32 neededRole)"
  ]);
  
  try {
    const decoded = iface.parseError(errorData);
    console.log("\n🔍 Decoded Error:");
    console.log("Error name:", decoded.name);
    console.log("Account:", decoded.args[0]);
    console.log("Needed Role:", decoded.args[1]);
    
    // Decode role name
    const roleHash = decoded.args[1];
    console.log("\n📋 Role Hash:", roleHash);
    
    // Check common roles
    const roles = {
      "MANUFACTURER_ROLE": ethers.keccak256(ethers.toUtf8Bytes("MANUFACTURER_ROLE")),
      "PHARMACY_ROLE": ethers.keccak256(ethers.toUtf8Bytes("PHARMACY_ROLE")),
      "DISTRIBUTOR_ROLE": ethers.keccak256(ethers.toUtf8Bytes("DISTRIBUTOR_ROLE")),
      "ADMIN_ROLE": ethers.keccak256(ethers.toUtf8Bytes("ADMIN_ROLE")),
    };
    
    console.log("\n🔑 Checking which role is needed:");
    for (const [roleName, roleHashValue] of Object.entries(roles)) {
      if (roleHashValue === roleHash) {
        console.log(`✅ MISSING ROLE: ${roleName}`);
        console.log(`   Account ${decoded.args[0]} không có role này!`);
      }
    }
    
  } catch (error) {
    console.error("Failed to decode:", error.message);
  }
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });

