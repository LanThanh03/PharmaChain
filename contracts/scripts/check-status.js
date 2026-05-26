const hre = require("hardhat");

async function main() {
    const blockNumber = await hre.ethers.provider.getBlockNumber();
    console.log("Current block number:", blockNumber);

    const block = await hre.ethers.provider.getBlock(blockNumber);
    console.log("Latest block hash:", block.hash);
    console.log("Transactions in latest block:", block.transactions.length);

    if (block.transactions.length > 0) {
        console.log("Latest transaction:", block.transactions[0]);
    }
}

main().catch(console.error);
