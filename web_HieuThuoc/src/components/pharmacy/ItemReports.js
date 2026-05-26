import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ItemReports.css';

/**
 * Báo cáo Item-Level
 * - Số lượng nhập/xuất theo item
 * - Hàng sắp hết hạn (theo item)
 * - Top sản phẩm bán chạy
 */
const ItemReports = () => {
    const [activeTab, setActiveTab] = useState('movements'); // 'movements' | 'expiry' | 'bestsellers'
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // Movement Report Data
    const [movementData, setMovementData] = useState({
        incoming: [],
        outgoing: [],
        summary: {
            totalIncoming: 0,
            totalOutgoing: 0,
            netChange: 0,
        },
    });

    // Expiry Report Data
    const [expiryData, setExpiryData] = useState([]);
    const [expiryDays, setExpiryDays] = useState(90); // Days threshold

    // Bestseller Report Data
    const [bestsellersData, setBestsellersData] = useState([]);

    // Date filters
    const [dateRange, setDateRange] = useState({
        fromDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0], // Last 30 days
        toDate: new Date().toISOString().split('T')[0],
    });

    useEffect(() => {
        switch (activeTab) {
            case 'movements':
                fetchMovementReport();
                break;
            case 'expiry':
                fetchExpiryReport();
                break;
            case 'bestsellers':
                fetchBestsellersReport();
                break;
            default:
                break;
        }
    }, [activeTab, dateRange, expiryDays]);

    const fetchMovementReport = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/product-items/reports/movements`,
                {
                    params: {
                        fromDate: dateRange.fromDate,
                        toDate: dateRange.toDate,
                    },
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            setMovementData(response.data);
        } catch (err) {
            console.error('Error fetching movement report:', err);
            setError('Không thể tải báo cáo nhập/xuất. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const fetchExpiryReport = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/product-items/reports/expiring-soon`,
                {
                    params: {
                        days: expiryDays,
                    },
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            setExpiryData(response.data);
        } catch (err) {
            console.error('Error fetching expiry report:', err);
            setError('Không thể tải báo cáo hết hạn. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const fetchBestsellersReport = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/product-items/reports/bestsellers`,
                {
                    params: {
                        fromDate: dateRange.fromDate,
                        toDate: dateRange.toDate,
                        limit: 50,
                    },
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            setBestsellersData(response.data);
        } catch (err) {
            console.error('Error fetching bestsellers report:', err);
            setError('Không thể tải báo cáo bán chạy. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const handleDateChange = (field, value) => {
        setDateRange(prev => ({
            ...prev,
            [field]: value,
        }));
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN');
    };

    const getDaysUntilExpiry = (expiryDate) => {
        const today = new Date();
        const expiry = new Date(expiryDate);
        const diff = expiry - today;
        const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
        return days;
    };

    const getExpiryWarningClass = (days) => {
        if (days < 0) return 'expiry-expired';
        if (days <= 30) return 'expiry-critical';
        if (days <= 60) return 'expiry-warning';
        return 'expiry-normal';
    };

    const exportReport = () => {
        let csvContent = '';
        let filename = '';

        switch (activeTab) {
            case 'movements':
                csvContent = generateMovementCSV();
                filename = `movement_report_${dateRange.fromDate}_${dateRange.toDate}.csv`;
                break;
            case 'expiry':
                csvContent = generateExpiryCSV();
                filename = `expiry_report_${new Date().toISOString().split('T')[0]}.csv`;
                break;
            case 'bestsellers':
                csvContent = generateBestsellersCSV();
                filename = `bestsellers_report_${dateRange.fromDate}_${dateRange.toDate}.csv`;
                break;
            default:
                return;
        }

        downloadCSV(csvContent, filename);
    };

    const generateMovementCSV = () => {
        const headers = ['Loại', 'Item Code', 'Batch ID', 'Tên thuốc', 'Từ', 'Đến', 'Ngày', 'Ghi chú'];
        const incomingRows = movementData.incoming.map(m => [
            'Nhập',
            m.itemCode,
            m.batchId,
            m.drugName || '',
            m.fromLocation || '',
            m.toLocation || '',
            m.movementDate,
            m.notes || '',
        ]);
        const outgoingRows = movementData.outgoing.map(m => [
            'Xuất',
            m.itemCode,
            m.batchId,
            m.drugName || '',
            m.fromLocation || '',
            m.toLocation || '',
            m.movementDate,
            m.notes || '',
        ]);

        return [
            headers.join(','),
            ...incomingRows.map(row => row.map(cell => `"${cell}"`).join(',')),
            ...outgoingRows.map(row => row.map(cell => `"${cell}"`).join(',')),
        ].join('\n');
    };

    const generateExpiryCSV = () => {
        const headers = ['Item Code', 'Batch ID', 'Tên thuốc', 'HSD', 'Còn lại (ngày)', 'Trạng thái', 'Vị trí'];
        const rows = expiryData.map(item => [
            item.itemCode,
            item.batchId,
            item.drugName || '',
            item.expiryDate,
            getDaysUntilExpiry(item.expiryDate),
            item.status,
            item.currentLocation || '',
        ]);

        return [
            headers.join(','),
            ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
        ].join('\n');
    };

    const generateBestsellersCSV = () => {
        const headers = ['Hạng', 'Tên thuốc', 'Batch ID', 'Số lượng bán', 'Doanh thu (ước tính)'];
        const rows = bestsellersData.map((item, index) => [
            index + 1,
            item.drugName || '',
            item.batchId || 'Nhiều lô',
            item.soldQuantity,
            item.estimatedRevenue || '-',
        ]);

        return [
            headers.join(','),
            ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
        ].join('\n');
    };

    const downloadCSV = (content, filename) => {
        const blob = new Blob(['\ufeff' + content], { type: 'text/csv;charset=utf-8;' }); // Add BOM for UTF-8
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    return (
        <div className="item-reports-container">
            <div className="reports-header">
                <h2>Báo cáo Item-Level</h2>
                <button className="btn-export" onClick={exportReport}>
                    📊 Export CSV
                </button>
            </div>

            {/* Tabs */}
            <div className="report-tabs">
                <button
                    className={activeTab === 'movements' ? 'active' : ''}
                    onClick={() => setActiveTab('movements')}
                >
                    📦 Nhập/Xuất
                </button>
                <button
                    className={activeTab === 'expiry' ? 'active' : ''}
                    onClick={() => setActiveTab('expiry')}
                >
                    ⏰ Sắp hết hạn
                </button>
                <button
                    className={activeTab === 'bestsellers' ? 'active' : ''}
                    onClick={() => setActiveTab('bestsellers')}
                >
                    🏆 Bán chạy
                </button>
            </div>

            {/* Date Range Filter (for movements and bestsellers) */}
            {(activeTab === 'movements' || activeTab === 'bestsellers') && (
                <div className="date-range-filter">
                    <div className="filter-group">
                        <label>Từ ngày:</label>
                        <input
                            type="date"
                            value={dateRange.fromDate}
                            onChange={(e) => handleDateChange('fromDate', e.target.value)}
                        />
                    </div>
                    <div className="filter-group">
                        <label>Đến ngày:</label>
                        <input
                            type="date"
                            value={dateRange.toDate}
                            onChange={(e) => handleDateChange('toDate', e.target.value)}
                        />
                    </div>
                </div>
            )}

            {/* Expiry Days Filter */}
            {activeTab === 'expiry' && (
                <div className="expiry-filter">
                    <label>Hiển thị sản phẩm sẽ hết hạn trong:</label>
                    <select value={expiryDays} onChange={(e) => setExpiryDays(Number(e.target.value))}>
                        <option value={30}>30 ngày</option>
                        <option value={60}>60 ngày</option>
                        <option value={90}>90 ngày</option>
                        <option value={180}>6 tháng</option>
                        <option value={365}>1 năm</option>
                    </select>
                </div>
            )}

            {/* Error */}
            {error && <div className="error-message">{error}</div>}

            {/* Loading */}
            {loading ? (
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Đang tải dữ liệu...</p>
                </div>
            ) : (
                <>
                    {/* Movement Report */}
                    {activeTab === 'movements' && (
                        <div className="movement-report">
                            <div className="summary-cards">
                                <div className="summary-card card-incoming">
                                    <div className="card-icon">📥</div>
                                    <div className="card-info">
                                        <div className="card-value">{movementData.summary.totalIncoming}</div>
                                        <div className="card-label">Tổng nhập</div>
                                    </div>
                                </div>
                                <div className="summary-card card-outgoing">
                                    <div className="card-icon">📤</div>
                                    <div className="card-info">
                                        <div className="card-value">{movementData.summary.totalOutgoing}</div>
                                        <div className="card-label">Tổng xuất</div>
                                    </div>
                                </div>
                                <div className="summary-card card-net">
                                    <div className="card-icon">📊</div>
                                    <div className="card-info">
                                        <div className="card-value">{movementData.summary.netChange >= 0 ? '+' : ''}{movementData.summary.netChange}</div>
                                        <div className="card-label">Thay đổi ròng</div>
                                    </div>
                                </div>
                            </div>

                            <div className="movement-tables">
                                <div className="movement-section">
                                    <h3>📥 Nhập hàng ({movementData.incoming.length})</h3>
                                    <table className="movement-table">
                                        <thead>
                                            <tr>
                                                <th>Item Code</th>
                                                <th>Batch ID</th>
                                                <th>Tên thuốc</th>
                                                <th>Từ</th>
                                                <th>Đến</th>
                                                <th>Ngày</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {movementData.incoming.length === 0 ? (
                                                <tr>
                                                    <td colSpan="6" className="no-data">Không có dữ liệu</td>
                                                </tr>
                                            ) : (
                                                movementData.incoming.slice(0, 10).map((movement, index) => (
                                                    <tr key={index}>
                                                        <td className="item-code">{movement.itemCode}</td>
                                                        <td>{movement.batchId}</td>
                                                        <td>{movement.drugName || '-'}</td>
                                                        <td>{movement.fromLocation || '-'}</td>
                                                        <td>{movement.toLocation || '-'}</td>
                                                        <td>{formatDate(movement.movementDate)}</td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="movement-section">
                                    <h3>📤 Xuất hàng ({movementData.outgoing.length})</h3>
                                    <table className="movement-table">
                                        <thead>
                                            <tr>
                                                <th>Item Code</th>
                                                <th>Batch ID</th>
                                                <th>Tên thuốc</th>
                                                <th>Từ</th>
                                                <th>Đến</th>
                                                <th>Ngày</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {movementData.outgoing.length === 0 ? (
                                                <tr>
                                                    <td colSpan="6" className="no-data">Không có dữ liệu</td>
                                                </tr>
                                            ) : (
                                                movementData.outgoing.slice(0, 10).map((movement, index) => (
                                                    <tr key={index}>
                                                        <td className="item-code">{movement.itemCode}</td>
                                                        <td>{movement.batchId}</td>
                                                        <td>{movement.drugName || '-'}</td>
                                                        <td>{movement.fromLocation || '-'}</td>
                                                        <td>{movement.toLocation || '-'}</td>
                                                        <td>{formatDate(movement.movementDate)}</td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Expiry Report */}
                    {activeTab === 'expiry' && (
                        <div className="expiry-report">
                            <div className="expiry-alert">
                                <span className="alert-icon">⚠️</span>
                                <span>Có {expiryData.length} sản phẩm sẽ hết hạn trong {expiryDays} ngày tới</span>
                            </div>

                            <table className="expiry-table">
                                <thead>
                                    <tr>
                                        <th>Item Code</th>
                                        <th>Batch ID</th>
                                        <th>Tên thuốc</th>
                                        <th>HSD</th>
                                        <th>Còn lại</th>
                                        <th>Trạng thái</th>
                                        <th>Vị trí</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {expiryData.length === 0 ? (
                                        <tr>
                                            <td colSpan="7" className="no-data">Không có sản phẩm sắp hết hạn</td>
                                        </tr>
                                    ) : (
                                        expiryData.map((item) => {
                                            const daysLeft = getDaysUntilExpiry(item.expiryDate);
                                            return (
                                                <tr key={item.itemCode}>
                                                    <td className="item-code">{item.itemCode}</td>
                                                    <td>{item.batchId}</td>
                                                    <td>{item.drugName || '-'}</td>
                                                    <td>{formatDate(item.expiryDate)}</td>
                                                    <td>
                                                        <span className={`days-badge ${getExpiryWarningClass(daysLeft)}`}>
                                                            {daysLeft < 0 ? `Đã quá hạn ${Math.abs(daysLeft)} ngày` : `${daysLeft} ngày`}
                                                        </span>
                                                    </td>
                                                    <td>{item.status}</td>
                                                    <td>{item.currentLocation || '-'}</td>
                                                </tr>
                                            );
                                        })
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Bestsellers Report */}
                    {activeTab === 'bestsellers' && (
                        <div className="bestsellers-report">
                            <table className="bestsellers-table">
                                <thead>
                                    <tr>
                                        <th>Hạng</th>
                                        <th>Tên thuốc</th>
                                        <th>Batch ID</th>
                                        <th>Số lượng bán</th>
                                        <th>% Tổng số</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {bestsellersData.length === 0 ? (
                                        <tr>
                                            <td colSpan="5" className="no-data">Không có dữ liệu</td>
                                        </tr>
                                    ) : (
                                        bestsellersData.map((item, index) => (
                                            <tr key={index} className={index < 3 ? 'top-rank' : ''}>
                                                <td className="rank">
                                                    {index === 0 && '🥇'}
                                                    {index === 1 && '🥈'}
                                                    {index === 2 && '🥉'}
                                                    {index > 2 && `#${index + 1}`}
                                                </td>
                                                <td className="drug-name">{item.drugName || '-'}</td>
                                                <td>{item.batchId || 'Nhiều lô'}</td>
                                                <td className="sold-quantity">{item.soldQuantity}</td>
                                                <td>
                                                    <div className="percentage-bar">
                                                        <div
                                                            className="bar-fill"
                                                            style={{
                                                                width: `${item.percentage || 0}%`,
                                                            }}
                                                        ></div>
                                                        <span className="percentage-text">{item.percentage || 0}%</span>
                                                    </div>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default ItemReports;

