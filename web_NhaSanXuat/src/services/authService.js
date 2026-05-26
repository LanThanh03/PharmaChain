import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Create axios instance for manufacturer auth
const authClient = axios.create({
  baseURL: `${API_BASE_URL}/manufacturer/auth`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include token
authClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('manufacturer_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle token expiration
authClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('manufacturer_token');
      localStorage.removeItem('manufacturer_user');
      localStorage.removeItem('walletAddress');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const manufacturerAuthService = {
  async login(email, password) {
    try {
      const response = await authClient.post('/login', {
        email,
        password
      });
      
      return {
        success: response.data.success,
        data: response.data.data,
        message: response.data.message
      };
    } catch (error) {
      console.error('Login API error:', error);
      
      if (error.response?.data) {
        return {
          success: false,
          message: error.response.data.message || 'Đăng nhập thất bại'
        };
      }
      
      return {
        success: false,
        message: 'Không thể kết nối đến server'
      };
    }
  },

  async register(registerData) {
    try {
      const response = await authClient.post('/register', registerData);
      
      return {
        success: response.data.success,
        data: response.data.data,
        message: response.data.message
      };
    } catch (error) {
      console.error('Register API error:', error);
      
      if (error.response?.data) {
        return {
          success: false,
          message: error.response.data.message || 'Đăng ký thất bại'
        };
      }
      
      return {
        success: false,
        message: 'Không thể kết nối đến server'
      };
    }
  },

  async getCurrentUser() {
    try {
      const response = await authClient.get('/me');
      
      return {
        success: response.data.success,
        data: response.data.data,
        message: response.data.message
      };
    } catch (error) {
      console.error('Get current user error:', error);
      
      return {
        success: false,
        message: 'Không thể lấy thông tin người dùng'
      };
    }
  },

  async logout() {
    try {
      await authClient.post('/logout');
    } catch (error) {
      console.error('Logout API error:', error);
    } finally {
      // Always clear local storage
      localStorage.removeItem('manufacturer_token');
      localStorage.removeItem('manufacturer_user');
      localStorage.removeItem('walletAddress');
    }
  }
};

// Utility functions
export const getStoredToken = () => {
  return localStorage.getItem('manufacturer_token');
};

export const getStoredUser = () => {
  const userStr = localStorage.getItem('manufacturer_user');
  try {
    return userStr ? JSON.parse(userStr) : null;
  } catch (error) {
    console.error('Error parsing stored user:', error);
    return null;
  }
};

export const isTokenValid = () => {
  const token = getStoredToken();
  if (!token) return false;

  try {
    // Basic JWT structure check
    const parts = token.split('.');
    if (parts.length !== 3) return false;

    // Decode payload to check expiration
    const payload = JSON.parse(atob(parts[1]));
    const currentTime = Date.now() / 1000;
    
    return payload.exp > currentTime;
  } catch (error) {
    console.error('Token validation error:', error);
    return false;
  }
};
