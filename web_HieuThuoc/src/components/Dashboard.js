import React, { useState, useEffect } from 'react';
import {
  Package,
  ShoppingCart,
  TrendingUp,
  AlertCircle,
  Activity,
  Clock,
  CheckCircle,
  AlertTriangle,
  BarChart3
} from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import pharmacyService from '../services/apiService';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({
    pendingReceive: 0,       // Lô hàng đang chờ nhận
    totalInventory: 0,       // Tổng số sản phẩm trong kho  
    lowStockItems: 0,        // Sản phẩm sắp hết hàng
    expiringItems: 0         // Sản phẩm sắp hết hạn
  });

  const [recentActivities, setRecentActivities] = useState([]);
  const [inventoryData, setInventoryData] = useState([]);
  const [salesData, setSalesData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        // Get real dashboard data from API
        const dashboardResponse = await pharmacyService.getDashboardStats();
        if (dashboardResponse.success) {
          setStats(dashboardResponse.data);

          // Update inventory distribution based on real data
          const inventory = dashboardResponse.data.inventory || [];
          const categoryStats = {};
          inventory.forEach(item => {
            if (!categoryStats[item.category]) {
              categoryStats[item.category] = 0;
            }
            categoryStats[item.category] += item.currentStock;
          });

          const inventoryData = Object.entries(categoryStats).map(([category, qty], index) => {
            const quantity = Number.isFinite(qty) ? qty : parseFloat(qty) || 0;
            return {
              category,
              quantity,
              color: ['#3498db', '#27ae60', '#f39c12', '#e74c3c', '#9b59b6', '#95a5a6'][index % 6]
            };
          });

          setInventoryData(inventoryData);
        } else {
          setStats({
            pendingReceive: 0,
            totalInventory: 0,
            lowStockItems: 0,
            expiringItems: 0
          });
          setInventoryData([]);
        }

        // Initialize empty activities and sales data
        setRecentActivities([]);
        setSalesData([]);

      } catch (err) {
        console.error('Error fetching dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const StatCard = ({ title, value, icon: Icon, color, change, changeType, alert }) => (
    <div className={`stat-card stat-card-${color} ${alert ? 'alert-card' : ''}`}>
      <div className="stat-header">
        <div className={`stat-icon stat-icon-${color}`}>
          <Icon size={24} />
        </div>
        <div className="stat-value">
          <span className="stat-number">{typeof value === 'number' ? value.toLocaleString() : value}</span>
          {change && (
            <span className={`stat-change stat-change-${changeType}`}>
              <TrendingUp size={16} />
              {change}%
            </span>
          )}
        </div>
      </div>
      <div className="stat-title">{title}</div>
      {alert && (
        <div className="stat-alert">
          <AlertCircle size={14} />
          Cần chú ý!
        </div>
      )}
    </div>
  );

  const formatTimeAgo = (timestamp) => {
    const now = new Date();
    const diff = now - timestamp;
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));

    if (minutes < 60) {
      return `${minutes} phút trước`;
    } else {
      return `${hours} giờ trước`;
    }
  };

  const formatCurrency = (value) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="loading">
          <Activity className="loading-spinner" />
          Đang tải dữ liệu...
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      {/* Welcome Section */}
      <div className="welcome-section">
        <h2>Chào mừng đến Pharmacy Portal! 🏥</h2>
        <p>Tổng quan tình hình nhập hàng và tồn kho hiệu thuốc hôm nay</p>
      </div>

      {/* Stats Grid - Theo yêu cầu Hiệu thuốc */}
      <div className="stats-grid">
        <StatCard
          title="Chờ nhận từ NPP"
          value={stats.pendingReceive}
          icon={Clock}
          color="blue"
          change={5}
          changeType="positive"
        />
        <StatCard
          title="Tồn kho (sản phẩm)"
          value={stats.totalInventory}
          icon={Package}
          color="green"
          change={8}
          changeType="positive"
        />
        <StatCard
          title="Sắp hết hàng"
          value={stats.lowStockItems}
          icon={AlertTriangle}
          color="orange"
          alert={stats.lowStockItems > 10}
        />
        <StatCard
          title="Sắp hết hạn"
          value={stats.expiringItems}
          icon={AlertCircle}
          color="red"
          alert={stats.expiringItems > 0}
        />
      </div>

      {/* Charts Section */}
      <div className="charts-section">
        <div className="chart-card">
          <div className="chart-header">
            <h3>Doanh số bán hàng tuần qua</h3>
            <p>Theo dõi xu hướng bán hàng và doanh thu hàng ngày</p>
          </div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={salesData || []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip
                  formatter={(value, name) => [
                    name === 'sales' ? value + ' đơn' : formatCurrency(value),
                    name === 'sales' ? 'Số đơn' : 'Doanh thu'
                  ]}
                />
                <Bar dataKey="sales" fill="#3498db" name="sales" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="chart-card">
          <div className="chart-header">
            <h3>Phân bố tồn kho theo danh mục</h3>
            <p>Tỷ lệ các loại thuốc hiện có trong kho</p>
          </div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                {(Array.isArray(inventoryData) && inventoryData.length > 0) && (
                  <Pie
                    data={inventoryData || []}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ category, percent }) => category ? `${category}: ${(percent * 100).toFixed(0)}%` : ''}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="quantity"
                    nameKey="category"
                  >
                    {(inventoryData || []).map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                )}
                <Tooltip formatter={(value) => [value.toLocaleString() + ' hộp', 'Tồn kho']} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Recent Activities */}
      <div className="activities-section">
        <div className="activities-header">
          <h3>Hoạt động gần đây</h3>
          <p>Theo dõi các hoạt động nhập hàng, bán hàng và xác thực mới nhất</p>
        </div>

        <div className="activities-list">
          {recentActivities.length === 0 ? (
            <div className="no-activities">
              <AlertCircle size={48} />
              <p>Chưa có hoạt động nào gần đây</p>
            </div>
          ) : (
            recentActivities.map((activity) => {
              const Icon = activity.icon;
              return (
                <div key={activity.id} className="activity-item">
                  <div className={`activity-icon activity-${activity.type}`}>
                    <Icon size={20} />
                  </div>
                  <div className="activity-content">
                    <div className="activity-message">{activity.message}</div>
                    <div className="activity-time">{formatTimeAgo(activity.timestamp)}</div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="quick-actions">
        <h3>Thao tác nhanh</h3>
        <div className="action-buttons">
          <button className="action-btn action-btn-primary">
            <ShoppingCart size={20} />
            Nhận hàng mới
          </button>
          <button className="action-btn action-btn-secondary">
            <Package size={20} />
            Kiểm tra kho
          </button>
          <button className="action-btn action-btn-accent">
            <CheckCircle size={20} />
            Xác thực sản phẩm
          </button>
          <button className="action-btn action-btn-neutral">
            <BarChart3 size={20} />
            Xem báo cáo
          </button>
        </div>
      </div>

      {/* Alerts Section */}
      {(stats.lowStockItems > 0 || stats.expiringItems > 0) && (
        <div className="alerts-section">
          <h3>Cảnh báo quan trọng</h3>
          <div className="alerts-list">
            {stats.lowStockItems > 0 && (
              <div className="alert alert-warning">
                <AlertTriangle size={20} />
                <div>
                  <strong>Sắp hết hàng:</strong> Có {stats.lowStockItems} sản phẩm sắp hết hàng, cần nhập thêm.
                </div>
              </div>
            )}
            {stats.expiringItems > 0 && (
              <div className="alert alert-danger">
                <AlertCircle size={20} />
                <div>
                  <strong>Sắp hết hạn:</strong> Có {stats.expiringItems} sản phẩm sắp hết hạn sử dụng.
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
