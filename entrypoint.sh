#!/bin/bash

echo "🚀 Bắt đầu khởi chạy hệ thống PharmaChain trên Hugging Face Spaces..."

# Cấu hình MariaDB dùng ít RAM để tránh OOM trên free tier
mkdir -p /etc/mysql/conf.d
cat > /etc/mysql/conf.d/low-memory.cnf << 'MYCNF'
[mysqld]
innodb_buffer_pool_size = 64M
innodb_log_file_size = 32M
innodb_flush_method = O_DSYNC
max_connections = 20
table_open_cache = 64
key_buffer_size = 8M
MYCNF

# 1. Khởi chạy MariaDB
echo "⏳ Đang chạy Database MariaDB..."
service mariadb start

# Đợi MariaDB sẵn sàng
until mysqladmin ping -u root --silent; do
    echo "⌛ Đang đợi MariaDB khởi động..."
    sleep 2
done
echo "✅ Database MariaDB đã sẵn sàng!"

# Đặt mật khẩu root TRƯỚC (MariaDB compatible)
mysql -u root -e "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('rootpassword'); FLUSH PRIVILEGES;" 2>/dev/null || \
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'rootpassword'; FLUSH PRIVILEGES;" 2>/dev/null || true

# Khởi tạo và import dữ liệu nếu chưa có database
if ! mysql -u root -p"rootpassword" -e "use BlockChain_DA" 2>/dev/null; then
    echo "📦 Tạo database và import dữ liệu mới nhất..."
    mysql -u root -p"rootpassword" -e "CREATE DATABASE IF NOT EXISTS BlockChain_DA CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    # SQL file đã được pre-process để tương thích MariaDB (không còn _binary syntax)
    mysql --force -u root -p"rootpassword" BlockChain_DA < "/app/blockchain_da FULL.sql"
    echo "✅ Đã nạp Database BlockChain_DA thành công!"
else
    echo "ℹ️ Database BlockChain_DA đã tồn tại, bỏ qua bước import."
fi

# 2. Khởi chạy Backend Spring Boot
echo "⏳ Đang khởi chạy Spring Boot Backend..."
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/BlockChain_DA?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="rootpassword"
export APP_BASE_URL="http://localhost:7860"

# Cài đặt giới hạn RAM để tránh bị OOM trên free containers
export JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2 -Xms128m -Xmx512m"

cd /app/backend
java $JAVA_OPTS -jar target/*.jar > /app/backend.log 2>&1 &
JAVA_PID=$!

# Đợi Spring Boot thực sự sẵn sàng trên port 8080 (tối đa 120 giây)
echo "⌛ Đang chờ Spring Boot khởi động hoàn toàn trên port 8080..."
WAITED=0
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1 || \
      curl -sf http://localhost:8080/api/manufacturer/auth/login -X POST > /dev/null 2>&1 || \
      nc -z localhost 8080 2>/dev/null; do
    if [ $WAITED -ge 120 ]; then
        echo "⚠️ Spring Boot chưa sẵn sàng sau 120 giây, kiểm tra /app/backend.log:"
        tail -n 30 /app/backend.log
        break
    fi
    echo "⌛ Spring Boot chưa lên... đã đợi ${WAITED}s"
    sleep 5
    WAITED=$((WAITED + 5))
done
echo "✅ Spring Boot Backend đã sẵn sàng sau ${WAITED}s!"

# 3. Khởi chạy Nginx ở chế độ foreground để giữ Container luôn sống
echo "⏳ Khởi chạy Nginx Web Server..."
nginx -g "daemon off;"
