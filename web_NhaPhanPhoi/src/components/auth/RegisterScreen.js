import React, { useState } from 'react';
import { Eye, EyeOff, User, Mail, Lock, Building2, Phone, MapPin, FileText, Wallet, AlertCircle, CheckCircle } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import './RegisterScreen.css';

function RegisterScreen({ onSwitchToLogin }) {
  const { register, loading, error, clearError } = useAuth();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    name: '',
    role: 'DISTRIBUTOR',
    companyName: '',
    companyAddress: '',
    phoneNumber: '',
    walletAddress: '',
    licenseNumber: '',
    licenseExpiryDate: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear validation error for this field
    if (validationErrors[name]) {
      setValidationErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    
    // Clear general error
    if (error) {
      clearError();
    }
  };

  const validateStep = (step) => {
    const errors = {};

    if (step === 1) {
      if (!formData.email.trim()) {
        errors.email = 'Email là bắt buộc';
      } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
        errors.email = 'Email không đúng định dạng';
      }

      if (!formData.password) {
        errors.password = 'Mật khẩu là bắt buộc';
      } else if (formData.password.length < 6) {
        errors.password = 'Mật khẩu phải có ít nhất 6 ký tự';
      }

      if (!formData.confirmPassword) {
        errors.confirmPassword = 'Xác nhận mật khẩu là bắt buộc';
      } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = 'Mật khẩu xác nhận không khớp';
      }

      if (!formData.name.trim()) {
        errors.name = 'Họ tên là bắt buộc';
      }
    }

    if (step === 2) {
      if (!formData.companyName.trim()) {
        errors.companyName = 'Tên công ty là bắt buộc';
      }

      if (!formData.companyAddress.trim()) {
        errors.companyAddress = 'Địa chỉ công ty là bắt buộc';
      }

      if (!formData.phoneNumber.trim()) {
        errors.phoneNumber = 'Số điện thoại là bắt buộc';
      } else if (!/^[0-9+\-\s()]+$/.test(formData.phoneNumber)) {
        errors.phoneNumber = 'Số điện thoại không đúng định dạng';
      }

      if (formData.walletAddress && !/^0x[a-fA-F0-9]{40}$/.test(formData.walletAddress)) {
        errors.walletAddress = 'Địa chỉ ví không đúng định dạng Ethereum';
      }
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleNextStep = () => {
    if (validateStep(currentStep)) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handlePrevStep = () => {
    setCurrentStep(currentStep - 1);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateStep(2)) {
      return;
    }

    // Prepare form data for API
    const registerPayload = {
      ...formData,
      licenseExpiryDate: formData.licenseExpiryDate ? new Date(formData.licenseExpiryDate).toISOString() : null
    };

    const result = await register(registerPayload);
    
    if (!result.success) {
      console.error('Registration failed:', result.error);
    }
  };

  const renderStep1 = () => (
    <div className="step-content">
      <div className="step-header">
        <h2>Thông tin tài khoản</h2>
        <p>Tạo tài khoản đăng nhập của bạn</p>
      </div>

      <div className="form-group">
        <label htmlFor="email">Email *</label>
        <div className="input-group">
          <Mail className="input-icon" size={20} />
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleInputChange}
            placeholder="Nhập email của bạn"
            className={validationErrors.email ? 'error' : ''}
            disabled={loading}
          />
        </div>
        {validationErrors.email && (
          <span className="field-error">{validationErrors.email}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="password">Mật khẩu *</label>
        <div className="input-group">
          <Lock className="input-icon" size={20} />
          <input
            type={showPassword ? 'text' : 'password'}
            id="password"
            name="password"
            value={formData.password}
            onChange={handleInputChange}
            placeholder="Nhập mật khẩu (ít nhất 6 ký tự)"
            className={validationErrors.password ? 'error' : ''}
            disabled={loading}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword(!showPassword)}
            disabled={loading}
          >
            {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>
        {validationErrors.password && (
          <span className="field-error">{validationErrors.password}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="confirmPassword">Xác nhận mật khẩu *</label>
        <div className="input-group">
          <Lock className="input-icon" size={20} />
          <input
            type={showConfirmPassword ? 'text' : 'password'}
            id="confirmPassword"
            name="confirmPassword"
            value={formData.confirmPassword}
            onChange={handleInputChange}
            placeholder="Nhập lại mật khẩu"
            className={validationErrors.confirmPassword ? 'error' : ''}
            disabled={loading}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
            disabled={loading}
          >
            {showConfirmPassword ? <EyeOff size={20} /> : <Eye size={20} />}
          </button>
        </div>
        {validationErrors.confirmPassword && (
          <span className="field-error">{validationErrors.confirmPassword}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="name">Họ và tên *</label>
        <div className="input-group">
          <User className="input-icon" size={20} />
          <input
            type="text"
            id="name"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            placeholder="Nhập họ và tên"
            className={validationErrors.name ? 'error' : ''}
            disabled={loading}
          />
        </div>
        {validationErrors.name && (
          <span className="field-error">{validationErrors.name}</span>
        )}
      </div>

      {/* Hidden role field - only distributors can register */}
      <input type="hidden" name="role" value="DISTRIBUTOR" />
      
      <div className="form-group">
        <label>Vai trò</label>
        <div className="input-group">
          <Building2 className="input-icon" size={20} />
          <div className="role-display">
            Nhà phân phối
          </div>
        </div>
      </div>
    </div>
  );

  const renderStep2 = () => (
    <div className="step-content">
      <div className="step-header">
        <h2>Thông tin công ty</h2>
        <p>Cung cấp thông tin về công ty của bạn</p>
      </div>

      <div className="form-group">
        <label htmlFor="companyName">Tên công ty *</label>
        <div className="input-group">
          <Building2 className="input-icon" size={20} />
          <input
            type="text"
            id="companyName"
            name="companyName"
            value={formData.companyName}
            onChange={handleInputChange}
            placeholder="Nhập tên công ty"
            className={validationErrors.companyName ? 'error' : ''}
            disabled={loading}
          />
        </div>
        {validationErrors.companyName && (
          <span className="field-error">{validationErrors.companyName}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="companyAddress">Địa chỉ công ty *</label>
        <div className="input-group">
          <MapPin className="input-icon" size={20} />
          <textarea
            id="companyAddress"
            name="companyAddress"
            value={formData.companyAddress}
            onChange={handleInputChange}
            placeholder="Nhập địa chỉ đầy đủ của công ty"
            className={validationErrors.companyAddress ? 'error' : ''}
            disabled={loading}
            rows={3}
          />
        </div>
        {validationErrors.companyAddress && (
          <span className="field-error">{validationErrors.companyAddress}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="phoneNumber">Số điện thoại *</label>
        <div className="input-group">
          <Phone className="input-icon" size={20} />
          <input
            type="tel"
            id="phoneNumber"
            name="phoneNumber"
            value={formData.phoneNumber}
            onChange={handleInputChange}
            placeholder="Nhập số điện thoại liên hệ"
            className={validationErrors.phoneNumber ? 'error' : ''}
            disabled={loading}
          />
        </div>
        {validationErrors.phoneNumber && (
          <span className="field-error">{validationErrors.phoneNumber}</span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="walletAddress">Địa chỉ ví Ethereum</label>
        <div className="input-group">
          <Wallet className="input-icon" size={20} />
          <input
            type="text"
            id="walletAddress"
            name="walletAddress"
            value={formData.walletAddress}
            onChange={handleInputChange}
            placeholder="0x... (tùy chọn)"
            className={validationErrors.walletAddress ? 'error' : ''}
            disabled={loading}
          />
        </div>
        {validationErrors.walletAddress && (
          <span className="field-error">{validationErrors.walletAddress}</span>
        )}
        <small className="field-hint">Để trống nếu chưa có. Có thể cập nhật sau.</small>
      </div>

      <div className="form-group">
        <label htmlFor="licenseNumber">Số giấy phép kinh doanh</label>
        <div className="input-group">
          <FileText className="input-icon" size={20} />
          <input
            type="text"
            id="licenseNumber"
            name="licenseNumber"
            value={formData.licenseNumber}
            onChange={handleInputChange}
            placeholder="Nhập số giấy phép (tùy chọn)"
            disabled={loading}
          />
        </div>
      </div>

      <div className="form-group">
        <label htmlFor="licenseExpiryDate">Ngày hết hạn giấy phép</label>
        <div className="input-group">
          <input
            type="date"
            id="licenseExpiryDate"
            name="licenseExpiryDate"
            value={formData.licenseExpiryDate}
            onChange={handleInputChange}
            disabled={loading}
            style={{ paddingLeft: '12px' }}
          />
        </div>
      </div>
    </div>
  );

  return (
    <div className="register-screen">
      <div className="register-container">
        <div className="register-header">
          <div className="register-logo">
            <Building2 size={48} />
          </div>
          <h1>Đăng ký tài khoản</h1>
          <p>Tham gia hệ thống quản lý chuỗi cung ứng dược phẩm</p>
        </div>

        {/* Progress indicator */}
        <div className="progress-indicator">
          <div className={`progress-step ${currentStep >= 1 ? 'active' : ''}`}>
            <div className="step-number">1</div>
            <span>Tài khoản</span>
          </div>
          <div className="progress-line"></div>
          <div className={`progress-step ${currentStep >= 2 ? 'active' : ''}`}>
            <div className="step-number">2</div>
            <span>Công ty</span>
          </div>
        </div>

        {error && (
          <div className="error-message">
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="register-form">
          {currentStep === 1 && renderStep1()}
          {currentStep === 2 && renderStep2()}

          <div className="form-actions">
            {currentStep > 1 && (
              <button
                type="button"
                className="prev-button"
                onClick={handlePrevStep}
                disabled={loading}
              >
                Quay lại
              </button>
            )}
            
            {currentStep < 2 ? (
              <button
                type="button"
                className="next-button"
                onClick={handleNextStep}
                disabled={loading}
              >
                Tiếp tục
              </button>
            ) : (
              <button
                type="submit"
                className="submit-button"
                disabled={loading}
              >
                {loading ? 'Đang đăng ký...' : 'Hoàn tất đăng ký'}
              </button>
            )}
          </div>
        </form>

        <div className="register-footer">
          <p>
            Đã có tài khoản?{' '}
            <button
              type="button"
              className="link-button"
              onClick={onSwitchToLogin}
              disabled={loading}
            >
              Đăng nhập ngay
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterScreen;

