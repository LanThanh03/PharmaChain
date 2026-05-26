@echo off
REM Blockchain Setup Validation Script
REM Checks all components before starting development

echo ========================================
echo Blockchain Setup Validation
echo ========================================
echo.

set ERROR_COUNT=0

REM ========================================
REM Check 1: Hardhat Node
REM ========================================
echo [1/6] Checking Hardhat Node...
curl -s -X POST -H "Content-Type: application/json" --data "{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}" http://localhost:8545 >nul 2>&1
if %errorlevel% equ 0 (
    echo    [32m√[0m Hardhat node is running
) else (
    echo    [31mX[0m Hardhat node is NOT running
    echo       Start with: cd contracts ^&^& npx hardhat node
    set /a ERROR_COUNT+=1
)

REM ========================================
REM Check 2: Blockscout
REM ========================================
echo [2/6] Checking Blockscout...
curl -s http://localhost:3000/api/v2/stats >nul 2>&1
if %errorlevel% equ 0 (
    echo    [32m√[0m Blockscout is accessible
) else (
    echo    [31mX[0m Blockscout is NOT accessible
    echo       Start with: docker-compose up -d
    set /a ERROR_COUNT+=1
)

REM ========================================
REM Check 3: Contract Deployment
REM ========================================
echo [3/6] Checking Contract Deployment...
if exist contracts\deployments\deployment-localhost.json (
    echo    [32m√[0m Contracts are deployed
) else (
    echo    [31mX[0m Contracts are NOT deployed
    echo       Deploy with: cd contracts ^&^& npx hardhat run scripts/deploy-all-contracts.js --network localhost
    set /a ERROR_COUNT+=1
)

REM ========================================
REM Check 4: Backend Configuration
REM ========================================
echo [4/6] Checking Backend Configuration...
if exist backend\src\main\resources\application.properties (
    findstr /C:"pharmaledger.contract.address=0x" backend\src\main\resources\application.properties >nul 2>&1
    if %errorlevel% equ 0 (
        echo    [32m√[0m Backend configuration exists
    ) else (
        echo    [31mX[0m Backend configuration incomplete
        echo       Run deployment script to auto-update
        set /a ERROR_COUNT+=1
    )
) else (
    echo    [31mX[0m Backend configuration file not found
    set /a ERROR_COUNT+=1
)

REM ========================================
REM Check 5: MySQL Database
REM ========================================
echo [5/6] Checking MySQL Database...
mysql -u root -e "USE BlockChain_DA;" 2>nul
if %errorlevel% equ 0 (
    echo    [32m√[0m MySQL database is accessible
) else (
    echo    [31mX[0m MySQL database is NOT accessible
    echo       Start MySQL and create database BlockChain_DA
    set /a ERROR_COUNT+=1
)

REM ========================================
REM Check 6: Node Modules
REM ========================================
echo [6/6] Checking Node Modules...
if exist contracts\node_modules (
    echo    [32m√[0m Contract dependencies installed
) else (
    echo    [31mX[0m Contract dependencies NOT installed
    echo       Install with: cd contracts ^&^& npm install
    set /a ERROR_COUNT+=1
)

echo.
echo ========================================
echo Validation Summary
echo ========================================

if %ERROR_COUNT% equ 0 (
    echo [32m√ All checks passed![0m
    echo.
    echo Your blockchain setup is ready!
    echo.
    echo Next steps:
    echo 1. Verify contracts: cd contracts ^&^& npx hardhat run scripts/verify-contracts-blockscout.js --network localhost
    echo 2. Test integration: npx hardhat run scripts/test-blockscout-integration.js --network localhost
    echo 3. Start backend: cd backend ^&^& mvn spring-boot:run
) else (
    echo [31mX Found %ERROR_COUNT% error(s)[0m
    echo.
    echo Please fix the errors above before proceeding.
    echo.
    echo Quick Setup Guide:
    echo 1. Start Hardhat: cd contracts ^&^& npx hardhat node
    echo 2. Deploy contracts: npx hardhat run scripts/deploy-all-contracts.js --network localhost
    echo 3. Start Blockscout: docker-compose up -d
    echo 4. Wait 30 seconds for Blockscout to initialize
    echo 5. Run this script again to verify
)

echo ========================================
echo.
pause
exit /b %ERROR_COUNT%

