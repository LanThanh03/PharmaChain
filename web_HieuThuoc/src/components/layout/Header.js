import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Bell, LogOut, Menu } from 'lucide-react';
import './Header.css';

function Header() {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    if (window.confirm('Bạn có chắc chắn muốn đăng xuất?')) {
      logout();
    }
  };

  return (
    <header className="header">
      <div className="header-left">
        <button className="mobile-menu-btn">
          <Menu />
        </button>
        <h1 className="page-title">
          {user.role === 'distributor' ? 'Nhà Phân Phối' : 'Hiệu Thuốc'} Dashboard
        </h1>
      </div>

      <div className="header-right">
        <button className="notification-btn">
          <Bell />
          <span className="notification-badge">3</span>
        </button>
        
        <div className="user-menu">
          <div className="user-info">
            <span className="user-name">{user.name}</span>
            <span className="user-email">{user.email}</span>
          </div>
          <div className="user-avatar">
            {user.name.charAt(0)}
          </div>
        </div>

        <button className="logout-btn" onClick={handleLogout} title="Đăng xuất">
          <LogOut />
        </button>
      </div>
    </header>
  );
}

export default Header;


