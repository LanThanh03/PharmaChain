import React, { useState, useEffect } from "react"
import {
  Package,
  Plus,
  QrCode,
  Calendar,
  Factory,
  CheckCircle,
  AlertCircle,
  Hash,
  Clipboard,
  Save,
} from "lucide-react"
import manufacturerService from "../services/apiService"
import "./BatchAllocation.css"

const BatchAllocation = () => {
  const [products, setProducts] = useState([])
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)

  const [batchForm, setBatchForm] = useState({
    productId: "",
    quantity: "",
    manufactureDate: "",
    expiryDate: "",
    productionLine: "",
    qualityControlNotes: "",
    storageLocation: "",
  })

  const [generatedBatch, setGeneratedBatch] = useState(null)

  useEffect(() => {
    fetchProducts()
    fetchRecentBatches()
  }, [])

  const fetchProducts = async () => {
    try {
      console.log("Fetching products from API...")
      const response = await manufacturerService.getProducts()
      console.log("Products response:", response)

      if (response.success && response.data) {
        setProducts(response.data.filter(p => p.status === "active"))
        console.log("Loaded products:", response.data.length)
      } else {
        throw new Error("Invalid response format")
      }
    } catch (err) {
      console.error("Error fetching products:", err)
      setError("Không thể tải danh sách sản phẩm: " + err.message)
      // Set empty array as fallback
      setProducts([])
    }
  }

  const fetchRecentBatches = async () => {
    try {
      console.log("Fetching recent batches from API...")
      const response = await manufacturerService.getBatches()
      console.log("Batches response:", response)

      if (response.success && response.data) {
        // Transform API data to expected format
        const transformedBatches = response.data.map(batch => {
          // Safe string processing with null checks
          const safeString = str => (str ? String(str) : "")
          const safeSplit = (str, separator, index = 0) => {
            if (!str) return ""
            const parts = String(str).split(separator)
            return parts[index] || ""
          }

          return {
            id: safeString(batch.batchNumber || batch.id),
            batchId: safeString(batch.batchId), // Keep batchId as string for precision
            productId: batch.id || 0,
            productName: safeString(batch.drugName),
            quantity: batch.quantity || 0,
            manufactureDate: safeSplit(batch.manufactureTimestamp, " ", 0),
            expiryDate: safeSplit(batch.expiryDate, " ", 0),
            qrCode: safeString(batch.qrCode),
            status: "completed",
            blockchainTx: safeString(batch.transactionHash),
            createdAt: safeString(batch.createdAt),
          }
        })
        setBatches(transformedBatches)
        console.log("Loaded batches:", transformedBatches.length)
      } else {
        setBatches([])
      }
    } catch (err) {
      console.error("Error fetching recent batches:", err)
      setBatches([])
    }
  }

  const generateBatchId = () => {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, "0")
    const day = String(now.getDate()).padStart(2, "0")
    const time =
      String(now.getHours()).padStart(2, "0") +
      String(now.getMinutes()).padStart(2, "0")
    return `BT${year}${month}${day}${time}`
  }

  const generateQRCode = batchId => {
    // Generate unique QR code for each individual product in the batch
    return `QR_${batchId}_${Date.now()}`
  }

  const generateIndividualQRCodes = (batchId, quantity) => {
    // Generate array of unique QR codes for each product in the batch
    const qrCodes = []
    for (let i = 1; i <= quantity; i++) {
      qrCodes.push(`QR_${batchId}_${i.toString().padStart(6, "0")}`)
    }
    return qrCodes
  }

  // Calculate expiry date handling TimeZone issues
  const calculateExpiryDate = (manufactureDate, shelfLifeMonths) => {
    try {
      if (!manufactureDate) return ""

      const date = new Date(manufactureDate)
      if (isNaN(date.getTime())) return "" // Invalid date

      // Add months
      date.setMonth(date.getMonth() + parseInt(shelfLifeMonths))

      // Ensure year is reasonable (e.g. not 0022)
      if (date.getFullYear() < 2000) {
        console.warn("Calculated expiry year is weird:", date.getFullYear())
        const mYear = new Date(manufactureDate).getFullYear()
        date.setFullYear(mYear + Math.floor(shelfLifeMonths / 12))
      }

      return date.toISOString().split("T")[0]
    } catch (e) {
      console.error("Error calculating expiry date:", e)
      return ""
    }
  }

  const handleProductChange = productId => {
    // Auto-calculate expiry date if manufacture date is set
    // Note: productId is passed as argument, so use it directly
    let newExpiryDate = batchForm.expiryDate

    if (batchForm.manufactureDate && productId) {
      const product = products.find(p => String(p.id) === String(productId))
      if (product && product.shelfLife) {
        const shelfLifeStr = String(product.shelfLife)
        const shelfLifeMonths = parseInt(shelfLifeStr.split(" ")[0])

        if (!isNaN(shelfLifeMonths)) {
          newExpiryDate = calculateExpiryDate(
            batchForm.manufactureDate,
            shelfLifeMonths,
          )
        }
      }
    }

    setBatchForm(prev => ({ ...prev, productId, expiryDate: newExpiryDate }))
  }

  const handleManufactureDateChange = date => {
    // We compute expiry date immediately here to ensure consistency
    let newExpiryDate = batchForm.expiryDate

    if (batchForm.productId && date) {
      const product = products.find(
        p => String(p.id) === String(batchForm.productId),
      )
      if (product && product.shelfLife) {
        const shelfLifeStr = String(product.shelfLife)
        const shelfLifeMonths = parseInt(shelfLifeStr.split(" ")[0])

        if (!isNaN(shelfLifeMonths)) {
          newExpiryDate = calculateExpiryDate(date, shelfLifeMonths)
        }
      }
    }

    // Set state once with both new values
    setBatchForm(prev => ({
      ...prev,
      manufactureDate: date,
      expiryDate: newExpiryDate,
    }))
  }

  const handleCreateBatch = async () => {
    // 1. Basic validation
    if (
      !batchForm.productId ||
      !batchForm.quantity ||
      !batchForm.manufactureDate
    ) {
      setError(
        "Vui lòng điền đầy đủ thông tin bắt buộc (Sản phẩm, Số lượng, Ngày sản xuất)",
      )
      return
    }

    try {
      setLoading(true)
      setError(null)

      // 2. Quantity validation
      const quantity = parseInt(batchForm.quantity)
      if (isNaN(quantity) || quantity <= 0) {
        throw new Error("Vui lòng nhập số lượng hợp lệ (> 0)")
      }

      // 3. Product validation
      const product = products.find(
        p => String(p.id) === String(batchForm.productId),
      )
      if (!product) {
        throw new Error(
          `Không tìm thấy sản phẩm với ID: ${batchForm.productId}`,
        )
      }

      // 4. Expiry Date validation
      let expiryDate = batchForm.expiryDate
      // Check if date is valid and reasonable (e.g. year >= 2000)
      const expDateObj = new Date(expiryDate)
      if (
        !expiryDate ||
        isNaN(expDateObj.getTime()) ||
        expDateObj.getFullYear() < 2000
      ) {
        // Try to recalculate if missing or invalid
        if (product && product.shelfLife) {
          const shelfLifeStr = String(product.shelfLife)
          const shelfLifeMonths = parseInt(shelfLifeStr.split(" ")[0])
          if (!isNaN(shelfLifeMonths)) {
            expiryDate = calculateExpiryDate(
              batchForm.manufactureDate,
              shelfLifeMonths,
            )
          }
        }

        // Verify again
        const newExpDateObj = new Date(expiryDate)
        if (
          !expiryDate ||
          isNaN(newExpDateObj.getTime()) ||
          newExpDateObj.getFullYear() < 2000
        ) {
          throw new Error(
            `Ngày hết hạn không hợp lệ (${expiryDate}). Vui lòng kiểm tra lại ngày sản xuất hoặc hạn sử dụng sản phẩm.`,
          )
        }
      }

      const batchId = generateBatchId()

      // Generate individual QR codes for each product in the batch
      const individualQRCodes = generateIndividualQRCodes(batchId, quantity)

      const batchData = {
        id: batchId,
        productId: parseInt(batchForm.productId), // Ensure number
        productName: product.name,
        quantity: quantity,
        manufactureDate: batchForm.manufactureDate,
        expiryDate: expiryDate, // Use validated/recalculated date
        productionLine: batchForm.productionLine,
        qualityControlNotes: batchForm.qualityControlNotes,
        storageLocation: batchForm.storageLocation,
        qrCodes: individualQRCodes,
        manufacturer: "Dược Hậu Giang", // Updated manufacturer name
        activeIngredient: product.activeIngredient,
        dosage: product.dosage,
        storageConditions: product.storageConditions,
      }

      // Final validation and logging before API call
      console.log("📦 Final batchData before API call:", batchData)
      console.log("📅 Expiry Date Details:", {
        raw: expiryDate,
        type: typeof expiryDate,
        parsed: new Date(expiryDate),
        year: new Date(expiryDate).getFullYear(),
      })

      // Call blockchain API to create batch
      const response = await manufacturerService.createBatch(batchData)

      if (response.success) {
        setGeneratedBatch({
          ...batchData,
          blockchainTx: response.data.transactionHash,
          status: "completed",
          createdAt: new Date().toISOString(),
        })

        setSuccess(
          `Đã tạo lô thuốc ${batchId} thành công! Transaction: ${response.data.transactionHash}`,
        )

        // Reset form
        setBatchForm({
          productId: "",
          quantity: "",
          manufactureDate: "",
          expiryDate: "",
          productionLine: "",
          qualityControlNotes: "",
          storageLocation: "",
        })

        // Refresh recent batches
        await fetchRecentBatches()
      } else {
        setError(response.message || "Không thể tạo lô thuốc")
      }
    } catch (err) {
      setError("Lỗi tạo lô thuốc: " + err.message)
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = text => {
    navigator.clipboard.writeText(text)
  }

  const formatDate = dateString => {
    return new Date(dateString).toLocaleDateString("vi-VN")
  }

  const formatDateTime = dateString => {
    return new Date(dateString).toLocaleString("vi-VN")
  }

  return (
    <div className="batch-allocation">
      <div className="page-header">
        <h1>
          <Package className="page-icon" />
          Cấp phát Lô thuốc mới
        </h1>
        <p>Tạo định danh duy nhất và ghi nhận lên blockchain</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={20} />
          {success}
        </div>
      )}

      <div className="content-grid">
        {/* Create Batch Form */}
        <div className="create-batch-section">
          <div className="section-card">
            <div className="section-header">
              <h3>
                <Plus size={24} />
                Tạo lô thuốc mới
              </h3>
              <p>Nhập thông tin để cấp phát lô thuốc và ghi lên blockchain</p>
            </div>

            <div className="batch-form form-grid">
              <div className="form-group">
                <label>Sản phẩm *</label>
                <select
                  value={batchForm.productId}
                  onChange={e => handleProductChange(e.target.value)}
                  className="form-select">
                  <option value="">Chọn sản phẩm</option>
                  {products.map(product => (
                    <option
                      key={product.id}
                      value={product.id}>
                      {product.name} - {product.dosage}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Số lượng (hộp) *</label>
                <input
                  type="number"
                  value={batchForm.quantity}
                  onChange={e => {
                    const value = Number(e.target.value)
                    if (value >= 1) {
                      setBatchForm({ ...batchForm, quantity: value })
                    }
                  }}
                  placeholder="VD: 100"
                  className="form-input"
                  min="1"
                />
              </div>

              <div className="form-group">
                <label>Ngày sản xuất *</label>
                <input
                  type="date"
                  value={batchForm.manufactureDate}
                  onChange={e => handleManufactureDateChange(e.target.value)}
                  className="form-input"
                  max={new Date().toISOString().split("T")[0]}
                />
              </div>

              <div className="form-group">
                <label>Ngày hết hạn</label>
                <input
                  id="expiration"
                  type="date"
                  disabled
                  value={batchForm.expiryDate}
                  onChange={e =>
                    setBatchForm({ ...batchForm, expiryDate: e.target.value })
                  }
                  className="form-input"
                  min={batchForm.manufactureDate}
                />
              </div>

              <div className="form-group">
                <label>Dây chuyền</label>
                <select
                  value={batchForm.productionLine}
                  onChange={e =>
                    setBatchForm({
                      ...batchForm,
                      productionLine: e.target.value,
                    })
                  }
                  className="form-select">
                  <option value="">Chọn dây chuyền</option>
                  <option value="LINE_A">Dây chuyền A</option>
                  <option value="LINE_B">Dây chuyền B</option>
                  <option value="LINE_C">Dây chuyền C</option>
                </select>
              </div>

              <div className="form-group">
                <label>Vị trí kho</label>
                <input
                  type="text"
                  value={batchForm.storageLocation}
                  onChange={e =>
                    setBatchForm({
                      ...batchForm,
                      storageLocation: e.target.value,
                    })
                  }
                  placeholder="Kho A - Kệ 1"
                  className="form-input"
                />
              </div>

              <div className="form-group full-width">
                <label>Ghi chú kiểm soát chất lượng</label>
                <textarea
                  value={batchForm.qualityControlNotes}
                  onChange={e =>
                    setBatchForm({
                      ...batchForm,
                      qualityControlNotes: e.target.value,
                    })
                  }
                  placeholder="Ghi chú mở rộng..."
                  className="form-textarea"
                  rows="2"
                />
              </div>

              <div className="form-actions">
                <button
                  onClick={handleCreateBatch}
                  disabled={
                    loading ||
                    !batchForm.productId ||
                    !batchForm.quantity ||
                    !batchForm.manufactureDate
                  }
                  className="btn btn-primary create-btn">
                  {loading ? (
                    <>
                      <div className="spinner-small"></div>
                      Đang tạo lô...
                    </>
                  ) : (
                    <>
                      <Save size={16} />
                      Tạo lô thuốc & Ghi blockchain
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Generated Batch Info */}
        {generatedBatch && (
          <div className="generated-batch-section">
            <div className="section-card success-card">
              <div
                className="section-header"
                style={{
                  borderBottom: "3px solid #28a745",
                  paddingBottom: "16px",
                  marginBottom: "24px",
                }}>
                <h3
                  style={{
                    fontSize: "20px",
                    fontWeight: "600",
                    color: "#155724",
                    margin: 0,
                  }}>
                  ✓ Lô thuốc đã được tạo thành công
                </h3>
                <p
                  style={{
                    margin: "8px 0 0 0",
                    color: "#666",
                    fontSize: "14px",
                  }}>
                  Thông tin lô thuốc và blockchain transaction
                </p>
              </div>

              <div
                className="batch-details"
                style={{ display: "grid", gap: "16px" }}>
                <div
                  className="detail-row"
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "14px 18px",
                    backgroundColor: "#f8f9fa",
                    borderRadius: "8px",
                    border: "1px solid #e9ecef",
                  }}>
                  <span
                    className="label"
                    style={{
                      fontWeight: "500",
                      color: "#495057",
                      fontSize: "14px",
                    }}>
                    Mã lô:
                  </span>
                  <span
                    className="value batch-id"
                    style={{
                      fontWeight: "700",
                      color: "#155724",
                      fontSize: "16px",
                      fontFamily: "monospace",
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                    }}>
                    {generatedBatch.id}
                    <button
                      onClick={() => copyToClipboard(generatedBatch.id)}
                      className="copy-btn"
                      title="Copy mã lô"
                      style={{
                        padding: "4px 10px",
                        fontSize: "12px",
                        backgroundColor: "#28a745",
                        color: "white",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        transition: "all 0.2s",
                      }}>
                      Copy
                    </button>
                  </span>
                </div>

                <div
                  className="detail-row"
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "14px 18px",
                    backgroundColor: "#f8f9fa",
                    borderRadius: "8px",
                    border: "1px solid #e9ecef",
                  }}>
                  <span
                    className="label"
                    style={{
                      fontWeight: "500",
                      color: "#495057",
                      fontSize: "14px",
                    }}>
                    Sản phẩm:
                  </span>
                  <span
                    className="value"
                    style={{
                      fontWeight: "600",
                      color: "#212529",
                      fontSize: "15px",
                    }}>
                    {generatedBatch.productName}
                  </span>
                </div>

                <div
                  className="detail-row"
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "14px 18px",
                    backgroundColor: "#f8f9fa",
                    borderRadius: "8px",
                    border: "1px solid #e9ecef",
                  }}>
                  <span
                    className="label"
                    style={{
                      fontWeight: "500",
                      color: "#495057",
                      fontSize: "14px",
                    }}>
                    Số lượng:
                  </span>
                  <span
                    className="value"
                    style={{
                      fontWeight: "600",
                      color: "#212529",
                      fontSize: "15px",
                    }}>
                    {generatedBatch.quantity.toLocaleString()} hộp
                  </span>
                </div>

                <div
                  className="detail-row"
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "14px 18px",
                    backgroundColor: "#e7f5ff",
                    borderRadius: "8px",
                    border: "1px solid #339af0",
                  }}>
                  <span
                    className="label"
                    style={{
                      fontWeight: "500",
                      color: "#1864ab",
                      fontSize: "14px",
                    }}>
                    Blockchain TX:
                  </span>
                  <span
                    className="value blockchain-tx"
                    style={{
                      fontWeight: "600",
                      color: "#1864ab",
                      fontSize: "13px",
                      fontFamily: "monospace",
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                      maxWidth: "400px",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                    }}>
                    {generatedBatch.blockchainTx}
                    <button
                      onClick={() =>
                        copyToClipboard(generatedBatch.blockchainTx)
                      }
                      className="copy-btn"
                      title="Copy transaction hash"
                      style={{
                        padding: "4px 10px",
                        fontSize: "12px",
                        backgroundColor: "#339af0",
                        color: "white",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        transition: "all 0.2s",
                        flexShrink: 0,
                      }}>
                      Copy
                    </button>
                  </span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Recent Batches */}
      <div className="recent-batches-section">
        <div className="section-card">
          <div className="section-header">
            <h3>
              <Factory size={24} />
              Lô thuốc gần đây ({batches.length})
            </h3>
            <p>Danh sách các lô thuốc đã được tạo</p>
          </div>

          <div className="batches-table">
            <table
              style={{
                width: "100%",
                padding: "10px",
              }}>
              <thead>
                <tr>
                  <th>Số lô (không đổi)</th>
                  <th>Tên thuốc</th>
                  <th>Nhà sản xuất</th>
                  <th>Số lượng</th>
                  <th>Ngày hết hạn</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {batches.length === 0 ? (
                  <tr>
                    <td
                      colSpan="7"
                      className="no-data">
                      <Package
                        size={48}
                        className="no-data-icon"
                      />
                      <div>
                        <h4>Chưa có lô thuốc nào</h4>
                        <p>Hãy tạo lô thuốc đầu tiên</p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  batches.map(batch => (
                    <tr key={batch.id}>
                      <td>
                        <div
                          style={{
                            fontWeight: "bold",
                            color: "#155724",
                            backgroundColor: "#d4edda",
                            padding: "6px 10px",
                            borderRadius: "6px",
                            display: "inline-block",
                            border: "1px solid #28a745",
                          }}>
                          📦 {batch.id}
                        </div>
                        <div
                          style={{
                            fontSize: "0.8em",
                            color: "#666",
                            marginTop: "4px",
                          }}>
                          Blockchain ID:{" "}
                          {batch.batchId
                            ? batch.batchId.substring(0, 12) + "..."
                            : "N/A"}
                        </div>
                      </td>
                      <td className="product-name">{batch.productName}</td>
                      <td className="manufacturer">Công ty Dược ABC</td>
                      <td className="quantity">
                        {batch.quantity?.toLocaleString() || 0}
                      </td>
                      <td className="date">{formatDate(batch.expiryDate)}</td>
                      <td className="status">
                        <span className={`status-badge status-${batch.status}`}>
                          <CheckCircle size={14} />
                          Hoàn thành
                        </span>
                      </td>
                      <td>
                        <button
                          onClick={() => copyToClipboard(batch.id)}
                          className="btn btn-outline btn-sm"
                          title="Copy Số lô">
                          <Clipboard size={12} /> Copy
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

export default BatchAllocation
