import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { 
  LayoutDashboard, 
  Package, 
  Truck, 
  ShoppingCart,
  Warehouse,
  Building2
} from 'lucide-react';
import './Sidebar.css';

function Sidebar() {
  const { user } = useAuth();
  const location = useLocation();

  const distributorMenuItems = [
    {
      path: '/dashboard',
      icon: LayoutDashboard,
      label: 'Trang chủ',
      exact: true
    },
    {
      path: '/dashboard/create-shipment',
      icon: Package,
      label: 'Tạo lô hàng mới'
    },
    {
      path: '/dashboard/manage-shipments',
      icon: Truck,
      label: 'Quản lý lô hàng'
    }
  ];

  const pharmacyMenuItems = [
    {
      path: '/dashboard',
      icon: LayoutDashboard,
      label: 'Trang chủ',
      exact: true
    },
    {
      path: '/dashboard/receive-goods',
      icon: ShoppingCart,
      label: 'Nhận hàng'
    },
    {
      path: '/dashboard/manage-inventory',
      icon: Warehouse,
      label: 'Quản lý kho'
    }
  ];

  const menuItems = user.role === 'distributor' ? distributorMenuItems : pharmacyMenuItems;

  const isActive = (path, exact = false) => {
    if (exact) {
      return location.pathname === path;
    }
    return location.pathname.startsWith(path);
  };

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <Building2 className="sidebar-logo" />
        <div className="sidebar-title">
          <h2>Partner Portal</h2>
          <p>{user.role === 'distributor' ? 'Nhà Phân Phối' : 'Hiệu Thuốc'}</p>
        </div>
      </div>

      <nav className="sidebar-nav">
        <ul>
          {menuItems.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.path}>
                <Link
                  to={item.path}
                  className={`nav-link ${isActive(item.path, item.exact) ? 'active' : ''}`}
                >
                  <Icon className="nav-icon" />
                  <span>{item.label}</span>
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="sidebar-footer">
        <div className="user-info">
          <div className="user-avatar">
            {user.name.charAt(0)}
          </div>
          <div className="user-details">
            <p className="user-name">{user.name}</p>
            <p className="user-role">
              {user.role === 'distributor' ? 'Nhà Phân Phối' : 'Hiệu Thuốc'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Sidebar;


