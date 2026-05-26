const fs = require('fs');
const path = require('path');

/**
 * Script để tạo file Standard Input JSON cho việc verify contract trên Blockscout
 * 
 * Sử dụng: npx hardhat run scripts/generate-verification-json.js --network localhost
 */

async function main() {
    console.log("📝 Đang tạo file verification JSON cho DrugTraceability...\n");

    // Đọc file deployment để lấy địa chỉ contract
    const deploymentPath = path.join(__dirname, '../deployments/DrugTraceability.json');
    let contractAddress = "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"; // Default

    try {
        const deployment = JSON.parse(fs.readFileSync(deploymentPath, 'utf8'));
        contractAddress = deployment.contractAddress || deployment.address || contractAddress;
        console.log(`📍 Contract Address: ${contractAddress}`);
    } catch (e) {
        console.log("⚠️ Không tìm thấy file deployment. Bạn cần nhập địa chỉ contract thủ công.");
    }

    // Đọc build-info để lấy solcInput chính xác từ Hardhat
    const artifactsDir = path.join(__dirname, '../artifacts');
    const buildInfoDir = path.join(artifactsDir, 'build-info');

    if (!fs.existsSync(buildInfoDir)) {
        console.error("❌ Không tìm thấy thư mục build-info. Hãy chạy 'npx hardhat compile' trước.");
        return;
    }

    const buildInfoFiles = fs.readdirSync(buildInfoDir).filter(f => f.endsWith('.json'));

    if (buildInfoFiles.length === 0) {
        console.error("❌ Không tìm thấy file build-info nào.");
        return;
    }

    // Đọc file build-info
    const buildInfoPath = path.join(buildInfoDir, buildInfoFiles[0]);
    console.log(`📂 Đọc build-info từ: ${buildInfoFiles[0]}`);

    const buildInfo = JSON.parse(fs.readFileSync(buildInfoPath, 'utf8'));

    // Lấy solcInput (đây là Standard Input JSON)
    const solcInput = buildInfo.input;

    // Xác nhận compiler version
    const solcVersion = buildInfo.solcVersion;
    console.log(`🔧 Compiler version: ${solcVersion}`);
    console.log(`⚙️  Optimizer enabled: ${solcInput.settings.optimizer?.enabled}`);
    console.log(`⚙️  Optimizer runs: ${solcInput.settings.optimizer?.runs}`);
    console.log(`⚙️  EVM Version: ${solcInput.settings.evmVersion || 'default'}`);
    console.log(`⚙️  Via IR: ${solcInput.settings.viaIR || false}`);

    // Tạo tên file với địa chỉ ngắn
    const shortAddress = contractAddress.substring(0, 6);
    const outputFileName = `DrugTraceability-verification-${shortAddress}.json`;
    const outputPath = path.join(__dirname, '..', outputFileName);

    // Ghi file
    fs.writeFileSync(outputPath, JSON.stringify(solcInput, null, 2));

    console.log(`\n✅ File verification JSON đã được tạo: ${outputFileName}`);
    console.log(`   Kích thước: ${(fs.statSync(outputPath).size / 1024).toFixed(2)} KB`);

    // Hiển thị hướng dẫn verify
    console.log(`
╔═══════════════════════════════════════════════════════════════════╗
║           HƯỚNG DẪN VERIFY TRÊN BLOCKSCOUT                        ║
╠═══════════════════════════════════════════════════════════════════╣
║                                                                   ║
║ 1. Truy cập: http://localhost:3000/address/${contractAddress}   ║
║                                                                   ║
║ 2. Click vào tab "Contract" → "Verify & Publish"                 ║
║                                                                   ║
║ 3. Chọn: "Solidity (Standard JSON input)"                        ║
║                                                                   ║
║ 4. Điền thông tin:                                               ║
║    • Contract Name: DrugTraceability                              ║
║    • Compiler: v${solcVersion.padEnd(22)}                         ║
║    • Optimization: Enabled (200 runs)                             ║
║                                                                   ║
║ 5. Upload file: ${outputFileName.padEnd(30)}                      ║
║                                                                   ║
║ 6. Click "Verify & Publish"                                       ║
║                                                                   ║
╚═══════════════════════════════════════════════════════════════════╝
`);

    // In ra settings chi tiết để kiểm tra
    console.log("\n📋 CHI TIẾT SETTINGS TRONG FILE VERIFICATION:");
    console.log(JSON.stringify(solcInput.settings, null, 2));
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
