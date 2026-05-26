/**
 * Query Item History Script
 *
 * Mục đích: Truy vấn lịch sử đầy đủ của sản phẩm từ blockchain
 * Hiển thị toàn bộ chuỗi cung ứng
 */

const { ethers } = require("hardhat");

// Configuration
const CONFIG = {
    CONTRACT_ADDRESS: process.env.CONTRACT_ADDRESS || ""
};

/**
 * Truy vấn lịch sử sản phẩm
 */
async function queryItemHistory(itemCode) {
    const [signer] = await ethers.getSigners();

    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);

    console.log("\n🔍 TRUY VẤN LỊCH SỬ SẢN PHẨM");
    console.log("=".repeat(70));
    console.log(`Item Code: ${itemCode}\n`);

    // 1. Lấy thông tin hiện tại
    const itemStatus = await contract.getItemStatus(itemCode);

    if (!itemStatus.itemCode) {
        console.log("❌ Sản phẩm không tồn tại trong hệ thống\n");
        return null;
    }

    console.log("📊 THÔNG TIN HIỆN TẠI");
    console.log("─".repeat(70));
    console.log(`Batch ID:        ${itemStatus.batchId}`);
    console.log(`Owner hiện tại:  ${itemStatus.currentOwner}`);
    console.log(`Trạng thái:      ${itemStatus.status}`);
    console.log(`Bị thu hồi:      ${itemStatus.isRecalled ? "❌ CÓ" : "✅ KHÔNG"}`);
    console.log(`Cập nhật lần cuối: ${new Date(Number(itemStatus.lastUpdateTimestamp) * 1000).toLocaleString()}`);

    // 2. Lấy thông tin batch
    const batch = await contract.getBatch(itemStatus.batchId);
    console.log("\n📦 THÔNG TIN LÔ HÀNG");
    console.log("─".repeat(70));
    console.log(`Số lô:           ${batch.batchNumber}`);
    console.log(`Tên thuốc:       ${batch.drugName}`);
    console.log(`Nhà sản xuất:    ${batch.manufacturer}`);
    console.log(`Ngày sản xuất:   ${new Date(Number(batch.manufactureTimestamp) * 1000).toLocaleString()}`);
    console.log(`Ngày hết hạn:    ${new Date(Number(batch.expiryTimestamp) * 1000).toLocaleString()}`);
    console.log(`Số sản phẩm:     ${batch.itemCount}`);
    console.log(`Đang hoạt động:  ${batch.isActive ? "✅ CÓ" : "❌ KHÔNG"}`);

    // 3. Kiểm tra tính hợp lệ
    const isExpired = await contract.isItemExpired(itemCode);
    const isRecalled = await contract.isItemRecalled(itemCode);

    console.log("\n🔐 KIỂM TRA TÍNH HỢP LỆ");
    console.log("─".repeat(70));
    console.log(`Đã hết hạn:      ${isExpired ? "❌ CÓ" : "✅ KHÔNG"}`);
    console.log(`Đã thu hồi:      ${isRecalled ? "❌ CÓ" : "✅ KHÔNG"}`);
    console.log(`Trạng thái:      ${(!isExpired && !isRecalled) ? "✅ HỢP LỆ" : "❌ KHÔNG HỢP LỆ"}`);

    // 4. Lấy lịch sử transfer
    const history = await contract.getItemHistory(itemCode);

    console.log("\n📜 LỊCH SỬ CHUYỂN GIAO");
    console.log("=".repeat(70));
    console.log(`Tổng số giao dịch: ${history.length}\n`);

    history.forEach((transfer, idx) => {
        const fromAddr = transfer.fromAddress === ethers.ZeroAddress
            ? "GENESIS (Sản xuất)"
            : transfer.fromAddress;
        const toAddr = transfer.toAddress === ethers.ZeroAddress
            ? "CONSUMER (Khách hàng)"
            : transfer.toAddress;

        console.log(`┌─ [${idx + 1}] ${transfer.transferType} ─────────────────────────────────────`);
        console.log(`│  Từ:       ${fromAddr}`);
        console.log(`│  Đến:      ${toAddr}`);
        console.log(`│  Thời gian: ${new Date(Number(transfer.timestamp) * 1000).toLocaleString()}`);
        console.log(`│  Ghi chú:   ${transfer.notes}`);
        console.log(`└${"─".repeat(69)}\n`);
    });

    // 5. Timeline visualization
    console.log("⏱️  TIMELINE");
    console.log("=".repeat(70));

    const stages = {
        "MANUFACTURE": "🏭 Sản xuất",
        "SHIP": "🚚 Vận chuyển",
        "RECEIVE": "📦 Nhận hàng",
        "SALE": "💰 Bán lẻ"
    };

    history.forEach((transfer, idx) => {
        const icon = stages[transfer.transferType] || "📌";
        const time = new Date(Number(transfer.timestamp) * 1000).toLocaleString();
        console.log(`${icon}  ${time} - ${transfer.transferType}`);

        if (idx < history.length - 1) {
            console.log("    │");
            console.log("    ↓");
        }
    });

    console.log("\n" + "=".repeat(70) + "\n");

    return {
        itemStatus,
        batch,
        history,
        isExpired,
        isRecalled
    };
}

/**
 * Query bằng events (efficient hơn)
 */
async function queryItemHistoryByEvents(itemCode) {
    const [signer] = await ethers.getSigners();

    const DrugItemTracker = await ethers.getContractFactory("DrugItemTracker");
    const contract = DrugItemTracker.attach(CONFIG.CONTRACT_ADDRESS);

    console.log(`\n🔍 Query events cho item: ${itemCode}\n`);

    // Lọc tất cả ItemTransferred events
    const filter = contract.filters.ItemTransferred(itemCode);
    const events = await contract.queryFilter(filter);

    console.log(`📊 Tìm thấy ${events.length} events:\n`);

    for (const event of events) {
        console.log(`Block ${event.blockNumber}:`);
        console.log(`  From: ${event.args.fromAddress}`);
        console.log(`  To: ${event.args.toAddress}`);
        console.log(`  Type: ${event.args.transferType}`);
        console.log(`  Timestamp: ${new Date(Number(event.args.timestamp) * 1000).toLocaleString()}`);
        console.log("");
    }

    return events;
}

// Export functions
module.exports = {
    queryItemHistory,
    queryItemHistoryByEvents
};

// CLI execution
if (require.main === module) {
    const args = process.argv.slice(2);

    if (args.length < 1) {
        console.log("\n❌ Usage:");
        console.log("   npx hardhat run scripts/query-item-history.js --network localhost ITEM_CODE\n");
        console.log("Options:");
        console.log("   --events    Query bằng events instead of storage\n");
        process.exit(1);
    }

    const itemCode = args[0];
    const useEvents = args.includes("--events");

    const queryFn = useEvents ? queryItemHistoryByEvents : queryItemHistory;

    queryFn(itemCode)
        .then(() => process.exit(0))
        .catch(error => {
            console.error("❌ Error:", error);
            process.exit(1);
        });
}

