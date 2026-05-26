const fs = require('fs');

async function verify() {
    const address = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
    const jsonPath = "DrugTraceability-verification-0x5FbD.json";
    
    console.log("Reading verification JSON...");
    if (!fs.existsSync(jsonPath)) {
        console.log("Error: JSON file not found.");
        return;
    }
    const sourceCode = fs.readFileSync(jsonPath, "utf8");
    
    const formData = new URLSearchParams();
    formData.append("module", "contract");
    formData.append("action", "verifysourcecode");
    formData.append("codeformat", "solidity-standard-json-input");
    formData.append("contractaddress", address);
    formData.append("contractname", "DrugTraceability");
    formData.append("compilerversion", "v0.8.20+commit.a1b79de6");
    formData.append("sourceCode", sourceCode);

    console.log(`Submitting verification for ${address} to Blockscout API...`);
    try {
        const response = await fetch("http://127.0.0.1:3000/api/", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: formData.toString()
        });
        
        const data = await response.json();
        console.log("Response:", data);
        
        if (data.status === "1") {
            console.log("✅ Verification submitted successfully!");
            console.log("GUID:", data.result);
            
            // Check status loop
            let attempt = 0;
            while (attempt < 10) {
                await new Promise(r => setTimeout(r, 2000));
                console.log(`Checking verification status (Attempt ${attempt+1})...`);
                const checkRes = await fetch(`http://127.0.0.1:3000/api/?module=contract&action=checkverifystatus&guid=${data.result}`);
                const checkData = await checkRes.json();
                console.log("Status:", checkData.result);
                
                if (checkData.result.includes("Pass") || checkData.result.includes("Success")) {
                    console.log("\n🎉 CONTRACT VERIFIED SUCCESSFULLY! No more weird characters on UI.");
                    break;
                } else if (checkData.result.includes("Fail")) {
                    console.log("\n❌ VERIFICATION FAILED");
                    break;
                }
                attempt++;
            }
        } else {
            console.log("❌ Failed to submit:", data.message, data.result);
        }
    } catch (e) {
        console.error("Fetch error:", e.message);
    }
}

verify();