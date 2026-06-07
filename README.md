---
title: PharmaChain
emoji: 💊
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---

# PharmaChain - Blockchain-based Drug Supply Chain System

Hệ thống tích hợp công nghệ Blockchain giúp giám sát, kiểm định chất lượng thuốc và tối ưu hóa quy trình phân phối dược phẩm thời gian thực.

## Các Phân Hệ & Portals trong Hệ Thống:
1. **Nhà Sản Xuất (Port 3001):** Quản lý lô sản xuất, cấp Merkle Proof và xuất hàng cho NPP.
2. **Nhà Phân Phối (Port 3002):** Xác thực nguồn gốc, nhập kho và xuất hàng cho Hiệu Thuốc.
3. **Hiệu Thuốc (Port 3003):** Quản lý kho dược phẩm, kê đơn và bán thuốc lẻ.
4. **Quản Trị Viên Admin (Port 3004):** Kiểm soát tài khoản, smart contract và lịch sử chuỗi cung ứng.
5. **Spring Boot Backend API (Port 8080):** Xử lý nghiệp vụ chính, tương tác cơ sở dữ liệu MySQL và Blockchain.
6. **Hardhat Blockchain Node (Port 8545):** Mạng Blockchain local giả lập Ethereum để deploy Smart Contracts.
7. **Blockscout Explorer (Port 3000):** Giao diện quét block, giao dịch và smart contract của chuỗi khối.
8. **Ứng Dụng Di Động (Flutter):** Ứng dụng di động để quét mã QR và theo dõi vòng đời thuốc.

---

## Hướng Dẫn Cài Đặt & Khởi Chạy Trên Windows

### I. Chuẩn Bị Công Cụ (Prerequisites)
Đảm bảo máy tính Windows của bạn đã cài đặt các công cụ sau:
1. **Node.js** (Phiên bản gợi ý: 18.x hoặc 20.x LTS)
2. **Java JDK** (Phiên bản 17 hoặc 21 để chạy Spring Boot Backend)
3. **Maven** (Đã được tích hợp sẵn qua file `mvnw.cmd` trong thư mục backend)
4. **MySQL / MariaDB** (Thông qua **XAMPP** hoặc cài đặt MySQL Server độc lập, chạy ở cổng `3306`)
5. **Docker Desktop** (Nếu muốn chạy Blockscout Explorer để xem giao dịch blockchain trực quan)
6. **Flutter SDK** (Nếu muốn chạy hoặc build app di động)

---

### II. Cấu Hình Ban Đầu (Setup)

#### 1. Khởi tạo Cơ sở dữ liệu MySQL (Sử dụng XAMPP & phpMyAdmin)
Nếu sử dụng **XAMPP** để chạy MySQL, hãy làm theo các bước sau:
1. Mở ứng dụng **XAMPP Control Panel**.
2. Nhấn nút **Start** ở dòng **MySQL** để khởi động cơ sở dữ liệu (chạy tại cổng mặc định `3306`).
3. Nhấn nút **Admin** ở dòng MySQL hoặc truy cập vào trình duyệt địa chỉ: `http://localhost/phpmyadmin`.
4. **Tạo Database mới**:
   * Tại thanh menu bên trái phpMyAdmin, click vào **New** (Mới).
   * Ô tên Cơ sở dữ liệu điền: **`BlockChain_DA`**.
   * Phần bảng mã (collation) chọn: `utf8mb4_general_ci` hoặc `utf8_general_ci`.
   * Nhấn nút **Create** (Tạo).
5. **Import dữ liệu**:
   * Click chọn database `BlockChain_DA` vừa tạo ở cột bên trái.
   * Chọn tab **Import** (Nhập) ở menu phía trên.
   * Nhấn nút **Choose File** (Chọn tệp) và trỏ tới file backup SQL trong thư mục dự án (khuyên dùng file `blockchain_da_latest.sql` hoặc `blockchain_da FULL.sql`).
   * Kéo xuống dưới cùng và nhấn **Import / Go** (Nhập / Thực hiện) để hoàn tất.

#### 2. Cấu hình file `application.properties` (Backend)
Vào thư mục `backend/src/main/resources/application.properties` chỉnh sửa thông tin kết nối database:
* Nếu sử dụng **XAMPP mặc định**, tài khoản kết nối sẽ là:
  ```properties
  spring.datasource.username=root
  spring.datasource.password=
  ```
  *(Mật khẩu để trống, trùng khớp hoàn toàn với cấu hình mặc định của XAMPP)*
