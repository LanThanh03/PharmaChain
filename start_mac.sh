#!/bin/bash

# Hàm xử lý khi nhấn Ctrl+C
trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

echo "🚀 Đang khởi động TOÀN BỘ hệ thống PharmaChain (Full Stack)..."

BASE_DIR=$(pwd)

# 0. Khởi động Docker (Blockscout)
echo "🐳 [0/6] Đang khởi động Blockscout qua Docker..."
docker-compose up -d
echo "✅ Docker containers đang chạy ngầm."

# 1. Khởi động Hardhat Node
echo "📦 [1/6] Khởi động Hardhat Node (Port 8545)..."
cd "$BASE_DIR/contracts"
# Kiểm tra nếu port 8545 đã bị chiếm
if lsof -Pi :8545 -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️ Port 8545 đã bận, bỏ qua khởi động Hardhat."
else
    npx hardhat node > hardhat.log 2>&1 &
    sleep 5
fi

# 2. Khởi động Backend
echo "☕ [2/6] Khởi động Spring Boot Backend (Port 8080)..."
cd "$BASE_DIR/backend"
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️ Port 8080 đã bận, vui lòng tắt tiến trình cũ trước."
else
    ./mvnw spring-boot:run > "$BASE_DIR/backend_startup.log" 2>&1 &
    echo "⏳ Đang đợi Backend khởi động (khoảng 15s)..."
    sleep 15
fi

# 3. Khởi động Frontends
echo "🌐 [3/6] Khởi động Web Nhà Sản Xuất (Port 3001)..."
cd "$BASE_DIR/web_NhaSanXuat" && BROWSER=none PORT=3001 npm start > /dev/null 2>&1 &

echo "🌐 [4/6] Khởi động Web Nhà Phân Phối (Port 3002)..."
cd "$BASE_DIR/web_NhaPhanPhoi" && BROWSER=none PORT=3002 npm start > /dev/null 2>&1 &

echo "🌐 [5/6] Khởi động Web Hiệu Thuốc (Port 3003)..."
cd "$BASE_DIR/web_HieuThuoc" && BROWSER=none PORT=3003 npm start > /dev/null 2>&1 &

echo ""
echo "=========================================================="
echo "✅ HỆ THỐNG ĐÃ SẴN SÀNG!"
echo "----------------------------------------------------------"
echo "🔗 Explorer (Blockscout) : http://localhost:3000"
echo "🔗 Nhà Sản Xuất (NSX)    : http://localhost:3001"
echo "🔗 Nhà Phân Phối (NPP)   : http://localhost:3002"
echo "🔗 Hiệu Thuốc (HT)        : http://localhost:3003"
echo "🔗 Backend API Docs      : http://localhost:8080/swagger-ui.html"
echo "----------------------------------------------------------"
echo "👉 Xem log Backend tại: tail -f backend_startup.log"
echo "🛑 Nhấn [Ctrl + C] để dừng TOÀN BỘ hệ thống."
echo "=========================================================="

wait
