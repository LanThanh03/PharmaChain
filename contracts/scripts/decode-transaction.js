const { ethers } = require("hardhat");

async function main() {
    const txHash = "0xc09e15ca0332e24f1f2b8a3b8fabb21ae004f6c4f433a112e51ca39a12546a00";
    const contractAddress = "0x5fc8d32690cc91d4c39d9d3abcbd16989f875707";

    console.log("=== DECODE TRANSACTION ===\n");
    console.log("Transaction Hash:", txHash);
    console.log("Contract Address:", contractAddress);
    console.log("");

    // Get transaction
    const tx = await ethers.provider.getTransaction(txHash);

    if (!tx) {
        console.log("Transaction not found!");
        return;
    }

    console.log("--- Raw Transaction Data ---");
    console.log("From:", tx.from);
    console.log("To:", tx.to);
    console.log("Value:", ethers.formatEther(tx.value), "ETH");
    console.log("Gas Limit:", tx.gasLimit.toString());
    console.log("Input Data:", tx.data);
    console.log("");

    // Get transaction receipt
    const receipt = await ethers.provider.getTransactionReceipt(txHash);
    console.log("--- Transaction Receipt ---");
    console.log("Status:", receipt.status === 1 ? "SUCCESS" : "FAILED");
    console.log("Gas Used:", receipt.gasUsed.toString());
    console.log("Block Number:", receipt.blockNumber);
    console.log("");

    // Decode the input data using contract ABI
    const PharmaLedger = await ethers.getContractFactory("DrugTraceability");
    const iface = PharmaLedger.interface;

    try {
        const decoded = iface.parseTransaction({ data: tx.data });

        console.log("=== DECODED INPUT DATA ===\n");
        console.log("Function Name:", decoded.name);
        console.log("Function Signature:", decoded.signature);
        console.log("");
        console.log("--- Arguments ---");

        // Get function fragment to get argument names
        const fragment = decoded.fragment;

        fragment.inputs.forEach((input, index) => {
            const value = decoded.args[index];
            console.log(`\n[${index}] ${input.name} (${input.type}):`);

            if (input.type === 'tuple') {
                // It's a struct
                console.log("   {");
                input.components.forEach((comp, i) => {
                    console.log(`     ${comp.name}: ${value[i]}`);
                });
                console.log("   }");
            } else if (input.type === 'bytes32') {
                console.log(`   ${value}`);
            } else if (input.type === 'uint256') {
                // Check if it's a timestamp
                if (input.name.toLowerCase().includes('date')) {
                    const date = new Date(Number(value) * 1000);
                    console.log(`   ${value.toString()} (${date.toISOString()})`);
                } else {
                    console.log(`   ${value.toString()}`);
                }
            } else {
                console.log(`   ${value}`);
            }
        });

    } catch (error) {
        console.log("Could not decode transaction:", error.message);
    }

    // Decode events from logs
    console.log("\n\n=== DECODED EVENTS ===\n");

    for (const log of receipt.logs) {
        try {
            const parsedLog = iface.parseLog({
                topics: log.topics,
                data: log.data
            });

            if (parsedLog) {
                console.log("Event:", parsedLog.name);
                console.log("Arguments:");
                parsedLog.fragment.inputs.forEach((input, index) => {
                    const value = parsedLog.args[index];
                    if (input.type === 'uint256' && input.name.toLowerCase().includes('date')) {
                        const date = new Date(Number(value) * 1000);
                        console.log(`  - ${input.name}: ${value.toString()} (${date.toISOString()})`);
                    } else if (input.type === 'bytes32') {
                        console.log(`  - ${input.name}: ${value}`);
                    } else {
                        console.log(`  - ${input.name}: ${value}`);
                    }
                });
                console.log("");
            }
        } catch (e) {
            // Not a log from our contract
        }
    }
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
