import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { 
  Package, 
  AlertTriangle, 
  TrendingUp, 
  Clock, 
  CheckCircle, 
  Calendar,
  ShoppingCart,
  Archive,
  BarChart3
} from 'lucide-react';
import pharmacyService from '../../services/apiService';
import './PharmacyDashboard.css';

function PharmacyDashboard() {
  const [statsData, setStatsData] = useState([]);
  const [incomingShipments, setIncomingShipments] = useState([]);
  const [topSellingProducts, setTopSellingProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);

      // Fetch real data from APIs
      const [inventoryResponse, shipmentResponse] = await Promise.all([
        pharmacyService.getInventory(),
        pharmacyService.getPendingShipments()
      ]);

      // Process inventory data for stats
      const inventory = inventoryResponse.success ? inventoryResponse.data : [];
      const shipments = shipmentResponse.success ? shipmentResponse.data : [];

      const totalProducts = inventory.reduce((sum, item) => sum + item.currentStock, 0);
      const expiringProducts = inventory.filter(item => {
        const expiryDate = new Date(item.expiryDate);
        const thirtyDaysFromNow = new Date();
        thirtyDaysFromNow.setDate(thirtyDaysFromNow.getDate() + 30);
        return expiryDate <= thirtyDaysFromNow;
      }).length;
      const lowStockProducts = inventory.filter(item => item.currentStock <= item.minStock).length;

      setStatsData([
        {
          title: 'Lô hàng đang đến',
          value: shipments.length.toString(),
          icon: Package,
          color: 'bg-blue-500',
          change: `${shipments.length} lô hàng chờ nhận`
        },
        {
          title: 'Sản phẩm trong kho',
          value: totalProducts.toLocaleString(),
          icon: Archive,
          color: 'bg-green-500',
          change: `${inventory.length} loại sản phẩm`
        },
        {
          title: 'Sản phẩm sắp hết hạn',
          value: expiringProducts.toString(),
          icon: AlertTriangle,
          color: 'bg-yellow-500',
          change: 'Cần kiểm tra trong 30 ngày'
        },
        {
          title: 'Sản phẩm sắp hết hàng',
          value: lowStockProducts.toString(),
          icon: TrendingUp,
          color: 'bg-red-500',
          change: 'Cần đặt hàng bổ sung'
        }
      ]);

      setIncomingShipments(shipments.slice(0, 5).map(shipment => ({
        id: shipment.id || shipment.shipmentId,
        distributor: shipment.fromAddress || 'Nhà phân phối',
        expectedDate: shipment.expectedDeliveryDate || shipment.createdAt,
        status: getStatusText(shipment.status),
        items: 1 // Assuming each shipment has 1 batch for now
      })));

      // Use inventory data for top products (by stock quantity)
      const topProducts = inventory
        .sort((a, b) => b.currentStock - a.currentStock)
        .slice(0, 6)
        .map(item => ({
          name: item.name,
          sales: item.currentStock
        }));
      
      setTopSellingProducts(topProducts);

    } catch (error) {
      console.error('Error fetching dashboard data:', error);
      // Set empty data on error
      setStatsData([]);
      setIncomingShipments([]);
      setTopSellingProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const getStatusText = (status) => {
    switch (status?.toLowerCase()) {
      case 'in_transit':
        return 'Đang vận chuyển';
      case 'pending':
        return 'Chuẩn bị giao';
      default:
        return 'Chờ xử lý';
    }
  };

  const getShipmentStatusClass = (status) => {
    switch (status) {
      case 'Đang vận chuyển':
        return 'status-shipping';
      case 'Chuẩn bị giao':
        return 'status-ready';
      case 'Chờ xử lý':
        return 'status-pending';
      default:
        return '';
    }
  };

  if (loading) {
    return (
      <div className="pharmacy-dashboard">
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <p>Đang tải thông tin dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="pharmacy-dashboard">
      <div className="page-header">
        <h1>Dashboard - Hiệu thuốc</h1>
        <p>Tổng quan hoạt động nhận hàng và quản lý tồn kho</p>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        {statsData.map((stat, index) => {
          const IconComponent = stat.icon;
          return (
            <div key={index} className="stat-card">
              <div className="stat-content">
                <div className="stat-header">
                  <h3>{stat.title}</h3>
                  <div className={`stat-icon ${stat.color}`}>
                    <IconComponent size={24} />
                  </div>
                </div>
                <div className="stat-value">{stat.value}</div>
                <div className="stat-change">{stat.change}</div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="dashboard-content">
        {/* Incoming Shipments */}
        <div className="dashboard-section">
          <div className="section-header">
            <h2>
              <Package size={20} />
              Lô hàng sắp đến
            </h2>
          </div>
          <div className="shipments-list">
            {incomingShipments.length === 0 ? (
              <div className="no-data">
                <Package size={48} />
                <p>Không có lô hàng nào đang đến</p>
              </div>
            ) : (
              incomingShipments.map((shipment) => (
                <div key={shipment.id} className="shipment-item">
                  <div className="shipment-info">
                    <h4>#{shipment.id}</h4>
                    <p>{shipment.distributor}</p>
                    <small>{shipment.items} sản phẩm</small>
                  </div>
                  <div className="shipment-details">
                    <span className={`status ${getShipmentStatusClass(shipment.status)}`}>
                      {shipment.status}
                    </span>
                    <div className="expected-date">
                      <Calendar size={14} />
                      <span>{new Date(shipment.expectedDate).toLocaleDateString('vi-VN')}</span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Top Selling Products Chart */}
        <div className="dashboard-section">
          <div className="section-header">
            <h2>
              <ShoppingCart size={20} />
              Sản phẩm hàng đầu (theo tồn kho)
            </h2>
          </div>
          <div className="chart-container">
            {topSellingProducts.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={topSellingProducts}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis 
                    dataKey="name" 
                    tick={{ fontSize: 12 }}
                    angle={-45}
                    textAnchor="end"
                    height={100}
                  />
                  <YAxis />
                  <Tooltip />
                  <Bar dataKey="sales" fill="#3498db" />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="no-data">
                <BarChart3 size={48} />
                <p>Không có dữ liệu để hiển thị</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default PharmacyDashboard;