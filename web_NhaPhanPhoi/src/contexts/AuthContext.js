import React, { createContext, useContext, useState, useEffect } from 'react';
import authService from '../services/authService';

const AuthContext = createContext();

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('distributor_token');
      
      if (token) {
        // Verify token and get user info
        const userInfo = await authService.getCurrentUser();
        if (userInfo.success) {
          setUser(userInfo.data);
        } else {
          // Token invalid, remove it
          localStorage.removeItem('distributor_token');
          localStorage.removeItem('distributor_user');
        }
      }
    } catch (error) {
      console.error('Auth check failed:', error);
      localStorage.removeItem('distributor_token');
      localStorage.removeItem('distributor_user');
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await authService.login(email, password);
      
      if (response.success) {
        const { accessToken, user: userData } = response.data;
        
        // Save token and user data
        localStorage.setItem('distributor_token', accessToken);
        localStorage.setItem('distributor_user', JSON.stringify(userData));
        localStorage.setItem('walletAddress', userData.walletAddress || '');
        
        setUser(userData);
        return { success: true };
      } else {
        setError(response.message || 'Đăng nhập thất bại');
        return { success: false, error: response.message };
      }
    } catch (error) {
      console.error('Login error:', error);
      const errorMessage = error.message || 'Đã có lỗi xảy ra khi đăng nhập';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const register = async (registerData) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await authService.register(registerData);
      
      if (response.success) {
        const { accessToken, user: userData } = response.data;
        
        // Save token and user data
        localStorage.setItem('distributor_token', accessToken);
        localStorage.setItem('distributor_user', JSON.stringify(userData));
        localStorage.setItem('walletAddress', userData.walletAddress || '');
        
        setUser(userData);
        return { success: true };
      } else {
        setError(response.message || 'Đăng ký thất bại');
        return { success: false, error: response.message };
      }
    } catch (error) {
      console.error('Register error:', error);
      const errorMessage = error.message || 'Đã có lỗi xảy ra khi đăng ký';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('distributor_token');
    localStorage.removeItem('distributor_user');
    localStorage.removeItem('walletAddress');
    setUser(null);
    setError(null);
  };

  const updateUser = (updatedUser) => {
    setUser(updatedUser);
    localStorage.setItem('distributor_user', JSON.stringify(updatedUser));
  };

  const clearError = () => {
    setError(null);
  };

  const value = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    updateUser,
    clearError,
    isAuthenticated: !!user,
    isVerified: user?.isVerified || false
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

