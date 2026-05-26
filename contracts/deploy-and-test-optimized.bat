@echo off
REM ============================================================
REM Quick Deploy and Test - PharmaLedger Optimized
REM ============================================================

echo.
echo ========================================
echo PharmaLedger Optimized - Deploy and Test
echo ========================================
echo.

REM Check if we're in the contracts directory
if not exist "hardhat.config.js" (
    echo Error: Please run this script from the contracts directory
    echo Current directory: %CD%
    pause
    exit /b 1
)

echo Step 1: Compiling contracts...
echo.
call npx hardhat compile
if errorlevel 1 (
    echo.
    echo Error: Compilation failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Deploying PharmaLedgerOptimized...
echo ========================================
echo.
call npx hardhat run scripts/deploy-optimized.js --network localhost
if errorlevel 1 (
    echo.
    echo Error: Deployment failed!
    echo Make sure Hardhat node is running in another terminal:
    echo   npx hardhat node
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 3: Testing Optimized Workflow...
echo ========================================
echo.
call npx hardhat run scripts/test-optimized-workflow.js --network localhost
if errorlevel 1 (
    echo.
    echo Error: Test failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! All steps completed.
echo ========================================
echo.
echo Contract deployed and tested successfully!
echo.
echo Next steps:
echo 1. Check deployments/PharmaLedgerOptimized.json for contract address
echo 2. Update backend configuration
echo 3. Update frontend configuration
echo.
pause

