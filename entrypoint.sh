#!/bin/bash

echo "🚀 Bắt đầu khởi chạy hệ thống PharmaChain trên Hugging Face Spaces..."

# 1. Khởi chạy MariaDB
echo "⏳ Đang chạy Database MariaDB..."
service mariadb start

# Đợi MariaDB sẵn sàng
until mysqladmin ping -u root --silent; do
    echo "⌛ Đang đợi MariaDB khởi động..."
    sleep 2
done
echo "✅ Database MariaDB đã sẵn sàng!"

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

echo "⌛ Đợi 5 giây cho Spring Boot ổn định..."
sleep 5
echo "✅ Spring Boot Backend đang chạy ngầm!"

# 3. Khởi chạy Nginx ở chế độ foreground để giữ Container luôn sống
echo "⏳ Khởi chạy Nginx Web Server..."
nginx -g "daemon off;"
