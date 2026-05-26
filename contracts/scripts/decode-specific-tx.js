const hre = require("hardhat");

async function main() {
    const txHash = "0x95c1552a6a41ac3b3ec8ddba644e733653922292bd4801afb9cc67560470dba4";
    
    console.log("Searching for transaction:", txHash);
    
    try {
        const tx = await hre.ethers.provider.getTransaction(txHash);
        
        if (!tx) {
            console.log("\n❌ Transaction NOT FOUND on this local network.");
            console.log("Reason: This might be a transaction from an older run before the node restarted, or it's on a different network (like Sepolia Mainnet).");
            return;
        }

        const receipt = await hre.ethers.provider.getTransactionReceipt(txHash);
        
        console.log("\n✅ TRANSACTION FOUND!");
        console.log("--------------------------------------------------");
        console.log("Block Number :", tx.blockNumber);
        console.log("From (Sender):", tx.from);
        console.log("To (Receiver):", tx.to);
        console.log("Value sent   :", hre.ethers.formatEther(tx.value), "ETH");
        console.log("Status       :", receipt.status === 1 ? "SUCCESS (1)" : "FAILED (0)");
        
        // Try to decode assuming it went to our contract
        const contractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
        
        if (tx.to && tx.to.toLowerCase() === contractAddress.toLowerCase()) {
            console.log("\n📦 It's an interaction with the DrugTraceability Contract.");
            
            const DrugTraceability = await hre.ethers.getContractFactory("DrugTraceability");
            const iface = DrugTraceability.interface;
            
            try {
                const decoded = iface.parseTransaction({ data: tx.data });
                console.log("\n🛠 FUNCTION CALLED:");
                console.log("Name:", decoded.name);
                
                console.log("\n📋 ARGUMENTS PASSED:");
                decoded.fragment.inputs.forEach((input, index) => {
                    let val = decoded.args[index];
                    // Format BigInts nicely
                    if (typeof val === 'bigint') val = val.toString();
                    
                    // Special formatting for dates if we guess it's a timestamp
                    if (input.name.toLowerCase().includes('date') && !isNaN(val)) {
                         const dateObj = new Date(Number(val) * 1000);
                         console.log(`  [${input.name}]: ${val} -> ${dateObj.toLocaleString()}`);
                    } else if (input.type === 'tuple') {
                         console.log(`  [${input.name}]: (Struct data)`);
                         console.log(val);
                    } else {
                         console.log(`  [${input.name}]: ${val}`);
                    }
                });
                
                console.log("\n🔔 EVENTS EMITTED (Logs):");
                let eventFound = false;
                for (const log of receipt.logs) {
                    try {
                        const parsedLog = iface.parseLog({ topics: log.topics, data: log.data });
                        if (parsedLog) {
                            console.log(`  - Event: ${parsedLog.name}`);
                            eventFound = true;
                        }
                    } catch(e) {}
                }
                if (!eventFound) console.log("  (No recognized events)");

            } catch (e) {
                console.log("\n⚠️ Could not decode input data using the contract ABI.");
                console.log("Raw Data:", tx.data);
            }
        } else {
            console.log("\n🌐 This is NOT a contract interaction (or sent to a different contract).");
            console.log("Raw Data:", tx.data);
            try {
                const text = hre.ethers.toUtf8String(tx.data);
                if (text && text.trim().length > 0) {
                    console.log("Decoded Text:", text);
                }
            } catch(e) {}
        }
        console.log("--------------------------------------------------");

    } catch (error) {
        console.error("Error querying provider:", error.message);
    }
}

main().catch(console.error);
