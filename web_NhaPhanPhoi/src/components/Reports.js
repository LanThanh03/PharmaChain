import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts';
import { TrendingUp, Package, Truck, CheckCircle, Download, Calendar, Filter } from 'lucide-react';
import { distributorService } from '../services/apiService';
import './Reports.css';

const Reports = () => {
    const [loading, setLoading] = useState(true);
    const [reportData, setReportData] = useState(null);
    const [selectedPeriod, setSelectedPeriod] = useState('month');
    const [selectedReport, setSelectedReport] = useState('overview');

    useEffect(() => {
        fetchReportData();
    }, [selectedPeriod]);

    const fetchReportData = async () => {
        try {
            setLoading(true);
            // Mock data - replace with actual API call
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Get real data from API
            const batchesResponse = await distributorService.getBatches();
            const batches = batchesResponse.success ? batchesResponse.data : [];

            const realData = {
                overview: {
                    totalBatches: batches.length,
                    totalShipments: 0, // TODO: Implement shipments API
                    deliveredShipments: 0,
                    pendingShipments: 0,
                    totalValue: batches.reduce((sum, batch) => sum + (batch.quantity * 5000), 0), // Estimate value
                    growth: 0 // TODO: Calculate growth
                },
                monthlyShipments: [
                    { month: 'T1', shipments: 45, delivered: 42, value: 890000000 },
                    { month: 'T2', shipments: 52, delivered: 48, value: 1020000000 },
                    { month: 'T3', shipments: 38, delivered: 35, value: 750000000 },
                    { month: 'T4', shipments: 61, delivered: 58, value: 1200000000 },
                    { month: 'T5', shipments: 49, delivered: 46, value: 980000000 },
                    { month: 'T6', shipments: 55, delivered: 52, value: 1100000000 }
                ],
                statusDistribution: [
                    { name: 'Đã giao hàng', value: 67, color: '#27ae60' },
                    { name: 'Đang vận chuyển', value: 15, color: '#3498db' },
                    { name: 'Chờ xuất kho', value: 7, color: '#f39c12' }
                ],
                topDrugs: batches.map(batch => ({
                    name: batch.drugName,
                    shipments: 1, // TODO: Count actual shipments
                    quantity: batch.quantity
                })).slice(0, 5),
                performanceMetrics: {
                    avgDeliveryTime: 2.3,
                    onTimeDelivery: 94.5,
                    customerSatisfaction: 4.7,
                    damageRate: 0.8
                }
            };

            setReportData(realData);
        } catch (err) {
            console.error('Error fetching report data:', err);
        } finally {
            setLoading(false);
        }
    };

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(value);
    };

    const formatNumber = (value) => {
        return new Intl.NumberFormat('vi-VN').format(value);
    };

    const exportReport = () => {
        // Mock export functionality
        alert('Chức năng xuất báo cáo sẽ được triển khai trong phiên bản tiếp theo.');
    };

    if (loading) {
        return (
            <div className="reports">
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Đang tải dữ liệu báo cáo...</p>
                </div>
            </div>
        );
    }

    if (!reportData) {
        return (
            <div className="reports">
                <div className="error-container">
                    <p>Không thể tải dữ liệu báo cáo</p>
                </div>
            </div>
        );
    }

    return (
        <div className="reports">
            <div className="page-header">
                <h1>
                    <TrendingUp className="page-icon" />
                    Báo cáo & Thống kê
                </h1>
                <p>Theo dõi hiệu suất hoạt động và phân tích dữ liệu kinh doanh</p>
            </div>

            <div className="controls">
                <div className="filter-group">
                    <label>Khoảng thời gian:</label>
                    <select
                        value={selectedPeriod}
                        onChange={(e) => setSelectedPeriod(e.target.value)}
                        className="period-select"
                    >
                        <option value="week">7 ngày qua</option>
                        <option value="month">30 ngày qua</option>
                        <option value="quarter">3 tháng qua</option>
                        <option value="year">12 tháng qua</option>
                    </select>
                </div>

                <div className="filter-group">
                    <label>Loại báo cáo:</label>
                    <select
                        value={selectedReport}
                        onChange={(e) => setSelectedReport(e.target.value)}
                        className="report-select"
                    >
                        <option value="overview">Tổng quan</option>
                        <option value="shipments">Vận chuyển</option>
                        <option value="performance">Hiệu suất</option>
                        <option value="products">Sản phẩm</option>
                    </select>
                </div>

                <button onClick={exportReport} className="btn btn-primary export-btn">
                    <Download size={16} />
                    Xuất báo cáo
                </button>
            </div>

            {/* Overview Cards */}
            <div className="metrics-grid">
                <div className="metric-card">
                    <div className="metric-icon batches">
                        <Package size={24} />
                    </div>
                    <div className="metric-content">
                        <div className="metric-value">{formatNumber(reportData.overview.totalBatches)}</div>
                        <div className="metric-label">Tổng số lô hàng</div>
                        <div className="metric-change positive">
                            +{reportData.overview.growth}% so với kỳ trước
                        </div>
                    </div>
                </div>

                <div className="metric-card">
                    <div className="metric-icon shipments">
                        <Truck size={24} />
                    </div>
                    <div className="metric-content">
                        <div className="metric-value">{formatNumber(reportData.overview.totalShipments)}</div>
                        <div className="metric-label">Tổng số chuyến hàng</div>
                        <div className="metric-change positive">
                            +8.2% so với kỳ trước
                        </div>
                    </div>
                </div>

                <div className="metric-card">
                    <div className="metric-icon delivered">
                        <CheckCircle size={24} />
                    </div>
                    <div className="metric-content">
                        <div className="metric-value">{formatNumber(reportData.overview.deliveredShipments)}</div>
                        <div className="metric-label">Đã giao hàng</div>
                        <div className="metric-change positive">
                            +5.1% so với kỳ trước
                        </div>
                    </div>
                </div>

                <div className="metric-card">
                    <div className="metric-icon value">
                        <TrendingUp size={24} />
                    </div>
                    <div className="metric-content">
                        <div className="metric-value">{formatCurrency(reportData.overview.totalValue)}</div>
                        <div className="metric-label">Tổng giá trị</div>
                        <div className="metric-change positive">
                            +{reportData.overview.growth}% so với kỳ trước
                        </div>
                    </div>
                </div>
            </div>

            {/* Charts Section */}
            <div className="charts-grid">
                {/* Monthly Shipments Chart */}
                <div className="chart-card">
                    <div className="chart-header">
                        <h3>Thống kê vận chuyển theo tháng</h3>
                        <p>So sánh số lượng lô hàng và tỷ lệ giao thành công</p>
                    </div>
                    <div className="chart-container">
                        <ResponsiveContainer width="100%" height={300}>
                            <BarChart data={reportData.monthlyShipments}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="month" />
                                <YAxis />
                                <Tooltip
                                    formatter={(value, name) => [
                                        name === 'shipments' ? value + ' lô hàng' : value + ' đã giao',
                                        name === 'shipments' ? 'Tổng số' : 'Đã giao'
                                    ]}
                                />
                                <Bar dataKey="shipments" fill="#3498db" name="shipments" />
                                <Bar dataKey="delivered" fill="#27ae60" name="delivered" />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Status Distribution Pie Chart */}
                <div className="chart-card">
                    <div className="chart-header">
                        <h3>Phân bố trạng thái lô hàng</h3>
                        <p>Tỷ lệ các trạng thái của lô hàng hiện tại</p>
                    </div>
                    <div className="chart-container">
                        <ResponsiveContainer width="100%" height={300}>
                            <PieChart>
                                <Pie
                                    data={reportData.statusDistribution}
                                    cx="50%"
                                    cy="50%"
                                    labelLine={false}
                                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                                    outerRadius={80}
                                    fill="#8884d8"
                                    dataKey="value"
                                >
                                    {reportData.statusDistribution.map((entry, index) => (
                                        <Cell key={`cell-${index}`} fill={entry.color} />
                                    ))}
                                </Pie>
                                <Tooltip />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Top Products Table */}
            <div className="table-card">
                <div className="table-header">
                    <h3>Top sản phẩm được vận chuyển</h3>
                    <p>Danh sách các sản phẩm có số lượng vận chuyển cao nhất</p>
                </div>
                <div className="table-container">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Sản phẩm</th>
                                <th>Số lô hàng</th>
                                <th>Tổng số lượng</th>
                                <th>Tỷ lệ</th>
                            </tr>
                        </thead>
                        <tbody>
                            {reportData.topDrugs.map((drug, index) => (
                                <tr key={index}>
                                    <td className="product-name">{drug.name}</td>
                                    <td>{drug.shipments}</td>
                                    <td>{formatNumber(drug.quantity)} hộp</td>
                                    <td>
                                        <div className="progress-bar">
                                            <div
                                                className="progress-fill"
                                                style={{
                                                    width: `${(drug.shipments / reportData.topDrugs[0].shipments) * 100}%`
                                                }}
                                            ></div>
                                            <span className="progress-text">
                                                {((drug.shipments / reportData.overview.totalShipments) * 100).toFixed(1)}%
                                            </span>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Performance Metrics */}
            <div className="performance-grid">
                <div className="performance-card">
                    <div className="performance-icon">
                        <Calendar size={32} />
                    </div>
                    <div className="performance-content">
                        <div className="performance-value">{reportData.performanceMetrics.avgDeliveryTime} ngày</div>
                        <div className="performance-label">Thời gian giao hàng trung bình</div>
                    </div>
                </div>

                <div className="performance-card">
                    <div className="performance-icon">
                        <CheckCircle size={32} />
                    </div>
                    <div className="performance-content">
                        <div className="performance-value">{reportData.performanceMetrics.onTimeDelivery}%</div>
                        <div className="performance-label">Tỷ lệ giao hàng đúng hạn</div>
                    </div>
                </div>

                <div className="performance-card">
                    <div className="performance-icon">
                        <TrendingUp size={32} />
                    </div>
                    <div className="performance-content">
                        <div className="performance-value">{reportData.performanceMetrics.customerSatisfaction}/5</div>
                        <div className="performance-label">Điểm hài lòng khách hàng</div>
                    </div>
                </div>

                <div className="performance-card">
                    <div className="performance-icon">
                        <Package size={32} />
                    </div>
                    <div className="performance-content">
                        <div className="performance-value">{reportData.performanceMetrics.damageRate}%</div>
                        <div className="performance-label">Tỷ lệ hàng hóa hư hỏng</div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Reports;
