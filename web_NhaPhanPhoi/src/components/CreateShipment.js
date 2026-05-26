import React, { useState, useEffect } from 'react';
import {
  Package,
  MapPin,
  Truck,
  Calendar,
  Hash,
  AlertCircle,
  CheckCircle,
  Search,
  User,
  Phone,
  Mail,
  Building,
  ArrowRight,
  Loader
} from 'lucide-react';
import { distributorService } from '../services/apiService';
import './CreateShipment.css';

console.log('🔄 CreateShipment.js LOADED - Version 2.0');

const CreateShipment = () => {
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [batches, setBatches] = useState([]);
  const [pharmacies, setPharmacies] = useState([]);

  const [formData, setFormData] = useState({
    batchId: '',
    pharmacyId: '',
    quantity: '',
    trackingNumber: '',
    notes: '',
    estimatedDelivery: '',
    priority: 'normal'
  });

  const [selectedBatch, setSelectedBatch] = useState(null);
  const [selectedPharmacy, setSelectedPharmacy] = useState(null);
  const [errors, setErrors] = useState({});

  // Fetch real data from API
  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch distributor INVENTORY (not batches) to get real available quantities
        const ownerAddress = localStorage.getItem('walletAddress');
        if (!ownerAddress) {
          setErrors({ general: 'Không tìm thấy địa chỉ ví nhà phân phối. Vui lòng đăng nhập.' });
          return;
        }

        // Use inventory endpoint instead of batch endpoint
        const inventoryResponse = await distributorService.getInventoryByWallet(ownerAddress);
        console.log('Inventory response for CreateShipment:', inventoryResponse);

        if (inventoryResponse.success && inventoryResponse.data) {
          console.log('✅ All inventory from backend:', inventoryResponse.data);
          console.log('📊 Array length:', inventoryResponse.data.length);

          // ✅ API /warehouse/exportable trả về DrugBatch objects
          // Map fields từ DrugBatch sang format frontend cần
          const availableBatches = inventoryResponse.data
            .filter(batch => {
              // Filter: quantity > 0 VÀ chưa hết hạn
              const hasQuantity = batch.quantity && batch.quantity > 0;

              // Check if expired
              let isExpired = false;
              if (batch.expiryDate) {
                const expiryDate = new Date(batch.expiryDate);
                isExpired = expiryDate < new Date();
              }

              console.log(`🔍 Batch ${batch.batchId}: quantity=${batch.quantity}, hasQuantity=${hasQuantity}, isExpired=${isExpired}`);

              // ✅ CHỈ cho phép xuất kho nếu: có số lượng VÀ chưa hết hạn
              return hasQuantity && !isExpired;
            })
            .map(batch => {
              // ⭐ CRITICAL: ALWAYS use blockchain batch ID for consistency
              const blockchainBatchId = batch.batchId;
              const databaseId = batch.id;

              if (!blockchainBatchId) {
                console.error('❌ CRITICAL: Batch missing blockchain batch ID!', batch);
              }

              // Convert batchId to string để tránh vấn đề với số lớn
              const batchIdStr = String(blockchainBatchId || databaseId);

              const mapped = {
                id: batchIdStr, // ⭐ Use blockchain batch ID
                batchId: blockchainBatchId, // ⭐ Keep original for reference
                databaseId: databaseId, // Database ID for internal use only
                drugName: batch.drugName,
                manufacturer: batch.manufacturer || 'N/A',
                availableQuantity: Number(batch.quantity),  // Ensure it's a number
                totalQuantity: Number(batch.quantity),
                expiryDate: batch.expiryDate ? batch.expiryDate.split('T')[0] : '',
                location: 'Kho chính',
                batchNumber: batch.batchNumber,
                // Add expiry warning flag
                isExpired: batch.expiryDate ? new Date(batch.expiryDate) < new Date() : false
              };

              console.log('📦 Mapped batch - Blockchain ID:', blockchainBatchId, 'Database ID:', databaseId);
              return mapped;
            });

          console.log('✅ Final filtered batches for UI:', availableBatches);
          console.log('📊 Final array length:', availableBatches.length);
          setBatches(availableBatches);
        } else {
          console.error('❌ Invalid response:', inventoryResponse);
        }

        // Fetch real pharmacies
        const pharmaciesResponse = await distributorService.getPharmacies();
        if (pharmaciesResponse.success && pharmaciesResponse.data) {
          setPharmacies(pharmaciesResponse.data);
        }
      } catch (error) {
        console.error('❌ Error fetching data:', error);
        console.error('❌ Error details:', error.message, error.stack);
        // Don't clear batches on error - keep existing data
        // setBatches([]);
        // setPharmacies([]);
      }
    };

    fetchData();
  }, []);

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));

    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  const validateStep = (stepNumber) => {
    const newErrors = {};

    switch (stepNumber) {
      case 1:
        if (!formData.batchId) {
          newErrors.batchId = 'Vui lòng chọn lô hàng';
        }
        // Xuất nguyên lô - số lượng được tự động điền
        break;

      case 2:
        if (!formData.pharmacyId) {
          newErrors.pharmacyId = 'Vui lòng chọn hiệu thuốc';
        }
        if (!formData.estimatedDelivery) {
          newErrors.estimatedDelivery = 'Vui lòng chọn ngày giao hàng dự kiến';
        }
        break;

      case 3:
        if (!formData.trackingNumber) {
          newErrors.trackingNumber = 'Vui lòng nhập mã vận đơn';
        }
        break;
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep(step)) {
      setStep(prev => prev + 1);
    }
  };

  const handlePrevious = () => {
    setStep(prev => prev - 1);
  };

  const handleBatchSelect = (batch) => {
    setSelectedBatch(batch);
    // ⭐ CRITICAL: Use blockchain batch ID, NOT database ID
    const blockchainBatchId = batch.batchId || batch.id;
    console.log('✅ Selected batch - Blockchain ID:', blockchainBatchId, 'Database ID:', batch.databaseId);
    handleInputChange('batchId', blockchainBatchId);
    // ⭐ Xuất nguyên lô - tự động điền số lượng
    handleInputChange('quantity', String(batch.availableQuantity));
  };

  const handlePharmacySelect = (pharmacy) => {
    setSelectedPharmacy(pharmacy);
    handleInputChange('pharmacyId', pharmacy.id);
  };

  const generateTrackingNumber = () => {
    const prefix = 'SH';
    const timestamp = Date.now().toString().slice(-6);
    const random = Math.random().toString(36).substring(2, 5).toUpperCase();
    const trackingNumber = `${prefix}${timestamp}${random}`;
    handleInputChange('trackingNumber', trackingNumber);
  };

  const handleSubmit = async () => {
    if (!validateStep(3)) return;

    setLoading(true);

    try {
      // Prepare shipment data for API
      const shipmentData = {
        batchId: formData.batchId, // ⭐ This should be blockchain batch ID
        pharmacyId: formData.pharmacyId,
        quantity: formData.quantity,
        trackingNumber: formData.trackingNumber,
        notes: formData.notes,
        transportMethod: 'Xe tải', // Can be made configurable later
        driverName: '', // Can be added to form later
        driverPhone: '' // Can be added to form later
      };

      console.log('🚀 Creating shipment with data:', {
        batchId: formData.batchId,
        batchIdType: typeof formData.batchId,
        selectedBatchBlockchainId: selectedBatch?.batchId,
        selectedBatchDatabaseId: selectedBatch?.databaseId,
        pharmacyId: formData.pharmacyId,
        quantity: formData.quantity
      });

      // Call actual API
      const response = await distributorService.createShipment(shipmentData);

      if (response.success) {
        alert(`Shipment đã được tạo thành công! Mã shipment: ${response.data?.shipmentCode || 'N/A'}`);

        // Refresh inventory to show updated quantities
        const ownerAddress = localStorage.getItem('walletAddress');
        if (ownerAddress) {
          const inventoryResponse = await distributorService.getInventoryByWallet(ownerAddress);
          if (inventoryResponse.success && inventoryResponse.data) {
            const availableBatches = inventoryResponse.data
              .filter(batch => batch.quantity && batch.quantity > 0)
              .map(batch => ({
                id: batch.batchId?.toString() || batch.id,
                drugName: batch.drugName,
                manufacturer: batch.manufacturer || 'N/A',
                availableQuantity: batch.quantity,
                totalQuantity: batch.quantity,
                expiryDate: batch.expiryDate ? batch.expiryDate.split('T')[0] : '',
                location: 'Kho chính',
                batchNumber: batch.batchNumber,
                isExpired: batch.expiryDate ? new Date(batch.expiryDate) < new Date() : false
              }));
            setBatches(availableBatches);
          }
        }

        // Reset form
        setFormData({
          batchId: '',
          pharmacyId: '',
          quantity: '',
          trackingNumber: '',
          notes: '',
          estimatedDelivery: '',
          priority: 'normal'
        });
        setSelectedBatch(null);
        setSelectedPharmacy(null);
        setStep(1);
      } else {
        throw new Error(response.message || 'Không thể tạo shipment');
      }

    } catch (error) {
      console.error('Error creating shipment:', error);
      alert('Có lỗi xảy ra khi tạo shipment: ' + (error.message || 'Vui lòng thử lại.'));
    } finally {
      setLoading(false);
    }
  };

  const getMinDate = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  };

  const StepIndicator = () => (
    <div className="step-indicator">
      {[1, 2, 3].map((stepNumber) => (
        <div key={stepNumber} className="step-item">
          <div className={`step-circle ${step >= stepNumber ? 'active' : ''}`}>
            {step > stepNumber ? <CheckCircle size={16} /> : stepNumber}
          </div>
          <span className={`step-label ${step >= stepNumber ? 'active' : ''}`}>
            {stepNumber === 1 && 'Chọn lô hàng'}
            {stepNumber === 2 && 'Chọn điểm đến'}
            {stepNumber === 3 && 'Xác nhận'}
          </span>
          {stepNumber < 3 && <ArrowRight className="step-arrow" size={16} />}
        </div>
      ))}
    </div>
  );

  // Debug log for render
  console.log('🎨 CreateShipment render - batches:', batches, 'length:', batches.length);

  return (
    <div className="create-shipment">
      {/* Header */}
      <div className="page-header">
        <div className="header-content">
          <h1>Tạo vận đơn xuất kho</h1>
          <p>Tạo đơn vận chuyển thuốc đến hiệu thuốc</p>
        </div>
      </div>

      <StepIndicator />

      <div className="form-container">
        {/* Step 1: Select Batch */}
        {step === 1 && (
          <div className="step-content">
            <h2>Bước 1: Chọn lô hàng</h2>

            <div className="batch-selection">
              <div className="section-header">
                <Package size={20} />
                <span>Chọn lô hàng muốn vận chuyển</span>
              </div>

              <div className="batch-grid">
                {batches.length === 0 ? (
                  <div style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>
                    Không có lô hàng nào có sẵn. Vui lòng kiểm tra kho hàng.
                  </div>
                ) : (
                  batches.map((batch) => (
                    <div
                      key={batch.id}
                      className={`batch-card ${selectedBatch?.id === batch.id ? 'selected' : ''} ${batch.isExpired ? 'expired' : ''}`}
                      onClick={() => handleBatchSelect(batch)}
                    >
                      {batch.isExpired && (
                        <div className="expired-badge">
                          <AlertCircle size={14} />
                          <span>ĐÃ HẾT HẠN</span>
                        </div>
                      )}
                      <div className="batch-header">
                        <strong>{batch.drugName}</strong>
                        <span className="batch-id" style={{ color: '#28a745', fontWeight: 'bold' }}>📦 {batch.batchNumber || 'N/A'}</span>
                      </div>
                      <div className="batch-details">
                        <p>Nhà sản xuất: {batch.manufacturer}</p>
                        <p>Vị trí: {batch.location}</p>
                        <p className={batch.isExpired ? 'expired-text' : ''}>
                          Hết hạn: {batch.expiryDate}
                        </p>
                      </div>
                      <div className="batch-quantity">
                        <span className="available">{batch.availableQuantity}</span>
                        <span className="total">/ {batch.totalQuantity} hộp</span>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {errors.batchId && (
                <div className="error-message">
                  <AlertCircle size={16} />
                  {errors.batchId}
                </div>
              )}
            </div>

            {/* Xuất nguyên lô - hiển thị thông tin thay vì nhập số lượng */}
            {selectedBatch && (
              <div className="quantity-info" style={{
                backgroundColor: '#e8f5e9',
                padding: '16px',
                borderRadius: '8px',
                border: '2px solid #4caf50',
                marginTop: '16px'
              }}>
                <h4 style={{ margin: '0 0 8px 0', color: '#2e7d32' }}>📦 Xuất nguyên lô</h4>
                <p style={{ margin: '0 0 4px 0', fontWeight: 'bold', fontSize: '1.1em' }}>
                  Số lượng: {selectedBatch.availableQuantity} hộp
                </p>
                <p style={{ margin: 0, color: '#666', fontSize: '0.9em' }}>
                  Toàn bộ lô hàng sẽ được vận chuyển đến nhà thuốc.
                </p>
                <input type="hidden" value={formData.quantity} />
              </div>
            )}
          </div>
        )}

        {/* Step 2: Select Pharmacy */}
        {step === 2 && (
          <div className="step-content">
            <h2>Bước 2: Chọn điểm đến</h2>

            <div className="pharmacy-selection">
              <div className="section-header">
                <Building size={20} />
                <span>Chọn hiệu thuốc nhận hàng</span>
              </div>

              <div className="pharmacy-grid">
                {pharmacies.map((pharmacy) => (
                  <div
                    key={pharmacy.id}
                    className={`pharmacy-card ${selectedPharmacy?.id === pharmacy.id ? 'selected' : ''}`}
                    onClick={() => handlePharmacySelect(pharmacy)}
                  >
                    <div className="pharmacy-header">
                      <h3>{pharmacy.name}</h3>
                      <span className="pharmacy-id">{pharmacy.id}</span>
                    </div>
                    <div className="pharmacy-details">
                      <div className="detail-item">
                        <MapPin size={16} />
                        <span>{pharmacy.address}</span>
                      </div>
                      <div className="detail-item">
                        <User size={16} />
                        <span>{pharmacy.contactPerson}</span>
                      </div>
                      <div className="detail-item">
                        <Phone size={16} />
                        <span>{pharmacy.phone}</span>
                      </div>
                      <div className="detail-item">
                        <Mail size={16} />
                        <span>{pharmacy.email}</span>
                      </div>
                    </div>
                    <div className="wallet-address">
                      <Hash size={14} />
                      <span>{pharmacy.walletAddress}</span>
                    </div>
                  </div>
                ))}
              </div>

              {errors.pharmacyId && (
                <div className="error-message">
                  <AlertCircle size={16} />
                  {errors.pharmacyId}
                </div>
              )}
            </div>

            <div className="delivery-details">
              <h3>Chi tiết giao hàng</h3>

              <div className="form-row">
                <div className="form-group">
                  <label>Ngày giao hàng dự kiến</label>
                  <input
                    type="date"
                    value={formData.estimatedDelivery}
                    onChange={(e) => handleInputChange('estimatedDelivery', e.target.value)}
                    min={getMinDate()}
                  />
                  {errors.estimatedDelivery && (
                    <div className="error-message">
                      <AlertCircle size={16} />
                      {errors.estimatedDelivery}
                    </div>
                  )}
                </div>

                <div className="form-group">
                  <label>Độ ưu tiên</label>
                  <select
                    value={formData.priority}
                    onChange={(e) => handleInputChange('priority', e.target.value)}
                  >
                    <option value="normal">Bình thường</option>
                    <option value="high">Cao</option>
                    <option value="urgent">Khẩn cấp</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Confirmation */}
        {step === 3 && (
          <div className="step-content">
            <h2>Bước 3: Xác nhận thông tin</h2>

            <div className="confirmation-summary">
              <div className="summary-section">
                <h3>Thông tin lô hàng</h3>
                <div className="summary-card">
                  <div className="summary-item" style={{ backgroundColor: '#d4edda', padding: '8px 12px', borderRadius: '6px', border: '2px solid #28a745' }}>
                    <label>📦 Số lô (truy vết):</label>
                    <span style={{ color: '#155724', fontWeight: 'bold', fontSize: '1.1em' }}>{selectedBatch?.batchNumber || 'N/A'}</span>
                  </div>
                  <div className="summary-item" style={{ fontSize: '0.9em', color: '#666' }}>
                    <label>Blockchain ID (kỹ thuật):</label>
                    <span className="blockchain-id">{selectedBatch?.batchId || selectedBatch?.id}</span>
                  </div>
                  <div className="summary-item">
                    <label>Tên thuốc:</label>
                    <span>{selectedBatch?.drugName}</span>
                  </div>
                  <div className="summary-item">
                    <label>Số lượng:</label>
                    <span>{formData.quantity} hộp (xuất nguyên lô)</span>
                  </div>
                </div>
              </div>

              <div className="summary-section">
                <h3>Thông tin điểm đến</h3>
                <div className="summary-card">
                  <div className="summary-item">
                    <label>Hiệu thuốc:</label>
                    <span>{selectedPharmacy?.name}</span>
                  </div>
                  <div className="summary-item">
                    <label>Địa chỉ:</label>
                    <span>{selectedPharmacy?.address}</span>
                  </div>
                  <div className="summary-item">
                    <label>Người liên hệ:</label>
                    <span>{selectedPharmacy?.contactPerson}</span>
                  </div>
                  <div className="summary-item">
                    <label>Điện thoại:</label>
                    <span>{selectedPharmacy?.phone}</span>
                  </div>
                </div>
              </div>

              <div className="summary-section">
                <h3>Thông tin vận chuyển</h3>
                <div className="summary-card">
                  <div className="form-group">
                    <label>Mã vận đơn</label>
                    <div className="input-group">
                      <input
                        type="text"
                        value={formData.trackingNumber}
                        onChange={(e) => handleInputChange('trackingNumber', e.target.value)}
                        placeholder="Nhập mã vận đơn"
                      />
                      <button type="button" onClick={generateTrackingNumber} className="btn-generate">
                        Tự động tạo
                      </button>
                    </div>
                    {errors.trackingNumber && (
                      <div className="error-message">
                        <AlertCircle size={16} />
                        {errors.trackingNumber}
                      </div>
                    )}
                  </div>

                  <div className="summary-item">
                    <label>Ngày giao dự kiến:</label>
                    <span>{formData.estimatedDelivery}</span>
                  </div>
                  <div className="summary-item">
                    <label>Độ ưu tiên:</label>
                    <span>
                      {formData.priority === 'normal' && 'Bình thường'}
                      {formData.priority === 'high' && 'Cao'}
                      {formData.priority === 'urgent' && 'Khẩn cấp'}
                    </span>
                  </div>

                  <div className="form-group">
                    <label>Ghi chú (tùy chọn)</label>
                    <textarea
                      value={formData.notes}
                      onChange={(e) => handleInputChange('notes', e.target.value)}
                      placeholder="Nhập ghi chú về shipment..."
                      rows={3}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Navigation Buttons */}
        <div className="form-navigation">
          <div className="nav-left">
            {step > 1 && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handlePrevious}
              >
                Quay lại
              </button>
            )}
          </div>

          <div className="nav-right">
            {step < 3 ? (
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleNext}
              >
                Tiếp tục
                <ArrowRight size={16} />
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-success"
                onClick={handleSubmit}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <Loader size={16} className="loading-spinner" />
                    Đang tạo...
                  </>
                ) : (
                  <>
                    <CheckCircle size={16} />
                    Tạo Shipment
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CreateShipment;
