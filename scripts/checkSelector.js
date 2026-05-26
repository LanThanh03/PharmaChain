const { ethers } = require('ethers');

const signatures = [
  'registerItems(uint256,string[],bytes32[][])',
  'createBatchWithMerkleRoot(uint256,string,string,uint256,bytes32,uint256)',
  'transferItem(string,address,string,string,uint256,bytes32[])',
  'executeItemTrackerTransaction(bytes)',
  'safeEncode(bytes)',
];

for (const sig of signatures) {
  const selector = ethers.keccak256(ethers.toUtf8Bytes(sig)).slice(0, 10);
  console.log(`${selector}  ${sig}`);
}

