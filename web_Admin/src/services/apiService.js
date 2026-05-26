import axios from 'axios';

// Base configuration
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('manufacturer_token') || localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error);

    if (error.response?.status === 401) {
      localStorage.removeItem('manufacturer_token');
      localStorage.removeItem('manufacturer_user');
      localStorage.removeItem('walletAddress');
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

// Helpers
const toDateTimeString = (input) => {
  const d = new Date(input);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// Blockchain API calls
const blockchainAPI = {
  // Create new batch (core function) -> POST /api/blockchain/drugs/batches
  createBatch: async (batchData) => {
    // Ensure proper date formatting for LocalDateTime
    const formatExpiryDate = (dateString) => {
      if (!dateString) return null;
      // If already has time component, use as is
      if (dateString.includes('T')) {
        return dateString;
      }
      // Otherwise add time component for end of day
      return `${dateString}T23:59:59`;
    };

    const payload = {
      drugName: batchData.productName,
      manufacturer: batchData.manufacturer,
      batchNumber: batchData.id,
      quantity: parseInt(batchData.quantity) || 0,
      expiryDate: formatExpiryDate(batchData.expiryDate),
      storageConditions: batchData.storageConditions || 'Bảo quản ở nhiệt độ phòng'
    };

    // Debug log
    console.log('Creating batch with payload:', payload);
    try {
      const response = await apiClient.post('/blockchain/drugs/batches', payload);
      return response;
    } catch (error) {
      console.error('Batch creation error details:', error.response?.data);
      console.error('Full error response:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        headers: error.response?.headers
      });
      console.error('Payload that caused error:', JSON.stringify(payload, null, 2));
      throw error;
    }
  },

  // Create shipment -> POST /api/blockchain/drugs/shipments
  createShipment: async (shipmentData) => {
    console.log('API Service - Creating shipment with data:', shipmentData);

    // Prepare payload for CreateShipmentRequest
    const payload = {
      batchId: String(shipmentData.batchId), // BigInteger as string
      toAddress: String(shipmentData.toAddress || shipmentData.pharmacyAddress),
      quantity: parseInt(shipmentData.quantity),
      trackingInfo: shipmentData.trackingInfo || `Shipment for batch ${shipmentData.batchId}`
    };

    console.log('API Service - Sending payload to /blockchain/drugs/shipments:', payload);

    try {
      const response = await apiClient.post('/blockchain/drugs/shipments', payload);
      console.log('API Service - Response:', response);
      return response;
    } catch (error) {
      console.error('API Service - Error details:', error.response?.data || error.message);
      throw error;
    }
  },

  // Get batch details -> GET /api/blockchain/drugs/batches/{batchId}
  getBatchById: async (batchId) => {
    return await apiClient.get(`/blockchain/drugs/batches/${batchId}`);
  },

  // Get manufacturer statistics
  getManufacturerStats: async () => {
    try {
      return await apiClient.get('/blockchain/manufacturer/stats');
    } catch (error) {
      console.error('Failed to get manufacturer stats:', error.message);
      throw error;
    }
  }
};

// Manufacturer Service
const manufacturerService = {
  // Dashboard
  getDashboardData: async () => {
    try {
      // Try to get real stats from API
      const response = await blockchainAPI.getManufacturerStats();
      if (response.success) {
        return response;
      }
      throw new Error('Failed to get manufacturer stats');
    } catch (error) {
      console.error('Failed to get dashboard data:', error.message);
      throw error;
    }
  },

  // Image Upload
  uploadImage: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const token = localStorage.getItem('manufacturer_token') || localStorage.getItem('authToken');
      console.log('Uploading file:', file.name, 'size:', file.size, 'type:', file.type);
      console.log('Upload URL:', `${API_BASE_URL}/upload`);

      const response = await fetch(`${API_BASE_URL}/upload`, {
        method: 'POST',
        headers: {
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: formData
      });

      const responseData = await response.json();
      console.log('Upload response:', response.status, responseData);

      if (!response.ok) {
        throw new Error(responseData.message || `Upload failed with status: ${response.status}`);
      }

      return responseData;
    } catch (error) {
      console.error('Failed to upload image:', error.message);
      throw error;
    }
  },

  // Product Management
  getProducts: async () => {
    try {
      // Use real manufacturer products
      const response = await apiClient.get('/products');
      if (response.success && response.data) {
        // Backend already returns drug_products; no transform/duplication
        return { success: true, data: response.data };
      }
      throw new Error('Invalid response format');
    } catch (error) {
      console.error('Failed to get products from API:', error.message);
      throw error;
    }
  },

  createProduct: async (productData) => {
    try {
      return await apiClient.post('/products', productData);
    } catch (error) {
      console.error('Failed to create product:', error.message);
      throw error;
    }
  },

  updateProduct: async (productId, productData) => {
    try {
      return await apiClient.put(`/products/${productId}`, productData);
    } catch (error) {
      console.error('Failed to update product:', error.message);
      throw error;
    }
  },

  deleteProduct: async (productId) => {
    try {
      return await apiClient.delete(`/products/${productId}`);
    } catch (error) {
      console.error('Failed to delete product:', error.message);
      throw error;
    }
  },

  // Batch Allocation - Core function
  createBatch: async (batchData) => {
    try {
      return await blockchainAPI.createBatch(batchData);
    } catch (error) {
      console.error('Failed to create batch:', error.message);
      throw error;
    }
  },

  getBatches: async () => {
    try {
      return await apiClient.get('/blockchain/drugs/batches');
    } catch (error) {
      console.error('Failed to get batches:', error.message);
      throw error;
    }
  },

  getBatchesReadyForShipment: async () => {
    try {
      return await apiClient.get('/blockchain/drugs/batches/ready-for-shipment');
    } catch (error) {
      console.error('Failed to get batches ready for shipment:', error.message);
      throw error;
    }
  },

  getDistributors: async () => {
    try {
      const response = await apiClient.get('/blockchain/drugs/distributors');

      // ✅ FORCE UPDATE: Nếu backend trả về tên cũ "Nhà phân phối XYZ", đổi ngay tại đây
      if (response.success && response.data) {
        const updatedData = response.data.map(d => ({
          ...d,
          name: (d.name.includes('XYZ') || d.name.includes('Distributor')) ? 'CPC1 Hà Nội' : d.name,
          address: (d.address.includes('XYZ') || !d.address) ? '15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội' : d.address
        }));

        // Nếu danh sách rỗng (do backend chưa seed), thêm CPC1 vào
        if (updatedData.length === 0) {
          updatedData.push({
            id: 2, // ID giả định của NPP
            name: 'CPC1 Hà Nội',
            address: '15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội',
            walletAddress: '0x70997970C51812dc3A010C7d01b50e0d17dc79C8' // Hardhat Account #1 typically used for Distributor
          });
        }
        return { ...response, data: updatedData };
      }
      return response;
    } catch (error) {
      console.error('Failed to get distributors:', error.message);
      // Fallback nếu API lỗi
      return {
        success: true,
        data: [
          { id: 2, name: 'CPC1 Hà Nội', address: '15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội', walletAddress: '0x70997970C51812dc3A010C7d01b50e0d17dc79C8' }
        ]
      };
    }
  },

  // Shipment Management
  createShipment: async (shipmentData) => {
    try {
      return await blockchainAPI.createShipment(shipmentData);
    } catch (error) {
      console.error('Failed to create shipment:', error.message);
      throw error;
    }
  },

  getShipments: async () => {
    try {
      return await apiClient.get('/blockchain/drugs/shipments');
    } catch (error) {
      console.error('Failed to get shipments:', error.message);
      throw error;
    }
  },

  updateShipmentStatus: async (shipmentId, status) => {
    try {
      return await apiClient.patch(`/blockchain/drugs/shipments/${shipmentId}/status`, { status });
    } catch (error) {
      console.error('Failed to update shipment status:', error.message);
      throw error;
    }
  },

  // Reports
  getProductionReport: async (dateRange) => {
    try {
      return await apiClient.get('/reports/production', { params: dateRange });
    } catch (error) {
      console.error('Failed to get production report:', error.message);
      throw error;
    }
  },

  getShipmentReport: async (dateRange) => {
    try {
      return await apiClient.get('/reports/shipments', { params: dateRange });
    } catch (error) {
      console.error('Failed to get shipment report:', error.message);
      throw error;
    }
  },

  // Account Management
  getCompanyInfo: async (companyId) => {
    try {
      const response = await apiClient.get(`/companies/${companyId}`);
      return response;
    } catch (error) {
      console.error('Failed to get company info:', error.message);
      // Return default data if API fails
      return {
        success: true,
        data: {
          id: companyId,
          name: 'Dược Hậu Giang',
          address: '288 Bis Nguyễn Văn Cừ, An Hòa, Ninh Kiều, Cần Thơ',
          phone: '0292 3891433',
          email: 'contact@dhhg.com',
          license: 'GPL-2024-001',
          website: 'https://dhhg.com',
          type: 'MANUFACTURER'
        }
      };
    }
  },

  updateCompanyInfo: async (companyId, companyData) => {
    try {
      const response = await apiClient.put(`/companies/${companyId}`, companyData);
      return response;
    } catch (error) {
      console.error('Failed to update company info:', error.message);
      // Simulate success for now
      return {
        success: true,
        message: 'Thông tin đã được lưu (chế độ demo)',
        data: companyData
      };
    }
  },

  getEmployees: async () => {
    try {
      return await apiClient.get('/account/employees');
    } catch (error) {
      console.error('Failed to get employees:', error.message);
      throw error;
    }
  },

  // Verification
  verifyBatch: async (batchId) => {
    try {
      return await blockchainAPI.getBatchById(batchId);
    } catch (error) {
      console.error('Failed to verify batch:', error.message);
      throw error;
    }
  }
};

// Export default
export default manufacturerService;
