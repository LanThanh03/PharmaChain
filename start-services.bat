@echo off
echo ========================================
echo    PharmaLedger - Drug Traceability System
echo ========================================
echo.

echo Starting services in separate windows...
echo.

echo 1. Starting Blockchain (Hardhat)...
start "Blockchain" cmd /k "cd contracts && npx hardhat node"
timeout /t 5

echo 2. Starting Web Nha San Xuat (Port 3001)...
start "Web-NhaSanXuat" cmd /k "cd web_NhaSanXuat && npm start"
timeout /t 2

echo 3. Starting Web Nha Phan Phoi (Port 3002)...
start "Web-NhaPhanPhoi" cmd /k "cd web_NhaPhanPhoi && npm start"
timeout /t 2

echo 4. Starting Web Hieu Thuoc (Port 3003)...
start "Web-HieuThuoc" cmd /k "cd web_HieuThuoc && npm start"
timeout /t 2

echo.
echo ========================================
echo    All Services Started Successfully!
echo ========================================
echo.
echo Service URLs:
echo • Blockchain: http://localhost:8545
echo • Backend API: http://localhost:8080
echo • Web Nha San Xuat: http://localhost:3001
echo • Web Nha Phan Phoi: http://localhost:3002
echo • Web Hieu Thuoc: http://localhost:3003
echo.
echo BlockScout Explorer: http://localhost:3000
echo.
echo Press any key to exit...
pause
