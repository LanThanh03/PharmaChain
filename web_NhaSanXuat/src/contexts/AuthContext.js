import React, { createContext, useContext, useState, useEffect } from 'react';
import { manufacturerAuthService } from '../services/authService';

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
      const token = localStorage.getItem('manufacturer_token');
      const storedUser = localStorage.getItem('manufacturer_user');
      
      if (token && storedUser) {
        try {
          // First, try to use stored user data
          const userData = JSON.parse(storedUser);
          setUser(userData);
          
          // Then verify token in background (optional)
          // This prevents blocking the UI if server is down
          manufacturerAuthService.getCurrentUser()
            .then(userInfo => {
              if (userInfo.success) {
                setUser(userInfo.data);
                localStorage.setItem('manufacturer_user', JSON.stringify(userInfo.data));
              } else {
                // Token invalid, remove it
                localStorage.removeItem('manufacturer_token');
                localStorage.removeItem('manufacturer_user');
                localStorage.removeItem('walletAddress');
                setUser(null);
              }
            })
            .catch(error => {
              console.warn('Background auth check failed:', error);
              // Don't clear user data if it's just a network error
              // Only clear if it's a 401 (handled by interceptor)
            });
        } catch (parseError) {
          console.error('Error parsing stored user:', parseError);
          localStorage.removeItem('manufacturer_token');
          localStorage.removeItem('manufacturer_user');
          localStorage.removeItem('walletAddress');
        }
      }
    } catch (error) {
      console.error('Auth check failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const login = async (email, password) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await manufacturerAuthService.login(email, password);
      
      if (response.success) {
        const { accessToken, user: userData } = response.data;
        
        // Save token and user data
        localStorage.setItem('manufacturer_token', accessToken);
        localStorage.setItem('manufacturer_user', JSON.stringify(userData));
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
      
      const response = await manufacturerAuthService.register(registerData);
      
      if (response.success) {
        const { accessToken, user: userData } = response.data;
        
        // Save token and user data
        localStorage.setItem('manufacturer_token', accessToken);
        localStorage.setItem('manufacturer_user', JSON.stringify(userData));
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
    localStorage.removeItem('manufacturer_token');
    localStorage.removeItem('manufacturer_user');
    localStorage.removeItem('walletAddress');
    setUser(null);
    setError(null);
  };

  const updateUser = (updatedUser) => {
    setUser(updatedUser);
    localStorage.setItem('manufacturer_user', JSON.stringify(updatedUser));
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
