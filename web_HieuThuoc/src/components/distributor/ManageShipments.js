import React, { useState, useEffect } from 'react';
import { Search, Filter, Eye, Calendar, MapPin, Package, AlertTriangle } from 'lucide-react';
import pharmacyService from '../../services/apiService';
import './ManageShipments.css';

function ManageShipments() {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [dateFilter, setDateFilter] = useState('all');
  const [selectedShipment, setSelectedShipment] = useState(null);

  // Real data from API
  const [shipments, setShipments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchShipments();
  }, []);

  const fetchShipments = async () => {
    try {
      setLoading(true);
      setError(null);

      // Get shipments from API
      const response = await pharmacyService.getAllShipments();

      if (response.success && response.data) {
        // Transform API data to component format
        const transformedData = response.data.map(shipment => ({
          id: shipment.shipmentCode || shipment.id,
          pharmacy: shipment.toCompanyName || 'Hiệu thuốc',
          pharmacyAddress: shipment.toAddress || 'N/A',
          createdDate: shipment.shipmentDate || shipment.createdAt,
          deliveryDate: shipment.expectedDeliveryDate || shipment.createdAt,
          status: mapShipmentStatus(shipment.shipmentStatus),
          items: shipment.items || [],
          transportMethod: shipment.transportMethod || 'Xe tải',
          notes: shipment.notes || ''
        }));

        setShipments(transformedData);
      } else {
        setShipments([]);
      }
    } catch (err) {
      console.error('Error fetching shipments:', err);
      setError('Không thể tải dữ liệu lô hàng');
      setShipments([]);
    } finally {
      setLoading(false);
    }
  };

  const mapShipmentStatus = (status) => {
    const statusMap = {
      'PENDING': 'shipping',
      'IN_TRANSIT': 'shipping',
      'DELIVERED': 'delivered',
      'CANCELLED': 'delayed'
    };
    return statusMap[status?.toUpperCase()] || 'shipping';
  };

  const getStatusDisplay = (status) => {
    const statusMap = {
      shipping: { label: 'Đang vận chuyển', class: 'status-shipping' },
      delivered: { label: 'Đã giao thành công', class: 'status-delivered' },
      delayed: { label: 'Trễ hẹn', class: 'status-delayed' }
    };
    return statusMap[status] || { label: status, class: '' };
  };

  const filteredShipments = shipments.filter(shipment => {
    const matchesSearch = shipment.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         shipment.pharmacy.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'all' || shipment.status === statusFilter;

    let matchesDate = true;
    if (dateFilter !== 'all') {
      const today = new Date();
      const shipmentDate = new Date(shipment.createdDate);
      const diffTime = Math.abs(today - shipmentDate);
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

      switch (dateFilter) {
        case 'today':
          matchesDate = diffDays <= 1;
          break;
        case 'week':
          matchesDate = diffDays <= 7;
          break;
        case 'month':
          matchesDate = diffDays <= 30;
          break;
        default:
          matchesDate = true;
      }
    }

    return matchesSearch && matchesStatus && matchesDate;
  });

  const handleViewDetails = (shipment) => {
    setSelectedShipment(shipment);
  };

  const closeModal = () => {
    setSelectedShipment(null);
  };

  if (loading) {
    return (
      <div className="manage-shipments">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Đang tải dữ liệu lô hàng...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="manage-shipments">
        <div className="error-container">
          <AlertTriangle size={48} />
          <p>{error}</p>
          <button className="btn btn-primary" onClick={fetchShipments}>Thử lại</button>
        </div>
      </div>
    );
  }

  return (
    <div className="manage-shipments">
      <div className="page-header">
        <h1>Quản lý Lô hàng</h1>
        <p>Theo dõi và quản lý tất cả các lô hàng đã gửi</p>
      </div>

      <div className="shipments-container">
        {/* Filters */}
        <div className="filters-section">
          <div className="search-wrapper">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Tìm kiếm theo mã lô hàng hoặc tên hiệu thuốc..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input search-input"
            />
          </div>

          <div className="filter-group">
            <div className="filter-item">
              <label htmlFor="status-filter">Trạng thái:</label>
              <select
                id="status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="input filter-select"
              >
                <option value="all">Tất cả</option>
                <option value="shipping">Đang vận chuyển</option>
                <option value="delivered">Đã giao</option>
                <option value="delayed">Trễ hẹn</option>
              </select>
            </div>

            <div className="filter-item">
              <label htmlFor="date-filter">Thời gian:</label>
              <select
                id="date-filter"
                value={dateFilter}
                onChange={(e) => setDateFilter(e.target.value)}
                className="input filter-select"
              >
                <option value="all">Tất cả</option>
                <option value="today">Hôm nay</option>
                <option value="week">7 ngày qua</option>
                <option value="month">30 ngày qua</option>
              </select>
            </div>
          </div>
        </div>

        {/* Shipments Table */}
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Mã lô hàng</th>
                <th>Người nhận</th>
                <th>Ngày tạo</th>
                <th>Ngày giao</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredShipments.map(shipment => {
                const statusInfo = getStatusDisplay(shipment.status);
                return (
                  <tr key={shipment.id}>
                    <td className="shipment-id">{shipment.id}</td>
                    <td>
                      <div className="pharmacy-cell">
                        <div className="pharmacy-name">{shipment.pharmacy}</div>
                        <div className="pharmacy-address">{shipment.pharmacyAddress}</div>
                      </div>
                    </td>
                    <td>{new Date(shipment.createdDate).toLocaleDateString('vi-VN')}</td>
                    <td>{new Date(shipment.deliveryDate).toLocaleDateString('vi-VN')}</td>
                    <td>
                      <span className={`status-badge ${statusInfo.class}`}>
                        {statusInfo.label}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary btn-small"
                        onClick={() => handleViewDetails(shipment)}
                      >
                        <Eye />
                        Xem chi tiết
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {filteredShipments.length === 0 && (
            <div className="empty-state">
              <Package className="empty-icon" />
              <h3>Không tìm thấy lô hàng nào</h3>
              <p>Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</p>
            </div>
          )}
        </div>
      </div>

      {/* Detail Modal */}
      {selectedShipment && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Chi tiết lô hàng {selectedShipment.id}</h2>
              <button className="modal-close" onClick={closeModal}>×</button>
            </div>

            <div className="modal-body">
              <div className="detail-section">
                <h3>Thông tin giao hàng</h3>
                <div className="detail-grid">
                  <div className="detail-item">
                    <MapPin className="detail-icon" />
                    <div>
                      <strong>Người nhận:</strong>
                      <p>{selectedShipment.pharmacy}</p>
                      <p>{selectedShipment.pharmacyAddress}</p>
                    </div>
                  </div>
                  <div className="detail-item">
                    <Calendar className="detail-icon" />
                    <div>
                      <strong>Thời gian:</strong>
                      <p>Tạo: {new Date(selectedShipment.createdDate).toLocaleDateString('vi-VN')}</p>
                      <p>Giao: {new Date(selectedShipment.deliveryDate).toLocaleDateString('vi-VN')}</p>
                    </div>
                  </div>
                </div>
                <div className="detail-item">
                  <strong>Phương tiện vận chuyển:</strong> {selectedShipment.transportMethod}
                </div>
                {selectedShipment.notes && (
                  <div className="detail-item">
                    <strong>Ghi chú:</strong> {selectedShipment.notes}
                  </div>
                )}
              </div>

              <div className="detail-section">
                <h3>Danh sách sản phẩm</h3>
                <div className="products-list">
                  {selectedShipment.items.map((item, index) => (
                    <div key={index} className="product-item">
                      <Package className="product-icon" />
                      <div className="product-info">
                        <h4>{item.name}</h4>
                        <p>Số lượng: {item.quantity}</p>
                        <p>Số lô: {item.batchNumber}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ManageShipments;



