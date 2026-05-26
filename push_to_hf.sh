#!/bin/bash

# Script hỗ trợ đẩy code PharmaChain lên Hugging Face Spaces nhanh nhất

echo "=========================================================="
echo "🚀 CHUẨN BỊ ĐẨY PHARMACHAIN LÊN HUGGING FACE SPACES..."
echo "=========================================================="

# Đọc tên Space
SPACE_NAME="pharmachain"
USERNAME="thanh141203"

echo "Tài khoản Hugging Face: $USERNAME"
echo "Tên Space dự kiến: $SPACE_NAME"
echo "----------------------------------------------------------"

# Khởi tạo git nếu chưa có
if [ ! -d ".git" ]; then
    echo "⚙️ Khởi tạo Git repository cục bộ..."
    git init
    git branch -M main
fi

# Thêm remote Hugging Face (xóa cái cũ nếu có để tránh lỗi)
git remote remove hf 2>/dev/null || true
git remote add hf "https://huggingface.co/spaces/$USERNAME/$SPACE_NAME"

echo "📝 Đang gom toàn bộ code, DB và cấu hình chuẩn bị đẩy đi..."
git add .
git commit -m "deploy: multi-process docker for Hugging Face" --allow-empty

echo ""
echo "=========================================================="
echo "⚠️ BƯỚC CUỐI CÙNG (HÀNH ĐỘNG CỦA BẠN):"
echo "----------------------------------------------------------"
echo "Hệ thống sẽ chạy lệnh đẩy code đi."
echo "Hugging Face sẽ hỏi:"
echo "1. Username: Bạn nhập 'thanh141203' và ấn Enter."
echo "2. Password: Bạn dán ACCESS TOKEN (loại Write) bạn vừa tạo vào và ấn Enter."
echo "   (Lưu ý: Không dùng mật khẩu đăng nhập của tài khoản, phải dùng Access Token nhé!)"
echo "=========================================================="
echo ""

read -p "👉 Bấm Enter để bắt đầu đẩy code đi..."

git push -f hf main
