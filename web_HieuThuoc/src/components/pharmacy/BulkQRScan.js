import React, { useState, useRef, useEffect } from 'react';
import { 
  Camera, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Package,
  Scan,
  Upload,
  Loader
} from 'lucide-react';
import { Html5Qrcode } from 'html5-qrcode';
import pharmacyService from '../../services/apiService';
import './BulkQRScan.css';

/**
 * Component quét QR hàng loạt cho NPP/Hiệu thuốc
 * Dùng khi nhận hàng hoặc xuất hàng
 */
function BulkQRScan({ shipmentId, mode = 'receive', onComplete }) {
  const [isScanning, setIsScanning] = useState(false);
  const [scannedItems, setScannedItems] = useState([]);
  const [currentScan, setCurrentScan] = useState(null);
  const [stats, setStats] = useState({
    total: 0,
    valid: 0,
    invalid: 0,
    duplicate: 0
  });
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);
  
  const scannerRef = useRef(null);
  const html5QrCodeRef = useRef(null);

  useEffect(() => {
    return () => {
      stopScanning();
    };
  }, []);

  const startScanning = async () => {
    try {
      setError(null);
      
      const config = {
        fps: 10,
        qrbox: { width: 250, height: 250 },
        aspectRatio: 1.0
      };

      html5QrCodeRef.current = new Html5Qrcode('qr-reader');
      
      await html5QrCodeRef.current.start(
        { facingMode: 'environment' },
        config,
        onScanSuccess,
        onScanError
      );
      
      setIsScanning(true);
    } catch (err) {
      console.error('Error starting scanner:', err);
      setError('Không thể khởi động camera. Vui lòng kiểm tra quyền truy cập.');
    }
  };

  const stopScanning = async () => {
    if (html5QrCodeRef.current && isScanning) {
      try {
        await html5QrCodeRef.current.stop();
        html5QrCodeRef.current.clear();
      } catch (err) {
        console.error('Error stopping scanner:', err);
      }
    }
    setIsScanning(false);
  };

  const onScanSuccess = (decodedText) => {
    setCurrentScan(decodedText);
    
    // Extract item code from QR
    const itemCode = extractItemCode(decodedText);
    
    if (!itemCode) {
      playErrorSound();
      return;
    }

    // Check if already scanned
    const exists = scannedItems.find(item => item.code === itemCode);
    if (exists) {
      playErrorSound();
      setStats(prev => ({ ...prev, duplicate: prev.duplicate + 1 }));
      return;
    }

    // Add to scanned list (temporary valid)
    const newItem = {
      id: Date.now(),
      code: itemCode,
      status: 'pending', // Will verify later
      timestamp: new Date().toISOString(),
      scanOrder: scannedItems.length + 1
    };

    setScannedItems(prev => [...prev, newItem]);
    setStats(prev => ({
      ...prev,
      total: prev.total + 1,
      valid: prev.valid + 1
    }));

    playSuccessSound();
  };

  const onScanError = (err) => {
    // Ignore scan errors (continuous scanning)
  };

  const extractItemCode = (qrText) => {
    // Extract from URL format: https://app.com/verify/PARA-BATCH001-0123
    if (qrText.includes('/verify/')) {
      const parts = qrText.split('/verify/');
      if (parts.length > 1) {
        return parts[1].split('?')[0];
      }
    }
    
    // Direct item code
    if (qrText.includes('BATCH')) {
      return qrText;
    }
    
    return null;
  };

  const playSuccessSound = () => {
    const audio = new Audio('/sounds/beep-success.mp3');
    audio.play().catch(() => {});
  };

  const playErrorSound = () => {
    const audio = new Audio('/sounds/beep-error.mp3');
    audio.play().catch(() => {});
  };

  const handleSubmit = async () => {
    if (scannedItems.length === 0) {
      setError('Chưa quét sản phẩm nào');
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const itemCodes = scannedItems.map(item => item.code);
      
      const response = await pharmacyService.bulkScanItems(shipmentId, itemCodes);

      if (response.success) {
        const result = response.data;
        
        // Update item statuses based on result
        const updatedItems = scannedItems.map(item => {
          if (result.invalidCodes?.includes(item.code)) {
            return { ...item, status: 'invalid' };
          } else if (result.alreadyReceivedCodes?.includes(item.code)) {
            return { ...item, status: 'duplicate' };
          } else {
            return { ...item, status: 'valid' };
          }
        });

        setScannedItems(updatedItems);
        setStats({
          total: result.totalScanned,
          valid: result.validCount,
          invalid: result.invalidCount,
          duplicate: result.alreadyReceivedCount
        });

        if (result.validCount === result.totalScanned) {
          alert(`✅ Đã xác nhận nhận ${result.validCount} sản phẩm thành công!`);
          if (onComplete) onComplete(result);
        } else {
          alert(`⚠️ Đã xử lý ${result.validCount}/${result.totalScanned} sản phẩm.\n` +
                `Không hợp lệ: ${result.invalidCount}, Đã nhận trước: ${result.alreadyReceivedCount}`);
        }
      } else {
        setError(response.message || 'Lỗi khi xử lý');
      }
    } catch (err) {
      console.error('Error submitting scans:', err);
      setError('Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRemoveItem = (itemId) => {
    const item = scannedItems.find(i => i.id === itemId);
    if (!item) return;

    setScannedItems(prev => prev.filter(i => i.id !== itemId));
    setStats(prev => ({
      ...prev,
      total: prev.total - 1,
      valid: item.status === 'valid' ? prev.valid - 1 : prev.valid,
      invalid: item.status === 'invalid' ? prev.invalid - 1 : prev.invalid,
      duplicate: item.status === 'duplicate' ? prev.duplicate - 1 : prev.duplicate
    }));
  };

  const handleClearAll = () => {
    if (window.confirm('Xóa tất cả sản phẩm đã quét?')) {
      setScannedItems([]);
      setStats({ total: 0, valid: 0, invalid: 0, duplicate: 0 });
    }
  };

  return (
    <div className="bulk-qr-scan">
      <div className="scan-header">
        <h2>Quét QR hàng loạt</h2>
        <p className="scan-subtitle">
          {mode === 'receive' ? 'Quét để nhận hàng từ NPP' : 'Quét để xuất hàng'}
        </p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertTriangle size={20} />
          <span>{error}</span>
        </div>
      )}

      <div className="scan-container">
        {/* Camera View */}
        <div className="camera-section">
          <div className="camera-wrapper">
            {!isScanning ? (
              <div className="camera-placeholder">
                <Camera size={64} className="camera-icon" />
                <p>Bấm nút bên dưới để bắt đầu quét</p>
              </div>
            ) : (
              <div id="qr-reader" style={{ width: '100%' }}></div>
            )}
          </div>

          <div className="camera-controls">
            {!isScanning ? (
              <button 
                className="btn btn-primary btn-large"
                onClick={startScanning}
              >
                <Camera size={20} />
                Bắt đầu quét QR
              </button>
            ) : (
              <button 
                className="btn btn-secondary btn-large"
                onClick={stopScanning}
              >
                Dừng quét
              </button>
            )}
          </div>

          {currentScan && (
            <div className="current-scan">
              <Scan size={16} />
              <span>Đang quét: {currentScan.substring(0, 30)}...</span>
            </div>
          )}
        </div>

        {/* Stats & List */}
        <div className="results-section">
          {/* Stats Cards */}
          <div className="stats-cards">
            <div className="stat-card stat-total">
              <Package size={24} />
              <div>
                <div className="stat-value">{stats.total}</div>
                <div className="stat-label">Tổng đã quét</div>
              </div>
            </div>
            <div className="stat-card stat-valid">
              <CheckCircle size={24} />
              <div>
                <div className="stat-value">{stats.valid}</div>
                <div className="stat-label">Hợp lệ</div>
              </div>
            </div>
            <div className="stat-card stat-invalid">
              <XCircle size={24} />
              <div>
                <div className="stat-value">{stats.invalid}</div>
                <div className="stat-label">Không hợp lệ</div>
              </div>
            </div>
            <div className="stat-card stat-duplicate">
              <AlertTriangle size={24} />
              <div>
                <div className="stat-value">{stats.duplicate}</div>
                <div className="stat-label">Trùng lặp</div>
              </div>
            </div>
          </div>

          {/* Scanned Items List */}
          <div className="scanned-items">
            <div className="items-header">
              <h3>Danh sách đã quét ({scannedItems.length})</h3>
              {scannedItems.length > 0 && (
                <button 
                  className="btn btn-text btn-danger"
                  onClick={handleClearAll}
                >
                  Xóa tất cả
                </button>
              )}
            </div>

            {scannedItems.length === 0 ? (
              <div className="empty-state">
                <Scan size={48} className="empty-icon" />
                <p>Chưa có sản phẩm nào được quét</p>
              </div>
            ) : (
              <div className="items-list">
                {scannedItems.map(item => (
                  <div 
                    key={item.id} 
                    className={`item-row item-${item.status}`}
                  >
                    <div className="item-order">#{item.scanOrder}</div>
                    <div className="item-code">{item.code}</div>
                    <div className="item-status">
                      {item.status === 'valid' && <CheckCircle size={20} className="status-icon" />}
                      {item.status === 'invalid' && <XCircle size={20} className="status-icon" />}
                      {item.status === 'duplicate' && <AlertTriangle size={20} className="status-icon" />}
                      {item.status === 'pending' && <Loader size={20} className="status-icon spin" />}
                      <span className="status-text">
                        {item.status === 'valid' && 'Hợp lệ'}
                        {item.status === 'invalid' && 'Không hợp lệ'}
                        {item.status === 'duplicate' && 'Đã nhận'}
                        {item.status === 'pending' && 'Đang chờ'}
                      </span>
                    </div>
                    <button 
                      className="btn-remove"
                      onClick={() => handleRemoveItem(item.id)}
                      title="Xóa"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Action Buttons */}
          <div className="action-buttons">
            <button 
              className="btn btn-secondary"
              onClick={() => window.history.back()}
              disabled={isProcessing}
            >
              Hủy
            </button>
            <button 
              className="btn btn-primary"
              onClick={handleSubmit}
              disabled={scannedItems.length === 0 || isProcessing}
            >
              {isProcessing ? (
                <>
                  <Loader size={20} className="spin" />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <Upload size={20} />
                  Xác nhận nhận {scannedItems.length} sản phẩm
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BulkQRScan;

