import React, { useState, useEffect } from 'react';
import { X, Download, QrCode, Package, Calendar, CheckCircle, Clock } from 'lucide-react';
import axios from 'axios';
import './BatchProductItems.css';

/**
 * Modal hiển thị tất cả QR codes của sản phẩm trong lô
 */
const BatchProductItems = ({ batch, onClose }) => {
    const [items, setItems] = useState([]);
    const [batchInfo, setBatchInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (batch && batch.id) {
            fetchItems();
        }
    }, [batch]);

    const fetchItems = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/batches/${batch.id}/items`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                }
            );

            if (response.data && response.data.success) {
                setItems(response.data.data.items || []);
                setBatchInfo(response.data.data.batch || null);
            } else {
                setError(response.data.message || 'Không thể tải danh sách items');
            }
        } catch (err) {
            console.error('Error fetching items:', err);
            setError('Lỗi khi tải danh sách items: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleDownloadAll = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/batches/${batch.id}/items/qr-codes`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                    responseType: 'blob',
                }
            );

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `QR_Codes_${batch.batchNumber}.zip`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            console.error('Error downloading QR codes:', err);
            alert('Lỗi khi tải QR codes: ' + err.message);
        }
    };

    const getStatusIcon = (status) => {
        const icons = {
            'MANUFACTURED': <CheckCircle size={14} className="status-icon manufactured" />,
            'IN_STOCK': <Package size={14} className="status-icon in-stock" />,
            'IN_TRANSIT': <Clock size={14} className="status-icon in-transit" />,
            'SOLD': <CheckCircle size={14} className="status-icon sold" />,
        };
        return icons[status] || null;
    };

    const getStatusText = (status) => {
        const texts = {
            'MANUFACTURED': 'Đã sản xuất',
            'IN_STOCK': 'Trong kho',
            'IN_TRANSIT': 'Vận chuyển',
            'SOLD': 'Đã bán',
            'RETURNED': 'Trả lại',
            'RECALLED': 'Thu hồi',
        };
        return texts[status] || status;
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
        });
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content batch-items-modal" onClick={(e) => e.stopPropagation()}>
                {/* Modal Header */}
                <div className="modal-header">
                    <div className="header-info">
                        <QrCode size={28} className="header-icon" />
                        <div>
                            <h2>{batch.drugName}</h2>
                            <p className="batch-number-label">Lô: {batch.batchNumber}</p>
                        </div>
                    </div>
                    <div className="header-actions">
                        <button 
                            className="btn-download-all"
                            onClick={handleDownloadAll}
                            disabled={items.length === 0}
                        >
                            <Download size={16} />
                            Tải tất cả QR ({items.length})
                        </button>
                        <button className="btn-close" onClick={onClose}>
                            <X size={20} />
                        </button>
                    </div>
                </div>

                {/* Batch Info Summary */}
                {batchInfo && (
                    <div className="batch-info-summary">
                        <div className="info-item">
                            <span className="info-label">Nhà sản xuất:</span>
                            <span className="info-value">{batchInfo.manufacturer}</span>
                        </div>
                        <div className="info-item">
                            <span className="info-label">Số lượng lô:</span>
                            <span className="info-value">{batchInfo.quantity} đơn vị</span>
                        </div>
                        <div className="info-item">
                            <Calendar size={14} />
                            <span className="info-label">NSX:</span>
                            <span className="info-value">{formatDate(batchInfo.manufactureDate)}</span>
                        </div>
                        <div className="info-item">
                            <Calendar size={14} />
                            <span className="info-label">HSD:</span>
                            <span className="info-value">{formatDate(batchInfo.expiryDate)}</span>
                        </div>
                    </div>
                )}

                {/* Loading */}
                {loading && (
                    <div className="loading-section">
                        <div className="spinner"></div>
                        <p>Đang tải QR codes...</p>
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="error-section">
                        <p>{error}</p>
                    </div>
                )}

                {/* Items Grid */}
                {!loading && !error && (
                    <div className="items-container">
                        <div className="items-header">
                            <h3>Danh sách QR Codes ({items.length} sản phẩm)</h3>
                        </div>

                        {items.length === 0 ? (
                            <div className="no-items">
                                <QrCode size={48} className="no-items-icon" />
                                <p>Chưa có sản phẩm nào trong lô này</p>
                            </div>
                        ) : (
                            <div className="items-grid">
                                {items.map((item, index) => (
                                    <div key={item.itemCode} className="item-card">
                                        <div className="item-header">
                                            <span className="item-number">#{index + 1}</span>
                                            <div className="item-status">
                                                {getStatusIcon(item.status)}
                                                <span>{getStatusText(item.status)}</span>
                                            </div>
                                        </div>

                                        <div className="qr-code-container">
                                            {item.qrCodeData ? (
                                                <img
                                                    src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(item.qrCodeData)}`}
                                                    alt={`QR Code ${item.itemCode}`}
                                                    className="qr-code-image"
                                                />
                                            ) : (
                                                <div className="qr-placeholder">
                                                    <QrCode size={48} />
                                                    <p>No QR</p>
                                                </div>
                                            )}
                                        </div>

                                        <div className="item-code-section">
                                            <span className="item-code-label">Mã sản phẩm:</span>
                                            <span className="item-code">{item.itemCode}</span>
                                        </div>

                                        <div className="item-details">
                                            <div className="detail-item">
                                                <span className="detail-label">NSX:</span>
                                                <span className="detail-value">{formatDate(item.manufactureDate)}</span>
                                            </div>
                                            <div className="detail-item">
                                                <span className="detail-label">HSD:</span>
                                                <span className="detail-value">{formatDate(item.expiryDate)}</span>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default BatchProductItems;

