import React, { useState, useEffect } from "react"
import {
  Users,
  Shield,
  Search,
  CheckCircle,
  XCircle,
  ExternalLink,
  Filter,
  RefreshCw,
  Plus,
  X
} from "lucide-react"
import { adminClient } from "../services/authService"
import "./AccountManagement.css"

const AccountManagement = () => {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [filterRole, setFilterRole] = useState("ALL")
  const [showModal, setShowModal] = useState(false)
  const [newUser, setNewUser] = useState({
    email: "",
    password: "",
    name: "",
    companyName: "",
    role: "MANUFACTURER",
    walletAddress: ""
  })

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    try {
      setLoading(true)
      const response = await adminClient.get("/users/all")
      if (Array.isArray(response.data)) {
        setUsers(response.data)
      }
    } catch (err) {
      console.error("Error fetching users:", err)
    } finally {
      setLoading(false)
    }
  }

  const toggleVerify = async (id, role) => {
    try {
      const response = await adminClient.post(`/users/verify/${id}?role=${role}`)
      if (response.data?.success) {
        setUsers(users.map(u => u.id === id ? { ...u, status: response.data.isVerified ? "Verified" : "Pending" } : u))
      }
    } catch (err) {
      console.error("Error toggling verify:", err)
    }
  }

  const handleCreateUser = async (e) => {
    e.preventDefault()
    try {
      const response = await adminClient.post("/users/create", newUser)
      if (response.data?.success) {
        setShowModal(false)
        fetchUsers()
        setNewUser({ email: "", password: "", name: "", companyName: "", role: "MANUFACTURER", walletAddress: "" })
      }
    } catch (err) {
      console.error("Error creating user:", err)
    }
  }

  const filteredUsers = users.filter(user => {
    const matchesSearch = (user.name || "").toLowerCase().includes(searchTerm.toLowerCase()) || 
                         (user.company || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (user.email || "").toLowerCase().includes(searchTerm.toLowerCase())
    const matchesRole = filterRole === "ALL" || user.role === filterRole
    return matchesSearch && matchesRole
  })

  return (
    <div className="admin-page-container">
      <div className="page-header">
        <div className="header-title">
          <h1>
            <Shield className="page-icon" />
            Quản lý Phân quyền Doanh nghiệp
          </h1>
          <p>Khởi tạo và phê duyệt quyền truy cập cho các đơn vị trong mạng lưới</p>
        </div>
        <div className="header-actions">
          <button className="btn-secondary" onClick={fetchUsers} disabled={loading}>
            <RefreshCw size={18} className={loading ? "spin" : ""} />
            Làm mới
          </button>
          <button className="btn-primary" onClick={() => setShowModal(true)}>
            <Plus size={18} />
            Thêm Doanh nghiệp mới
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="filters-card">
        <div className="search-box">
          <Search size={20} color="#6b7280" />
          <input 
            type="text" 
            placeholder="Tìm kiếm theo tên, email, công ty..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        
        <div className="filter-group">
          <Filter size={18} color="#6b7280" />
          <select value={filterRole} onChange={(e) => setFilterRole(e.target.value)}>
            <option value="ALL">Tất cả vai trò</option>
            <option value="MANUFACTURER">Nhà sản xuất</option>
            <option value="DISTRIBUTOR">Nhà phân phối</option>
            <option value="PHARMACY">Hiệu thuốc</option>
          </select>
        </div>
      </div>

      {/* Users Table */}
      <div className="table-card">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Đơn vị / Đại diện</th>
              <th>Vai trò</th>
              <th>Địa chỉ Ví (Blockchain)</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5" style={{textAlign: 'center', padding: '40px'}}>Đang tải dữ liệu...</td></tr>
            ) : filteredUsers.length === 0 ? (
              <tr><td colSpan="5" style={{textAlign: 'center', padding: '40px'}}>Không tìm thấy doanh nghiệp nào</td></tr>
            ) : filteredUsers.map(user => (
              <tr key={user.id}>
                <td>
                  <div className="user-info">
                    <span className="user-company">{user.company || "Cá nhân"}</span>
                    <span className="user-email">{user.email}</span>
                  </div>
                </td>
                <td>
                  <span className={`role-badge role-${user.role.toLowerCase()}`}>
                    {user.role === 'MANUFACTURER' ? 'NSX' : user.role === 'DISTRIBUTOR' ? 'NPP' : 'Hiệu thuốc'}
                  </span>
                </td>
                <td className="wallet-cell">
                  <code>{user.wallet ? `${user.wallet.substring(0,6)}...${user.wallet.substring(38)}` : "Chưa có ví"}</code>
                </td>
                <td>
                  <span className={`status-badge status-${user.status.toLowerCase()}`}>
                    {user.status === 'Verified' ? <CheckCircle size={14} /> : <XCircle size={14} />}
                    {user.status === 'Verified' ? 'Đã duyệt' : 'Chờ duyệt'}
                  </span>
                </td>
                <td>
                  <div className="action-buttons">
                    <button 
                      className={`btn-action ${user.status === 'Verified' ? 'btn-revoke' : 'btn-approve'}`}
                      onClick={() => toggleVerify(user.id, user.role)}
                    >
                      {user.status === 'Verified' ? 'Thu hồi' : 'Phê duyệt'}
                    </button>
                    {user.wallet && (
                       <a href={`https://sepolia.etherscan.io/address/${user.wallet}`} target="_blank" rel="noopener noreferrer" className="btn-icon">
                        <ExternalLink size={18} />
                      </a>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal Thêm mới */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Thêm mới Doanh nghiệp</h3>
              <button className="close-btn" onClick={() => setShowModal(false)}><X /></button>
            </div>
            <form onSubmit={handleCreateUser}>
              <div className="form-grid">
                <div className="form-group">
                  <label>Tên Doanh nghiệp / Hiệu thuốc</label>
                  <input required type="text" value={newUser.companyName} onChange={e => setNewUser({...newUser, companyName: e.target.value})} placeholder="VD: Dược Hậu Giang" />
                </div>
                <div className="form-group">
                  <label>Người đại diện</label>
                  <input required type="text" value={newUser.name} onChange={e => setNewUser({...newUser, name: e.target.value})} placeholder="VD: Nguyễn Văn A" />
                </div>
                <div className="form-group">
                  <label>Email đăng nhập</label>
                  <input required type="email" value={newUser.email} onChange={e => setNewUser({...newUser, email: e.target.value})} placeholder="email@company.com" />
                </div>
                <div className="form-group">
                  <label>Mật khẩu khởi tạo</label>
                  <input required type="password" value={newUser.password} onChange={e => setNewUser({...newUser, password: e.target.value})} />
                </div>
                <div className="form-group">
                  <label>Vai trò hệ thống</label>
                  <select value={newUser.role} onChange={e => setNewUser({...newUser, role: e.target.value})}>
                    <option value="MANUFACTURER">Nhà sản xuất (NSX)</option>
                    <option value="DISTRIBUTOR">Nhà phân phối (NPP)</option>
                    <option value="PHARMACY">Hiệu thuốc (HT)</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Địa chỉ Ví Blockchain (Ví MetaMask)</label>
                  <input required type="text" value={newUser.walletAddress} onChange={e => setNewUser({...newUser, walletAddress: e.target.value})} placeholder="0x..." />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-cancel" onClick={() => setShowModal(false)}>Hủy</button>
                <button type="submit" className="btn-save">Khởi tạo Tài khoản</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default AccountManagement
