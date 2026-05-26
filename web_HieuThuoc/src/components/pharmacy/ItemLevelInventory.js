import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ItemLevelInventory.css';

/**
 * Quản lý kho theo từng item
 * Features:
 * - Xem theo batch hoặc item
 * - Filter theo status (IN_STOCK, SOLD, IN_TRANSIT, RETURNED)
 * - Search theo item_code, batch_id, drug_name
 * - Phân trang
 * - Export danh sách
 */
const ItemLevelInventory = () => {
    const [viewMode, setViewMode] = useState('item'); // 'item' | 'batch'
    const [items, setItems] = useState([]);
    const [batches, setBatches] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    
    // Filters
    const [filters, setFilters] = useState({
        status: '',
        searchQuery: '',
        batchId: '',
        drugName: '',
        fromDate: '',
        toDate: '',
    });
    
    // Pagination
    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [pageSize] = useState(20);
    
    // Selected items for bulk actions
    const [selectedItems, setSelectedItems] = useState([]);
    
    // Statistics
    const [stats, setStats] = useState({
        totalItems: 0,
        inStock: 0,
        sold: 0,
        inTransit: 0,
        returned: 0,
    });

    useEffect(() => {
        if (viewMode === 'item') {
            fetchItems();
        } else {
            fetchBatches();
        }
    }, [viewMode, filters, currentPage]);

    const fetchItems = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const params = {
                page: currentPage - 1,
                size: pageSize,
                ...filters,
            };
            
            // Remove empty filters
            Object.keys(params).forEach(key => {
                if (params[key] === '' || params[key] === null) {
                    delete params[key];
                }
            });

            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/product-items/search`,
                {
                    params,
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            setItems(response.data.content || response.data);
            setTotalPages(response.data.totalPages || 1);
            
            // Fetch statistics
            fetchStatistics();
        } catch (err) {
            console.error('Error fetching items:', err);
            setError('Không thể tải danh sách sản phẩm. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const fetchBatches = async () => {
        setLoading(true);
        setError(null);
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/batches`,
                {
                    params: {
                        page: currentPage - 1,
                        size: pageSize,
                    },
                    headers: { Authorization: `Bearer ${token}` },
                }
            );

            setBatches(response.data.content || response.data);
            setTotalPages(response.data.totalPages || 1);
        } catch (err) {
            console.error('Error fetching batches:', err);
            setError('Không thể tải danh sách lô hàng. Vui lòng thử lại.');
        } finally {
            setLoading(false);
        }
    };

    const fetchStatistics = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await axios.get(
                `${process.env.REACT_APP_API_BASE_URL}/api/product-items/statistics`,
                {
                    headers: { Authorization: `Bearer ${token}` },
                }
            );
            setStats(response.data);
        } catch (err) {
            console.error('Error fetching statistics:', err);
        }
    };

    const handleFilterChange = (field, value) => {
        setFilters(prev => ({
            ...prev,
            [field]: value,
        }));
        setCurrentPage(1); // Reset to first page
    };

    const handleClearFilters = () => {
        setFilters({
            status: '',
            searchQuery: '',
            batchId: '',
            drugName: '',
            fromDate: '',
            toDate: '',
        });
        setCurrentPage(1);
    };

    const handleSelectItem = (itemCode) => {
        setSelectedItems(prev => {
            if (prev.includes(itemCode)) {
                return prev.filter(code => code !== itemCode);
            } else {
                return [...prev, itemCode];
            }
        });
    };

    const handleSelectAll = () => {
        if (selectedItems.length === items.length) {
            setSelectedItems([]);
        } else {
            setSelectedItems(items.map(item => item.itemCode));
        }
    };

    const handleExportCSV = () => {
        const csvContent = generateCSV(items);
        downloadCSV(csvContent, `inventory_${new Date().toISOString().split('T')[0]}.csv`);
    };

    const generateCSV = (data) => {
        const headers = ['Item Code', 'Batch ID', 'Drug Name', 'Status', 'Current Location', 'Manufacture Date', 'Expiry Date'];
        const rows = data.map(item => [
            item.itemCode,
            item.batchId,
            item.drugName || '',
            item.status,
            item.currentLocation || '',
            item.manufactureDate || '',
            item.expiryDate || '',
        ]);

        return [
            headers.join(','),
            ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
        ].join('\n');
    };

    const downloadCSV = (content, filename) => {
        const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', filename);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    };

    const getStatusBadgeClass = (status) => {
        const statusClasses = {
            'IN_STOCK': 'status-in-stock',
            'SOLD': 'status-sold',
            'IN_TRANSIT': 'status-in-transit',
            'RETURNED': 'status-returned',
            'RECALLED': 'status-recalled',
        };
        return statusClasses[status] || 'status-default';
    };

    const getStatusText = (status) => {
        const statusTexts = {
            'IN_STOCK': 'Trong kho',
            'SOLD': 'Đã bán',
            'IN_TRANSIT': 'Đang vận chuyển',
            'RETURNED': 'Trả lại',
            'RECALLED': 'Thu hồi',
        };
        return statusTexts[status] || status;
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return date.toLocaleDateString('vi-VN');
    };

    return (
        <div className="item-inventory-container">
            <div className="inventory-header">
                <h2>Quản lý kho theo sản phẩm</h2>
                <div className="view-mode-toggle">
                    <button
                        className={viewMode === 'item' ? 'active' : ''}
                        onClick={() => setViewMode('item')}
                    >
                        Theo Item
                    </button>
                    <button
                        className={viewMode === 'batch' ? 'active' : ''}
                        onClick={() => setViewMode('batch')}
                    >
                        Theo Batch
                    </button>
                </div>
            </div>

            {/* Statistics Cards */}
            <div className="stats-cards">
                <div className="stat-card">
                    <div className="stat-icon">📦</div>
                    <div className="stat-info">
                        <div className="stat-value">{stats.totalItems}</div>
                        <div className="stat-label">Tổng số sản phẩm</div>
                    </div>
                </div>
                <div className="stat-card stat-in-stock">
                    <div className="stat-icon">✅</div>
                    <div className="stat-info">
                        <div className="stat-value">{stats.inStock}</div>
                        <div className="stat-label">Trong kho</div>
                    </div>
                </div>
                <div className="stat-card stat-in-transit">
                    <div className="stat-icon">🚚</div>
                    <div className="stat-info">
                        <div className="stat-value">{stats.inTransit}</div>
                        <div className="stat-label">Đang vận chuyển</div>
                    </div>
                </div>
                <div className="stat-card stat-sold">
                    <div className="stat-icon">💰</div>
                    <div className="stat-info">
                        <div className="stat-value">{stats.sold}</div>
                        <div className="stat-label">Đã bán</div>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-section">
                <div className="filter-row">
                    <div className="filter-group">
                        <label>Tìm kiếm</label>
                        <input
                            type="text"
                            placeholder="Item code, Batch ID, Tên thuốc..."
                            value={filters.searchQuery}
                            onChange={(e) => handleFilterChange('searchQuery', e.target.value)}
                        />
                    </div>
                    <div className="filter-group">
                        <label>Trạng thái</label>
                        <select
                            value={filters.status}
                            onChange={(e) => handleFilterChange('status', e.target.value)}
                        >
                            <option value="">Tất cả</option>
                            <option value="IN_STOCK">Trong kho</option>
                            <option value="SOLD">Đã bán</option>
                            <option value="IN_TRANSIT">Đang vận chuyển</option>
                            <option value="RETURNED">Trả lại</option>
                            <option value="RECALLED">Thu hồi</option>
                        </select>
                    </div>
                    <div className="filter-group">
                        <label>Từ ngày</label>
                        <input
                            type="date"
                            value={filters.fromDate}
                            onChange={(e) => handleFilterChange('fromDate', e.target.value)}
                        />
                    </div>
                    <div className="filter-group">
                        <label>Đến ngày</label>
                        <input
                            type="date"
                            value={filters.toDate}
                            onChange={(e) => handleFilterChange('toDate', e.target.value)}
                        />
                    </div>
                </div>
                <div className="filter-actions">
                    <button className="btn-clear" onClick={handleClearFilters}>
                        Xóa bộ lọc
                    </button>
                    <button className="btn-export" onClick={handleExportCSV}>
                        📊 Export CSV
                    </button>
                </div>
            </div>

            {/* Error Message */}
            {error && <div className="error-message">{error}</div>}

            {/* Loading */}
            {loading ? (
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Đang tải dữ liệu...</p>
                </div>
            ) : (
                <>
                    {/* Item View */}
                    {viewMode === 'item' && (
                        <div className="items-table-container">
                            <div className="table-actions">
                                <label>
                                    <input
                                        type="checkbox"
                                        checked={selectedItems.length === items.length && items.length > 0}
                                        onChange={handleSelectAll}
                                    />
                                    Chọn tất cả ({selectedItems.length} đã chọn)
                                </label>
                            </div>
                            <table className="items-table">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th>Item Code</th>
                                        <th>Batch ID</th>
                                        <th>Tên thuốc</th>
                                        <th>Trạng thái</th>
                                        <th>Vị trí hiện tại</th>
                                        <th>NSX</th>
                                        <th>HSD</th>
                                        <th>Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {items.length === 0 ? (
                                        <tr>
                                            <td colSpan="9" className="no-data">
                                                Không có dữ liệu
                                            </td>
                                        </tr>
                                    ) : (
                                        items.map((item) => (
                                            <tr key={item.itemCode}>
                                                <td>
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedItems.includes(item.itemCode)}
                                                        onChange={() => handleSelectItem(item.itemCode)}
                                                    />
                                                </td>
                                                <td className="item-code">{item.itemCode}</td>
                                                <td>{item.batchId}</td>
                                                <td>{item.drugName || '-'}</td>
                                                <td>
                                                    <span className={`status-badge ${getStatusBadgeClass(item.status)}`}>
                                                        {getStatusText(item.status)}
                                                    </span>
                                                </td>
                                                <td>{item.currentLocation || '-'}</td>
                                                <td>{formatDate(item.manufactureDate)}</td>
                                                <td>{formatDate(item.expiryDate)}</td>
                                                <td>
                                                    <button
                                                        className="btn-view-detail"
                                                        onClick={() => window.open(`/product/verify/${item.itemCode}`, '_blank')}
                                                    >
                                                        Chi tiết
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Batch View */}
                    {viewMode === 'batch' && (
                        <div className="batches-table-container">
                            <table className="batches-table">
                                <thead>
                                    <tr>
                                        <th>Batch ID</th>
                                        <th>Tên thuốc</th>
                                        <th>Số lượng</th>
                                        <th>Đã bán</th>
                                        <th>Còn lại</th>
                                        <th>NSX</th>
                                        <th>HSD</th>
                                        <th>Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {batches.length === 0 ? (
                                        <tr>
                                            <td colSpan="8" className="no-data">
                                                Không có dữ liệu
                                            </td>
                                        </tr>
                                    ) : (
                                        batches.map((batch) => (
                                            <tr key={batch.batchId}>
                                                <td className="batch-id">{batch.batchId}</td>
                                                <td>{batch.drugName}</td>
                                                <td>{batch.totalQuantity || 0}</td>
                                                <td>{batch.soldQuantity || 0}</td>
                                                <td>{(batch.totalQuantity || 0) - (batch.soldQuantity || 0)}</td>
                                                <td>{formatDate(batch.manufactureDate)}</td>
                                                <td>{formatDate(batch.expiryDate)}</td>
                                                <td>
                                                    <button
                                                        className="btn-view-items"
                                                        onClick={() => {
                                                            handleFilterChange('batchId', batch.batchId);
                                                            setViewMode('item');
                                                        }}
                                                    >
                                                        Xem items
                                                    </button>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* Pagination */}
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                disabled={currentPage === 1}
                                onClick={() => setCurrentPage(currentPage - 1)}
                            >
                                ← Trước
                            </button>
                            <span>
                                Trang {currentPage} / {totalPages}
                            </span>
                            <button
                                disabled={currentPage === totalPages}
                                onClick={() => setCurrentPage(currentPage + 1)}
                            >
                                Sau →
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default ItemLevelInventory;

