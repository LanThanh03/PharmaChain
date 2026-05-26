import React, { useState, useEffect } from 'react';
import {
  ShoppingCart,
  Scan,
  CheckCircle,
  AlertCircle,
  Package,
  Info,
  QrCode,
  Truck
} from 'lucide-react';
import pharmacyService from '../services/apiService';
import { useAuth } from '../contexts/AuthContext';
import './ReceiveGoods.css';

const ReceiveGoods = () => {
  const { user } = useAuth();
  const [scanInput, setScanInput] = useState('');
  const [shipmentDetails, setShipmentDetails] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [pendingShipments, setPendingShipments] = useState([]);

  useEffect(() => {
    fetchPendingShipments();
  }, []);

  // Helper function to normalize shipment data from API
  const normalizeShipmentData = (shipment) => {
    if (!shipment) return null;

    // Extract data from drugBatch if available
    const batch = shipment.drugBatch || {};
    const fromCompany = shipment.fromCompany || {};
    const toCompany = shipment.toCompany || {};

    // Build products array from batch data
    let products = [];
    if (batch.drugName) {
      products = [{
        name: batch.drugName || 'Sản phẩm',
        batchCode: batch.batchNumber || batch.batchCode || shipment.shipmentCode, // ⭐ Use human-readable batch number (BT...)
        batchNumber: batch.batchNumber, // Batch number (BT202511102252)
        blockchainBatchId: batch.batchId, // Blockchain batch ID for internal tracking
        quantity: shipment.quantity || batch.quantity || 0,
        expiry: batch.expiryDate || batch.expireDate,
        manufacturer: batch.manufacturer || fromCompany.companyName || 'N/A',
        manufactureDate: batch.manufactureTimestamp || batch.manufactureDate
      }];
    }

    // If no batch data, try to construct from shipment items
    if (products.length === 0 && shipment.shipmentItems && Array.isArray(shipment.shipmentItems)) {
      products = shipment.shipmentItems.map(item => ({
        name: item.drugName || item.name || 'Sản phẩm',
        batchCode: item.batchNumber || item.batchCode, // ⭐ Use human-readable batch number
        batchNumber: item.batchNumber,
        blockchainBatchId: item.blockchainBatchId,
        quantity: item.quantity || 0,
        expiry: item.expiryDate || item.expireDate,
        manufacturer: item.manufacturer || 'N/A'
      }));
    }

    // Fallback: create at least one product entry
    if (products.length === 0) {
      products = [{
        name: 'Sản phẩm',
        batchCode: 'N/A',
        batchNumber: shipment.shipmentCode,
        blockchainBatchId: null,
        quantity: shipment.quantity || 0,
        expiry: null,
        manufacturer: fromCompany.companyName || 'N/A'
      }];
    }

    // Determine the correct database ID
    // Priority: shipment.id (database primary key)
    let databaseId = shipment.id;

    // If no ID found, try to extract from shipmentCode (format: SHIP-{id})
    if (!databaseId && shipment.shipmentCode) {
      const match = shipment.shipmentCode.match(/SHIP-(\d+)/);
      if (match) {
        databaseId = parseInt(match[1]);
      }
    }

    return {
      id: databaseId, // Database primary key - CRITICAL for receive operation
      shipmentId: shipment.shipmentId, // Blockchain ID (can be null)
      shipmentCode: shipment.shipmentCode,
      trackingCode: shipment.trackingInfo || shipment.shipmentCode || `SHIP-${databaseId || 'N/A'}`,
      from: fromCompany.companyName || fromCompany.pharmacyName || 'Nhà phân phối',
      fromType: fromCompany.companyType === 'MANUFACTURER' ? 'manufacturer' : 'distributor',
      fromAddress: shipment.fromAddress || fromCompany.walletAddress,
      toAddress: shipment.toAddress || toCompany.walletAddress,
      expectedDate: shipment.expectedDeliveryDate || shipment.shipmentDate || shipment.createdAt,
      shipmentDate: shipment.shipmentDate || shipment.shipmentTimestamp,
      totalValue: (shipment.quantity || 0) * 15000, // Estimated value
      quantity: shipment.quantity || 0,
      status: shipment.status,
      driverName: shipment.driverName,
      vehicleNumber: shipment.vehicleNumber,
      notes: shipment.notes,
      products: products,
      transactionHash: shipment.transactionHash || shipment.createTxHash,
      blockNumber: shipment.blockNumber
    };
  };

  const fetchPendingShipments = async () => {
    try {
      // Get shipments targeted to this pharmacy from API
      const response = await pharmacyService.getPendingShipments();
      if (response.success && Array.isArray(response.data)) {
        // Normalize all pending shipments
        const normalized = response.data.map(shipment => normalizeShipmentData(shipment));
        setPendingShipments(normalized);
      } else {
        setPendingShipments([]);
      }
    } catch (err) {
      console.error('Error fetching pending shipments:', err);
      setPendingShipments([]);
    }
  };

  const handleScan = async () => {
    if (!scanInput.trim()) {
      setError('Vui lòng nhập Mã vận đơn (VD: SHIP-17656257365533795) để nhận hàng');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      let searchTerm = scanInput.trim();
      console.log('🔍 Searching with input:', searchTerm);

      // ⭐ STRATEGY 1: Find shipment by Shipment ID (SHIP-xxx) - PRIMARY for receiving goods
      // Extract shipment ID from format SHIP-xxx
      let shipmentId = null;
      if (searchTerm.toUpperCase().startsWith('SHIP-')) {
        shipmentId = searchTerm.substring(5); // Remove "SHIP-" prefix
        console.log('🚚 Looking up shipment by ID:', shipmentId);
      } else if (/^\d+$/.test(searchTerm)) {
        // Pure numeric - could be shipment ID or batch ID
        shipmentId = searchTerm;
        console.log('🔢 Numeric input, trying as shipment ID first:', shipmentId);
      }

      // Try to find by Shipment ID first
      if (shipmentId) {
        try {
          console.log('📦 Step 1: Trying to get shipment by ID:', shipmentId);
          const shipmentResponse = await pharmacyService.getShipmentById(shipmentId);
          console.log('📦 Shipment response:', shipmentResponse);

          if (shipmentResponse.success && shipmentResponse.data) {
            const shipment = shipmentResponse.data;
            const pharmacyAddress = localStorage.getItem('walletAddress');
            const toAddr = shipment.toAddress || shipment.toCompany?.walletAddress;
            const isForMe = toAddr?.toLowerCase() === pharmacyAddress?.toLowerCase();
            const isPending = shipment.status === 'PENDING' || shipment.status === 'IN_TRANSIT';

            console.log(`✅ Found shipment: toAddr=${toAddr}, isForMe=${isForMe}, status=${shipment.status}`);

            if (isForMe && isPending) {
              const normalized = normalizeShipmentData(shipment);
              console.log('✅ Normalized shipment:', normalized);
              setShipmentDetails(normalized);
              return;
            } else if (!isForMe) {
              setError(`Lô hàng này không được gửi đến hiệu thuốc của bạn.`);
              return;
            } else if (!isPending) {
              setError(`Lô hàng này đã được nhận (trạng thái: ${shipment.status}).`);
              return;
            }
          }
        } catch (shipmentError) {
          console.warn('❌ Shipment ID lookup failed:', shipmentError.message);
          // Continue to try batch lookup
        }
      }

      // ⭐ STRATEGY 2: Find by Batch Number/ID (for tracing - fallback)
      // This is only used if shipment ID lookup fails
      try {
        console.log('🔍 Step 2: Trying batch lookup (for tracing):', searchTerm);
        const shipmentsResponse = await pharmacyService.getShipmentsByBatch(searchTerm);
        console.log('📦 Shipments by batch response:', shipmentsResponse);

        if (shipmentsResponse.success && shipmentsResponse.data?.length > 0) {
          const pharmacyAddress = localStorage.getItem('walletAddress');
          console.log('🏥 My pharmacy address:', pharmacyAddress);

          // Find shipment sent to this pharmacy that's pending receipt
          const myShipment = shipmentsResponse.data.find(s => {
            const toAddr = s.toAddress || s.toCompany?.walletAddress;
            const isForMe = toAddr?.toLowerCase() === pharmacyAddress?.toLowerCase();
            const isPending = s.status === 'PENDING' || s.status === 'IN_TRANSIT';
            return isForMe && isPending;
          });

          if (myShipment) {
            console.log('✅ Found my shipment via batch lookup:', myShipment);
            const normalized = normalizeShipmentData(myShipment);
            setShipmentDetails(normalized);
            return;
          } else {
            setError(`Tìm thấy lô thuốc với Số lô: ${searchTerm}, nhưng không có shipment nào đang chờ nhận cho hiệu thuốc của bạn.`);
            return;
          }
        }
      } catch (batchError) {
        console.warn('❌ Batch lookup also failed:', batchError.message);
      }

      // If all lookups fail
      setError(`Không tìm thấy lô hàng với mã: ${searchTerm}. Vui lòng nhập đúng Mã vận đơn (VD: SHIP-17656257365533795).`);

    } catch (err) {
      console.error('Error in handleScan:', err);
      setError('Lỗi khi tìm kiếm lô hàng: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmReceive = async () => {
    if (!shipmentDetails) return;

    // Check authentication
    if (!user || !user.walletAddress) {
      setError('Bạn cần đăng nhập để xác nhận nhận hàng');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSuccess(null);

      // Log the shipment details for debugging
      console.log('Confirming receipt for shipment:', {
        id: shipmentDetails.id,
        shipmentId: shipmentDetails.shipmentId,
        shipmentCode: shipmentDetails.shipmentCode,
        trackingCode: shipmentDetails.trackingCode,
        user: user.name,
        walletAddress: user.walletAddress
      });

      // Backend tries multiple strategies to find shipment:
      // 1. By shipmentId (blockchain ID)
      // 2. By database ID
      // 3. By shipmentCode
      // We should use the database ID first as it's most reliable
      let shipmentIdToUse = shipmentDetails.id; // Use database ID first

      // If database ID doesn't exist, try shipmentId (blockchain)
      if (!shipmentIdToUse && shipmentDetails.shipmentId) {
        shipmentIdToUse = shipmentDetails.shipmentId;
      }

      // If still no ID, try parsing from shipmentCode (SHIP-{id})
      if (!shipmentIdToUse && shipmentDetails.shipmentCode) {
        const codeMatch = shipmentDetails.shipmentCode.match(/SHIP-(\d+)/);
        if (codeMatch) {
          shipmentIdToUse = parseInt(codeMatch[1]);
        }
      }

      if (!shipmentIdToUse) {
        setError('Không tìm thấy ID lô hàng hợp lệ để xác nhận nhận hàng');
        return;
      }

      console.log('Using shipment ID for receive:', shipmentIdToUse);
      const response = await pharmacyService.receiveShipment(shipmentIdToUse);
      console.log('📦 receiveShipment API response:', response);

      if (response.success) {
        // ✅ FIX: Use receiveTransactionHash (new unique TX from receiveShipment),
        // NOT transactionHash which is the OLD createShipment hash.
        const data = response.data || {};
        const receiveTxHash = data.receiveTransactionHash || data.receiveTxHash || data.blockchainTxHash;
        const createTxHash = data.transactionHash || data.createTxHash;
        const txHash = receiveTxHash || createTxHash || 'N/A';
        const confirmedAt = data.confirmedAt || new Date().toISOString();

        console.log('🔗 createTxHash:', createTxHash);
        console.log('🔗 receiveTxHash:', receiveTxHash, '← should be DIFFERENT');

        setSuccess(
          `✅ Đã xác nhận nhận hàng thành công!\n\n` +
          `📦 Lô hàng: ${shipmentDetails.trackingCode || shipmentDetails.id}\n` +
          `🏥 Hiệu thuốc: ${user.name}\n` +
          `👤 Xác nhận bởi: ${user.email}\n` +
          `⛓️ Blockchain TX (nhận hàng): ${txHash}\n` +
          `📅 Thời gian: ${new Date(confirmedAt).toLocaleString('vi-VN')}\n\n` +
          `Quyền sở hữu đã được chuyển sang hiệu thuốc trên blockchain. Hàng đã vào kho!`
        );

        // Reset form
        setTimeout(() => {
          setScanInput('');
          setShipmentDetails(null);

          // Refresh pending shipments
          fetchPendingShipments();
        }, 5000); // Give user time to read the success message
      } else {
        setError(response.message || 'Không thể xác nhận nhận hàng. Vui lòng thử lại.');
      }
    } catch (err) {
      console.error('Error confirming receipt:', err);
      setError('Lỗi xác nhận nhận hàng: ' + (err.message || 'Không rõ nguyên nhân. Vui lòng kiểm tra kết nối và thử lại.'));
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      // Check if date is valid
      if (isNaN(date.getTime())) return 'N/A';
      return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    } catch (error) {
      console.error('Error formatting date:', error);
      return 'N/A';
    }
  };

  return (
    <div className="receive-goods">
      <div className="page-header">
        <h1>
          <ShoppingCart className="page-icon" />
          Nhận hàng
        </h1>
        <p>Quét và xác nhận đã nhận lô hàng, cập nhật quyền giám sát trên blockchain</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={20} />
          {success}
        </div>
      )}

      {/* Scanner Section */}
      <div className="scanner-section">
        <div className="scanner-card">
          <div className="scanner-header">
            <QrCode size={32} />
            <h3>Quét Mã vận đơn để nhận hàng</h3>
            <p>Nhập <strong>Mã vận đơn</strong> (VD: SHIP-17656257365533795) để xác nhận nhận hàng.<br />
              <span style={{ fontSize: '0.9em', color: '#666' }}>💡 Mã vận đơn được NPP/NSX cung cấp khi gửi hàng. Số lô (BT...) dùng để truy vết thuốc.</span></p>
          </div>

          <div className="scanner-input">
            <div className="input-group">
              <Scan className="input-icon" />
              <input
                type="text"
                value={scanInput}
                onChange={(e) => setScanInput(e.target.value)}
                placeholder="Nhập Mã vận đơn (VD: SHIP-17656257365533795)"
                className="scan-input"
                onKeyPress={(e) => e.key === 'Enter' && handleScan()}
              />
              <button
                onClick={handleScan}
                disabled={loading}
                className="btn btn-primary scan-btn"
              >
                {loading ? 'Đang tìm...' : 'Nhận hàng'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Shipment Details */}
      {shipmentDetails && (
        <div className="shipment-details">
          <div className="details-card">
            <div className="details-header">
              <h3>
                <Info size={24} />
                Chi tiết lô hàng
              </h3>
              <div className="shipment-id">Shipment #{shipmentDetails.id}</div>
            </div>

            <div className="details-content">
              {/* ⭐ MÃ VẬN ĐƠN - DÙNG ĐỂ NHẬN HÀNG */}
              <div className="info-section shipment-id-section" style={{ backgroundColor: '#cce5ff', padding: '16px', borderRadius: '8px', border: '2px solid #004085', marginBottom: '16px' }}>
                <h4 style={{ color: '#004085', marginBottom: '12px' }}>🚚 Mã vận đơn (để nhận hàng)</h4>
                <div className="shipment-id-display">
                  <div className="shipment-id-value" style={{ fontSize: '1.5em', fontWeight: 'bold', color: '#004085', marginBottom: '8px' }}>
                    SHIP-{shipmentDetails.id}
                  </div>
                  <p className="shipment-id-note" style={{ marginTop: '10px', fontSize: '0.9em', color: '#004085' }}>
                    💡 <strong>Mã vận đơn</strong> dùng để xác nhận nhận hàng. Mã này thay đổi mỗi lần gửi hàng.
                  </p>
                </div>
              </div>

              {/* 📦 SỐ LÔ - DÙNG ĐỂ TRUY VẾT */}
              <div className="info-section batch-number-section" style={{ backgroundColor: '#d4edda', padding: '16px', borderRadius: '8px', border: '2px solid #28a745', marginBottom: '16px' }}>
                <h4 style={{ color: '#155724', marginBottom: '12px' }}>📦 Số lô (để truy vết)</h4>
                <div className="batch-number-display">
                  <div className="batch-number-value" style={{ fontSize: '1.3em', fontWeight: 'bold', color: '#155724', marginBottom: '8px' }}>
                    {shipmentDetails.products?.[0]?.batchNumber || 'N/A'}
                  </div>
                  <div className="blockchain-id-value" style={{ fontSize: '0.85em', color: '#666' }}>
                    Blockchain ID: {shipmentDetails.products?.[0]?.blockchainBatchId || 'N/A'}
                  </div>
                  <p className="batch-number-note" style={{ marginTop: '10px', fontSize: '0.9em', color: '#155724' }}>
                    ⭐ <strong>Số lô này giữ nguyên</strong> từ NSX → NPP → Hiệu thuốc. Dùng để truy vết nguồn gốc thuốc.
                  </p>
                </div>
              </div>

              <div className="info-section">
                <h4>Thông tin vận chuyển</h4>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="label">Từ:</span>
                    <span className="value">
                      {shipmentDetails.from || 'N/A'}
                      {shipmentDetails.fromType && (
                        <span className={`source-type ${shipmentDetails.fromType}`}>
                          ({shipmentDetails.fromType === 'manufacturer' ? 'NSX' : 'NPP'})
                        </span>
                      )}
                    </span>
                  </div>
                  <div className="info-item">
                    <span className="label">Mã vận đơn (nội bộ):</span>
                    <span className="value">{shipmentDetails.trackingCode || shipmentDetails.shipmentCode || 'N/A'}</span>
                  </div>
                  <div className="info-item">
                    <span className="label">Ngày gửi hàng:</span>
                    <span className="value">{formatDate(shipmentDetails.shipmentDate)}</span>
                  </div>
                  <div className="info-item">
                    <span className="label">Ngày dự kiến:</span>
                    <span className="value">{formatDate(shipmentDetails.expectedDate)}</span>
                  </div>
                  <div className="info-item">
                    <span className="label">Số lượng:</span>
                    <span className="value">{shipmentDetails.quantity || 0} sản phẩm</span>
                  </div>
                  <div className="info-item">
                    <span className="label">Tổng giá trị:</span>
                    <span className="value highlight">{formatCurrency(shipmentDetails.totalValue || 0)}</span>
                  </div>
                  <div className="info-item">
                    <span className="label">Trạng thái:</span>
                    <span className="value">
                      <span className={`status-badge status-${(shipmentDetails.status || '').toLowerCase()}`}>
                        {shipmentDetails.status || 'PENDING'}
                      </span>
                    </span>
                  </div>
                  {shipmentDetails.driverName && (
                    <div className="info-item">
                      <span className="label">Tài xế:</span>
                      <span className="value">{shipmentDetails.driverName}</span>
                    </div>
                  )}
                  {shipmentDetails.vehicleNumber && (
                    <div className="info-item">
                      <span className="label">Biển số xe:</span>
                      <span className="value">{shipmentDetails.vehicleNumber}</span>
                    </div>
                  )}
                </div>
              </div>

              <div className="products-section">
                <h4>Danh sách sản phẩm ({shipmentDetails.products?.length || 0})</h4>
                <div className="products-table">
                  <table>
                    <thead>
                      <tr>
                        <th>Tên sản phẩm</th>
                        <th>Mã lô</th>
                        <th>Số lượng</th>
                        <th>Hạn sử dụng</th>
                        <th>Nguồn gốc</th>
                        <th>Trạng thái</th>
                      </tr>
                    </thead>
                    <tbody>
                      {shipmentDetails.products && shipmentDetails.products.length > 0 ? (
                        shipmentDetails.products.map((product, index) => (
                          <tr key={index}>
                            <td className="product-name">{product.name || 'N/A'}</td>
                            <td className="batch-code">{product.batchCode || 'N/A'}</td>
                            <td className="quantity">{product.quantity ? product.quantity.toLocaleString() : '0'} hộp</td>
                            <td className="expiry">{product.expiry ? formatDate(product.expiry) : 'N/A'}</td>
                            <td className="source">{product.manufacturer || shipmentDetails.from || 'N/A'}</td>
                            <td className="status">
                              <span className="status-badge status-verified">
                                <CheckCircle size={14} />
                                Đã xác thực
                              </span>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: '#6b7280' }}>
                            <Package size={32} style={{ margin: '0 auto 1rem', display: 'block', opacity: 0.5 }} />
                            Không có thông tin sản phẩm chi tiết
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {shipmentDetails.notes && (
                <div className="notes-section">
                  <h4>Ghi chú đặc biệt</h4>
                  <div className="notes-content">
                    <AlertCircle size={16} />
                    {shipmentDetails.notes}
                  </div>
                </div>
              )}

              <div className="blockchain-info">
                <h4>Thông tin Blockchain</h4>
                <div className="blockchain-note">
                  <Info size={16} />
                  Khi xác nhận nhận hàng, quyền giám sát sản phẩm sẽ được chuyển từ <strong>{shipmentDetails.fromType === 'manufacturer' ? 'Nhà sản xuất' : 'Nhà phân phối'}</strong> sang <strong>Hiệu thuốc</strong> và ghi nhận trên blockchain.
                </div>
              </div>
            </div>

            <div className="details-actions">
              <button
                onClick={() => {
                  setScanInput('');
                  setShipmentDetails(null);
                  setError(null);
                  setSuccess(null);
                }}
                className="btn btn-secondary"
              >
                Hủy
              </button>
              <button
                onClick={handleConfirmReceive}
                disabled={loading}
                className="btn btn-success"
              >
                <CheckCircle size={16} />
                {loading ? 'Đang xác nhận...' : 'Xác nhận đã nhận hàng & Cập nhật Blockchain'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Pending Shipments */}
      <div className="pending-shipments">
        <div className="pending-header">
          <h3>
            <Truck size={24} />
            Lô hàng đang chờ nhận ({pendingShipments?.length || 0})
          </h3>
        </div>

        <div className="pending-grid">
          {!pendingShipments || pendingShipments.length === 0 ? (
            <div className="no-pending">
              <Package size={48} className="no-data-icon" />
              <h4>Không có lô hàng nào đang chờ</h4>
              <p>Tất cả lô hàng đã được xử lý</p>
            </div>
          ) : (
            pendingShipments.map(shipment => (
              <div key={shipment.id} className="pending-card">
                <div className="pending-header-info">
                  <div className="shipment-id" style={{ backgroundColor: '#cce5ff', padding: '6px 10px', borderRadius: '6px', border: '2px solid #004085', fontWeight: 'bold' }}>
                    🚚 Mã vận đơn: <strong style={{ color: '#004085' }}>SHIP-{shipment.id}</strong>
                  </div>
                  <div className="tracking-code" style={{ backgroundColor: '#d4edda', padding: '4px 8px', borderRadius: '4px', border: '1px solid #28a745', marginTop: '4px' }}>
                    📦 Số lô (truy vết): <strong style={{ color: '#155724' }}>{shipment.products?.[0]?.batchNumber || 'N/A'}</strong>
                  </div>
                </div>
                <div className="pending-content">
                  <div className="from-info">
                    <strong>{shipment.from}</strong>
                    <span className={`source-badge ${shipment.fromType}`}>
                      {shipment.fromType === 'manufacturer' ? 'NSX' : 'NPP'}
                    </span>
                  </div>
                  <div className="expected-date">
                    Dự kiến: {formatDate(shipment.expectedDate)}
                  </div>
                  <div className="products-count">
                    {shipment.products?.[0]?.name || 'Sản phẩm'} - {shipment.quantity} hộp
                  </div>
                  <div className="total-value">
                    {formatCurrency(shipment.totalValue || 0)}
                  </div>
                </div>
                <div className="pending-actions">
                  <button
                    onClick={() => {
                      // ⭐ Use Shipment ID (SHIP-xxx) for receiving goods
                      const shipmentCode = `SHIP-${shipment.id}`;
                      console.log('🚚 Auto-filling Mã vận đơn:', shipmentCode);
                      setScanInput(shipmentCode);
                      handleScan();
                    }}
                    className="btn btn-primary"
                    style={{ fontWeight: 'bold' }}
                  >
                    📥 Nhận hàng
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default ReceiveGoods;
