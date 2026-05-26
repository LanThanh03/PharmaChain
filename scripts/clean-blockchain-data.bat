@echo off
REM Blockchain Data Cleanup Script
REM This script cleans all blockchain-related data for a fresh start

echo ========================================
echo Blockchain Data Cleanup
echo ========================================
echo.
echo This will:
echo 1. Stop Blockscout containers
echo 2. Clear blockchain data directories
echo 3. Clear contract deployment files
echo.

set /p confirm="Continue? (y/n): "
if /i not "%confirm%"=="y" (
    echo Cleanup cancelled.
    exit /b 0
)

echo.
echo [1/5] Stopping Blockscout containers...
docker-compose down -v
if %errorlevel% neq 0 (
    echo Warning: Failed to stop containers, they may not be running
)

echo.
echo [2/5] Clearing contract artifacts...
if exist contracts\artifacts rmdir /s /q contracts\artifacts
if exist contracts\cache rmdir /s /q contracts\cache
if exist contracts\deployments rmdir /s /q contracts\deployments
echo Artifacts cleared.

echo.
echo [3/5] Clearing contract ABI exports...
if exist contracts\abi rmdir /s /q contracts\abi
echo ABI exports cleared.

echo.
echo [4/5] Creating fresh directories...
mkdir contracts\deployments
mkdir contracts\abi
echo Directories created.

echo.
echo [5/5] Backend database cleanup...
echo Please run these SQL commands in your MySQL database:
echo.
echo    TRUNCATE TABLE blockchain_transactions;
echo    TRUNCATE TABLE blockchain_events;
echo.

echo ========================================
echo Cleanup completed!
echo ========================================
echo.
echo Next steps:
echo 1. Run SQL commands shown above to clear backend tables
echo 2. Start Hardhat node: cd contracts ^&^& npx hardhat node
echo 3. Deploy contracts: npx hardhat run scripts/deploy-all-contracts.js --network localhost
echo 4. Start Blockscout: docker-compose up -d
echo 5. Start backend: cd backend ^&^& mvn spring-boot:run
echo.
pause
