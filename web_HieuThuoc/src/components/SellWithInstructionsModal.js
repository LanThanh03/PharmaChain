import React, { useState, useEffect } from 'react';
import { pharmacyService } from '../services/apiService';
import './SellWithInstructionsModal.css';

/**
 * Modal for selling an item with usage instructions
 * 
 * Features:
 * - Customer info input (name, phone)
 * - Dosage configuration (amount, frequency, meal relation)
 * - Specific times for medication
 * - Duration (number of days)
 * - Special notes from pharmacist
 */
const SellWithInstructionsModal = ({
    isOpen,
    onClose,
    itemData,
    onSuccess
}) => {
    // Form state
    const [formData, setFormData] = useState({
        customerName: '',
        customerPhone: '',
        dosage: '1 viên',
        frequency: 3,
        mealRelation: 'AFTER',
        specificTimes: '08:00,12:00,20:00',
        durationDays: 7,
        specialNotes: '',
        pharmacistName: '',
        salePrice: ''
    });

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    // Reset form when modal opens
    useEffect(() => {
        if (isOpen) {
            setFormData({
                customerName: '',
                customerPhone: '',
                dosage: '1 viên',
                frequency: 3,
                mealRelation: 'AFTER',
                specificTimes: '08:00,12:00,20:00',
                durationDays: 7,
                specialNotes: '',
                pharmacistName: localStorage.getItem('pharmacist_name') || '',
                salePrice: ''
            });
            setError('');
            setSuccess(false);
        }
    }, [isOpen]);

    // Update specific times when frequency changes
    useEffect(() => {
        const timesByFrequency = {
            1: '20:00',
            2: '08:00,20:00',
            3: '08:00,12:00,20:00',
            4: '07:00,12:00,17:00,22:00'
        };
        setFormData(prev => ({
            ...prev,
            specificTimes: timesByFrequency[prev.frequency] || '08:00,12:00,20:00'
        }));
    }, [formData.frequency]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError('');

        try {
            // Validate item code
            if (!itemData?.itemCode) {
                throw new Error('Thiếu mã sản phẩm');
            }

            const request = {
                itemCode: itemData.itemCode,
                customerName: formData.customerName || null,
                customerPhone: formData.customerPhone || null,
                dosage: formData.dosage,
                frequency: parseInt(formData.frequency),
                mealRelation: formData.mealRelation,
                specificTimes: formData.specificTimes,
                durationDays: parseInt(formData.durationDays),
                specialNotes: formData.specialNotes || null,
                pharmacistName: formData.pharmacistName || null,
                salePrice: formData.salePrice ? parseFloat(formData.salePrice) : null
            };

            console.log('Dispense request:', request);

            const response = await pharmacyService.dispenseWithInstructions(request);

            if (response.success) {
                setSuccess(true);
                setTimeout(() => {
                    onSuccess && onSuccess(response.data);
                    onClose();
                }, 1500);
            } else {
                setError(response.message || 'Không thể bán hàng');
            }
        } catch (err) {
            console.error('Sell error:', err);
            setError(err.message || 'Có lỗi xảy ra');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    const mealRelationLabels = {
        BEFORE: 'Trước bữa ăn',
        AFTER: 'Sau bữa ăn',
        WITH: 'Trong bữa ăn',
        ANY: 'Bất kỳ lúc nào'
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="sell-modal-content" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="sell-modal-header">
                    <h2>Bán hàng với Hướng dẫn sử dụng</h2>
                    <button className="close-btn" onClick={onClose}>×</button>
                </div>

                {/* Success Message */}
                <div className="success-message">
                    <div>
                        <strong>Bán hàng thành công!</strong>
                        <p>Đã lưu hướng dẫn sử dụng cho khách hàng</p>
                    </div>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="error-message">
                        <span>{error}</span>
                    </div>
                )}

                {/* Drug Info Banner */}
                <div className="drug-info-banner">
                    <div className="drug-details">
                        <h3>{itemData.drugName || 'Thuốc'}</h3>
                        <p>Mã: <code>{itemData.itemCode}</code></p>
                        <p>Lô: {itemData.batchNumber || 'N/A'} | HSD: {itemData.expiryDate || 'N/A'}</p>
                    </div>
                </div>

                <form onSubmit={handleSubmit} className="sell-form-container">
                    <div className="form-column-left">
                        {/* Customer Info Section */}
                        <div className="form-section">
                            <h3>Thông tin khách hàng</h3>
                            <div className="form-group">
                                <label>Tên khách hàng</label>
                                <input
                                    type="text"
                                    name="customerName"
                                    value={formData.customerName}
                                    onChange={handleChange}
                                    placeholder="Nguyễn Văn A"
                                />
                            </div>
                            <div className="form-group">
                                <label>Số điện thoại</label>
                                <input
                                    type="tel"
                                    name="customerPhone"
                                    value={formData.customerPhone}
                                    onChange={handleChange}
                                    placeholder="0901234567"
                                    pattern="[0-9]{10}"
                                />
                                <small>Để nhận nhắc nhở qua app</small>
                            </div>
                        </div>

                        {/* Pharmacist & Sale Info */}
                        <div className="form-section">
                            <h3>Thông tin bán hàng</h3>
                            <div className="form-group">
                                <label>Dược sĩ</label>
                                <input
                                    type="text"
                                    name="pharmacistName"
                                    value={formData.pharmacistName}
                                    onChange={handleChange}
                                    placeholder="Tên dược sĩ"
                                />
                            </div>
                            <div className="form-group">
                                <label>Giá bán (VNĐ)</label>
                                <input
                                    type="number"
                                    name="salePrice"
                                    value={formData.salePrice}
                                    onChange={handleChange}
                                    placeholder="50000"
                                    min={0}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="form-column-right">
                        {/* Usage Instructions Section */}
                        <div className="form-section">
                            <h3>Hướng dẫn sử dụng</h3>

                            <div className="form-row">
                                <div className="form-group">
                                    <label>Liều lượng mỗi lần *</label>
                                    <input
                                        type="text"
                                        name="dosage"
                                        value={formData.dosage}
                                        onChange={handleChange}
                                        placeholder="1 viên"
                                        required
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Số lần uống/ngày *</label>
                                    <select
                                        name="frequency"
                                        value={formData.frequency}
                                        onChange={handleChange}
                                        required
                                    >
                                        <option value={1}>1 lần/ngày</option>
                                        <option value={2}>2 lần/ngày</option>
                                        <option value={3}>3 lần/ngày</option>
                                        <option value={4}>4 lần/ngày</option>
                                    </select>
                                </div>
                            </div>

                            <div className="form-row">
                                <div className="form-group">
                                    <label>Thời điểm uống *</label>
                                    <select
                                        name="mealRelation"
                                        value={formData.mealRelation}
                                        onChange={handleChange}
                                        required
                                    >
                                        {Object.entries(mealRelationLabels).map(([key, label]) => (
                                            <option key={key} value={key}>{label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Số ngày dùng *</label>
                                    <input
                                        type="number"
                                        name="durationDays"
                                        value={formData.durationDays}
                                        onChange={handleChange}
                                        min={1}
                                        max={90}
                                        required
                                    />
                                </div>
                            </div>

                            <div className="form-group">
                                <label>Giờ uống cụ thể</label>
                                <input
                                    type="text"
                                    name="specificTimes"
                                    value={formData.specificTimes}
                                    onChange={handleChange}
                                    placeholder="08:00,12:00,20:00"
                                />
                                <small>Phân cách bằng dấu phẩy</small>
                            </div>

                            <div className="form-group">
                                <label>Ghi chú đặc biệt</label>
                                <textarea
                                    name="specialNotes"
                                    value={formData.specialNotes}
                                    onChange={handleChange}
                                    placeholder="Uống với nhiều nước..."
                                    rows={3}
                                />
                            </div>
                        </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="modal-actions full-width">
                        <button type="button" className="btn-cancel" onClick={onClose} disabled={isSubmitting}>
                            Hủy
                        </button>
                        <button type="submit" className="btn-submit" disabled={isSubmitting || success}>
                            {isSubmitting ? (
                                <>
                                    <span className="spinner"></span>
                                    Đang xử lý...
                                </>
                            ) : (
                                <>
                                    Xác nhận bán hàng
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default SellWithInstructionsModal;
