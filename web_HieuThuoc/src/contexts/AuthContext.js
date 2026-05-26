import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext();

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is logged in (from localStorage)
    const storedUser = localStorage.getItem('pharmacy_user');
    const storedToken = localStorage.getItem('pharmacy_token');
    if (storedUser && storedToken && storedUser !== 'undefined' && storedUser !== 'null' && storedUser.trim() !== '') {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.warn('Invalid pharmacy_user JSON in storage, clearing');
        localStorage.removeItem('pharmacy_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      // Call pharmacy login API
      const response = await axios.post(`${API_BASE_URL}/pharmacy/auth/login`, {
        email,
        password
      });

      if (response.data.success && response.data.data) {
        const authData = response.data.data;
        const pharmacy = authData.user;
        
        const userData = {
          id: pharmacy.id,
          email: pharmacy.email,
          name: pharmacy.pharmacyName,
          role: 'pharmacy',
          walletAddress: pharmacy.walletAddress,
          pharmacyCode: pharmacy.pharmacyCode,
          address: pharmacy.address,
          phone: pharmacy.phone,
          avatar: '/api/placeholder/40/40'
        };
        
        setUser(userData);
        localStorage.setItem('pharmacy_user', JSON.stringify(userData));
        localStorage.setItem('pharmacy_token', authData.accessToken);
        localStorage.setItem('walletAddress', pharmacy.walletAddress);
        
        return userData;
      } else {
        throw new Error(response.data.message || 'Đăng nhập thất bại');
      }
    } catch (error) {
      console.error('Login error:', error);
      throw new Error(error.response?.data?.message || error.message || 'Email hoặc mật khẩu không đúng');
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('pharmacy_user');
    localStorage.removeItem('pharmacy_token');
    localStorage.removeItem('walletAddress');
  };

  const value = {
    user,
    login,
    logout,
    loading
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

