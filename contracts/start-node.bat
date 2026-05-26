@echo off
echo ========================================
echo  Starting Hardhat Node (Public Access)
echo ========================================
echo.
echo This allows Blockscout (Docker) to connect
echo Listening on: 0.0.0.0:8545
echo.
echo Press Ctrl+C to stop
echo.

cd /d "%~dp0"
npx hardhat node --hostname 0.0.0.0 --port 8545

pause

