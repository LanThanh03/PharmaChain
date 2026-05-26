import React, { useState, useEffect } from 'react';
import { 
  Users, 
  Package, 
  Activity, 
  ShieldCheck, 
  Server,
  AlertTriangle
} from 'lucide-react';
import { adminClient } from '../services/authService';
import './Dashboard.css';

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalBatches: 0,
    activeNodes: 0,
    networkStatus: 'Loading...'
  });

  useEffect(() => {
    // Gọi API thật từ Backend sử dụng adminClient (đã có Token)
    adminClient.get('/dashboard')
      .then(response => {
        if (response.data) {
           setStats(prev => ({...prev, ...response.data}));
        }
      })
      .catch(err => {
        console.error("Error fetching stats", err);
        setStats(prev => ({...prev, networkStatus: 'Error'}));
      });
  }, []);

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h2>Tổng quan Hệ Thống (Mạng Sepolia)</h2>
        <div className="status-badge healthy">
          <Server size={16} />
          <span>Network {stats.networkStatus}</span>
        </div>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper blue">
            <Users size={24} className="stat-icon" />
          </div>
          <div className="stat-content">
            <p className="stat-label">Doanh nghiệp tham gia</p>
            <h3 className="stat-value">{stats.totalUsers}</h3>
            <p className="stat-trend positive">+3 trong tháng này</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper purple">
            <Package size={24} className="stat-icon" />
          </div>
          <div className="stat-content">
            <p className="stat-label">Tổng Lô thuốc quản lý</p>
            <h3 className="stat-value">{stats.totalBatches}</h3>
            <p className="stat-trend positive">+12 tuần này</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper green">
            <Activity size={24} className="stat-icon" />
          </div>
          <div className="stat-content">
            <p className="stat-label">Smart Contract Nodes</p>
            <h3 className="stat-value">{stats.activeNodes}</h3>
            <p className="stat-trend neutral">Đang đồng bộ (Synced)</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper orange">
            <ShieldCheck size={24} className="stat-icon" />
          </div>
          <div className="stat-content">
            <p className="stat-label">Bảo mật (Roles)</p>
            <h3 className="stat-value">100%</h3>
            <p className="stat-trend positive">Đã phân quyền On-chain</p>
          </div>
        </div>
      </div>

      <div className="dashboard-main-content">
        <div className="chart-section">
          <h3>Lưu lượng Giao dịch (7 ngày gần nhất)</h3>
          <div className="placeholder-chart">
            {/* Simple mock chart visualization */}
            <div className="bar" style={{height: '40%'}}><span>T2</span></div>
            <div className="bar" style={{height: '60%'}}><span>T3</span></div>
            <div className="bar" style={{height: '35%'}}><span>T4</span></div>
            <div className="bar" style={{height: '80%'}}><span>T5</span></div>
            <div className="bar" style={{height: '90%'}}><span>T6</span></div>
            <div className="bar" style={{height: '50%'}}><span>T7</span></div>
            <div className="bar" style={{height: '70%'}}><span>CN</span></div>
          </div>
        </div>
        
        <div className="alerts-section">
          <h3>Cảnh báo Hệ thống</h3>
          <div className="alert-list">
             <div className="alert-item warning">
               <AlertTriangle size={20} />
               <div>
                 <p className="alert-title">Gas Fee biến động</p>
                 <p className="alert-desc">Phí gas mạng Sepolia đang ở mức cao (25 Gwei).</p>
               </div>
             </div>
             <div className="alert-item success">
               <ShieldCheck size={20} />
               <div>
                 <p className="alert-title">Bản vá bảo mật</p>
                 <p className="alert-desc">Smart contract mới nhất đã được verify trên Blockscout.</p>
               </div>
             </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
