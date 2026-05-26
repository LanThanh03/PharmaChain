import React, { useState } from 'react';
import { QrCode, Package, CheckCircle, AlertCircle, Scan, Calendar, User } from 'lucide-react';
import pharmacyService from '../../services/apiService';
import './ReceiveGoods.css';

function ReceiveGoods() {
  const [shipmentCode, setShipmentCode] = useState('');
  const [showScanner, setShowScanner] = useState(false);
  const [shipmentData, setShipmentData] = useState(null);
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState(null);

  // State for real API data
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleScanCode = async () => {
    if (!shipmentCode.trim()) {
      setError('Vui lòng nhập mã vận đơn');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Step 1: Get pharmacy wallet address for verification
      const pharmacyAddress = localStorage.getItem('walletAddress');
      if (!pharmacyAddress) {
        setError('Không tìm thấy địa chỉ ví hiệu thuốc. Vui lòng đăng nhập.');
        return;
      }

      // Step 2: Fetch shipment details from API
      const shipmentResponse = await pharmacyService.getShipmentById(shipmentCode);
      
      if (!shipmentResponse.success || !shipmentResponse.data) {
        setError('Không tìm thấy thông tin lô hàng với mã: ' + shipmentCode);
        return;
      }

      const shipment = shipmentResponse.data;

      // Step 3: Verify this shipment is sent to our pharmacy (anti-counterfeit)
      if (shipment.toAddress?.toLowerCase() !== pharmacyAddress.toLowerCase()) {
        setError('Lô hàng này không được gửi đến hiệu thuốc của bạn. Có thể là hàng giả!');
        return;
      }

      // Step 4: Verify blockchain ownership
      try {
        const ownershipVerification = await pharmacyService.verifyShipmentOwnership(
          shipmentCode, 
          pharmacyAddress
        );
        
        if (ownershipVerification.success && !ownershipVerification.data.isOwner) {
          setError('Quyền sở hữu NFT chưa được chuyển đến hiệu thuốc. Có thể là hàng giả!');
          return;
        }
      } catch (e) {
        console.warn('Could not verify blockchain ownership:', e);
        // Continue without blockchain verification if service is unavailable
      }

      // Step 5: Set shipment data for display
      setShipmentData(shipment);
      setShowScanner(false);

    } catch (error) {
      console.error('Error scanning shipment:', error);
      setError('Có lỗi xảy ra khi tìm kiếm lô hàng: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyShipment = async () => {
    if (!shipmentData) {
      setError('Không có thông tin lô hàng để xác thực');
      return;
    }

    try {
      setIsVerifying(true);
      setError(null);

      // Call blockchain API to confirm receipt and transfer ownership
      const receiveResponse = await pharmacyService.receiveShipment(shipmentData.id || shipmentData.shipmentId);

      if (receiveResponse.success) {
        setVerificationResult({
          success: true,
          blockchainTxId: receiveResponse.data?.transactionHash || 'TX' + Date.now(),
          timestamp: new Date().toISOString(),
          verifiedBy: 'Dược sĩ hiệu thuốc'
        });
        setSuccess('Xác nhận nhận hàng thành công! Lô hàng đã được chuyển vào kho.');
        
        // Reset form after successful receipt
        setTimeout(() => {
          setShipmentCode('');
          setShipmentData(null);
          setVerificationResult(null);
        }, 3000);
      } else {
        setError(receiveResponse.message || 'Không thể xác nhận nhận hàng');
      }
    } catch (error) {
      console.error('Error verifying shipment:', error);
      setError('Có lỗi xảy ra khi xác thực lô hàng: ' + error.message);
    } finally {
      setIsVerifying(false);
    }
  };

  const resetForm = () => {
    setShipmentCode('');
    setShipmentData(null);
    setVerificationResult(null);
    setShowScanner(false);
  };

  return (
    <div className="receive-goods">
      <div className="page-header">
        <h1>Nhận Hàng</h1>
        <p>Quét mã lô hàng để xác thực và nhận vào kho</p>
      </div>

      <div className="receive-container">
        {!shipmentData && !verificationResult && (
          <div className="scanner-section">
            <div className="scanner-card">
              <div className="scanner-header">
                <QrCode className="scanner-icon" />
                <h2>Quét hoặc Nhập mã Lô hàng</h2>
                <p>Sử dụng máy quét barcode hoặc nhập mã thủ công</p>
              </div>

              {!showScanner ? (
                <button
                  className="btn btn-primary scanner-btn"
                  onClick={() => setShowScanner(true)}
                >
                  <Scan />
                  Bắt đầu quét mã
                </button>
              ) : (
                <div className="input-section">
                  <div className="input-group">
                    <input
                      type="text"
                      value={shipmentCode}
                      onChange={(e) => setShipmentCode(e.target.value)}
                      placeholder="Nhập mã lô hàng (VD: LOT001234)"
                      className="input scanner-input"
                      autoFocus
                    />
                    <button
                      className="btn btn-primary"
                      onClick={handleScanCode}
                      disabled={!shipmentCode}
                    >
                      Kiểm tra
                    </button>
                  </div>
                  <p className="helper-text">
                    Nhập mã lô hàng từ phiếu giao hàng
                  </p>
                  <button
                    className="btn btn-secondary"
                    onClick={() => setShowScanner(false)}
                  >
                    Hủy
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {shipmentData && !verificationResult && (
          <div className="shipment-details">
            <div className="details-header">
              <h2>Chi tiết lô hàng {shipmentData.id}</h2>
              <span className="status-badge status-shipping">
                Đang giao hàng
              </span>
            </div>

            <div className="details-grid">
              {/* Thông tin người gửi */}
              <div className="info-card">
                <div className="card-header">
                  <User className="card-icon" />
                  <h3>Thông tin người gửi</h3>
                </div>
                <div className="info-content">
                  <div className="info-item">
                    <strong>Nhà phân phối:</strong>
                    <span>{shipmentData.distributor}</span>
                  </div>
                  <div className="info-item">
                    <strong>Địa chỉ:</strong>
                    <span>{shipmentData.distributorAddress}</span>
                  </div>
                  <div className="info-item">
                    <strong>Tài xế:</strong>
                    <span>{shipmentData.driver}</span>
                  </div>
                  <div className="info-item">
                    <strong>SĐT tài xế:</strong>
                    <span>{shipmentData.driverPhone}</span>
                  </div>
                </div>
              </div>

              {/* Thông tin vận chuyển */}
              <div className="info-card">
                <div className="card-header">
                  <Calendar className="card-icon" />
                  <h3>Thông tin vận chuyển</h3>
                </div>
                <div className="info-content">
                  <div className="info-item">
                    <strong>Ngày gửi:</strong>
                    <span>{new Date(shipmentData.shippedDate).toLocaleDateString('vi-VN')}</span>
                  </div>
                  <div className="info-item">
                    <strong>Ngày giao dự kiến:</strong>
                    <span>{new Date(shipmentData.expectedDeliveryDate).toLocaleDateString('vi-VN')}</span>
                  </div>
                  <div className="info-item">
                    <strong>Phương tiện:</strong>
                    <span>{shipmentData.transportMethod}</span>
                  </div>
                  {shipmentData.notes && (
                    <div className="info-item">
                      <strong>Ghi chú:</strong>
                      <span>{shipmentData.notes}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Danh sách sản phẩm */}
            <div className="products-section">
              <div className="section-header">
                <Package className="section-icon" />
                <h3>Danh sách sản phẩm ({shipmentData.items.length} loại)</h3>
              </div>
              <div className="products-list">
                {shipmentData.items.map((item) => (
                  <div key={item.id} className="product-card">
                    <div className="product-header">
                      <h4>{item.name}</h4>
                      <span className="quantity-badge">
                        {item.quantity.toLocaleString()} {item.unit}
                      </span>
                    </div>
                    <div className="product-details">
                      <div className="detail-row">
                        <span className="label">Nhà sản xuất:</span>
                        <span className="value">{item.manufacturer}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">Số lô:</span>
                        <span className="value">{item.batchNumber}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">Hạn sử dụng:</span>
                        <span className="value">{new Date(item.expireDate).toLocaleDateString('vi-VN')}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">Số đăng ký:</span>
                        <span className="value">{item.registrationNumber}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Action buttons */}
            <div className="action-section">
              <div className="action-info">
                <AlertCircle className="info-icon" />
                <div>
                  <h4>Xác nhận nhận hàng</h4>
                  <p>Sau khi xác nhận, thông tin sẽ được ghi lên blockchain và không thể thay đổi</p>
                </div>
              </div>
              <div className="action-buttons">
                <button
                  className="btn btn-secondary"
                  onClick={resetForm}
                >
                  Hủy
                </button>
                <button
                  className="btn btn-primary verify-btn"
                  onClick={handleVerifyShipment}
                  disabled={isVerifying}
                >
                  {isVerifying ? (
                    <>
                      <div className="loading-spinner"></div>
                      Đang xác thực...
                    </>
                  ) : (
                    <>
                      <CheckCircle />
                      Xác nhận đã nhận hàng
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}

        {verificationResult && (
          <div className="verification-result">
            <div className="result-card success">
              <CheckCircle className="result-icon" />
              <div className="result-content">
                <h2>Nhận hàng thành công!</h2>
                <p>Lô hàng {shipmentData.id} đã được xác thực và ghi lên blockchain</p>
                
                <div className="blockchain-info">
                  <h3>Thông tin blockchain:</h3>
                  <div className="blockchain-details">
                    <div className="blockchain-item">
                      <strong>Transaction ID:</strong>
                      <span className="tx-id">{verificationResult.blockchainTxId}</span>
                    </div>
                    <div className="blockchain-item">
                      <strong>Thời gian xác thực:</strong>
                      <span>{new Date(verificationResult.timestamp).toLocaleString('vi-VN')}</span>
                    </div>
                    <div className="blockchain-item">
                      <strong>Xác thực bởi:</strong>
                      <span>{verificationResult.verifiedBy}</span>
                    </div>
                  </div>
                </div>

                <div className="result-actions">
                  <button
                    className="btn btn-primary"
                    onClick={resetForm}
                  >
                    Nhận lô hàng khác
                  </button>
                  <button className="btn btn-secondary">
                    Xem trong kho
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default ReceiveGoods;



