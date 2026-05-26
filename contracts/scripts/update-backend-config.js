const fs = require('fs');
const path = require('path');

async function updateBackendConfig() {
    console.log('📝 Updating backend configuration with latest contract addresses...\n');
    
    // Read deployment files
    const drugItemDeployment = JSON.parse(
        fs.readFileSync('deployments/DrugItemTracker.json', 'utf8')
    );
    const pharmaDeployment = JSON.parse(
        fs.readFileSync('deployments/PharmaLedger.json', 'utf8')
    );
    
    const drugItemAddress = drugItemDeployment.address || drugItemDeployment.contractAddress;
    const pharmaAddress = pharmaDeployment.contractAddress;
    
    console.log('Contract Addresses from deployments:');
    console.log(`  DrugItemTracker: ${drugItemAddress}`);
    console.log(`  PharmaLedger: ${pharmaAddress}\n`);
    
    // Read application.properties
    const propsPath = path.join('..', 'backend', 'src', 'main', 'resources', 'application.properties');
    let props = fs.readFileSync(propsPath, 'utf8');
    
    // Update addresses
    const oldDrugItemMatch = props.match(/drugitemtracker\.contract\.address=0x[a-fA-F0-9]{40}/);
    const oldPharmaMatch = props.match(/pharmaledger\.contract\.address=0x[a-fA-F0-9]{40}/);
    
    if (oldDrugItemMatch) {
        console.log(`Updating DrugItemTracker address:`);
        console.log(`  Old: ${oldDrugItemMatch[0].split('=')[1]}`);
        console.log(`  New: ${drugItemAddress}`);
        props = props.replace(
            /drugitemtracker\.contract\.address=0x[a-fA-F0-9]{40}/,
            `drugitemtracker.contract.address=${drugItemAddress}`
        );
    }
    
    if (oldPharmaMatch) {
        console.log(`Updating PharmaLedger address:`);
        console.log(`  Old: ${oldPharmaMatch[0].split('=')[1]}`);
        console.log(`  New: ${pharmaAddress}`);
        props = props.replace(
            /pharmaledger\.contract\.address=0x[a-fA-F0-9]{40}/,
            `pharmaledger.contract.address=${pharmaAddress}`
        );
    }
    
    // Also update the generic blockchain.contract.address to DrugItemTracker
    props = props.replace(
        /blockchain\.contract\.address=0x[a-fA-F0-9]{40}/,
        `blockchain.contract.address=${drugItemAddress}`
    );
    
    // Write back
    fs.writeFileSync(propsPath, props, 'utf8');
    
    console.log('\n✅ Backend configuration updated!');
    console.log('\n📋 Summary:');
    console.log(`  File: backend/src/main/resources/application.properties`);
    console.log(`  DrugItemTracker: ${drugItemAddress}`);
    console.log(`  PharmaLedger: ${pharmaAddress}`);
    console.log('\n⚠️  Remember to restart your Spring Boot application!');
}

updateBackendConfig()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error('❌ Error:', error.message);
        process.exit(1);
    });

