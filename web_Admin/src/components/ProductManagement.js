import React, { useState, useEffect } from "react"
import {
  Package,
  Plus,
  Edit,
  Trash2,
  Search,
  Filter,
  CheckCircle,
  XCircle,
  Eye,
  Save,
  X,
} from "lucide-react"
import manufacturerService from "../services/apiService"
import "./ProductManagement.css"

const ProductManagement = () => {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [fetchingRef, setFetchingRef] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [showAddModal, setShowAddModal] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [newProduct, setNewProduct] = useState({
    name: "",
    category: "",
    dosage: "",
    unit: "",
    description: "",
    activeIngredient: "",
    storageConditions: "",
    shelfLife: "",
    status: "active",
    imageUrl: "",
  })

  const categories = [
    "Giảm đau hạ sốt",
    "Kháng sinh",
    "Vitamin & Khoáng chất",
    "Thuốc tim mạch",
    "Thuốc tiêu hóa",
    "Thuốc hô hấp",
    "Thuốc da liễu",
    "Khác",
  ]

  const units = ["viên", "ml", "gói", "lọ", "ống", "chai"]

  useEffect(() => {
    fetchProducts()
  }, [])

  const fetchProducts = async () => {
    // Prevent multiple simultaneous calls
    if (fetchingRef) {
      console.log("ProductManagement: Already fetching, skipping...")
      return
    }

    try {
      setFetchingRef(true)
      setLoading(true)
      setError(null)

      console.log("ProductManagement: Starting fetchProducts...")

      // Fetch real data from API
      const response = await manufacturerService.getProducts()

      if (response.success && response.data) {
        setProducts(response.data)
      } else {
        setProducts([])
        setError(response.message || "Không thể tải danh sách sản phẩm")
      }
    } catch (err) {
      console.error("Error fetching products:", err)
      setError("Không thể tải danh sách sản phẩm: " + err.message)
      setProducts([])
    } finally {
      setLoading(false)
      setFetchingRef(false)
    }
  }

  const filteredProducts = products.filter(product => {
    const matchesSearch =
      (product.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (product.category || "")
        .toLowerCase()
        .includes(searchTerm.toLowerCase()) ||
      (product.activeIngredient || "")
        .toLowerCase()
        .includes(searchTerm.toLowerCase())

    const matchesStatus =
      statusFilter === "all" || product.status === statusFilter

    return matchesSearch && matchesStatus
  })

  // Function to group products by time
  const groupProductsByTime = productsArray => {
    const now = new Date()
    const groups = {
      "Hôm nay": [],
      "Hôm qua": [],
      "Tuần này": [],
      "Tháng này": [],
      "Cũ hơn": [],
    }

    productsArray.forEach(product => {
      // Handle the case where createdAt might be undefined or null in early mock data
      const createdDate = product.createdAt
        ? new Date(product.createdAt)
        : new Date(2020, 0, 1)

      const diffTime = Math.abs(now - createdDate)
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

      if (diffDays <= 1 && now.getDate() === createdDate.getDate()) {
        groups["Hôm nay"].push(product)
      } else if (diffDays <= 2 && now.getDate() - createdDate.getDate() === 1) {
        groups["Hôm qua"].push(product)
      } else if (diffDays <= 7) {
        groups["Tuần này"].push(product)
      } else if (diffDays <= 30) {
        groups["Tháng này"].push(product)
      } else {
        groups["Cũ hơn"].push(product)
      }
    })

    // Remove empty groups
    return Object.entries(groups).filter(([_, items]) => items.length > 0)
  }

  const groupedProducts = groupProductsByTime(filteredProducts)

  // Debug log
  console.log(
    "ProductManagement: Render - products count:",
    products.length,
    "filtered count:",
    filteredProducts.length,
  )

  const handleAddProduct = async () => {
    try {
      // Call real API
      const response = await manufacturerService.createProduct(newProduct)

      if (response.success) {
        // Refresh products list
        await fetchProducts()
        setShowAddModal(false)
        setNewProduct({
          name: "",
          category: "",
          dosage: "",
          unit: "",
          description: "",
          activeIngredient: "",
          storageConditions: "",
          shelfLife: "",
          status: "active",
          imageUrl: "",
        })
      } else {
        setError(response.message || "Không thể tạo sản phẩm")
      }
    } catch (err) {
      console.error("Error adding product:", err)
      setError("Lỗi khi tạo sản phẩm: " + err.message)
    }
  }

  const handleEditProduct = async (productId, updatedData) => {
    try {
      // Call real API
      const response = await manufacturerService.updateProduct(
        productId,
        updatedData,
      )

      if (response.success) {
        // Refresh products list
        await fetchProducts()
        setEditingProduct(null)
      } else {
        setError(response.message || "Không thể cập nhật sản phẩm")
      }
    } catch (err) {
      console.error("Error updating product:", err)
      setError("Lỗi khi cập nhật sản phẩm: " + err.message)
    }
  }

  const handleImageUpload = async (e, isEditing = false) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      // Show loading state if needed
      const response = await manufacturerService.uploadImage(file)
      if (response.success && response.data) {
        if (isEditing) {
          setEditingProduct({ ...editingProduct, imageUrl: response.data })
        } else {
          setNewProduct({ ...newProduct, imageUrl: response.data })
        }
      } else {
        setError(response.message || "Tải ảnh lên thất bại")
      }
    } catch (err) {
      console.error("Error uploading image:", err)
      setError("Lỗi tải ảnh: " + err.message)
    }
  }

  const handleDeleteProduct = async productId => {
    if (window.confirm("Bạn có chắc chắn muốn xóa sản phẩm này?")) {
      try {
        // Call real API
        const response = await manufacturerService.deleteProduct(productId)

        if (response.success) {
          // Refresh products list
          await fetchProducts()
        } else {
          setError(response.message || "Không thể xóa sản phẩm")
        }
      } catch (err) {
        console.error("Error deleting product:", err)
        setError("Lỗi khi xóa sản phẩm: " + err.message)
      }
    }
  }

  const handleToggleStatus = async (productId, newStatus) => {
    try {
      // Find the product and update its status
      const product = products.find(p => p.id === productId)
      if (!product) return

      const updatedProduct = { ...product, status: newStatus }
      const response = await manufacturerService.updateProduct(
        productId,
        updatedProduct,
      )

      if (response.success) {
        // Refresh products list
        await fetchProducts()
      } else {
        setError(response.message || "Không thể cập nhật trạng thái sản phẩm")
      }
    } catch (err) {
      console.error("Error updating product status:", err)
      setError("Lỗi khi cập nhật trạng thái: " + err.message)
    }
  }

  const formatDate = dateString => {
    return new Date(dateString).toLocaleDateString("vi-VN")
  }

  if (loading) {
    return (
      <div className="product-management">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Đang tải danh sách sản phẩm...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="product-management">
      <div className="page-header">
        <h1>
          <Package className="page-icon" />
          Quản lý Dòng sản phẩm
        </h1>
      </div>

      <div className="controls-section">
        <div className="search-filter">
          <div className="search-box">
            <Search className="search-icon" />
            <input
              type="text"
              placeholder="Tìm kiếm theo tên sản phẩm, danh mục..."
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
              className="search-input"
            />
          </div>

          <div className="filter-box">
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              className="filter-select">
              <option value="all">Tất cả trạng thái</option>
              <option value="active">Đang hoạt động</option>
              <option value="inactive">Ngưng sản xuất</option>
            </select>
          </div>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="btn btn-primary add-btn">
          <Plus size={16} />
          Thêm sản phẩm mới
        </button>
      </div>

      <div className="products-table-container">
        <table>
          <thead>
            <tr>
              <th>Hình ảnh</th>
              <th>Mã SP</th>
              <th>Tên sản phẩm</th>
              <th>Danh mục</th>
              <th>Liều lượng</th>
              <th>Hoạt chất</th>
              <th>Trạng thái</th>
              <th>Số lô SX</th>
              <th>Tổng SL</th>
              <th>Ngày tạo</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {filteredProducts.length === 0 ? (
              <tr>
                <td
                  colSpan="11"
                  className="no-data">
                  <Package
                    size={48}
                    className="no-data-icon"
                  />
                  <div>
                    <h4>Không có sản phẩm nào</h4>
                    <p>
                      {searchTerm || statusFilter !== "all"
                        ? "Không tìm thấy sản phẩm nào phù hợp với bộ lọc."
                        : "Chưa có sản phẩm nào được tạo."}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              groupedProducts.map(([groupName, groupItems]) => (
                <React.Fragment key={groupName}>
                  <tr className="group-header">
                    <td colSpan="11">
                      <div className="group-header-content">
                        <span className="group-title">{groupName}</span>
                        <span className="group-count">
                          {groupItems.length} sản phẩm
                        </span>
                      </div>
                    </td>
                  </tr>
                  {groupItems.map(product => (
                    <tr
                      key={product.id}
                      className="product-row">
                      <td className="product-image">
                        {product.imageUrl ? (
                          <img
                            src={product.imageUrl}
                            alt={product.name}
                            className="product-thumb"
                          />
                        ) : (
                          <div className="product-thumb-placeholder">
                            <Package size={20} />
                          </div>
                        )}
                      </td>
                      <td className="product-id">{product.id}</td>
                      <td className="product-name">
                        <div className="product-name-wrapper">
                          <strong>{product.name}</strong>
                          {groupName === "Hôm nay" && (
                            <span className="new-badge">Mới</span>
                          )}
                        </div>
                      </td>
                      <td className="category">
                        <span className="category-pill">
                          {product.category}
                        </span>
                      </td>
                      <td className="dosage">{product.dosage}</td>
                      <td className="ingredient">{product.activeIngredient}</td>
                      <td className="status">
                        <span
                          className={`status-badge status-${product.status}`}>
                          {product.status === "active" ? (
                            <>
                              <CheckCircle size={14} />
                              Hoạt động
                            </>
                          ) : (
                            <>
                              <XCircle size={14} />
                              Ngưng SX
                            </>
                          )}
                        </span>
                      </td>
                      <td className="batches">{product.totalBatches || 0}</td>
                      <td className="produced">
                        {(product.totalProduced || 0).toLocaleString()}
                      </td>
                      <td className="date">
                        {product.createdAt
                          ? formatDate(product.createdAt)
                          : "N/A"}
                      </td>
                      <td>
                        <div className="actions">
                          <button
                            onClick={() => setEditingProduct(product)}
                            className="action-btn edit-btn"
                            title="Chỉnh sửa">
                            <Edit size={14} />
                          </button>
                          <button
                            onClick={() =>
                              handleToggleStatus(
                                product.id,
                                product.status === "active"
                                  ? "inactive"
                                  : "active",
                              )
                            }
                            className={`action-btn toggle-btn ${product.status === "active" ? "deactivate" : "activate"}`}
                            title={
                              product.status === "active"
                                ? "Ngưng sản xuất"
                                : "Kích hoạt"
                            }>
                            {product.status === "active" ? (
                              <XCircle size={14} />
                            ) : (
                              <CheckCircle size={14} />
                            )}
                          </button>
                          <button
                            onClick={() => handleDeleteProduct(product.id)}
                            className="action-btn delete-btn"
                            title="Xóa">
                            <Trash2 size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </React.Fragment>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Add Product Modal */}
      {showAddModal && (
        <div
          className="modal-overlay"
          onClick={() => setShowAddModal(false)}>
          <div
            className="modal-content"
            onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Thêm sản phẩm mới</h2>
              <button
                onClick={() => setShowAddModal(false)}
                className="close-button">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-group">
                  <label>Tên sản phẩm *</label>
                  <input
                    type="text"
                    value={newProduct.name}
                    onChange={e =>
                      setNewProduct({ ...newProduct, name: e.target.value })
                    }
                    placeholder="VD: Paracetamol 500mg"
                  />
                </div>
                <div className="form-group">
                  <label>Danh mục *</label>
                  <select
                    value={newProduct.category}
                    onChange={e =>
                      setNewProduct({ ...newProduct, category: e.target.value })
                    }>
                    <option value="">Chọn danh mục</option>
                    {categories.map(cat => (
                      <option
                        key={cat}
                        value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Liều lượng *</label>
                  <input
                    type="text"
                    value={newProduct.dosage}
                    onChange={e =>
                      setNewProduct({ ...newProduct, dosage: e.target.value })
                    }
                    placeholder="VD: 500mg"
                  />
                </div>
                <div className="form-group">
                  <label>Đơn vị *</label>
                  <select
                    value={newProduct.unit}
                    onChange={e =>
                      setNewProduct({ ...newProduct, unit: e.target.value })
                    }>
                    <option value="">Chọn đơn vị</option>
                    {units.map(unit => (
                      <option
                        key={unit}
                        value={unit}>
                        {unit}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group full-width">
                  <label>Hoạt chất *</label>
                  <input
                    type="text"
                    value={newProduct.activeIngredient}
                    onChange={e =>
                      setNewProduct({
                        ...newProduct,
                        activeIngredient: e.target.value,
                      })
                    }
                    placeholder="VD: Paracetamol"
                  />
                </div>
                <div className="form-group full-width">
                  <label>Hình ảnh (Tải lên hoặc nhập URL)</label>
                  <div
                    className="image-input-container"
                    style={{
                      display: "flex",
                      gap: "10px",
                      alignItems: "center",
                    }}>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={e => handleImageUpload(e, false)}
                      style={{ flex: 1 }}
                    />
                    <input
                      type="url"
                      value={newProduct.imageUrl || ""}
                      onChange={e =>
                        setNewProduct({
                          ...newProduct,
                          imageUrl: e.target.value,
                        })
                      }
                      hidden
                      placeholder="Hoặc nhập URL ảnh..."
                      style={{ flex: 2 }}
                    />
                  </div>
                  {newProduct.imageUrl && (
                    <div style={{ marginTop: "10px" }}>
                      <img
                        src={newProduct.imageUrl}
                        alt="Preview"
                        style={{
                          height: "80px",
                          borderRadius: "4px",
                          objectFit: "cover",
                        }}
                      />
                    </div>
                  )}
                </div>
                <div className="form-group full-width">
                  <label>Mô tả</label>
                  <textarea
                    value={newProduct.description}
                    onChange={e =>
                      setNewProduct({
                        ...newProduct,
                        description: e.target.value,
                      })
                    }
                    placeholder="Mô tả chi tiết về sản phẩm..."
                    rows="3"
                  />
                </div>
                <div className="form-group">
                  <label>Điều kiện bảo quản</label>
                  <input
                    type="text"
                    value={newProduct.storageConditions}
                    onChange={e =>
                      setNewProduct({
                        ...newProduct,
                        storageConditions: e.target.value,
                      })
                    }
                    placeholder="VD: Nơi khô ráo, tránh ánh sáng"
                  />
                </div>
                <div className="form-group">
                  <label>Hạn sử dụng *</label>
                  <input
                    type="text"
                    value={newProduct.shelfLife}
                    onChange={e =>
                      setNewProduct({
                        ...newProduct,
                        shelfLife: e.target.value,
                      })
                    }
                    placeholder="VD: 36 tháng"
                    required
                  />
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button
                onClick={() => setShowAddModal(false)}
                className="btn btn-secondary">
                Hủy
              </button>
              <button
                onClick={handleAddProduct}
                className="btn btn-primary"
                disabled={
                  !newProduct.name ||
                  !newProduct.category ||
                  !newProduct.dosage ||
                  !newProduct.unit ||
                  !newProduct.shelfLife ||
                  !newProduct.activeIngredient
                }>
                <Save size={16} />
                Thêm sản phẩm
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Product Modal */}
      {editingProduct && (
        <div
          className="modal-overlay"
          onClick={() => setEditingProduct(null)}>
          <div
            className="modal-content"
            onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Chỉnh sửa sản phẩm</h2>
              <button
                onClick={() => setEditingProduct(null)}
                className="close-button">
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-group">
                  <label>Tên sản phẩm *</label>
                  <input
                    type="text"
                    value={editingProduct.name}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        name: e.target.value,
                      })
                    }
                  />
                </div>
                <div className="form-group">
                  <label>Danh mục *</label>
                  <select
                    value={editingProduct.category}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        category: e.target.value,
                      })
                    }>
                    {categories.map(cat => (
                      <option
                        key={cat}
                        value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Liều lượng *</label>
                  <input
                    type="text"
                    value={editingProduct.dosage}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        dosage: e.target.value,
                      })
                    }
                  />
                </div>
                <div className="form-group">
                  <label>Đơn vị *</label>
                  <select
                    value={editingProduct.unit}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        unit: e.target.value,
                      })
                    }>
                    {units.map(unit => (
                      <option
                        key={unit}
                        value={unit}>
                        {unit}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-group full-width">
                  <label>Hoạt chất *</label>
                  <input
                    type="text"
                    value={editingProduct.activeIngredient}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        activeIngredient: e.target.value,
                      })
                    }
                  />
                </div>
                <div className="form-group full-width">
                  <label>Hình ảnh (Tải lên hoặc nhập URL)</label>
                  <div
                    className="image-input-container"
                    style={{
                      display: "flex",
                      gap: "10px",
                      alignItems: "center",
                    }}>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={e => handleImageUpload(e, true)}
                      style={{ flex: 1 }}
                    />
                    <input
                      type="url"
                      value={editingProduct.imageUrl || ""}
                      onChange={e =>
                        setEditingProduct({
                          ...editingProduct,
                          imageUrl: e.target.value,
                        })
                      }
                      placeholder="Hoặc nhập URL ảnh..."
                      style={{ flex: 2 }}
                    />
                  </div>
                  {editingProduct.imageUrl && (
                    <div style={{ marginTop: "10px" }}>
                      <img
                        src={editingProduct.imageUrl}
                        alt="Preview"
                        style={{
                          height: "80px",
                          borderRadius: "4px",
                          objectFit: "cover",
                        }}
                      />
                    </div>
                  )}
                </div>
                <div className="form-group full-width">
                  <label>Mô tả</label>
                  <textarea
                    value={editingProduct.description}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        description: e.target.value,
                      })
                    }
                    rows="3"
                  />
                </div>
                <div className="form-group">
                  <label>Điều kiện bảo quản</label>
                  <input
                    type="text"
                    value={editingProduct.storageConditions}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        storageConditions: e.target.value,
                      })
                    }
                  />
                </div>
                <div className="form-group">
                  <label>Hạn sử dụng</label>
                  <input
                    type="text"
                    value={editingProduct.shelfLife}
                    onChange={e =>
                      setEditingProduct({
                        ...editingProduct,
                        shelfLife: e.target.value,
                      })
                    }
                  />
                </div>
              </div>
            </div>
            <div className="modal-actions">
              <button
                onClick={() => setEditingProduct(null)}
                className="btn btn-secondary">
                Hủy
              </button>
              <button
                onClick={() =>
                  handleEditProduct(editingProduct.id, editingProduct)
                }
                className="btn btn-primary">
                <Save size={16} />
                Cập nhật
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ProductManagement
