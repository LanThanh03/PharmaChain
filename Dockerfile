# Multi-process container for PharmaChain full-stack deployment
FROM ubuntu:22.04

# 1. Tránh prompt tương tác khi apt-get install
ENV DEBIAN_FRONTEND=noninteractive

# 2. Cài đặt các package cơ bản
RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    openjdk-21-jdk \
    nginx \
    mariadb-server \
    git \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# 3. Cài đặt Node.js 18 & npm
RUN mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_18.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list && \
    apt-get update && apt-get install nodejs -y

# 4. Tạo thư mục app
WORKDIR /app

# 5. Copy toàn bộ dự án vào Docker
COPY . .

# 6. Database MariaDB will be initialized at runtime in entrypoint.sh to avoid Docker build sandbox restrictions.

# 7. Cấu hình .env và Build 4 React Frontends statically
# Chúng ta build statically với PUBLIC_URL tương ứng để Nginx phân phối trực tiếp
ENV REACT_APP_API_BASE_URL=""
ENV REACT_APP_API_URL="/api"
ENV REACT_APP_BLOCKCHAIN_API_URL="/api/blockchain"

RUN echo "REACT_APP_API_BASE_URL=$REACT_APP_API_BASE_URL" > /app/web_NhaSanXuat/.env && \
    echo "REACT_APP_API_URL=$REACT_APP_API_URL" >> /app/web_NhaSanXuat/.env && \
    echo "REACT_APP_BLOCKCHAIN_API_URL=$REACT_APP_BLOCKCHAIN_API_URL" >> /app/web_NhaSanXuat/.env

RUN cp /app/web_NhaSanXuat/.env /app/web_NhaPhanPhoi/.env && \
    cp /app/web_NhaSanXuat/.env /app/web_HieuThuoc/.env && \
    cp /app/web_NhaSanXuat/.env /app/web_Admin/.env

# Build web_NhaSanXuat
RUN cd /app/web_NhaSanXuat && npm install && PUBLIC_URL=/nsx npm run build

# Build web_NhaPhanPhoi
RUN cd /app/web_NhaPhanPhoi && npm install && PUBLIC_URL=/npp npm run build

# Build web_HieuThuoc
RUN cd /app/web_HieuThuoc && npm install && PUBLIC_URL=/ht npm run build

# Build web_Admin
RUN cd /app/web_Admin && npm install && PUBLIC_URL=/admin npm run build

# 8. Build Backend Spring Boot
RUN cd /app/backend && chmod +x mvnw && ./mvnw clean package -DskipTests

# 9. Cấu hình Nginx
RUN rm /etc/nginx/sites-enabled/default
COPY nginx.conf /etc/nginx/sites-enabled/default

# 10. Copy trang Landing Page cực đẹp cho trang chủ
COPY index.html /var/www/html/index.html

# 11. Copy entrypoint script để khởi chạy các dịch vụ cùng lúc
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 12. Cấu hình port cho Hugging Face Spaces (mặc định là 7860)
EXPOSE 7860
ENV PORT=7860

# 13. Khởi chạy thông qua entrypoint
CMD ["/app/entrypoint.sh"]
