const fs = require('fs');
const path = require('path');

// Verify we are in the scripts directory or adjust paths
// This script assumes it is run from the project root or scripts folder
// We will look for artifacts/build-info

const projectRoot = path.join(__dirname, '..'); // Assuming script is in /scripts
const buildInfoDir = path.join(projectRoot, 'artifacts', 'build-info');

if (!fs.existsSync(buildInfoDir)) {
    console.error(`Error: Directory not found: ${buildInfoDir}`);
    console.error('Make sure you have compiled the contracts: npx hardhat compile');
    process.exit(1);
}

const files = fs.readdirSync(buildInfoDir).filter(f => f.endsWith('.json'));

if (files.length === 0) {
    console.error('Error: No build-info files found.');
    process.exit(1);
}

// Find the build-info file that contains our specific contract
let targetFile = null;
let targetInput = null;

// Sort files by mtime (newest first) to prioritize latest build
const sortedFiles = files.map(file => {
    const filePath = path.join(buildInfoDir, file);
    const stats = fs.statSync(filePath);
    return { file, mtime: stats.mtime, filePath };
}).sort((a, b) => b.mtime - a.mtime);

console.log(`Found ${sortedFiles.length} build-info files.`);

for (const { file, filePath } of sortedFiles) {
    try {
        console.log(`Checking ${file}...`);
        const content = fs.readFileSync(filePath, 'utf8');
        const buildInfo = JSON.parse(content);

        // Check if this build has DrugTraceability
        // Hardhat build-info structure: output.contracts['path/to/file.sol']['ContractName']
        if (
            buildInfo.output &&
            buildInfo.output.contracts &&
            buildInfo.output.contracts['contracts/DrugTraceability.sol'] &&
            buildInfo.output.contracts['contracts/DrugTraceability.sol']['DrugTraceability']
        ) {
            console.log(`✅ Found DrugTraceability in ${file}`);
            targetFile = file;
            targetInput = buildInfo.input;
            break;
        }
    } catch (err) {
        console.warn(`Skipping ${file} due to read error:`, err.message);
    }
}

if (targetInput) {
    const outputPath = path.join(projectRoot, 'standard-input.json');
    fs.writeFileSync(outputPath, JSON.stringify(targetInput, null, 2));
    console.log(`\n🎉 Success! Standard JSON input extracted to:`);
    console.log(outputPath);
    console.log(`\nNext steps for Blockscout Verification:`);
    console.log(`1. Go to Blockscout Verify Contract page.`);
    console.log(`2. Select 'Solidity (Standard JSON input)'.`);
    console.log(`3. Upload 'standard-input.json'.`);
    console.log(`4. Contract Name: DrugTraceability`);
    console.log(`5. Leave Constructor Arguments empty (Contract has no args).`);
    console.log(`6. Click Verify.`);
} else {
    console.error('\n❌ Could not find DrugTraceability in any build-info file.');
    console.error('Please run "npx hardhat compile --force" and try again.');
    process.exit(1);
}
