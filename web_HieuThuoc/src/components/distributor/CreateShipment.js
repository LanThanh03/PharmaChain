import React, { useState, useEffect } from 'react';
import { Search, Plus, Trash2, QrCode, Package, Truck, Calendar, Check, ChevronRight, MapPin, FileText } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import pharmacyService from '../../services/apiService';
import './CreateShipment.css';

function CreateShipment() {
  const { user } = useAuth();
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedPharmacy, setSelectedPharmacy] = useState(null);
  const [medicineItems, setMedicineItems] = useState([]);
  const [formData, setFormData] = useState({
    deliveryDate: '',
    transportMethod: '',
    notes: ''
  });
  const [showScanner, setShowScanner] = useState(false);
  const [batchCode, setBatchCode] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const [pharmacies, setPharmacies] = useState([]);
  const [loading, setLoading] = useState(false);

  const steps = [
    { id: 1, title: 'Chọn lô hàng', icon: Package, description: 'Chọn lô hàng muốn vận chuyển' },
    { id: 2, title: 'Chọn điểm đến', icon: MapPin, description: 'Chọn hiệu thuốc nhận hàng' },
    { id: 3, title: 'Xác nhận', icon: FileText, description: 'Xác nhận thông tin và gửi đi' }
  ];

  useEffect(() => {
    fetchPharmacies();
  }, []);

  const fetchPharmacies = async () => {
    try {
      setLoading(true);
      // Fetch pharmacies from API - this should be implemented in backend
      // For now, we'll use the existing pharmacies from database
      const response = await pharmacyService.getPharmacies();
      if (response.success) {
        setPharmacies(response.data);
      }
    } catch (error) {
      console.error('Error fetching pharmacies:', error);
      setPharmacies([]);
    } finally {
      setLoading(false);
    }
  };

  const handlePharmacySelect = (pharmacy) => {
    setSelectedPharmacy(pharmacy);
    setCurrentStep(3); // Move to confirmation step
  };

  const handleNextStep = () => {
    if (currentStep === 1 && medicineItems.length > 0) {
      setCurrentStep(2);
    } else if (currentStep === 2 && selectedPharmacy) {
      setCurrentStep(3);
    }
  };

  const handlePreviousStep = () => {
    if (currentStep > 1) {
      setCurrentStep(currentStep - 1);
    }
  };

  const isStepCompleted = (stepId) => {
    switch (stepId) {
      case 1: return medicineItems.length > 0;
      case 2: return selectedPharmacy !== null;
      case 3: return false; // Never completed until final submission
      default: return false;
    }
  };

  const filteredPharmacies = pharmacies.filter(pharmacy =>
    pharmacy.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    pharmacy.address.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleScanCode = async () => {
    if (!batchCode.trim()) {
      alert('Vui lòng nhập mã lô thuốc');
      return;
    }

    try {
      // Try to get batch info from blockchain first
      const response = await pharmacyService.getBatchInfo(batchCode);

      if (response.success && response.data) {
        const batchData = response.data;
        const medicine = {
          id: Date.now(),
          name: batchData.name,
          manufacturer: batchData.manufacturer,
          quantity: batchData.quantity,
          expireDate: batchData.expireDate,
          batchNumber: batchData.batchNumber,
          qrCode: batchData.qrCode,
          transactionHash: batchData.transactionHash
        };

        // Check if batch already added
        if (medicineItems.find(item => item.batchNumber === batchData.batchNumber)) {
          alert('Lô thuốc này đã được thêm vào danh sách');
          return;
        }

        setMedicineItems([...medicineItems, medicine]);
        setBatchCode('');
        setShowScanner(false);
        alert(`Đã thêm thành công: ${medicine.name}`);
      } else {
        alert(response.message || 'Không tìm thấy thông tin lô thuốc với mã: ' + batchCode);
      }
    } catch (error) {
      console.error('Error scanning batch code:', error);
      alert('Lỗi khi quét mã lô: ' + error.message);
    }
  };

  const removeMedicineItem = (id) => {
    setMedicineItems(medicineItems.filter(item => item.id !== id));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedPharmacy) {
      alert('Vui lòng chọn hiệu thuốc nhận hàng');
      return;
    }
    if (medicineItems.length === 0) {
      alert('Vui lòng thêm ít nhất một lô thuốc');
      return;
    }

    try {
      // Prepare shipment data for blockchain
      // Create shipment data with real user information
      const shipmentData = {
        fromAddress: user?.walletAddress,
        toAddress: selectedPharmacy.walletAddress, // Pharmacy should have wallet address
        batchIds: medicineItems.map(item => item.batchId || item.id), // Batch IDs for blockchain
        trackingInfo: `Shipment from ${user?.name} to ${selectedPharmacy.name}. Transport: ${formData.transportMethod}. Notes: ${formData.notes}`,
        pharmacyId: selectedPharmacy.id,
        pharmacyInfo: {
          name: selectedPharmacy.name,
          address: selectedPharmacy.address,
          phone: selectedPharmacy.phone,
          walletAddress: selectedPharmacy.walletAddress
        },
        products: medicineItems.map(item => ({
          name: item.name,
          manufacturer: item.manufacturer,
          batchNumber: item.batchNumber,
          quantity: item.quantity,
          expireDate: item.expireDate,
          qrCode: item.qrCode,
          batchId: item.batchId || item.id
        })),
        deliveryDate: formData.deliveryDate,
        transportMethod: formData.transportMethod,
        notes: formData.notes,
        distributorInfo: {
          name: user?.name || 'Nhà phân phối',
          walletAddress: user?.walletAddress
        },
        createdAt: new Date().toISOString()
      };

      // Create shipment via blockchain API
      const response = await pharmacyService.createShipment(shipmentData);

      if (response.success) {
        alert(`Tạo lô hàng thành công!\nMã lô hàng: ${response.data.shipmentId}\nMã vận đơn: ${response.data.trackingCode}\nTransaction: ${response.data.transactionHash}`);

        // Reset form
        setSelectedPharmacy(null);
        setMedicineItems([]);
        setFormData({ deliveryDate: '', transportMethod: '', notes: '' });
      } else {
        alert('Lỗi tạo lô hàng: ' + response.message);
      }
    } catch (error) {
      console.error('Error creating shipment:', error);
      alert('Lỗi tạo lô hàng: ' + error.message);
    }
  };

  const ProgressStepper = () => (
    <div className="progress-stepper">
      {steps.map((step, index) => {
        const StepIcon = step.icon;
        const isActive = currentStep === step.id;
        const isCompleted = isStepCompleted(step.id);
        const isConnected = index < steps.length - 1;

        return (
          <div key={step.id} className="step-container">
            <div className={`step ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}>
              <div className="step-circle">
                {isCompleted ? (
                  <Check size={20} />
                ) : (
                  <StepIcon size={20} />
                )}
              </div>
              <div className="step-content">
                <h3 className="step-title">{step.title}</h3>
                <p className="step-description">{step.description}</p>
              </div>
            </div>
            {isConnected && <ChevronRight className="step-connector" size={24} />}
          </div>
        );
      })}
    </div>
  );

  return (
    <div className="create-shipment">
      <div className="page-header">
        <div className="header-content">
          <h1>
            <Package className="header-icon" />
            Xuất kho (HT)
          </h1>
          <p>Tạo lô hàng gửi đến các hiệu thuốc đối tác</p>
        </div>
        <div className="blockchain-status">
          <div className="status-indicator connected">
            <div className="status-dot"></div>
            Blockchain Connected
          </div>
        </div>
      </div>

      <ProgressStepper />

      <div className="shipment-container">
        {/* Step 1: Select Medicine Batches */}
        {currentStep === 1 && (
          <div className="step-content" key="step1">
            <div className="step-header">
              <Package className="step-icon" />
              <div>
                <h2>Bước 1: Chọn lô hàng</h2>
                <p>Chọn lô hàng muốn vận chuyển</p>
              </div>
            </div>

            <div className="medicine-scanner-section">
              {!showScanner ? (
                <div className="scanner-placeholder">
                  <QrCode size={64} className="scanner-icon" />
                  <h3>Quét mã lô thuốc</h3>
                  <p>Quét hoặc nhập mã lô thuốc để thêm vào lô hàng</p>
                  <button
                    type="button"
                    className="btn btn-primary scanner-btn"
                    onClick={() => setShowScanner(true)}
                  >
                    <QrCode size={20} />
                    Quét/Nhập mã lô thuốc
                  </button>
                </div>
              ) : (
                <div className="scanner-form">
                  <div className="input-group">
                    <QrCode className="input-icon" />
                    <input
                      type="text"
                      value={batchCode}
                      onChange={(e) => setBatchCode(e.target.value)}
                      placeholder="Nhập mã lô thuốc (VD: LOT2024001)"
                      className="input"
                      onKeyPress={(e) => e.key === 'Enter' && handleScanCode()}
                    />
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={handleScanCode}
                    >
                      Thêm
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary"
                      onClick={() => setShowScanner(false)}
                    >
                      Hủy
                    </button>
                  </div>
                  <div className="helper-text">
                    <p>💡 Mã demo có sẵn: LOT2024001, LOT2024002, BT2024001, BT2024002</p>
                  </div>
                </div>
              )}
            </div>

            {/* Medicine List */}
            {medicineItems.length > 0 && (
              <div className="medicine-list">
                <div className="list-header">
                  <h3>Danh sách thuốc trong lô hàng ({medicineItems.length})</h3>
                  <div className="list-summary">
                    Tổng: {medicineItems.reduce((sum, item) => sum + parseInt(item.quantity || 0), 0)} hộp
                  </div>
                </div>
                <div className="medicine-grid">
                  {medicineItems.map(item => (
                    <div key={item.id} className="medicine-card">
                      <div className="medicine-header">
                        <Package className="medicine-icon" />
                        <button
                          type="button"
                          className="btn-remove"
                          onClick={() => removeMedicineItem(item.id)}
                          title="Xóa khỏi lô hàng"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                      <div className="medicine-info">
                        <h4>{item.name}</h4>
                        <div className="medicine-details">
                          <div className="detail-item">
                            <span className="label">NSX:</span>
                            <span className="value">{item.manufacturer}</span>
                          </div>
                          <div className="detail-item">
                            <span className="label">Số lượng:</span>
                            <span className="value">{parseInt(item.quantity || 0).toLocaleString()} hộp</span>
                          </div>
                          <div className="detail-item">
                            <span className="label">Hạn sử dụng:</span>
                            <span className="value">{item.expireDate}</span>
                          </div>
                          <div className="detail-item">
                            <span className="label">Số lô:</span>
                            <span className="value">{item.batchNumber}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {medicineItems.length === 0 && !showScanner && (
              <div className="empty-state">
                <Package size={48} className="empty-icon" />
                <h3>Chưa có thuốc nào trong lô hàng</h3>
                <p>Hãy quét mã lô thuốc để bắt đầu tạo lô hàng</p>
              </div>
            )}
          </div>
        )}

        {/* Step 2: Select Pharmacy */}
        {currentStep === 2 && (
          <div className="step-content" key="step2">
            <div className="step-header">
              <MapPin className="step-icon" />
              <div>
                <h2>Bước 2: Chọn điểm đến</h2>
                <p>Chọn hiệu thuốc nhận hàng</p>
              </div>
            </div>

            {!selectedPharmacy ? (
              <div className="pharmacy-search-section">
                <div className="search-wrapper">
                  <Search className="search-icon" />
                  <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    placeholder="Tìm kiếm hiệu thuốc theo tên hoặc địa chỉ..."
                    className="input search-input"
                  />
                </div>
                <div className="pharmacy-grid">
                  {filteredPharmacies.length > 0 ? (
                    filteredPharmacies.map(pharmacy => (
                      <div
                        key={pharmacy.id}
                        className="pharmacy-card"
                        onClick={() => handlePharmacySelect(pharmacy)}
                      >
                        <div className="pharmacy-info">
                          <h3>{pharmacy.name}</h3>
                          <p className="address">{pharmacy.address}</p>
                          <p className="contact">📞 {pharmacy.phone}</p>
                          {pharmacy.email && <p className="contact">✉️ {pharmacy.email}</p>}
                        </div>
                        <ChevronRight className="select-arrow" />
                      </div>
                    ))
                  ) : (
                    <div className="empty-search">
                      <Search size={48} className="empty-icon" />
                      <h3>Không tìm thấy hiệu thuốc</h3>
                      <p>Thử tìm kiếm với từ khóa khác</p>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="selected-pharmacy">
                <div className="selected-header">
                  <h3>Hiệu thuốc đã chọn</h3>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setSelectedPharmacy(null)}
                  >
                    Đổi hiệu thuốc
                  </button>
                </div>
                <div className="pharmacy-details">
                  <h4>{selectedPharmacy.name}</h4>
                  <p>📍 {selectedPharmacy.address}</p>
                  <p>📞 {selectedPharmacy.phone}</p>
                  {selectedPharmacy.email && <p>✉️ {selectedPharmacy.email}</p>}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 3: Confirmation */}
        {currentStep === 3 && (
          <div className="step-content" key="step3">
            <div className="step-header">
              <FileText className="step-icon" />
              <div>
                <h2>Bước 3: Xác nhận</h2>
                <p>Xác nhận thông tin và gửi đi</p>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="confirmation-form">
              {/* Shipment Summary */}
              <div className="shipment-summary">
                <h3>Tóm tắt lô hàng</h3>

                {/* Selected Medicines Summary */}
                <div className="summary-section">
                  <h4>Danh sách thuốc ({medicineItems.length} loại)</h4>
                  <div className="medicine-summary-grid">
                    {medicineItems.map(item => (
                      <div key={item.id} className="medicine-summary-card">
                        <div className="medicine-name">{item.name}</div>
                        <div className="medicine-quantity">{parseInt(item.quantity || 0).toLocaleString()} hộp</div>
                        <div className="medicine-batch">Lô: {item.batchNumber}</div>
                      </div>
                    ))}
                  </div>
                  <div className="total-summary">
                    <strong>Tổng số lượng: {medicineItems.reduce((sum, item) => sum + parseInt(item.quantity || 0), 0).toLocaleString()} hộp</strong>
                  </div>
                </div>

                {/* Selected Pharmacy Summary */}
                {selectedPharmacy && (
                  <div className="summary-section">
                    <h4>Điểm đến</h4>
                    <div className="pharmacy-summary">
                      <div className="pharmacy-name">{selectedPharmacy.name}</div>
                      <div className="pharmacy-address">📍 {selectedPharmacy.address}</div>
                      <div className="pharmacy-contact">📞 {selectedPharmacy.phone}</div>
                    </div>
                  </div>
                )}
              </div>

              {/* Shipping Information Form */}
              <div className="shipping-info">
                <h3>Thông tin vận chuyển</h3>
                <div className="form-grid">
                  <div className="form-group">
                    <label htmlFor="deliveryDate">Ngày gửi dự kiến</label>
                    <div className="input-wrapper">
                      <Calendar className="input-icon" />
                      <input
                        id="deliveryDate"
                        type="date"
                        value={formData.deliveryDate}
                        onChange={(e) => setFormData({ ...formData, deliveryDate: e.target.value })}
                        className="input"
                        required
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label htmlFor="transportMethod">Phương tiện vận chuyển</label>
                    <div className="input-wrapper">
                      <Truck className="input-icon" />
                      <select
                        id="transportMethod"
                        value={formData.transportMethod}
                        onChange={(e) => setFormData({ ...formData, transportMethod: e.target.value })}
                        className="input"
                        required
                      >
                        <option value="">Chọn phương tiện</option>
                        <option value="truck">Xe tải</option>
                        <option value="van">Xe van</option>
                        <option value="motorcycle">Xe máy</option>
                      </select>
                    </div>
                  </div>
                </div>
                <div className="form-group">
                  <label htmlFor="notes">Ghi chú</label>
                  <textarea
                    id="notes"
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    placeholder="Ghi chú thêm về lô hàng..."
                    className="input textarea"
                    rows="3"
                  />
                </div>
              </div>

              {/* Action Buttons */}
              <div className="confirmation-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handlePreviousStep}
                >
                  ← Quay lại
                </button>
                <button
                  type="submit"
                  className="btn btn-primary submit-btn"
                  disabled={loading}
                >
                  <Package size={20} />
                  {loading ? 'Đang tạo lô hàng...' : 'Xác nhận và Gửi đi'}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Navigation Buttons */}
        {currentStep < 3 && (
          <div className="step-navigation">
            {currentStep > 1 && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handlePreviousStep}
              >
                ← Quay lại
              </button>
            )}
            {currentStep === 1 && medicineItems.length > 0 && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleNextStep}
              >
                Tiếp theo: Chọn điểm đến →
              </button>
            )}
            {currentStep === 2 && selectedPharmacy && (
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleNextStep}
              >
                Tiếp theo: Xác nhận →
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default CreateShipment;



