import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import ProductManagement from './components/ProductManagement';
import BatchAllocation from './components/BatchAllocation';
import ShipmentManagement from './components/ShipmentManagement';
import Reports from './components/Reports';
import AccountManagement from './components/AccountManagement';
import LoginScreen from './components/auth/LoginScreen';
import BatchProductManagement from './components/BatchProductManagement';

import RawMaterialManagement from './components/RawMaterialManagement';

// Auth wrapper component
function AuthWrapper() {
  const { isAuthenticated, loading } = useAuth();
  const [authMode, setAuthMode] = useState('login');

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-spinner"></div>
        <p>Đang kiểm tra đăng nhập...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginScreen onSwitchToRegister={() => setAuthMode('register')} />;
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/products" replace />} />
        <Route path="/login" element={<Navigate to="/products" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/products" element={<ProductManagement />} />
        <Route path="/raw-materials" element={<RawMaterialManagement />} />
        <Route path="/batch-allocation" element={<BatchAllocation />} />
        <Route path="/batch-products" element={<BatchProductManagement />} />
        <Route path="/shipments" element={<ShipmentManagement />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/account" element={<AccountManagement />} />
      </Routes>
    </Layout>
  );
}

function App() {
  return (
    <AuthProvider>
      <Router basename={process.env.PUBLIC_URL || ''}>
        <AuthWrapper />
      </Router>
    </AuthProvider>
  );
}

export default App;
