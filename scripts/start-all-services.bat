@echo off
REM Automated Blockchain Setup Script
REM This script starts all services automatically

echo ========================================
echo Blockchain Integration - Auto Start
echo ========================================
echo.

REM Check if this is first time setup
set /p first_time="Is this first time setup? (y/n): "

if /i "%first_time%"=="y" (
    echo.
    echo [1/7] Cleaning previous data...
    call scripts\clean-blockchain-data.bat
    
    echo.
    echo [2/7] Please run MySQL command manually:
    echo mysql -u root -p BlockChain_DA ^< scripts\clear-database-tables.sql
    echo.
    pause
)

echo.
echo [3/7] Starting Hardhat Node...
start "Hardhat Node" cmd /k "cd contracts && npx hardhat node"

echo Waiting 5 seconds for Hardhat to start...
timeout /t 5 /nobreak >nul

echo.
echo [4/7] Deploying contracts...
echo IMPORTANT: Wait for deployment to complete before continuing!
cd contracts
call npx hardhat run scripts/deploy-all-contracts.js --network localhost
cd ..

echo.
echo Deployment completed. Press any key to start Blockscout...
pause

echo.
echo [5/7] Starting Blockscout...
start "Blockscout" cmd /k "docker-compose up && pause"

echo Waiting 30 seconds for Blockscout to initialize...
timeout /t 30 /nobreak >nul

echo.
echo [6/7] Validating setup...
call scripts\validate-blockchain-setup.bat

echo.
echo [7/7] Starting Backend...
start "Backend" cmd /k "cd backend && mvn spring-boot:run"

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Terminals opened:
echo 1. Hardhat Node (port 8545)
echo 2. Deploy Contracts (completed)
echo 3. Blockscout (port 3000)
echo 4. Backend (port 8080)
echo.
echo URLs:
echo - Blockscout: http://localhost:3000
echo - Backend API: http://localhost:8080
echo.
echo To test integration:
echo cd contracts
echo npx hardhat run scripts/test-blockscout-integration.js --network localhost
echo.
pause

