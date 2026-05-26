import React, { useState, useEffect } from "react"
import {
  Users,
  Building,
  Edit,
  Save,
  X,
  Plus,
  Trash2,
  Shield,
  Mail,
  Phone,
  Calendar,
  FileText,
  Key,
} from "lucide-react"
import pharmacyService from "../services/apiService"
import "./AccountManagement.css"

const AccountManagement = () => {
  const [activeTab, setActiveTab] = useState("pharmacy")
  const [pharmacyInfo, setPharmacyInfo] = useState({})
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [editingPharmacy, setEditingPharmacy] = useState(false)
  const [editingEmployee, setEditingEmployee] = useState(null)
  const [showAddEmployee, setShowAddEmployee] = useState(false)
  const [showChangePassword, setShowChangePassword] = useState(false)
  const [newEmployee, setNewEmployee] = useState({
    name: "",
    email: "",
    phone: "",
    role: "staff",
    position: "",
    licenseNumber: "",
  })
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  })

  const tabs = [
    {
      id: "pharmacy",
      name: "Thông tin hiệu thuốc",
      icon: Building,
    },
    // {
    //   id: 'employees',
    //   name: 'Quản lý nhân viên',
    //   icon: Users
    // }
  ]

  const roles = [
    { value: "manager", label: "Quản lý", color: "blue" },
    { value: "pharmacist", label: "Dược sĩ", color: "green" },
    { value: "staff", label: "Nhân viên", color: "gray" },
  ]

  useEffect(() => {
    fetchAccountData()
  }, [])

  const fetchAccountData = async () => {
    try {
      setLoading(true)

      // Get real pharmacy info from API
      const pharmaciesResponse = await pharmacyService.getPharmacies()
      let pharmacyInfo = {}

      if (pharmaciesResponse.success && pharmaciesResponse.data.length > 0) {
        // Use first pharmacy as current pharmacy
        const pharmacy = pharmaciesResponse.data[0]
        pharmacyInfo = {
          name: pharmacy.name,
          address: pharmacy.address,
          phone: pharmacy.phone || "N/A",
          email: pharmacy.email || "N/A",
          website: pharmacy.website || "N/A",
          licenseNumber: pharmacy.licenseNumber || "N/A",
          licenseIssueDate: pharmacy.licenseIssueDate
            ? pharmacy.licenseIssueDate.split("T")[0]
            : "N/A",
          licenseExpiryDate: pharmacy.licenseExpiryDate
            ? pharmacy.licenseExpiryDate.split("T")[0]
            : "N/A",
          taxCode: pharmacy.taxCode || "N/A",
          manager: pharmacy.manager || "N/A",
          establishedYear: pharmacy.establishedYear || "N/A",
          description: pharmacy.description || "N/A",
        }
      } else {
        // Fallback pharmacy info
        pharmacyInfo = {
          name: "Long Châu",
          address: "379-381 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM",
          phone: "1800 6928",
          email: "cskh@nhathuoclongchau.com",
          website: "https://nhathuoclongchau.com",
          licenseNumber: "GPL-2024-003",
          licenseIssueDate: "2024-01-15",
          licenseExpiryDate: "2027-01-14",
          taxCode: "0123456789",
          manager: "Dược sĩ Hoàng Văn E",
          establishedYear: "2007",
          description:
            "Hệ thống nhà thuốc Long Châu - Uy tín hàng đầu Việt Nam với hơn 1000 cửa hàng trên toàn quốc.",
        }
      }

      // Fetch employees
      const mockEmployees = [
        {
          id: 1,
          name: "Dược sĩ Nguyễn Văn A",
          email: "manager@abc-pharmacy.com",
          phone: "0987654321",
          role: "manager",
          position: "Quản lý hiệu thuốc",
          licenseNumber: "DS-2024-001",
          joinDate: "2020-01-15",
          status: "active",
          avatar: null,
        },
        {
          id: 2,
          name: "Dược sĩ Trần Thị B",
          email: "pharmacist@abc-pharmacy.com",
          phone: "0976543210",
          role: "pharmacist",
          position: "Dược sĩ tư vấn",
          licenseNumber: "DS-2024-002",
          joinDate: "2021-03-10",
          status: "active",
          avatar: null,
        },
        {
          id: 3,
          name: "Lê Văn C",
          email: "staff1@abc-pharmacy.com",
          phone: "0965432109",
          role: "staff",
          position: "Nhân viên bán hàng",
          licenseNumber: "",
          joinDate: "2022-06-20",
          status: "active",
          avatar: null,
        },
        {
          id: 4,
          name: "Phạm Thị D",
          email: "staff2@abc-pharmacy.com",
          phone: "0954321098",
          role: "staff",
          position: "Nhân viên kho",
          licenseNumber: "",
          joinDate: "2023-02-15",
          status: "active",
          avatar: null,
        },
      ]

      setPharmacyInfo(pharmacyInfo)
      setEmployees(mockEmployees)
    } catch (error) {
      console.error("Error fetching account data:", error)
    } finally {
      setLoading(false)
    }
  }

  const handleSavePharmacy = async () => {
    try {
      const companyId = localStorage.getItem("companyId") || "3"
      await pharmacyService.updateCompanyInfo(companyId, pharmacyInfo)

      // ✅ CẬP NHẬT NGAY LẬP TỨC vào LocalStorage để Header/Sidebar hiển thị tên mới
      const userStr = localStorage.getItem("pharmacy_user")
      if (userStr) {
        const user = JSON.parse(userStr)
        const updatedUser = {
          ...user,
          companyName: pharmacyInfo.name,
          name: pharmacyInfo.manager || user.name,
        }
        localStorage.setItem("pharmacy_user", JSON.stringify(updatedUser))

        // Dispatch custom event để các component khác (như Layout) biết mà cập nhật
        window.dispatchEvent(new Event("storage"))

        // Force reload nhẹ để cập nhật UI nếu context không bắt được event
        if (
          window.confirm(
            "Cập nhật thành công! Trang web sẽ tải lại để áp dụng tên mới.",
          )
        ) {
          window.location.reload()
        }
      } else {
        setEditingPharmacy(false)
        alert("Đã cập nhật thông tin hiệu thuốc thành công!")
      }
    } catch (error) {
      alert("Lỗi khi cập nhật thông tin: " + error.message)
    }
  }

  const handleAddEmployee = async () => {
    if (!newEmployee.name || !newEmployee.email || !newEmployee.phone) {
      alert("Vui lòng điền đầy đủ thông tin bắt buộc")
      return
    }

    try {
      const employee = {
        id: employees.length + 1,
        ...newEmployee,
        joinDate: new Date().toISOString().split("T")[0],
        status: "active",
        avatar: null,
      }

      setEmployees([...employees, employee])
      setNewEmployee({
        name: "",
        email: "",
        phone: "",
        role: "staff",
        position: "",
        licenseNumber: "",
      })
      setShowAddEmployee(false)
      alert("Đã thêm nhân viên thành công!")
    } catch (error) {
      alert("Lỗi khi thêm nhân viên: " + error.message)
    }
  }

  const handleUpdateEmployee = async (employeeId, updatedData) => {
    try {
      const updatedEmployees = employees.map(emp =>
        emp.id === employeeId ? { ...emp, ...updatedData } : emp,
      )
      setEmployees(updatedEmployees)
      setEditingEmployee(null)
      alert("Đã cập nhật thông tin nhân viên thành công!")
    } catch (error) {
      alert("Lỗi khi cập nhật nhân viên: " + error.message)
    }
  }

  const handleDeleteEmployee = async employeeId => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa nhân viên này?")) {
      return
    }

    try {
      const updatedEmployees = employees.filter(emp => emp.id !== employeeId)
      setEmployees(updatedEmployees)
      alert("Đã xóa nhân viên thành công!")
    } catch (error) {
      alert("Lỗi khi xóa nhân viên: " + error.message)
    }
  }

  const handleChangePassword = async () => {
    if (
      !passwordData.currentPassword ||
      !passwordData.newPassword ||
      !passwordData.confirmPassword
    ) {
      alert("Vui lòng điền đầy đủ thông tin")
      return
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      alert("Mật khẩu xác nhận không khớp")
      return
    }

    if (passwordData.newPassword.length < 6) {
      alert("Mật khẩu mới phải có ít nhất 6 ký tự")
      return
    }

    try {
      // Mock API call
      alert("Đã đổi mật khẩu thành công!")
      setPasswordData({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      })
      setShowChangePassword(false)
    } catch (error) {
      alert("Lỗi khi đổi mật khẩu: " + error.message)
    }
  }

  const getRoleInfo = role => {
    return roles.find(r => r.value === role) || roles[2]
  }

  const formatDate = dateString => {
    return new Date(dateString).toLocaleDateString("vi-VN")
  }

  const PharmacyInfoTab = () => (
    <div className="tab-content">
      <div className="section-header">
        <h3>
          <Building size={20} />
          Thông tin hiệu thuốc
        </h3>
        <div className="header-actions">
          {editingPharmacy ? (
            <>
              <button
                onClick={handleSavePharmacy}
                className="btn btn-success">
                <Save size={16} />
                Lưu
              </button>
              <button
                onClick={() => setEditingPharmacy(false)}
                className="btn btn-secondary">
                <X size={16} />
                Hủy
              </button>
            </>
          ) : (
            <button
              onClick={() => setEditingPharmacy(true)}
              className="btn btn-primary">
              <Edit size={16} />
              Chỉnh sửa
            </button>
          )}
        </div>
      </div>

      <div className="pharmacy-info-grid">
        <div className="info-section">
          <h4>Thông tin cơ bản</h4>
          <div className="form-grid">
            <div className="form-group">
              <label>Tên hiệu thuốc *</label>
              <input
                type="text"
                value={pharmacyInfo.name || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, name: e.target.value })
                }
                disabled={!editingPharmacy}
                required
              />
            </div>
            <div className="form-group">
              <label>Địa chỉ *</label>
              <textarea
                value={pharmacyInfo.address || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, address: e.target.value })
                }
                disabled={!editingPharmacy}
                rows="2"
                required
              />
            </div>
            <div className="form-group">
              <label>Số điện thoại *</label>
              <input
                type="tel"
                value={pharmacyInfo.phone || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, phone: e.target.value })
                }
                disabled={!editingPharmacy}
                required
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input
                type="email"
                value={pharmacyInfo.email || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, email: e.target.value })
                }
                disabled={!editingPharmacy}
              />
            </div>
            <div className="form-group">
              <label>Website</label>
              <input
                type="url"
                value={pharmacyInfo.website || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, website: e.target.value })
                }
                disabled={!editingPharmacy}
              />
            </div>
            <div className="form-group">
              <label>Người quản lý</label>
              <input
                type="text"
                value={pharmacyInfo.manager || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, manager: e.target.value })
                }
                disabled={!editingPharmacy}
              />
            </div>
          </div>
        </div>

        <div className="info-section">
          <h4>Thông tin pháp lý</h4>
          <div className="form-grid">
            <div className="form-group">
              <label>Số giấy phép kinh doanh *</label>
              <input
                type="text"
                value={pharmacyInfo.licenseNumber || ""}
                onChange={e =>
                  setPharmacyInfo({
                    ...pharmacyInfo,
                    licenseNumber: e.target.value,
                  })
                }
                disabled={!editingPharmacy}
                required
              />
            </div>
            <div className="form-group">
              <label>Ngày cấp</label>
              <input
                type="date"
                value={pharmacyInfo.licenseIssueDate || ""}
                onChange={e =>
                  setPharmacyInfo({
                    ...pharmacyInfo,
                    licenseIssueDate: e.target.value,
                  })
                }
                disabled={!editingPharmacy}
              />
            </div>
            <div className="form-group">
              <label>Ngày hết hạn</label>
              <input
                type="date"
                value={pharmacyInfo.licenseExpiryDate || ""}
                onChange={e =>
                  setPharmacyInfo({
                    ...pharmacyInfo,
                    licenseExpiryDate: e.target.value,
                  })
                }
                disabled={!editingPharmacy}
              />
            </div>
            <div className="form-group">
              <label>Mã số thuế</label>
              <input
                type="text"
                value={pharmacyInfo.taxCode || ""}
                onChange={e =>
                  setPharmacyInfo({ ...pharmacyInfo, taxCode: e.target.value })
                }
                disabled={!editingPharmacy}
              />
            </div>
            <div className="form-group">
              <label>Năm thành lập</label>
              <input
                type="number"
                value={pharmacyInfo.establishedYear || ""}
                onChange={e =>
                  setPharmacyInfo({
                    ...pharmacyInfo,
                    establishedYear: e.target.value,
                  })
                }
                disabled={!editingPharmacy}
                min="1900"
                max={new Date().getFullYear()}
              />
            </div>
          </div>
        </div>

        <div className="info-section full-width">
          <h4>Mô tả</h4>
          <div className="form-group">
            <textarea
              value={pharmacyInfo.description || ""}
              onChange={e =>
                setPharmacyInfo({
                  ...pharmacyInfo,
                  description: e.target.value,
                })
              }
              disabled={!editingPharmacy}
              rows="4"
              placeholder="Mô tả về hiệu thuốc..."
            />
          </div>
        </div>
      </div>

      {/* Security Section */}
      <div className="security-section">
        <h4>
          <Shield size={20} />
          Bảo mật
        </h4>
        <button
          onClick={() => setShowChangePassword(true)}
          className="btn btn-outline">
          <Key size={16} />
          Đổi mật khẩu
        </button>
      </div>

      {/* Change Password Modal */}
      {showChangePassword && (
        <div
          className="modal-overlay"
          onClick={() => setShowChangePassword(false)}>
          <div
            className="modal-content"
            onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Đổi mật khẩu</h3>
              <button
                onClick={() => setShowChangePassword(false)}
                className="modal-close">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label>Mật khẩu hiện tại *</label>
                <input
                  type="password"
                  value={passwordData.currentPassword}
                  onChange={e =>
                    setPasswordData({
                      ...passwordData,
                      currentPassword: e.target.value,
                    })
                  }
                  required
                />
              </div>
              <div className="form-group">
                <label>Mật khẩu mới *</label>
                <input
                  type="password"
                  value={passwordData.newPassword}
                  onChange={e =>
                    setPasswordData({
                      ...passwordData,
                      newPassword: e.target.value,
                    })
                  }
                  minLength="6"
                  required
                />
              </div>
              <div className="form-group">
                <label>Xác nhận mật khẩu mới *</label>
                <input
                  type="password"
                  value={passwordData.confirmPassword}
                  onChange={e =>
                    setPasswordData({
                      ...passwordData,
                      confirmPassword: e.target.value,
                    })
                  }
                  minLength="6"
                  required
                />
              </div>
            </div>
            <div className="modal-actions">
              <button
                onClick={() => setShowChangePassword(false)}
                className="btn btn-secondary">
                Hủy
              </button>
              <button
                onClick={handleChangePassword}
                className="btn btn-primary">
                Đổi mật khẩu
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )

  const EmployeesTab = () => (
    <div className="tab-content">
      <div className="section-header">
        <h3>
          <Users size={20} />
          Quản lý nhân viên ({employees.length})
        </h3>
        <button
          onClick={() => setShowAddEmployee(true)}
          className="btn btn-primary">
          <Plus size={16} />
          Thêm nhân viên
        </button>
      </div>

      <div className="employees-grid">
        {employees.map(employee => {
          const roleInfo = getRoleInfo(employee.role)
          const isEditing = editingEmployee === employee.id

          return (
            <div
              key={employee.id}
              className="employee-card">
              <div className="employee-header">
                <div className="employee-avatar">
                  <Users size={32} />
                </div>
                <div className="employee-basic">
                  <h4>{employee.name}</h4>
                  <span className={`role-badge role-${roleInfo.color}`}>
                    {roleInfo.label}
                  </span>
                </div>
                <div className="employee-actions">
                  {isEditing ? (
                    <>
                      <button
                        onClick={() =>
                          handleUpdateEmployee(employee.id, employee)
                        }
                        className="action-btn save">
                        <Save size={16} />
                      </button>
                      <button
                        onClick={() => setEditingEmployee(null)}
                        className="action-btn cancel">
                        <X size={16} />
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => setEditingEmployee(employee.id)}
                        className="action-btn edit">
                        <Edit size={16} />
                      </button>
                      <button
                        onClick={() => handleDeleteEmployee(employee.id)}
                        className="action-btn delete">
                        <Trash2 size={16} />
                      </button>
                    </>
                  )}
                </div>
              </div>

              <div className="employee-details">
                <div className="detail-item">
                  <Mail size={16} />
                  {isEditing ? (
                    <input
                      type="email"
                      value={employee.email}
                      onChange={e => {
                        const updatedEmployees = employees.map(emp =>
                          emp.id === employee.id
                            ? { ...emp, email: e.target.value }
                            : emp,
                        )
                        setEmployees(updatedEmployees)
                      }}
                    />
                  ) : (
                    <span>{employee.email}</span>
                  )}
                </div>

                <div className="detail-item">
                  <Phone size={16} />
                  {isEditing ? (
                    <input
                      type="tel"
                      value={employee.phone}
                      onChange={e => {
                        const updatedEmployees = employees.map(emp =>
                          emp.id === employee.id
                            ? { ...emp, phone: e.target.value }
                            : emp,
                        )
                        setEmployees(updatedEmployees)
                      }}
                    />
                  ) : (
                    <span>{employee.phone}</span>
                  )}
                </div>

                <div className="detail-item">
                  <FileText size={16} />
                  {isEditing ? (
                    <input
                      type="text"
                      value={employee.position}
                      onChange={e => {
                        const updatedEmployees = employees.map(emp =>
                          emp.id === employee.id
                            ? { ...emp, position: e.target.value }
                            : emp,
                        )
                        setEmployees(updatedEmployees)
                      }}
                    />
                  ) : (
                    <span>{employee.position}</span>
                  )}
                </div>

                {employee.licenseNumber && (
                  <div className="detail-item">
                    <Shield size={16} />
                    {isEditing ? (
                      <input
                        type="text"
                        value={employee.licenseNumber}
                        onChange={e => {
                          const updatedEmployees = employees.map(emp =>
                            emp.id === employee.id
                              ? { ...emp, licenseNumber: e.target.value }
                              : emp,
                          )
                          setEmployees(updatedEmployees)
                        }}
                      />
                    ) : (
                      <span>Số chứng chỉ: {employee.licenseNumber}</span>
                    )}
                  </div>
                )}

                <div className="detail-item">
                  <Calendar size={16} />
                  <span>Gia nhập: {formatDate(employee.joinDate)}</span>
                </div>
              </div>
            </div>
          )
        })}
      </div>

      {/* Add Employee Modal */}
      {showAddEmployee && (
        <div
          className="modal-overlay"
          onClick={() => setShowAddEmployee(false)}>
          <div
            className="modal-content large"
            onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Thêm nhân viên mới</h3>
              <button
                onClick={() => setShowAddEmployee(false)}
                className="modal-close">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-group">
                  <label>Họ và tên *</label>
                  <input
                    type="text"
                    value={newEmployee.name}
                    onChange={e =>
                      setNewEmployee({ ...newEmployee, name: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Email *</label>
                  <input
                    type="email"
                    value={newEmployee.email}
                    onChange={e =>
                      setNewEmployee({ ...newEmployee, email: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Số điện thoại *</label>
                  <input
                    type="tel"
                    value={newEmployee.phone}
                    onChange={e =>
                      setNewEmployee({ ...newEmployee, phone: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="form-group">
                  <label>Chức vụ</label>
                  <select
                    value={newEmployee.role}
                    onChange={e =>
                      setNewEmployee({ ...newEmployee, role: e.target.value })
                    }>
                    {roles.map(role => (
                      <option
                        key={role.value}
                        value={role.value}>
                        {role.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Vị trí công việc</label>
                  <input
                    type="text"
                    value={newEmployee.position}
                    onChange={e =>
                      setNewEmployee({
                        ...newEmployee,
                        position: e.target.value,
                      })
                    }
                  />
                </div>
                <div className="form-group">
                  <label>Số chứng chỉ hành nghề</label>
                  <input
                    type="text"
                    value={newEmployee.licenseNumber}
                    onChange={e =>
                      setNewEmployee({
                        ...newEmployee,
                        licenseNumber: e.target.value,
                      })
                    }
                  />
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button
                onClick={() => setShowAddEmployee(false)}
                className="btn btn-secondary">
                Hủy
              </button>
              <button
                onClick={handleAddEmployee}
                className="btn btn-primary">
                Thêm nhân viên
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )

  if (loading) {
    return (
      <div className="account-management">
        <div className="loading">Đang tải thông tin tài khoản...</div>
      </div>
    )
  }

  return (
    <div className="account-management">
      <div className="page-header">
        <h1>
          <Users className="page-icon" />
          Quản lý Tài khoản
        </h1>
        {/* <p>Thông tin hiệu thuốc và quản lý nhân viên</p> */}
        <p>Thông tin hiệu thuốc</p>
      </div>

      {/* Tab Navigation */}
      <div className="tab-navigation">
        {tabs.map(tab => {
          const Icon = tab.icon
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`tab-btn ${activeTab === tab.id ? "active" : ""}`}>
              <Icon size={20} />
              {tab.name}
            </button>
          )
        })}
      </div>

      {/* Tab Content */}
      {activeTab === "pharmacy" && <PharmacyInfoTab />}
      {activeTab === "employees" && <EmployeesTab />}
    </div>
  )
}

export default AccountManagement
