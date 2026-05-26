import React, { useState, useEffect } from "react"
import {
  Package,
  Calendar,
  MapPin,
  Search,
  Filter,
  Download,
  Eye,
  TrendingUp,
  TrendingDown,
} from "lucide-react"
import { distributorService } from "../services/apiService"
import "./ExportManagement.css"

const ExportManagement = () => {
  const [shipments, setShipments] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [filterStatus, setFilterStatus] = useState("all")
  const [dateRange, setDateRange] = useState({ from: "", to: "" })

  useEffect(() => {
    fetchExportHistory()
  }, [])

  const fetchExportHistory = async () => {
    try {
      setLoading(true)
      const walletAddress = localStorage.getItem("walletAddress")

      if (!walletAddress) {
        console.error("Không tìm thấy địa chỉ ví")
        return
      }

      // Lấy danh sách shipments đã gửi đi
      const response =
        await distributorService.getShipmentsBySender(walletAddress)

      if (response.success && response.data) {
        console.log("📦 Export history:", response.data)
        setShipments(response.data)
      }
    } catch (error) {
      console.error("❌ Error fetching export history:", error)
    } finally {
      setLoading(false)
    }
  }

  // Filter shipments
  const filteredShipments = shipments.filter(shipment => {
    // Search filter
    const matchesSearch =
      searchTerm === "" ||
      shipment.shipmentCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      shipment.drugBatch?.drugName
        ?.toLowerCase()
        .includes(searchTerm.toLowerCase()) ||
      shipment.toCompany?.name?.toLowerCase().includes(searchTerm.toLowerCase())

    // Status filter
    const matchesStatus =
      filterStatus === "all" || shipment.status === filterStatus

    // Date range filter
    let matchesDate = true
    if (dateRange.from && dateRange.to) {
      const shipmentDate = new Date(shipment.shipmentDate)
      matchesDate =
        shipmentDate >= new Date(dateRange.from) &&
        shipmentDate <= new Date(dateRange.to)
    }

    return matchesSearch && matchesStatus && matchesDate
  })

  // Statistics
  const stats = {
    total: shipments.length,
    pending: shipments.filter(s => s.status === "PENDING").length,
    inTransit: shipments.filter(s => s.status === "IN_TRANSIT").length,
    delivered: shipments.filter(s => s.status === "DELIVERED").length,
    cancelled: shipments.filter(s => s.status === "CANCELLED").length,
  }

  const getStatusBadge = status => {
    const badges = {
      PENDING: { text: "Chờ xử lý", class: "status-pending" },
      IN_TRANSIT: { text: "Đang vận chuyển", class: "status-in-transit" },
      DELIVERED: { text: "Đã giao", class: "status-delivered" },
      CANCELLED: { text: "Đã hủy", class: "status-cancelled" },
    }
    return badges[status] || { text: status, class: "status-unknown" }
  }

  const formatDate = dateString => {
    if (!dateString) return "N/A"
    const date = new Date(dateString)
    return date.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  return (
    <div className="export-management">
      <div className="page-header">
        <div className="header-content">
          <h1>Quản lý Xuất kho</h1>
          <p>Theo dõi lịch sử xuất kho và vận chuyển thuốc</p>
        </div>
        <button
          className="btn-refresh"
          onClick={fetchExportHistory}>
          <Download size={18} />
          Làm mới
        </button>
      </div>

      {/* Statistics Cards */}
      <div className="stats-grid">
        <div className="stat-card total">
          <div className="stat-icon">
            <Package size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Tổng đơn xuất</span>
            <span className="stat-value">{stats.total}</span>
          </div>
        </div>

        <div className="stat-card pending">
          <div className="stat-icon">
            <Calendar size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Chờ xử lý</span>
            <span className="stat-value">{stats.pending}</span>
          </div>
        </div>

        <div className="stat-card in-transit">
          <div className="stat-icon">
            <TrendingUp size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Đang vận chuyển</span>
            <span className="stat-value">{stats.inTransit}</span>
          </div>
        </div>

        <div className="stat-card delivered">
          <div className="stat-icon">
            <TrendingDown size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Đã giao</span>
            <span className="stat-value">{stats.delivered}</span>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="filters-section">
        <div className="search-box">
          <Search size={20} />
          <input
            type="text"
            placeholder="Tìm kiếm theo mã đơn, tên thuốc, nhà thuốc..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="filter-group">
          <Filter size={20} />
          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}>
            <option value="all">Tất cả trạng thái</option>
            <option value="PENDING">Chờ xử lý</option>
            <option value="IN_TRANSIT">Đang vận chuyển</option>
            <option value="DELIVERED">Đã giao</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>

        <div className="date-range">
          <input
            type="date"
            value={dateRange.from}
            onChange={e => setDateRange({ ...dateRange, from: e.target.value })}
            placeholder="Từ ngày"
          />
          <span>đến</span>
          <input
            type="date"
            value={dateRange.to}
            onChange={e => setDateRange({ ...dateRange, to: e.target.value })}
            placeholder="Đến ngày"
          />
        </div>
      </div>

      {/* Shipments Table */}
      <div className="shipments-table-container">
        {loading ? (
          <div className="loading-state">
            <div className="spinner"></div>
            <p>Đang tải dữ liệu...</p>
          </div>
        ) : filteredShipments.length === 0 ? (
          <div className="empty-state">
            <Package size={48} />
            <p>Không có đơn xuất kho nào</p>
          </div>
        ) : (
          <table className="shipments-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Tên thuốc</th>
                <th>Số lô</th>
                <th>Số lượng</th>
                <th>Nơi nhận</th>
                <th>Ngày xuất</th>
                <th>Ngày giao dự kiến</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {filteredShipments.map(shipment => {
                const status = getStatusBadge(shipment.status)
                return (
                  <tr key={shipment.id}>
                    <td>
                      <span
                        className="shipment-code"
                        onClick={() =>
                          navigator.clipboard
                            .writeText(shipment.shipmentCode)
                            .then(() => alert("Copy thành công"))
                        }>
                        {shipment.shipmentCode}
                      </span>
                    </td>
                    <td>
                      <div className="drug-info">
                        <strong>{shipment.drugBatch?.drugName || "N/A"}</strong>
                        <small>{shipment.drugBatch?.manufacturer || ""}</small>
                      </div>
                    </td>
                    <td>
                      <div className="batch-id-cell">
                        <span
                          className="batch-id-main"
                          onClick={() => {
                            const batchNumber = shipment.drugBatch?.batchNumber
                            if (batchNumber) {
                              navigator.clipboard.writeText(String(batchNumber))
                              alert(
                                "Đã copy Số lô: " +
                                  batchNumber +
                                  "\n\nSố lô này dùng để truy vết thuốc từ đầu đến cuối.",
                              )
                            }
                          }}
                          title="Click để copy Số lô - Mã này giữ nguyên từ NSX đến Hiệu thuốc"
                          style={{
                            cursor: "pointer",
                            color: "#28a745",
                            fontWeight: "bold",
                          }}>
                          📦 {shipment.drugBatch?.batchNumber || "N/A"}
                        </span>
                        <small
                          className="batch-number-sub"
                          style={{
                            display: "block",
                            color: "#666",
                            fontSize: "0.8em",
                          }}>
                          (Blockchain ID: {shipment.drugBatch?.batchId || "N/A"}
                          )
                        </small>
                      </div>
                    </td>
                    <td>
                      <span className="quantity">{shipment.quantity} hộp</span>
                    </td>
                    <td>
                      <div className="company-info">
                        <MapPin size={14} />
                        <span>{shipment.toCompany?.name || "N/A"}</span>
                      </div>
                    </td>
                    <td>{formatDate(shipment.shipmentDate)}</td>
                    <td>{formatDate(shipment.expectedDeliveryDate)}</td>
                    <td>
                      <span className={`status-badge ${status.class}`}>
                        {status.text}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn-action"
                        onClick={() =>
                          (window.location.href = `/shipment-tracking?id=${shipment.id}`)
                        }>
                        <Eye size={16} />
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

export default ExportManagement
