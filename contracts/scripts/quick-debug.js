const hre = require("hardhat");

async function main() {
    const txHash = "0xf8460752b6755dab28b2ca36a9dde34c8ccc9b6cb41674e7609b5bc5d4e89f84";
    console.log("Checking transaction:", txHash);

    const tx = await hre.ethers.provider.getTransaction(txHash);
    if (!tx) {
        console.log("Error: Transaction not found. The blockchain might have been reset.");
        return;
    }

    const receipt = await hre.ethers.provider.getTransactionReceipt(txHash);
    console.log("Status:", receipt.status === 1 ? "SUCCESS" : "FAILED");

    if (receipt.status === 0) {
        console.log("Reason for failure:");
        try {
            // Re-run the transaction as a call to see the revert reason
            await hre.ethers.provider.call(tx, tx.blockNumber);
        } catch (error) {
            console.log(error.message);
        }
    }
}

main().catch(console.error);
