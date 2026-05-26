import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import ReceiveGoods from './components/ReceiveGoods';
import BatchManagement from './components/BatchManagement';
import CreateShipment from './components/CreateShipment';
import ExportManagement from './components/ExportManagement';
import ShipmentTracking from './components/ShipmentTracking';
import Reports from './components/Reports';
import LoginScreen from './components/auth/LoginScreen';
import RegisterScreen from './components/auth/RegisterScreen';
import AccountManagement from './components/AccountManagement';

function AuthWrapper() {
  const { isAuthenticated, loading } = useAuth();
  const [showRegister, setShowRegister] = useState(false);

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-spinner"></div>
        <p>Đang kiểm tra đăng nhập...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return showRegister ? (
      <RegisterScreen onSwitchToLogin={() => setShowRegister(false)} />
    ) : (
      <LoginScreen onSwitchToRegister={() => setShowRegister(true)} />
    );
  }

  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/receive-goods" replace />} />
        <Route path="/login" element={<Navigate to="/receive-goods" replace />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/receive-goods" element={<ReceiveGoods />} />
        <Route path="/batches" element={<BatchManagement />} />
        <Route path="/create-shipment" element={<CreateShipment />} />
        <Route path="/export-management" element={<ExportManagement />} />
        <Route path="/shipments" element={<ShipmentTracking />} />
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

