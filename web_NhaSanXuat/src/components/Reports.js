import React, { useState, useEffect } from 'react';
import { BarChart3, TrendingUp, Package, Truck, Warehouse } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import manufacturerService from '../services/apiService';
import './Reports.css';

const Reports = () => {
  const [reportData, setReportData] = useState({
    monthlyProduction: [],
    totalProduction: 0,
    totalShipped: 0,
    totalExported: 0,
    efficiency: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReportData();
  }, []);

  const fetchReportData = async () => {
    try {
      setLoading(true);
      const [batchesRes, shipmentsRes] = await Promise.all([
        manufacturerService.getBatches(),
        manufacturerService.getShipments()
      ]);

      let totalProd = 0;
      let totalShip = 0;
      const batches = batchesRes.success && batchesRes.data ? batchesRes.data : [];
      const shipments = shipmentsRes.success && shipmentsRes.data ? shipmentsRes.data : [];

      batches.forEach(b => totalProd += parseInt(b.quantity) || 0);
      shipments.forEach(s => totalShip += parseInt(s.quantity) || 0);

      const efficiency = totalProd > 0 ? Math.round((totalShip / totalProd) * 100) : 0;

      // Build last 6 months
      const monthlyData = {};
      const now = new Date();

      for (let i = 5; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const monthNum = d.getMonth() + 1;
        const key = `m${monthNum}`;
        monthlyData[key] = { month: `Tháng ${monthNum}`, production: 0, shipped: 0, year: d.getFullYear(), m: d.getMonth() };
      }

      batches.forEach(b => {
        const d = b.createdAt ? new Date(b.createdAt) : new Date(b.manufactureTimestamp || now);
        const key = `m${d.getMonth() + 1}`;
        if (monthlyData[key]) {
          monthlyData[key].production += parseInt(b.quantity) || 0;
        }
      });

      shipments.forEach(s => {
        const d = s.createdAt ? new Date(s.createdAt) : new Date();
        const key = `m${d.getMonth() + 1}`;
        if (monthlyData[key]) {
          monthlyData[key].shipped += parseInt(s.quantity) || 0;
        }
      });

      const chartData = Object.values(monthlyData).sort((a, b) => {
        if (a.year !== b.year) return a.year - b.year;
        return a.m - b.m;
      });

      // totalExported = shipments that are delivered
      const totalExported = shipments.filter(s =>
        s.status === 'DELIVERED' || s.status === 'delivered'
      ).reduce((sum, s) => sum + (parseInt(s.quantity) || 0), 0);

      setReportData({
        monthlyProduction: chartData,
        totalProduction: totalProd,
        totalShipped: totalShip,
        totalExported,
        efficiency
      });

    } catch (err) {
      console.error('Error fetching report data:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="reports">
      <div className="page-header">
        <h1>
          <BarChart3 className="page-icon" />
          Báo cáo & Thống kê
        </h1>
      </div>

      <div className="metrics-grid">
        <div className="metric-card">
          <Package size={20} className="metric-icon-inline" />
          <div className="metric-label">Tổng sản lượng</div>
          <div className="metric-value">{reportData.totalProduction.toLocaleString()} Hộp</div>
        </div>

        <div className="metric-card">
          <Truck size={20} className="metric-icon-inline" />
          <div className="metric-label">Đã xuất hàng</div>
          <div className="metric-value">{reportData.totalShipped.toLocaleString()} Hộp</div>
        </div>

        <div className="metric-card">
          <Warehouse size={20} className="metric-icon-inline" />
          <div className="metric-label">Đã xuất kho</div>
          <div className="metric-value">{reportData.totalExported.toLocaleString()} Hộp</div>
        </div>

        <div className="metric-card">
          <TrendingUp size={20} className="metric-icon-inline" />
          <div className="metric-label">Hiệu suất xuất hàng</div>
          <div className="metric-value">{reportData.efficiency}%</div>
        </div>
      </div>

      <div className="chart-card">
        <div className="chart-header">
          <h3>Sản lượng và Xuất hàng theo tháng</h3>
          <p>So sánh khối lượng sản xuất với số lượng đã xuất hàng</p>
        </div>
        <div className="chart-container">
          <ResponsiveContainer width="100%" height={320}>
            <BarChart data={reportData.monthlyProduction} barGap={2} barSize={28}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f0f0f0" />
              <XAxis dataKey="month" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                formatter={(value, name) => [
                  value.toLocaleString() + ' hộp',
                  name === 'Sản lượng sản xuất' ? 'Sản lượng sản xuất' : 'Số lượng xuất hàng'
                ]}
              />
              <Legend wrapperStyle={{ fontSize: 13, paddingTop: 8 }} />
              <Bar dataKey="production" fill="#3b82f6" name="Sản lượng sản xuất" radius={[3, 3, 0, 0]} />
              <Bar dataKey="shipped" fill="#22c55e" name="Số lượng xuất hàng" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default Reports;
