import React, { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  Home,
  Package,
  Plus,
  Truck,
  BarChart3,
  Menu,
  X,
  Bell,
  User,
  Settings,
  Download,
  Upload,
  LogOut,
  Building2,
  ChevronDown
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import './Layout.css';

const Layout = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const location = useLocation();
  const { user, logout } = useAuth();

  const navigationItems = [
    {
      name: 'Nhập kho (NSX)',
      href: '/receive-goods',
      icon: Download
    },
    {
      name: 'Quản lý Kho',
      href: '/batches',
      icon: Package
    },
    {
      name: 'Tạo vận đơn xuất kho',
      href: '/create-shipment',
      icon: Upload
    },
    {
      name: 'Quản lý Xuất kho',
      href: '/export-management',
      icon: BarChart3
    },
    {
      name: 'Theo dõi Vận chuyển',
      href: '/shipments',
      icon: Truck
    },

    {
      name: 'Tài khoản',
      href: '/account',
      icon: User
    }
  ];

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  return (
    <div className="layout">
      {/* Mobile sidebar overlay */}
      {sidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <div className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-header">
          <div className="logo">
            <Package className="logo-icon" />
            <span className="logo-text">Nhà Phân Phối</span>
          </div>
          <button
            className="sidebar-close mobile-only"
            onClick={() => setSidebarOpen(false)}
          >
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.name}
                to={item.href}
                className={({ isActive }) =>
                  `nav-item ${isActive ? 'nav-item-active' : ''}`
                }
                onClick={() => setSidebarOpen(false)}
              >
                <Icon size={20} />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">
              <Building2 size={16} />
            </div>
            <div className="user-details">
              <div className="user-name">{user?.name || 'Người dùng'}</div>
              <div className="user-role">{user?.companyName || 'Công ty'}</div>
              <div className="user-status">
                {user?.isVerified ? (
                  <span className="status-verified">✓ Đã xác minh</span>
                ) : (
                  <span className="status-pending">⏳ Chờ xác minh</span>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="main-content">
        {/* Header */}
        <header className="header">
          <div className="header-left">
            <button
              className="menu-button"
              onClick={toggleSidebar}
            >
              <Menu size={20} />
            </button>
            <h1 className="page-title">
              {navigationItems.find(item => item.href === location.pathname)?.name || 'Dashboard'}
            </h1>
          </div>

          <div className="header-right">
            <button className="notification-button">
              <Bell size={20} />
              <span className="notification-badge">3</span>
            </button>

            <div className="blockchain-status">
              <div className="status-indicator status-connected"></div>
              <span className="status-text">Blockchain Connected</span>
            </div>

            {/* User menu */}
            <div className="user-menu-container">
              <button
                className="user-menu-trigger"
                onClick={() => setUserMenuOpen(!userMenuOpen)}
              >
                <div className="user-avatar-small">
                  <User size={16} />
                </div>
                <span className="user-name-header">{user?.name}</span>
                <ChevronDown size={16} />
              </button>

              {userMenuOpen && (
                <div className="user-menu-dropdown">
                  <div className="user-menu-header">
                    <div className="user-info-header">
                      <strong>{user?.name}</strong>
                      <small>{user?.email}</small>
                      <small>{user?.companyName}</small>
                    </div>
                  </div>

                  <div className="user-menu-divider"></div>

                  <button className="user-menu-item">
                    <Settings size={16} />
                    <span>Cài đặt</span>
                  </button>

                  <button className="user-menu-item">
                    <User size={16} />
                    <span>Hồ sơ</span>
                  </button>

                  <div className="user-menu-divider"></div>

                  <button
                    className="user-menu-item logout"
                    onClick={() => {
                      logout();
                      setUserMenuOpen(false);
                    }}
                  >
                    <LogOut size={16} />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Content */}
        <main className="content">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
