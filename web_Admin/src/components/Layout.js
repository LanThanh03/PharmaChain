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
  Shield,
  Users,
  LogOut,
  ChevronDown,
  UserCog,
  QrCode
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
      name: 'Thống kê Hệ thống',
      href: '/dashboard',
      icon: BarChart3
    },
    {
      name: 'Quản lý Tài khoản',
      href: '/account',
      icon: Users
    },
    {
      name: 'Lịch sử Blockchain',
      href: '/blockchain',
      icon: Shield
    }
  ];

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="sidebar-header">
          <div className="logo">
            <Shield size={32} color="#4f46e5" />
            <span className="logo-text">Admin Portal</span>
          </div>
          <button className="sidebar-close" onClick={toggleSidebar}>
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar-nav">
          <ul className="nav-list">
            {navigationItems.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.href;

              return (
                <li key={item.href} className="nav-item">
                  <NavLink
                    to={item.href}
                    className={`nav-link ${isActive ? 'nav-link-active' : ''}`}
                    onClick={() => setSidebarOpen(false)}
                  >
                    <Icon size={20} />
                    <span>{item.name}</span>
                  </NavLink>
                </li>
              );
            })}
          </ul>
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">
              <UserCog size={16} />
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
      </aside>

      {/* Sidebar Overlay */}
      {sidebarOpen && (
        <div className="sidebar-overlay" onClick={toggleSidebar} />
      )}

      {/* Main Content */}
      <div className="main-wrapper">
        {/* Header */}
        <header className="header">
          <div className="header-left">
            <button className="sidebar-toggle" onClick={toggleSidebar}>
              <Menu size={20} />
            </button>
            <h1 className="page-title">Hệ Thống Quản Trị Quốc Gia (Admin)</h1>
          </div>

          <div className="header-right">
            <button className="header-btn">
              <Bell size={20} />
              <span className="notification-badge">3</span>
            </button>

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

        {/* Page Content */}
        <main className="main-content">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
