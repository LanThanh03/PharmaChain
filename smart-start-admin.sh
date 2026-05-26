#!/bin/bash
# --- SMART START ADMIN ---
# Script khởi động an toàn cho Admin Portal

echo "📌 Đang giải phóng Port 8080 (BE) và 3004 (Admin)..."
lsof -ti:8080,3004 | xargs kill -9 || true

echo "🐳 Đang đảm bảo Docker MySQL khởi động..."
docker start mysql_blockchain || echo "⚠️ Cảnh báo: Không thể khởi động Docker MySQL"

# Kiểm tra JAVA_HOME
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
if [ ! -d "$JAVA_HOME" ]; then
    echo "⚠️ Không tìm thấy Java 21 tại $JAVA_HOME. Sử dụng mặc định."
    unset JAVA_HOME
fi

echo "☕ Đang khởi động Backend (chế độ tối ưu)..."
cd "$(dirname "$0")/backend"
./mvnw clean spring-boot:run > ../backend.log 2>&1 &

# Chờ Backend mở cổng
echo "⏳ Đang đợi Backend sẵn sàng (khoảng 20-30s)..."
for i in {1..30}; do
    if lsof -i:8080 > /dev/null; then
        echo "✅ Backend đã sẵn sàng tại port 8080!"
        break
    fi
    sleep 2
    echo -n "."
done

echo "🌐 Đang khởi động Web Admin (Port 3004)..."
cd ../web_Admin
PORT=3004 npm start > ../admin.log 2>&1 &

echo "🚀 Xong! Bạn có thể vào http://localhost:3004 để kiểm tra."
echo "👉 Xem log BE: tail -f backend.log"
