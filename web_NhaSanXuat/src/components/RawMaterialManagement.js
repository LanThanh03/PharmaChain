import React, { useState, useEffect } from "react";
import { Package, Plus, CheckCircle, XCircle, FileText, Save, Check, X } from "lucide-react";
import manufacturerService from "../services/apiService";
import "./RawMaterialManagement.css";

const RawMaterialManagement = () => {
  const [rawMaterials, setRawMaterials] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [showAddModal, setShowAddModal] = useState(false);
  const [newMaterial, setNewMaterial] = useState({
    batchNumber: "",
    materialName: "",
    supplierName: "",
    quantity: "",
    unit: "kg",
    manufactureDate: "",
    expiryDate: ""
  });

  useEffect(() => {
    fetchRawMaterials();
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    try {
      const response = await manufacturerService.getProducts();
      if (response.success) {
        setProducts(response.data);
      }
    } catch (err) {
      console.error("Lỗi tải danh sách sản phẩm:", err);
    }
  };

  const fetchRawMaterials = async () => {
    try {
      setLoading(true);
      const response = await manufacturerService.getRawMaterials();
      if (response.success) {
        setRawMaterials(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError("Lỗi tải danh sách nguyên liệu: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Trích xuất danh sách Hoạt chất chính (Active Ingredients) từ Master Data Dòng sản phẩm
  const activeIngredients = [...new Set(products.map(p => p.activeIngredient).filter(Boolean))];

  const handleAddMaterial = async () => {
    try {
      if (!newMaterial.batchNumber || !newMaterial.materialName || !newMaterial.quantity) {
        alert("Vui lòng điền các trường bắt buộc!");
        return;
      }
      
      const payload = {
        ...newMaterial,
        quantity: parseFloat(newMaterial.quantity)
      };

      const response = await manufacturerService.createRawMaterial(payload);
      if (response.success) {
        setShowAddModal(false);
        setNewMaterial({
          batchNumber: "", materialName: "", supplierName: "", quantity: "", unit: "kg", manufactureDate: "", expiryDate: ""
        });
        fetchRawMaterials();
      } else {
        alert(response.message);
      }
    } catch (err) {
      alert("Lỗi tạo nguyên liệu: " + err.message);
    }
  };

  const handleUpdateStatus = async (id, status) => {
    if (window.confirm(`Bạn xác nhận đổi trạng thái lô này thành ${status}?`)) {
      try {
        const response = await manufacturerService.updateRawMaterialStatus(id, status, "");
        if (response.success) {
          fetchRawMaterials();
        } else {
          alert(response.message);
        }
      } catch (err) {
        alert("Lỗi cập nhật: " + err.message);
      }
    }
  };

  const formatDate = (d) => {
    if (!d) return "N/A";
    return new Date(d).toLocaleDateString("vi-VN");
  };

  if (loading) return <div className="p-8">Đang tải dữ liệu...</div>;

  const filteredMaterials = rawMaterials.filter(rm => 
    rm.batchNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    rm.materialName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    rm.supplierName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="raw-material-management">
      <div className="page-header">
        <h1>
          <Package className="page-icon" />
          Quản lý Nguyên liệu
        </h1>
        <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
          <Plus size={16} /> Nhập Lô Nguyên Liệu Mới
        </button>
      </div>

      <div className="search-bar" style={{ marginBottom: "20px" }}>
        <input 
          type="text" 
          placeholder="Tìm kiếm theo Mã lô, Tên nguyên liệu, Nhà cung cấp..." 
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
          className="form-input"
          style={{ maxWidth: "400px" }}
        />
      </div>

      {error && <div className="alert-error">{error}</div>}

      <div className="materials-table-container">
        <table>
          <thead>
            <tr>
              <th>Mã Lô</th>
              <th>Tên Nguyên Liệu</th>
              <th>Nhà Cung Cấp</th>
              <th>Số lượng còn</th>
              <th>Ngày SX - HSD</th>
              <th>Trạng Thái QC</th>
              <th>Hành Động</th>
            </tr>
          </thead>
          <tbody>
            {filteredMaterials.length === 0 ? (
              <tr><td colSpan="7" className="text-center">Không tìm thấy dữ liệu phù hợp</td></tr>
            ) : (
              filteredMaterials.map(rm => (
                <tr key={rm.id}>
                  <td><strong>{rm.batchNumber}</strong></td>
                  <td>{rm.materialName}</td>
                  <td>{rm.supplierName}</td>
                  <td><strong style={{ color: rm.quantity > 0 ? '#059669' : '#dc2626' }}>{rm.quantity} {rm.unit}</strong></td>
                  <td>{formatDate(rm.manufactureDate)} <br/> {formatDate(rm.expiryDate)}</td>
                  <td>
                    <span className={`status-badge status-${rm.status.toLowerCase()}`}>
                      {rm.status}
                    </span>
                  </td>
                  <td>
                    {rm.status === 'PENDING' && (
                      <div className="action-buttons">
                        <button onClick={() => handleUpdateStatus(rm.id, 'APPROVED')} className="btn-approve" title="Duyệt QC Đạt">
                          <Check size={16} />
                        </button>
                        <button onClick={() => handleUpdateStatus(rm.id, 'REJECTED')} className="btn-reject" title="Duyệt Loại">
                          <X size={16} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Nhập Nguyên Liệu Đầu Vào</h2>
              <button onClick={() => setShowAddModal(false)} className="close-button">×</button>
            </div>
            <div className="modal-body form-grid">
              <div className="form-group">
                <label>Mã lô (Batch Number) *</label>
                <div style={{ display: "flex", gap: "8px" }}>
                  <input 
                    type="text" 
                    value={newMaterial.batchNumber} 
                    onChange={e => setNewMaterial({...newMaterial, batchNumber: e.target.value})} 
                    className="form-input" 
                    placeholder="Nhập mã lô hoặc bấm sinh mã..."
                  />
                  <button 
                    type="button" 
                    onClick={() => {
                      const prefix = newMaterial.materialName 
                        ? newMaterial.materialName.slice(0, 3).toUpperCase().replace(/[^A-Z]/g, "RAW") 
                        : "RAW";
                      const dateStr = new Date().toISOString().split("T")[0].replace(/-/g, "");
                      const randomSuffix = Math.floor(1000 + Math.random() * 9000);
                      setNewMaterial(prev => ({
                        ...prev,
                        batchNumber: `${prefix}-${dateStr}-${randomSuffix}`
                      }));
                    }}
                    className="btn btn-secondary"
                    style={{ whiteSpace: "nowrap", padding: "8px 12px" }}
                  >
                    Sinh mã
                  </button>
                </div>
              </div>
              <div className="form-group">
                <label>Tên Nguyên liệu (Hoạt chất) *</label>
                <input 
                  type="text" 
                  list="ingredients-list"
                  value={newMaterial.materialName} 
                  onChange={e => setNewMaterial({...newMaterial, materialName: e.target.value})} 
                  className="form-input"
                  placeholder="Gõ để tìm kiếm hoặc chọn hoạt chất..."
                />
                <datalist id="ingredients-list">
                  {activeIngredients.map((ai, idx) => (
                    <option key={idx} value={ai} />
                  ))}
                </datalist>
                {activeIngredients.length === 0 && (
                  <div style={{ marginTop: "4px", fontSize: "0.8rem", color: "#dc2626" }}>
                    * Chưa có Hoạt chất nào được định nghĩa trong "Dòng sản phẩm". Vui lòng tạo sản phẩm trước.
                  </div>
                )}
              </div>
              <div className="form-group">
                <label>Nhà cung cấp *</label>
                <input type="text" value={newMaterial.supplierName} onChange={e => setNewMaterial({...newMaterial, supplierName: e.target.value})} className="form-input" />
              </div>
              <div className="form-group" style={{ display: 'flex', gap: '10px' }}>
                <div style={{ flex: 1 }}>
                  <label>Số lượng *</label>
                  <input type="number" value={newMaterial.quantity} onChange={e => setNewMaterial({...newMaterial, quantity: e.target.value})} className="form-input" />
                </div>
                <div style={{ flex: 1 }}>
                  <label>Đơn vị</label>
                  <select value={newMaterial.unit} onChange={e => setNewMaterial({...newMaterial, unit: e.target.value})} className="form-select">
                    <option value="kg">Kg</option>
                    <option value="lit">Lít</option>
                    <option value="gam">Gam</option>
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>Ngày sản xuất</label>
                <input type="date" value={newMaterial.manufactureDate} onChange={e => setNewMaterial({...newMaterial, manufactureDate: e.target.value})} className="form-input" />
              </div>
              <div className="form-group">
                <label>Hạn sử dụng</label>
                <input type="date" value={newMaterial.expiryDate} onChange={e => setNewMaterial({...newMaterial, expiryDate: e.target.value})} className="form-input" />
              </div>
            </div>
            <div className="modal-actions">
              <button onClick={() => setShowAddModal(false)} className="btn btn-secondary">Hủy</button>
              <button onClick={handleAddMaterial} className="btn btn-primary"><Save size={16}/> Lưu vào kho</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RawMaterialManagement;
