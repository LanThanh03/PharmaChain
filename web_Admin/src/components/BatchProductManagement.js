import React, { useState, useEffect } from 'react';
import { Package, Eye, Download, Calendar, CheckCircle, Clock, AlertCircle, Trash2 } from 'lucide-react';
import axios from 'axios';
import './BatchProductManagement.css';
import BatchProductItems from './BatchProductItems';

/**
 * Quản lý sản phẩm theo lô
 * - Hiển thị danh sách lô
 * - Xem chi tiết QR codes của từng lô
 */
const BatchProductManagement = () => {
    const [batches, setBatches] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedBatch, setSelectedBatch] = useState(null);
    const [showItemsModal, setShowItemsModal] = useState(false);
    const [showDeleteModal, setShowDeleteModal] = useState(false);
    const [batchToDelete, setBatchToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);

    useEffect(() => {
        fetchBatches();
    }, []);

    const fetchBatches = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/batches/with-items`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                }
            );

            if (response.data && response.data.success) {
                setBatches(response.data.data);
            } else {
                setError(response.data.message || 'Không thể tải danh sách lô');
            }
        } catch (err) {
            console.error('Error fetching batches:', err);
            setError('Lỗi khi tải danh sách lô: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleViewItems = async (batch) => {
        setSelectedBatch(batch);
        setShowItemsModal(true);
    };

    const handleDownloadAllQR = async (batchId, batchNumber) => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/batches/${batchId}/items/qr-codes`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                    responseType: 'blob',
                }
            );

            // Create download link
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `QR_Codes_${batchNumber}.zip`);
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (err) {
            console.error('Error downloading QR codes:', err);
            alert('Lỗi khi tải QR codes: ' + err.message);
        }
    };

    const handleDeleteClick = (batch) => {
        setBatchToDelete(batch);
        setShowDeleteModal(true);
    };

    const handleDeleteConfirm = async () => {
        if (!batchToDelete) return;

        setDeleting(true);
        try {
            const token = localStorage.getItem('authToken');
            await axios.delete(
                `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}/api/batches/${batchToDelete.id}`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {},
                }
            );

            // Remove from UI
            setBatches(batches.filter(b => b.id !== batchToDelete.id));
            setShowDeleteModal(false);
            setBatchToDelete(null);

            // Show success message
            alert('Đã xóa lô thành công!');
        } catch (err) {
            console.error('Error deleting batch:', err);
            alert('Lỗi khi xóa lô: ' + (err.response?.data?.message || err.message));
        } finally {
            setDeleting(false);
        }
    };

    const handleDeleteCancel = () => {
        setShowDeleteModal(false);
        setBatchToDelete(null);
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            'MANUFACTURED': { icon: <CheckCircle size={16} />, label: 'Đã sản xuất', class: 'status-manufactured' },
            'IN_TRANSIT': { icon: <Clock size={16} />, label: 'Đang vận chuyển', class: 'status-transit' },
            'DELIVERED': { icon: <CheckCircle size={16} />, label: 'Đã giao', class: 'status-delivered' },
            'RECALLED': { icon: <AlertCircle size={16} />, label: 'Thu hồi', class: 'status-recalled' },
        };

        const config = statusConfig[status] || { icon: null, label: status, class: '' };

        return (
            <span className={`status-badge ${config.class}`}>
                {config.icon}
                {config.label}
            </span>
        );
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN');
    };

    // Grouping
    const groupBatchesByTime = (batchesArray) => {
        const now = new Date();
        const groups = {
            'Hôm nay': [],
            'Hôm qua': [],
            'Tuần này': [],
            'Tháng này': [],
            'Cũ hơn': []
        };

        batchesArray.forEach(batch => {
            const createdDate = batch.createdAt ? new Date(batch.createdAt) : new Date(batch.manufactureDate || 2020);

            const diffTime = Math.abs(now - createdDate);
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

            if (diffDays <= 1 && now.getDate() === createdDate.getDate()) {
                groups['Hôm nay'].push(batch);
            } else if (diffDays <= 2 && now.getDate() - createdDate.getDate() === 1) {
                groups['Hôm qua'].push(batch);
            } else if (diffDays <= 7) {
                groups['Tuần này'].push(batch);
            } else if (diffDays <= 30) {
                groups['Tháng này'].push(batch);
            } else {
                groups['Cũ hơn'].push(batch);
            }
        });

        return Object.entries(groups).filter(([_, items]) => items.length > 0);
    };

    const groupedBatches = groupBatchesByTime(batches);

    if (loading) {
        return (
            <div className="batch-management">
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Đang tải danh sách lô...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="batch-management">
            <div className="page-header">
                <div className="header-left">
                    <Package size={32} className="header-icon" />
                    <div>
                        <h1>Quản lý Sản phẩm theo Lô</h1>
                        <p className="header-subtitle">Xem QR codes của từng sản phẩm trong lô</p>
                    </div>
                </div>
                <div className="header-stats">
                    <div className="stat-item">
                        <span className="stat-value">{batches.length}</span>
                        <span className="stat-label">Tổng số lô</span>
                    </div>
                    <div className="stat-item">
                        <span className="stat-value">
                            {batches.reduce((sum, b) => sum + (b.itemsCount || 0), 0)}
                        </span>
                        <span className="stat-label">Tổng sản phẩm</span>
                    </div>
                </div>
            </div>

            {error && (
                <div className="error-message">
                    <AlertCircle size={20} />
                    {error}
                </div>
            )}

            <div className="batches-grid">
                {batches.length === 0 ? (
                    <div className="no-batches">
                        <Package size={64} className="no-data-icon" />
                        <p>Chưa có lô hàng nào</p>
                        <p className="hint">Tạo lô hàng mới để bắt đầu</p>
                    </div>
                ) : (
                    groupedBatches.map(([groupName, groupItems]) => (
                        <React.Fragment key={groupName}>
                            <div className="group-header-section">
                                <span className="group-title">{groupName}</span>
                                <span className="group-count">{groupItems.length} lô</span>
                            </div>
                            {groupItems.map((batch) => (
                                <div key={batch.id} className="batch-card">
                                    <div className="batch-header">
                                        <div className="batch-title-section">
                                            <h3>
                                                {batch.drugName}
                                                {groupName === 'Hôm nay' && <span className="new-badge">Mới</span>}
                                            </h3>
                                            <span className="batch-number">#{batch.batchNumber}</span>
                                        </div>
                                        {getStatusBadge(batch.status)}
                                    </div>

                                    <div className="batch-details">
                                        <div className="detail-row">
                                            <span className="detail-label">Nhà sản xuất:</span>
                                            <span className="detail-value">{batch.manufacturer}</span>
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Số lượng lô:</span>
                                            <span className="detail-value">{batch.quantity} hộp</span>
                                        </div>
                                        <div className="detail-row">
                                            <span className="detail-label">Số sản phẩm:</span>
                                            <span className="detail-value items-count">{batch.itemsCount || 0} items</span>
                                        </div>
                                        <div className="detail-row">
                                            <Calendar size={14} />
                                            <span className="detail-label">NSX:</span>
                                            <span className="detail-value">{formatDate(batch.manufactureDate)}</span>
                                        </div>
                                        <div className="detail-row">
                                            <Calendar size={14} />
                                            <span className="detail-label">HSD:</span>
                                            <span className="detail-value">{formatDate(batch.expiryDate)}</span>
                                        </div>
                                    </div>

                                    {/* Items Status Breakdown */}
                                    {batch.itemsStatusCounts && Object.keys(batch.itemsStatusCounts).length > 0 && (
                                        <div className="items-status-section">
                                            <div className="status-label">Trạng thái items:</div>
                                            <div className="status-counts">
                                                {Object.entries(batch.itemsStatusCounts).map(([status, count]) => (
                                                    <span key={status} className="status-count-badge">
                                                        {status}: {count}
                                                    </span>
                                                ))}
                                            </div>
                                        </div>
                                    )}

                                    <div className="batch-actions">
                                        <button
                                            className="btn-view-items"
                                            onClick={() => handleViewItems(batch)}
                                            disabled={!batch.itemsCount || batch.itemsCount === 0}
                                        >
                                            <Eye size={16} />
                                            Chi tiết QR
                                        </button>
                                        <button
                                            className="btn-download"
                                            onClick={() => handleDownloadAllQR(batch.id, batch.batchNumber)}
                                            disabled={!batch.itemsCount || batch.itemsCount === 0}
                                        >
                                            <Download size={16} />
                                            Tải QR
                                        </button>
                                        <button
                                            className="btn-delete"
                                            onClick={() => handleDeleteClick(batch)}
                                            title="Xóa lô này"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </React.Fragment>
                    ))
                )}
            </div>

            {/* Modal hiển thị items */}
            {showItemsModal && selectedBatch && (
                <BatchProductItems
                    batch={selectedBatch}
                    onClose={() => {
                        setShowItemsModal(false);
                        setSelectedBatch(null);
                    }}
                />
            )}

            {/* Modal xác nhận xóa */}
            {showDeleteModal && batchToDelete && (
                <div className="modal-overlay">
                    <div className="delete-modal">
                        <div className="delete-modal-header">
                            <AlertCircle size={48} className="warning-icon" />
                            <h2>Xác nhận xóa lô</h2>
                        </div>
                        <div className="delete-modal-body">
                            <p>Bạn có chắc chắn muốn xóa lô này?</p>
                            <div className="batch-info-to-delete">
                                <p><strong>Tên thuốc:</strong> {batchToDelete.drugName}</p>
                                <p><strong>Số lô:</strong> #{batchToDelete.batchNumber}</p>
                                <p><strong>Số lượng:</strong> {batchToDelete.quantity} hộp</p>
                                <p><strong>Số sản phẩm:</strong> {batchToDelete.itemsCount || 0} items</p>
                            </div>
                            <div className="warning-message">
                                <AlertCircle size={20} />
                                <span>Hành động này không thể hoàn tác!</span>
                            </div>
                        </div>
                        <div className="delete-modal-actions">
                            <button
                                className="btn-cancel"
                                onClick={handleDeleteCancel}
                                disabled={deleting}
                            >
                                Hủy
                            </button>
                            <button
                                className="btn-confirm-delete"
                                onClick={handleDeleteConfirm}
                                disabled={deleting}
                            >
                                {deleting ? (
                                    <>
                                        <div className="spinner-small"></div>
                                        Đang xóa...
                                    </>
                                ) : (
                                    <>
                                        <Trash2 size={16} />
                                        Xóa lô
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default BatchProductManagement;

