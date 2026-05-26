import React from 'react';
import { AlertTriangle, Mail, Phone } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import './VerificationBanner.css';

function VerificationBanner() {
  const { user } = useAuth();

  if (!user || user.isVerified) {
    return null;
  }

  return (
    <div className="verification-banner">
      <div className="banner-content">
        <div className="banner-icon">
          <AlertTriangle size={24} />
        </div>
        
        <div className="banner-text">
          <h3>Tài khoản chưa được xác minh</h3>
          <p>
            Tài khoản của bạn đang chờ xác minh từ quản trị viên. 
            Một số tính năng có thể bị hạn chế cho đến khi tài khoản được xác minh.
          </p>
          
          <div className="contact-info">
            <div className="contact-item">
              <Mail size={16} />
              <span>support@pharmachain.com</span>
            </div>
            <div className="contact-item">
              <Phone size={16} />
              <span>1900-123-456</span>
            </div>
          </div>
        </div>
        
        <div className="banner-actions">
          <button className="contact-button">
            Liên hệ hỗ trợ
          </button>
        </div>
      </div>
    </div>
  );
}

export default VerificationBanner;
