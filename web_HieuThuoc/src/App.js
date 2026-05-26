import React from "react"
import {
  BrowserRouter as Router,
  Routes,
  Route,
  Navigate,
} from "react-router-dom"
import { AuthProvider, useAuth } from "./contexts/AuthContext"
import Layout from "./components/Layout"
import Dashboard from "./components/Dashboard"
import ReceiveGoods from "./components/ReceiveGoods"
import InventoryManagement from "./components/InventoryManagement"
import CounterVerification from "./components/CounterVerification"
import Reports from "./components/Reports"
import AccountManagement from "./components/AccountManagement"
import LoginScreen from "./components/auth/LoginScreen"
import { Selling } from "./components/Selling"

// Protected Route Component
function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()

  if (loading) {
    return <div>Đang tải...</div>
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
      />
    )
  }

  return children
}

// App Routes Component
function AppRoutes() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route
        path="/login"
        element={
          user ? (
            <Navigate
              to="/receive-goods"
              replace
            />
          ) : (
            <LoginScreen />
          )
        }
      />
      <Route
        path="/"
        element={
          <Navigate
            to="/receive-goods"
            replace
          />
        }
      />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Layout>
              <Dashboard />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/receive-goods"
        element={
          <ProtectedRoute>
            <Layout>
              <ReceiveGoods />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/inventory"
        element={
          <ProtectedRoute>
            <Layout>
              <InventoryManagement />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/verification"
        element={
          <ProtectedRoute>
            <Layout>
              <CounterVerification />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <ProtectedRoute>
            <Layout>
              <Reports />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/selling"
        element={
          <ProtectedRoute>
            <Layout>
              <Selling />
            </Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/account"
        element={
          <ProtectedRoute>
            <Layout>
              <AccountManagement />
            </Layout>
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

function App() {
  return (
    <Router basename={process.env.PUBLIC_URL || ''}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}

export default App
