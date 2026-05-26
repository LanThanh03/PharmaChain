#!/bin/bash

# --- Khởi tạo Remote Demo Cố Định ---
# Script mở public port qua LocalTunnel với SUBDOMAIN CỐ ĐỊNH KHÔNG ĐỔI

# Tên miền phụ độc nhất của bạn (không sợ đụng hàng)
SUFFIX="thanhvlt"

# Hàm dọn dẹp khi nhấn Ctrl+C
trap "echo '🛑 Đang đóng tất cả kết nối Remote Demo...'; pkill -f 'localtunnel --port'; exit 0" SIGINT SIGTERM

echo "🚀 Đang setup LocalTunnel với các Link cố định (Không bao giờ đổi)..."

BASE_DIR=$(pwd)

echo "⏳ Đang dọn dẹp kết nối cũ..."
pkill -f 'localtunnel --port' || true
pkill -f 'lt --port' || true
rm -f backend_tunnel.log nsx_tunnel.log npp_tunnel.log ht_tunnel.log admin_tunnel.log

# 1. Start Backend tunnel
BACKEND_SUBDOMAIN="pharma-be-$SUFFIX"
echo "⏳ [1/5] Đang mở đường truyền Backend API tại: https://$BACKEND_SUBDOMAIN.loca.lt"
npx localtunnel --port 8080 --subdomain "$BACKEND_SUBDOMAIN" > backend_tunnel.log 2>&1 &

# Đợi vài giây cho kết nối ổn định
sleep 4

BACKEND_URL="https://$BACKEND_SUBDOMAIN.loca.lt"
echo "✅ Đã gán Backend URL cố định: $BACKEND_URL"

# 2. Cấu hình file .env cố định cho các Frontend
echo "📝 [2/5] Cấu hình tự động API URL cố định cho các Frontend..."

FRONTENDS=("web_NhaSanXuat" "web_NhaPhanPhoi" "web_HieuThuoc" "web_Admin")
for FE in "${FRONTENDS[@]}"; do
    if [ -d "$BASE_DIR/$FE" ]; then
        echo "REACT_APP_API_BASE_URL=$BACKEND_URL" > "$BASE_DIR/$FE/.env"
        echo "REACT_APP_API_URL=$BACKEND_URL/api" >> "$BASE_DIR/$FE/.env"
        echo "REACT_APP_BLOCKCHAIN_API_URL=$BACKEND_URL/api/blockchain" >> "$BASE_DIR/$FE/.env"
    fi
done

# 3. Start Frontend Tunnels với subdomain cố định
echo "🌐 [3/5] Public Web Nhà Sản Xuất tại: https://pharma-nsx-$SUFFIX.loca.lt"
npx localtunnel --port 3001 --subdomain "pharma-nsx-$SUFFIX" > nsx_tunnel.log 2>&1 &

echo "🌐 [4/5] Public Web Nhà Phân Phối tại: https://pharma-npp-$SUFFIX.loca.lt"
npx localtunnel --port 3002 --subdomain "pharma-npp-$SUFFIX" > npp_tunnel.log 2>&1 &

echo "🌐 [5/5] Public Web Hiệu Thuốc tại: https://pharma-ht-$SUFFIX.loca.lt"
npx localtunnel --port 3003 --subdomain "pharma-ht-$SUFFIX" > ht_tunnel.log 2>&1 &

echo "🌐 Public Web Admin tại: https://pharma-admin-$SUFFIX.loca.lt"
npx localtunnel --port 3004 --subdomain "pharma-admin-$SUFFIX" > admin_tunnel.log 2>&1 &

echo "⏳ Đang kích hoạt toàn bộ đường truyền..."
sleep 5

echo ""
echo "=========================================================="
echo "🎉 HỆ THỐNG DEMO ONLINE VỚI LINK CỐ ĐỊNH ĐÃ SẴN SÀNG!"
echo "Bạn có thể tắt máy, mở lại thoải mái. Các link này KHÔNG BAO GIỜ THAY ĐỔI!"
echo "----------------------------------------------------------"
echo "🔗 [WEB] Nhà Sản Xuất  : https://pharma-nsx-$SUFFIX.loca.lt"
echo "🔗 [WEB] Nhà Phân Phối : https://pharma-npp-$SUFFIX.loca.lt"
echo "🔗 [WEB] Hiệu Thuốc    : https://pharma-ht-$SUFFIX.loca.lt"
echo "🔗 [WEB] Admin         : https://pharma-admin-$SUFFIX.loca.lt"
echo "----------------------------------------------------------"
echo "🔗 [API] Backend       : https://pharma-be-$SUFFIX.loca.lt"
echo "----------------------------------------------------------"
echo "💡 Lưu ý cực quan trọng khi demo:"
echo "1. Lần đầu truy cập, bảo khách nhấn nút màu xanh 'Click to Continue' là vào thẳng."
echo "2. Khi nào mở lại máy tính, chỉ cần chạy lại ./start_remote_demo.sh là xong!"
echo "🛑 BẤM [Ctrl + C] ĐỂ TẮT TẤT CẢ LINK DEMO."
echo "=========================================================="

wait
