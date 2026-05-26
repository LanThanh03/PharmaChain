import React, { useState, useEffect } from 'react';
import { Package, Scan, CheckCircle, AlertCircle, Truck, Info, QrCode } from 'lucide-react';
import distributorService from '../services/apiService';
import './ReceiveGoods.css';

const ReceiveGoods = () => {
    const [scanInput, setScanInput] = useState('');
    const [shipmentDetails, setShipmentDetails] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [pendingShipments, setPendingShipments] = useState([]);

    // Helper function to normalize shipment data
    const normalizeShipmentData = (shipment) => {
        if (!shipment) return null;

        return {
            ...shipment,
            products: shipment.products || [
                {
                    name: shipment.drugName || 'Sản phẩm',
                    quantity: shipment.quantity || 0,
                    batchCode: shipment.batchNumber || shipment.id,
                    manufacturer: shipment.manufacturerName || 'N/A'
                }
            ]
        };
    };

    useEffect(() => {
        fetchPendingShipments();
    }, []);

    const fetchPendingShipments = async () => {
        try {
            setLoading(true);
            setPendingShipments([]); // Set to empty array first to avoid undefined

            // Use recipient address (distributor wallet) to get inbound shipments
            const recipientAddress = localStorage.getItem('walletAddress');

            if (!recipientAddress) {
                console.warn('No wallet address found');
                setPendingShipments([]);
                return;
            }

            const response = await distributorService.getInboundShipments(recipientAddress);
            console.log('📦 Inbound shipments response:', response);

            if (response && response.success && Array.isArray(response.data)) {
                // Filter out invalid shipments and normalize
                const validShipments = response.data.filter(shipment =>
                    shipment && (shipment.shipmentId || shipment.id)
                );
                const normalizedShipments = validShipments.map(shipment => normalizeShipmentData(shipment));
                setPendingShipments(normalizedShipments || []);
                console.log('✅ Set pending shipments:', normalizedShipments.length);
            } else {
                console.error('Failed to fetch pending shipments:', response?.message || 'Invalid response');
                setPendingShipments([]);
            }
        } catch (err) {
            console.error('❌ Error fetching pending shipments:', err);
            setError('Không thể tải danh sách shipment đang chờ. Vui lòng thử lại.');
            setPendingShipments([]);
        } finally {
            setLoading(false);
        }
    };

    const handleScan = async () => {
        if (!scanInput.trim()) {
            setError('Vui lòng nhập mã shipment (SHIP-xxxxxxxxxx hoặc số shipment ID)');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            // Step 1: Check if input is a shipment ID in pending list
            const foundShipment = (pendingShipments || []).find(
                shipment => shipment.trackingCode === scanInput ||
                    shipment.id === scanInput ||
                    shipment.shipmentId === scanInput ||
                    `SHIP-${shipment.shipmentId}` === scanInput ||
                    shipment.shipmentId?.toString() === scanInput
            );

            if (foundShipment) {
                // Step 2: Verify blockchain ownership before showing details
                const recipientAddress = localStorage.getItem('walletAddress');

                // Check if this shipment's NFT ownership was transferred to us
                try {
                    const verification = await distributorService.verifyShipmentOwnership(
                        foundShipment.id || foundShipment.shipmentId,
                        recipientAddress
                    );

                    if (verification.success && verification.data.isOwner) {
                        setShipmentDetails(normalizeShipmentData(foundShipment));
                        return;
                    } else {
                        setError('Lô hàng này chưa được chuyển quyền sở hữu trên blockchain. Có thể là hàng giả!');
                        return;
                    }
                } catch (e) {
                    console.error('Error verifying blockchain ownership:', e);
                    // Continue with normal flow if verification fails
                    setShipmentDetails(normalizeShipmentData(foundShipment));
                    return;
                }
            }

            // Step 2: Try to fetch shipment by ID directly from API
            try {
                const byId = await distributorService.getShipmentById(scanInput);
                if (byId.success && byId.data) {
                    // Verify this shipment belongs to us
                    const recipientAddress = localStorage.getItem('walletAddress');
                    if (byId.data.toAddress?.toLowerCase() === recipientAddress?.toLowerCase()) {
                        // Get batch information for display
                        let batchInfo = null;
                        if (byId.data.batchId) {
                            try {
                                const batchRes = await distributorService.getBatchDetails(byId.data.batchId);
                                if (batchRes.success && batchRes.data) {
                                    batchInfo = batchRes.data;
                                }
                            } catch (e) {
                                console.warn('Could not fetch batch details:', e);
                            }
                        }

                        // Create normalized shipment data with batch info
                        const normalized = {
                            id: byId.data.id,
                            shipmentId: byId.data.shipmentId, // Ensure shipmentId is preserved
                            trackingCode: byId.data.shipmentCode || `SHIP-${byId.data.shipmentId}`,
                            from: byId.data.fromAddress,
                            to: byId.data.toAddress,
                            status: byId.data.status,
                            quantity: byId.data.quantity,
                            shipmentTimestamp: byId.data.shipmentTimestamp,
                            trackingInfo: byId.data.trackingInfo,
                            expectedDate: byId.data.expectedDeliveryDate || byId.data.shipmentTimestamp,
                            totalValue: 0, // Will be calculated from products
                            products: [
                                {
                                    name: batchInfo?.drugName || 'Sản phẩm dược phẩm',
                                    quantity: byId.data.quantity || 0,
                                    batchCode: batchInfo?.batchNumber || 'N/A',
                                    manufacturer: batchInfo?.manufacturer || 'N/A',
                                    expiryDate: batchInfo?.expiryDate || 'N/A'
                                }
                            ]
                        };
                        setShipmentDetails(normalized);
                        return;
                    } else {
                        setError('Lô hàng này không được gửi đến địa chỉ của bạn');
                        return;
                    }
                }
            } catch (e) {
                console.log('Shipment not found by ID, trying other methods...');
            }

            // Fallback: user scanned batch code -> find pending shipments by batch (by code or internal id)
            try {
                let shipmentsRes = await distributorService.getShipmentsByBatch(scanInput);
                let shipments = Array.isArray(shipmentsRes?.data) ? shipmentsRes.data : [];

                // If backend rejected because scanInput is batchNumber, resolve batchId first
                if (!shipments.length) {
                    const allBatches = await distributorService.getBatches();
                    const foundBatch = (allBatches?.data || []).find(b =>
                        b.batchNumber === scanInput || b.id?.toString?.() === scanInput || b.batchId?.toString?.() === scanInput
                    );
                    if (foundBatch) {
                        const batchId = foundBatch.batchId?.toString?.() || foundBatch.id?.toString?.() || foundBatch.batchNumber;
                        shipmentsRes = await distributorService.getShipmentsByBatch(batchId);
                        shipments = Array.isArray(shipmentsRes?.data) ? shipmentsRes.data : [];
                    }
                }

                if (shipments.length) {
                    const recipientAddress = localStorage.getItem('walletAddress')?.toLowerCase?.();
                    const pendingForMe = shipments.find(s =>
                        (s.status?.toLowerCase?.() === 'pending' || s.status?.toLowerCase?.() === 'in_transit') &&
                        (s.recipientAddress?.toLowerCase?.() === recipientAddress || s.toAddress?.toLowerCase?.() === recipientAddress)
                    );
                    if (pendingForMe) {
                        // Normalize minimal fields for UI
                        const normalized = {
                            id: pendingForMe.id || pendingForMe.shipmentId,
                            shipmentId: pendingForMe.shipmentId, // Ensure shipmentId is available
                            trackingCode: pendingForMe.shipmentCode || pendingForMe.trackingCode || `SHIP-${pendingForMe.shipmentId}` || pendingForMe.id,
                            from: pendingForMe.senderName || pendingForMe.from || 'Nhà sản xuất',
                            expectedDate: pendingForMe.estimatedDelivery || pendingForMe.createdAt,
                            totalValue: pendingForMe.totalValue || 0,
                            products: [
                                {
                                    name: pendingForMe.drugName || 'Sản phẩm',
                                    quantity: pendingForMe.quantity || 0,
                                    batchCode: pendingForMe.batchNumber || scanInput
                                }
                            ]
                        };
                        setShipmentDetails(normalized);
                        return;
                    }
                }
                setError(`Không tìm thấy shipment với mã: ${scanInput}. Vui lòng kiểm tra lại mã shipment (định dạng: SHIP-xxxxxxxxxx hoặc số shipment ID).`);
            } catch (err) {
                console.warn('Could not find shipments by batch:', err);
                setError(`Không tìm thấy shipment với mã: ${scanInput}. Vui lòng kiểm tra lại mã shipment.`);
            }
        } catch (err) {
            setError('Không thể tìm thấy thông tin lô hàng: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleConfirmReceive = async () => {
        if (!shipmentDetails) return;

        try {
            setLoading(true);
            setError(null);

            // Confirm receipt via API - try multiple ID strategies
            let shipmentIdForApi = null;

            // Strategy 1: Use database ID if available (most reliable)
            if (shipmentDetails.id) {
                shipmentIdForApi = shipmentDetails.id.toString();
                console.log('Using database ID for API call:', shipmentIdForApi);
            }
            // Strategy 2: Use shipmentId from blockchain
            else if (shipmentDetails.shipmentId) {
                shipmentIdForApi = shipmentDetails.shipmentId.toString();
                console.log('Using blockchain shipmentId for API call:', shipmentIdForApi);
            }
            // Strategy 3: Extract from tracking code
            else if (shipmentDetails.trackingCode?.startsWith('SHIP-')) {
                shipmentIdForApi = shipmentDetails.trackingCode.slice(5);
                console.log('Using tracking code ID for API call:', shipmentIdForApi);
            }

            if (!shipmentIdForApi) {
                setError('Không xác định được shipment ID để nhận hàng');
                console.error('shipmentDetails:', shipmentDetails);
                return;
            }

            console.log('Receiving shipment with shipmentId:', shipmentIdForApi);
            const response = await distributorService.receiveShipment(shipmentIdForApi);

            if (response.success) {
                // ✅ FIX: Use receiveTransactionHash (new unique TX from receiveShipment).
                // response.data?.transactionHash is the OLD createShipment hash.
                const data = response.data?.data || response.data || {};
                const receiveTxHash = data.receiveTransactionHash || data.receiveTxHash;
                const createTxHash = data.transactionHash || data.createTxHash;
                const txHash = receiveTxHash || createTxHash || '';
                console.log('🔗 createTxHash:', createTxHash);
                console.log('🔗 receiveTxHash:', receiveTxHash, '← should be DIFFERENT');
                setSuccess(`Đã xác nhận nhận hàng thành công! Giao dịch blockchain (nhận hàng): ${txHash}`);

                // Reset form
                setScanInput('');
                setShipmentDetails(null);

                // Refresh pending shipments
                await fetchPendingShipments();
            } else {
                setError(response.message || 'Không thể xác nhận nhận hàng');
            }
        } catch (err) {
            setError('Lỗi xác nhận nhận hàng: ' + err.message);
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
            return date.toLocaleDateString('vi-VN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        } catch (error) {
            return dateString;
        }
    };


    return (
        <div className="receive-goods">
            <div className="page-header">
                <h1>
                    <Package className="page-icon" />
                    Quản lý Nhập kho
                </h1>
                <p>Xác thực và nhận hàng từ Nhà sản xuất</p>
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
                        <h3>Quét mã nhận hàng</h3>
                        <p>Quét mã shipment từ phiếu giao hàng hoặc nhập trực tiếp mã SHIP-xxxxxxxxxx</p>
                    </div>

                    <div className="scanner-input">
                        <div className="input-group">
                            <Scan className="input-icon" />
                            <input
                                type="text"
                                value={scanInput}
                                onChange={(e) => setScanInput(e.target.value)}
                                placeholder="Nhập mã shipment (VD: SHIP-17591528224436381 hoặc 17591528224436381)"
                                className="scan-input"
                                onKeyPress={(e) => e.key === 'Enter' && handleScan()}
                            />
                            <button
                                onClick={handleScan}
                                disabled={loading}
                                className="btn btn-primary scan-btn"
                            >
                                {loading ? 'Đang tìm...' : 'Quét'}
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
                            <div className="shipment-id">ID: {shipmentDetails.id}</div>
                        </div>

                        <div className="details-content">
                            <div className="info-section">
                                <h4>Thông tin vận chuyển</h4>
                                <div className="info-grid">
                                    <div className="info-item">
                                        <span className="label">Từ nhà sản xuất:</span>
                                        <span className="value">{shipmentDetails.from}</span>
                                    </div>
                                    <div className="info-item">
                                        <span className="label">Mã vận đơn:</span>
                                        <span className="value">{shipmentDetails.trackingCode}</span>
                                    </div>
                                    <div className="info-item">
                                        <span className="label">Ngày dự kiến:</span>
                                        <span className="value">{formatDate(shipmentDetails.expectedDate)}</span>
                                    </div>
                                    <div className="info-item">
                                        <span className="label">Tổng giá trị:</span>
                                        <span className="value highlight">{formatCurrency(shipmentDetails.totalValue)}</span>
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
                                <h4>Danh sách sản phẩm</h4>
                                <div className="products-table">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>Tên sản phẩm</th>
                                                <th>Mã lô</th>
                                                <th>Số lượng</th>
                                                <th>Hạn sử dụng</th>
                                                <th>Nhà sản xuất</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {(shipmentDetails.products || []).map((product, index) => (
                                                <tr key={index}>
                                                    <td className="product-name">{product.name}</td>
                                                    <td className="batch-code">{product.batchCode}</td>
                                                    <td className="quantity">{product.quantity?.toLocaleString() || 0} hộp</td>
                                                    <td className="expiry">{product.expiry || 'N/A'}</td>
                                                    <td className="manufacturer">{product.manufacturer || shipmentDetails.from}</td>
                                                </tr>
                                            ))}
                                            {(!shipmentDetails.products || shipmentDetails.products.length === 0) && (
                                                <tr>
                                                    <td colSpan="5" className="no-products">
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
                                {loading ? 'Đang xác nhận...' : 'Xác nhận đã nhận hàng'}
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
                        Lô hàng đang chờ nhận ({pendingShipments ? pendingShipments.length : 0})
                    </h3>
                </div>

                <div className="pending-grid">
                    {loading && (!pendingShipments || pendingShipments.length === 0) ? (
                        <div className="loading-pending">
                            <div className="loading-spinner"></div>
                            <p>Đang tải danh sách lô hàng đang chờ...</p>
                        </div>
                    ) : !pendingShipments || pendingShipments.length === 0 ? (
                        <div className="no-pending">
                            <Package size={48} className="no-data-icon" />
                            <h4>Không có lô hàng nào đang chờ</h4>
                            <p>Tất cả lô hàng đã được xử lý</p>
                        </div>
                    ) : (
                        (pendingShipments || []).map(shipment => (
                            <div key={shipment.id} className="pending-card">
                                <div className="pending-header-info">
                                    <div className="shipment-id">{shipment.id}</div>
                                    <div className="tracking-code">{shipment.trackingCode}</div>
                                </div>
                                <div className="pending-content">
                                    <div className="from-info">
                                        <strong>{shipment.from}</strong>
                                    </div>
                                    <div className="expected-date">
                                        Dự kiến: {formatDate(shipment.expectedDate)}
                                    </div>
                                    <div className="products-count">
                                        {(shipment.products || []).length} sản phẩm
                                    </div>
                                    <div className="total-value">
                                        {formatCurrency(shipment.totalValue)}
                                    </div>
                                </div>
                                <div className="pending-actions">
                                    <button
                                        onClick={() => {
                                            setScanInput(shipment.trackingCode);
                                            handleScan();
                                        }}
                                        className="btn btn-outline"
                                    >
                                        Xem chi tiết
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
