import React, { useState } from "react"
import { NavLink, useLocation, useNavigate } from "react-router-dom"
import {
  Package,
  ShoppingCart,
  Shield,
  Menu,
  X,
  Bell,
  User,
  Settings,
  Heart,
  Users,
  LogOut,
} from "lucide-react"
import { useAuth } from "../contexts/AuthContext"
import "./Layout.css"

const Layout = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  const navigationItems = [
    {
      name: "Nhận hàng",
      href: "/receive-goods",
      icon: ShoppingCart,
    },
    {
      name: "Quản lý Kho",
      href: "/inventory",
      icon: Package,
    },
    {
      name: "Xác thực tại quầy",
      href: "/verification",
      icon: Shield,
    },
    {
      name: "Bán hàng",
      href: "/selling",
      icon: ShoppingCart,
    },
    {
      name: "Tài khoản",
      href: "/account",
      icon: Users,
    },
  ]

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen)
  }

  const handleLogout = () => {
    logout()
    navigate("/login")
  }

  return (
    <div className="app-layout">
      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? "sidebar-open" : ""}`}>
        <div className="sidebar-header">
          <div className="logo">
            <Heart size={32} />
            <span className="logo-text">Pharmacy Portal</span>
          </div>
          <button
            className="sidebar-close"
            onClick={toggleSidebar}>
            <X size={20} />
          </button>
        </div>

        <nav className="sidebar-nav">
          <ul className="nav-list">
            {navigationItems.map(item => {
              const Icon = item.icon
              const isActive = location.pathname === item.href

              return (
                <li
                  key={item.href}
                  className="nav-item">
                  <NavLink
                    to={item.href}
                    className={`nav-link ${isActive ? "nav-link-active" : ""}`}
                    onClick={() => setSidebarOpen(false)}>
                    <Icon size={20} />
                    <span>{item.name}</span>
                  </NavLink>
                </li>
              )
            })}
          </ul>
        </nav>

        <div className="sidebar-footer">
          <div className="company-info">
            <div className="company-logo">
              <Heart size={24} />
            </div>
            <div className="company-details">
              <div className="company-name">
                {user?.name || "Hiệu thuốc ABC"}
              </div>
              <div className="company-role">Hiệu thuốc</div>
            </div>
          </div>
        </div>
      </aside>

      {/* Sidebar Overlay */}
      {sidebarOpen && (
        <div
          className="sidebar-overlay"
          onClick={toggleSidebar}
        />
      )}

      {/* Main Content */}
      <div className="main-wrapper">
        {/* Header */}
        <header className="header">
          <div className="header-left">
            <button
              className="sidebar-toggle"
              onClick={toggleSidebar}>
              <Menu size={20} />
            </button>
            <h1 className="page-title">Pharmacy Portal</h1>
          </div>

          <div className="header-right">
            <button className="header-btn">
              <Bell size={20} />
              <span className="notification-badge">2</span>
            </button>

            <div className="user-menu">
              <button
                className="user-btn"
                onClick={() => setUserMenuOpen(!userMenuOpen)}>
                <User size={20} />
                <span>{user?.name || "Dược sĩ"}</span>
              </button>

              {userMenuOpen && (
                <div className="user-dropdown">
                  <div className="user-info">
                    <p className="user-name">{user?.name}</p>
                    <p className="user-email">{user?.email}</p>
                  </div>
                  <div className="dropdown-divider"></div>
                  <button
                    className="dropdown-item"
                    onClick={() => navigate("/account")}>
                    <Settings size={16} />
                    <span>Cài đặt</span>
                  </button>
                  <button
                    className="dropdown-item logout-btn"
                    onClick={handleLogout}>
                    <LogOut size={16} />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="main-content">{children}</main>
      </div>
    </div>
  )
}

export default Layout
