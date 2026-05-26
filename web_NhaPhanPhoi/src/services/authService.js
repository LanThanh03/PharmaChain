import { apiClient } from './apiService';

const authService = {
  login: async (email, password) => {
    try {
      const response = await apiClient.post('/distributor/auth/login', { email, password });
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Auth Service - Login failed:', error);
      return { success: false, message: error.message };
    }
  },

  register: async (registerData) => {
    try {
      const response = await apiClient.post('/distributor/auth/register', registerData);
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Auth Service - Register failed:', error);
      return { success: false, message: error.message };
    }
  },

  logout: async () => {
    // For stateless JWT, logout is primarily client-side.
    // If backend invalidation is needed, add an API call here.
    return { success: true };
  },

  getCurrentUser: async () => {
    try {
      const response = await apiClient.get('/distributor/auth/me');
      return { success: true, data: response.data };
    } catch (error) {
      console.error('Auth Service - Get current user failed:', error);
      return { success: false, message: error.message };
    }
  }
};

export default authService;