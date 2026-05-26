import React, { useState, useEffect } from 'react';
import {
  Package,
  Search,
  AlertTriangle,
  AlertCircle,
  CheckCircle,
  Eye,
  Edit,
  BarChart3,
  Download
} from 'lucide-react';
import pharmacyService from '../services/apiService';
import './InventoryManagement.css';

const InventoryManagement = () => {
  const [inventory, setInventory] = useState([]);
  const [filteredInventory, setFilteredInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterCategory, setFilterCategory] = useState('all');
  const [sortBy, setSortBy] = useState('name');
  const [sortOrder, setSortOrder] = useState('asc');
  const [selectedItem, setSelectedItem] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [adjustmentData, setAdjustmentData] = useState({ type: 'add', quantity: 0, reason: '' });

  const categories = [
    'Giảm đau hạ sốt',
    'Kháng sinh',
    'Vitamin & KCS',
    'Thuốc tim mạch',
    'Thuốc tiêu hóa',
    'Thuốc da liễu',
    'Thuốc mắt tai mũi họng',
    'Khác'
  ];

  useEffect(() => {
    fetchInventory();
  }, []);

  useEffect(() => {
    applyFiltersAndSort();
  }, [inventory, searchTerm, filterStatus, filterCategory, sortBy, sortOrder]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchInventory = async () => {
    try {
      setLoading(true);

      // Get real inventory data from API
      const response = await pharmacyService.getInventory();
      if (response.success) {
        setInventory(response.data);
      } else {
        console.error('Failed to fetch inventory:', response.message);
        setInventory([]);
      }
    } catch (error) {
      console.error('Error fetching inventory:', error);
      setInventory([]);
    } finally {
      setLoading(false);
    }
  };


  const applyFiltersAndSort = () => {
    let filtered = [...inventory];

    // Search filter
    if (searchTerm) {
      filtered = filtered.filter(item =>
        item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.batchCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.manufacturer.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Status filter
    if (filterStatus !== 'all') {
      filtered = filtered.filter(item => item.status === filterStatus);
    }

    // Category filter
    if (filterCategory !== 'all') {
      filtered = filtered.filter(item => item.category === filterCategory);
    }

    // Sort
    filtered.sort((a, b) => {
      let aValue = a[sortBy];
      let bValue = b[sortBy];

      if (typeof aValue === 'string') {
        aValue = aValue.toLowerCase();
        bValue = bValue.toLowerCase();
      }

      if (sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    setFilteredInventory(filtered);
  };

  const getStatusBadge = (item) => {
    const now = new Date();
    const expiryDate = new Date(item.expiryDate);
    const daysToExpiry = Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24));

    if (item.currentStock <= item.minStock) {
      return { type: 'low_stock', label: 'Sắp hết', icon: AlertTriangle, color: 'orange' };
    } else if (daysToExpiry <= 90) {
      return { type: 'expiring_soon', label: 'Sắp hết hạn', icon: AlertCircle, color: 'red' };
    } else {
      return { type: 'good', label: 'Tốt', icon: CheckCircle, color: 'green' };
    }
  };

  const handleAdjustStock = async () => {
    if (!selectedItem || !adjustmentData.quantity || !adjustmentData.reason) {
      alert('Vui lòng điền đầy đủ thông tin');
      return;
    }

    try {
      const newQuantity = adjustmentData.type === 'add'
        ? selectedItem.currentStock + parseInt(adjustmentData.quantity)
        : selectedItem.currentStock - parseInt(adjustmentData.quantity);

      if (newQuantity < 0) {
        alert('Số lượng không thể âm');
        return;
      }

      // Update inventory
      const updatedInventory = inventory.map(item =>
        item.id === selectedItem.id
          ? {
            ...item,
            currentStock: newQuantity,
            totalValue: newQuantity * item.unitPrice,
            lastUpdated: new Date().toISOString()
          }
          : item
      );

      setInventory(updatedInventory);
      setShowAdjustModal(false);
      setAdjustmentData({ type: 'add', quantity: 0, reason: '' });

      alert(`Đã ${adjustmentData.type === 'add' ? 'thêm' : 'trừ'} ${adjustmentData.quantity} hộp thành công!`);
    } catch (error) {
      alert('Lỗi khi điều chỉnh kho: ' + error.message);
    }
  };

  const exportInventory = () => {
    const csvContent = [
      'Tên sản phẩm,Mã lô,Danh mục,NSX,Tồn kho,Giá,Tổng giá trị,Hạn sử dụng,Vị trí,Trạng thái',
      ...filteredInventory.map(item => [
        item.name,
        item.batchCode,
        item.category,
        item.manufacturer,
        item.currentStock,
        item.unitPrice,
        item.totalValue,
        new Date(item.expiryDate).toLocaleDateString('vi-VN'),
        item.location,
        getStatusBadge(item).label
      ].join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `pharmacy_inventory_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  if (loading) {
    return (
      <div className="inventory-management">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <div className="loading-text">Đang tải dữ liệu kho từ blockchain...</div>
          <div className="loading-subtitle">Vui lòng chờ trong giây lát</div>
        </div>
      </div>
    );
  }

  return (
    <div className="inventory-management">
      <div className="page-header">
        <h1>
          <Package className="page-icon" />
          Quản lý Kho
        </h1>
        <p>Xem và quản lý sản phẩm đã xác thực trong kho hiệu thuốc</p>
      </div>

      {/* Summary Stats */}
      <div className="inventory-stats">
        <div className="stat-card">
          <div className="stat-number">{inventory.length}</div>
          <div className="stat-label">Tổng mặt hàng</div>
        </div>
        <div className="stat-card">
          <div className="stat-number">{inventory.reduce((sum, item) => sum + item.currentStock, 0).toLocaleString()}</div>
          <div className="stat-label">Tổng số lượng</div>
        </div>
        <div className="stat-card warning">
          <div className="stat-number">{inventory.filter(item => item.currentStock <= item.minStock).length}</div>
          <div className="stat-label">Sắp hết hàng</div>
        </div>
        <div className="stat-card danger">
          <div className="stat-number">
            {inventory.filter(item => {
              const daysToExpiry = Math.ceil((new Date(item.expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
              return daysToExpiry <= 90;
            }).length}
          </div>
          <div className="stat-label">Sắp hết hạn</div>
        </div>
      </div>

      {/* Controls */}
      <div className="inventory-controls">
        <div className="search-filter-section">
          <div className="search-box">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Tìm kiếm sản phẩm, mã lô, NSX..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="filter-section">
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="filter-select"
            >
              <option value="all">Tất cả trạng thái</option>
              <option value="good">Tốt</option>
              <option value="low_stock">Sắp hết hàng</option>
              <option value="expiring_soon">Sắp hết hạn</option>
            </select>

            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="filter-select"
            >
              <option value="all">Tất cả danh mục</option>
              {categories.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>

            <select
              value={`${sortBy}-${sortOrder}`}
              onChange={(e) => {
                const [field, order] = e.target.value.split('-');
                setSortBy(field);
                setSortOrder(order);
              }}
              className="filter-select"
            >
              <option value="name-asc">Tên A-Z</option>
              <option value="name-desc">Tên Z-A</option>
              <option value="currentStock-asc">Tồn kho thấp → cao</option>
              <option value="currentStock-desc">Tồn kho cao → thấp</option>
              <option value="expiryDate-asc">Hết hạn sớm → muộn</option>
              <option value="expiryDate-desc">Hết hạn muộn → sớm</option>
            </select>
          </div>
        </div>

        <div className="action-buttons">
          <button onClick={exportInventory} className="btn btn-outline">
            <Download size={16} />
            Xuất Excel
          </button>
          <button className="btn btn-primary">
            <BarChart3 size={16} />
            Báo cáo
          </button>
        </div>
      </div>

      {/* Inventory Table */}
      <div className="inventory-table-container">
        <table className="inventory-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Mã lô</th>
              <th>Danh mục</th>
              <th>Tồn kho</th>
              <th>Giá/hộp</th>
              <th>Tổng giá trị</th>
              <th>Hạn sử dụng</th>
              <th>Vị trí</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {filteredInventory.length === 0 ? (
              <tr>
                <td colSpan="10" className="no-data">
                  <div className="empty-state">
                    <Package size={64} className="empty-icon" />
                    <h3>Không có sản phẩm nào</h3>
                    <p>
                      {searchTerm || filterStatus !== 'all' || filterCategory !== 'all'
                        ? 'Không tìm thấy sản phẩm phù hợp với bộ lọc'
                        : 'Kho hiện tại chưa có sản phẩm nào. Vui lòng nhập hàng từ nhà phân phối.'
                      }
                    </p>
                    {(searchTerm || filterStatus !== 'all' || filterCategory !== 'all') && (
                      <button
                        onClick={() => {
                          setSearchTerm('');
                          setFilterStatus('all');
                          setFilterCategory('all');
                        }}
                        className="btn btn-secondary"
                      >
                        Xóa bộ lọc
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ) : (
              filteredInventory.map(item => {
                const status = getStatusBadge(item);
                const StatusIcon = status.icon;

                return (
                  <tr key={item.id}>
                    <td className="product-name">
                      <div>
                        <strong>{item.name}</strong>
                        <div className="manufacturer">{item.manufacturer}</div>
                      </div>
                    </td>
                    <td className="batch-code">{item.batchCode}</td>
                    <td className="category">{item.category}</td>
                    <td className="stock">
                      <div className="stock-info">
                        <span className="current">{item.currentStock.toLocaleString()}</span>
                        <span className="range">({item.minStock} - {item.maxStock})</span>
                      </div>
                    </td>
                    <td className="unit-price">{formatCurrency(item.unitPrice)}</td>
                    <td className="total-value">{formatCurrency(item.totalValue)}</td>
                    <td className="expiry">{formatDate(item.expiryDate)}</td>
                    <td className="location">{item.location}</td>
                    <td className="status">
                      <span className={`status-badge status-${status.color}`}>
                        <StatusIcon size={14} />
                        {status.label}
                      </span>
                    </td>
                    <td className="actions">
                      <button
                        onClick={() => {
                          setSelectedItem(item);
                          setShowDetailModal(true);
                        }}
                        className="action-btn"
                        title="Xem chi tiết"
                      >
                        <Eye size={16} />
                      </button>
                      <button
                        onClick={() => {
                          setSelectedItem(item);
                          setShowAdjustModal(true);
                        }}
                        className="action-btn"
                        title="Điều chỉnh tồn kho"
                      >
                        <Edit size={16} />
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Detail Modal */}
      {showDetailModal && selectedItem && (
        <div className="modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Chi tiết sản phẩm</h3>
              <button onClick={() => setShowDetailModal(false)} className="modal-close">×</button>
            </div>
            <div className="modal-body">
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="label">Tên sản phẩm:</span>
                  <span className="value">{selectedItem.name}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Mã lô:</span>
                  <span className="value">{selectedItem.batchCode}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Nhà sản xuất:</span>
                  <span className="value">{selectedItem.manufacturer}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Danh mục:</span>
                  <span className="value">{selectedItem.category}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Tồn kho hiện tại:</span>
                  <span className="value">{selectedItem.currentStock.toLocaleString()} hộp</span>
                </div>
                <div className="detail-item">
                  <span className="label">Mức tồn kho:</span>
                  <span className="value">Tối thiểu: {selectedItem.minStock} | Tối đa: {selectedItem.maxStock}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Ngày sản xuất:</span>
                  <span className="value">{formatDate(selectedItem.manufactureDate)}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Hạn sử dụng:</span>
                  <span className="value">{formatDate(selectedItem.expiryDate)}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Vị trí:</span>
                  <span className="value">{selectedItem.location}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Điều kiện bảo quản:</span>
                  <span className="value">{selectedItem.storageConditions}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Adjust Stock Modal */}
      {showAdjustModal && selectedItem && (
        <div className="modal-overlay" onClick={() => setShowAdjustModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Điều chỉnh tồn kho - {selectedItem.name}</h3>
              <button onClick={() => setShowAdjustModal(false)} className="modal-close">×</button>
            </div>
            <div className="modal-body">
              <div className="current-stock">
                <strong>Tồn kho hiện tại: {selectedItem.currentStock.toLocaleString()} hộp</strong>
              </div>

              <div className="adjust-form">
                <div className="form-group">
                  <label>Loại điều chỉnh:</label>
                  <select
                    value={adjustmentData.type}
                    onChange={(e) => setAdjustmentData({ ...adjustmentData, type: e.target.value })}
                  >
                    <option value="add">Thêm vào kho</option>
                    <option value="subtract">Trừ khỏi kho</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Số lượng:</label>
                  <input
                    type="number"
                    min="1"
                    value={adjustmentData.quantity}
                    onChange={(e) => setAdjustmentData({ ...adjustmentData, quantity: e.target.value })}
                    placeholder="Nhập số lượng"
                  />
                </div>

                <div className="form-group">
                  <label>Lý do:</label>
                  <textarea
                    value={adjustmentData.reason}
                    onChange={(e) => setAdjustmentData({ ...adjustmentData, reason: e.target.value })}
                    placeholder="Nhập lý do điều chỉnh"
                    rows="3"
                  />
                </div>

                {adjustmentData.quantity > 0 && (
                  <div className="preview">
                    <strong>
                      Tồn kho sau điều chỉnh: {' '}
                      {adjustmentData.type === 'add'
                        ? selectedItem.currentStock + parseInt(adjustmentData.quantity || 0)
                        : selectedItem.currentStock - parseInt(adjustmentData.quantity || 0)
                      } hộp
                    </strong>
                  </div>
                )}
              </div>
            </div>
            <div className="modal-actions">
              <button onClick={() => setShowAdjustModal(false)} className="btn btn-secondary">
                Hủy
              </button>
              <button onClick={handleAdjustStock} className="btn btn-primary">
                Xác nhận điều chỉnh
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InventoryManagement;