* Nếu bạn đặt mật khẩu riêng cho tài khoản root trong XAMPP, hãy điền mật khẩu đó vào sau dấu `=`.

#### 3. Cài đặt các Dependency (Node Modules)
Mở CMD hoặc PowerShell tại thư mục gốc của dự án và chạy các lệnh cài đặt sau:
```cmd
# Cài đặt cho smart contract
cd contracts
npm install

# Cài đặt cho các trang Web Portal
cd ../web_Admin
npm install

cd ../web_NhaSanXuat
npm install

cd ../web_NhaPhanPhoi
npm install

cd ../web_HieuThuoc
npm install
```

---

### III. Khởi Chạy Hệ Thống

#### Bước 1: Chạy Blockchain Local (Hardhat Node)
Mở một cửa sổ CMD mới, di chuyển vào thư mục `contracts` và chạy:
```cmd
cd contracts
start-node.bat
```
*(Lệnh này sẽ khởi chạy mạng blockchain local lắng nghe tại cổng `localhost:8545`)*

#### Bước 2: Biên Dịch & Deploy Smart Contracts
Mở một cửa sổ CMD mới, di chuyển vào thư mục `contracts` và chạy:
```cmd
cd contracts
deploy-and-test-optimized.bat
```
*(Script này sẽ tự động biên dịch, deploy contract lên Blockchain Node vừa mở, và chạy kịch bản test thử nghiệm)*

#### Bước 3: Chạy Backend Spring Boot
Mở một cửa sổ CMD mới, di chuyển vào thư mục `backend` và chạy:
```cmd
cd backend
mvnw.cmd spring-boot:run
```
*(Chờ khoảng 15-20s đến khi log hiển thị `Started Application...` tại cổng `8080`)*

#### Bước 4: Chạy Các Giao Diện Web Portals (Frontend)
Mở các cửa sổ CMD tương ứng và chạy lệnh khởi động dưới đây (hoặc nhấp đúp vào `start-services.bat` để chạy nhanh các portal NSX, NPP, HT):

* **Nhà Sản Xuất (Port 3001)**:
  ```cmd
  cd web_NhaSanXuat
  set PORT=3001
  npm start
  ```
* **Nhà Phân Phối (Port 3002)**:
  ```cmd
  cd web_NhaPhanPhoi
  set PORT=3002
  npm start
  ```
* **Hiệu Thuốc (Port 3003)**:
  ```cmd
  cd web_HieuThuoc
  set PORT=3003
  npm start
  ```
* **Quản Trị Viên Admin (Port 3004)**:
  ```cmd
  cd web_Admin
  set PORT=3004
  npm start
  ```

#### Bước 5: Chạy Blockscout Explorer (Tùy chọn - Yêu cầu Docker)
Nếu muốn sử dụng giao diện quét giao dịch Blockchain:
1. Mở Docker Desktop.
2. Mở một cửa sổ CMD mới tại thư mục gốc của dự án và chạy:
   ```cmd
   docker-compose up -d
   ```
3. Truy cập vào `http://localhost:3000` để sử dụng Blockscout.

#### Bước 6: Chạy Ứng Dụng Di Động Flutter (Tùy chọn)
Kết nối điện thoại Android/iOS của bạn hoặc mở máy ảo lên, tại thư mục gốc chạy:
```cmd
flutter run
```

---

## IV. Bảng Tổng Hợp URL Truy Cập

| Phân hệ | Địa chỉ truy cập | Cổng (Port) |
| :--- | :--- | :--- |
| **Trang Admin** | `http://localhost:3004` | 3004 |
| **Nhà Sản Xuất (NSX)** | `http://localhost:3001` | 3001 |
| **Nhà Phân Phối (NPP)** | `http://localhost:3002` | 3002 |
| **Hiệu Thuốc (HT)** | `http://localhost:3003` | 3003 |
| **Spring Boot Backend API Docs** | `http://localhost:8080/swagger-ui.html` | 8080 |
| **Blockchain Local RPC** | `http://localhost:8545` | 8545 |
| **Blockscout Blockchain Explorer** | `http://localhost:3000` | 3000 |

---
*Dự án được đồng bộ và vận hành hoàn chỉnh trên cả môi trường Windows và macOS.*

