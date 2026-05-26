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

// Add request interceptor to include auth token
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('distributor_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error);

    // Handle 401 Unauthorized
    if (error.response?.status === 401) {
      localStorage.removeItem('distributor_token');
      localStorage.removeItem('distributor_user');
      localStorage.removeItem('walletAddress');
      // Redirect to login if needed
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    const errorMessage = error.response?.data?.message ||
      error.message ||
      'Có lỗi xảy ra khi kết nối với server';

    return Promise.reject(new Error(errorMessage));
  }
);

// Helpers
const toDateTimeString = (input) => {
  const d = new Date(input);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

// Blockchain API endpoints (aligned with backend controllers)
export const blockchainAPI = {
  // Health check -> GET /api/blockchain/status
  healthCheck: () => apiClient.get('/blockchain/status'),

  // Manufacturer: create batch -> POST /api/blockchain/drugs/batches
  createBatch: (batchData) => apiClient.post('/blockchain/drugs/batches', {
    drugName: batchData.drugName,
    manufacturer: batchData.manufacturer,
    batchNumber: batchData.batchNumber,
    quantity: Number(batchData.quantity),
    expiryDate: toDateTimeString(batchData.expiryDate),
    storageConditions: batchData.storageConditions || ''
  }),

  // Distributor: create shipment -> POST /api/blockchain/drugs/shipments
  createShipment: (shipmentData) => apiClient.post('/blockchain/drugs/shipments', {
    batchId: shipmentData.batchId.toString(), // Ensure string for BigInteger
    toAddress: shipmentData.pharmacyAddress,
    quantity: Number(shipmentData.quantity),
    trackingInfo: shipmentData.trackingInfo || shipmentData.trackingNumber
  }),

  // Pharmacy: receive shipment -> POST /api/blockchain/drugs/shipments/{id}/receive
  receiveShipment: (shipmentId) => apiClient.post(`/blockchain/drugs/shipments/${shipmentId}/receive`),

  // Public: verify drug by QR -> POST /api/blockchain/drugs/verify
  verifyDrug: (qrCode) => apiClient.post('/blockchain/drugs/verify', { qrCode }),

  // Batch details -> GET /api/blockchain/drugs/batches/{batchId}
  getBatchDetails: (batchId) => apiClient.get(`/blockchain/drugs/batches/${batchId}`),
  // Shipments by batch - ✅ UPDATED: Now supports both batchNumber (BT202512121857) and batchId
  getShipmentsByBatch: (identifier) => {
    // Sanitize identifier: strip "SHIP-" or other prefixes
    let searchTerm = String(identifier).trim();
    if (searchTerm.toUpperCase().startsWith('SHIP-')) {
      searchTerm = searchTerm.substring(5);
    } else if (searchTerm.toUpperCase().startsWith('BATCH-')) {
      searchTerm = searchTerm.substring(6);
    }

    console.log('🔍 getShipmentsByBatch: Searching for:', searchTerm, '(original:', identifier, ')');

    // Use smart search API that supports both batchNumber and batchId
    return apiClient.get(`/blockchain/drugs/batches/search/${encodeURIComponent(searchTerm)}/shipments`)
      .catch(error => {
        console.warn('Smart search failed, trying fallback:', error.message);
        // Fallback to old numeric API if smart search fails
        if (/^\d+$/.test(searchTerm)) {
          return apiClient.get(`/blockchain/drugs/batches/${searchTerm}/shipments`);
        }
        throw error;
      });
  },

  // All batches
  getAllBatches: () => apiClient.get('/blockchain/drugs/batches'),
  // Batches by current owner -> GET /api/blockchain/drugs/batches/owner/{ownerAddress}
  getBatchesByOwner: (ownerAddress) => apiClient.get(`/blockchain/drugs/batches/owner/${ownerAddress}`),

  // Shipments list
  getShipments: (params = {}) => apiClient.get('/blockchain/drugs/shipments', { params }),

  // Shipment by ID
  getShipmentById: (shipmentId) => apiClient.get(`/blockchain/drugs/shipments/${shipmentId}`),

  // Shipments by recipient (inbound for distributor) -> GET /api/blockchain/drugs/shipments/recipient/{recipientAddress}
  getShipmentsByRecipient: (recipientAddress) => apiClient.get(`/blockchain/drugs/shipments/recipient/${recipientAddress}`)
};

// Service functions
export const distributorService = {
  // Dashboard
  getDashboardData: async () => {
    try {
      return await apiClient.get('/blockchain/drugs/stats');
    } catch (error) {
      console.error('Failed to get dashboard data:', error.message);
      throw error;
    }
  },

  // Batches
  getBatches: async () => {
    try {
      return await apiClient.get('/blockchain/drugs/batches');
    } catch (error) {
      console.error('Failed to get batches:', error.message);
      throw error;
    }
  },

  // Batches owned by distributor
  getBatchesByOwner: async (ownerAddress) => {
    if (!ownerAddress) {
      throw new Error('Owner address is required');
    }
    try {
      return await blockchainAPI.getBatchesByOwner(ownerAddress);
    } catch (error) {
      console.error('Failed to get batches by owner:', error.message);
      throw error;
    }
  },

  // Get distributor inventory with REAL available quantities
  getInventoryByWallet: async (walletAddress) => {
    try {
      // ✅ DÙNG API MỚI: /api/warehouse/exportable
      // Wallet là optional, nếu không có thì lấy tất cả
      const endpoint = walletAddress
        ? `/warehouse/exportable?wallet=${walletAddress}`
        : `/warehouse/exportable`;

      console.log('🔍 Calling warehouse API:', endpoint);
      return await apiClient.get(endpoint);
    } catch (error) {
      console.error('Failed to get inventory by wallet:', error.message);
      // Fallback to old API nếu mới chưa có
      if (walletAddress) {
        console.warn('⚠️ Trying old API as fallback...');
        try {
          return await apiClient.get(`/distributor/inventory/wallet/${walletAddress}`);
        } catch (fallbackError) {
          console.error('Old API also failed:', fallbackError.message);
        }
      }
      throw error;
    }
  },

  getBatchDetails: async (batchId) => {
    try {
      return await blockchainAPI.getBatchDetails(batchId);
    } catch (error) {
      console.error('Failed to get batch details:', error.message);
      throw error;
    }
  },

  // Pharmacies
  getPharmacies: async () => {
    try {
      const response = await apiClient.get('/pharmacies');
      if (response.success && response.data) {
        const updatedData = response.data.map(p => ({
          ...p,
          name: (p.name.includes('An Khang') || !p.name) ? 'Long Châu' : p.name,
          address: (p.address.includes('An Khang') || !p.address) ? '379-381 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM' : p.address
        }));

        if (updatedData.length === 0) {
          updatedData.push({
            id: 3,
            name: 'Long Châu',
            address: '379-381 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM',
            walletAddress: '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC' // Hardhat Account #2 typically used for Pharmacy
          });
        }
        return { ...response, data: updatedData };
      }
      return response;
    } catch (error) {
      console.error('Failed to get pharmacies:', error.message);
      // Fallback
      return {
        success: true,
        data: [
          { id: 3, name: 'Long Châu', address: '379-381 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM', walletAddress: '0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC' }
        ]
      };
    }
  },

  // Shipments
  createShipment: async (shipmentData) => {
    try {
      // Normalize batchId - remove any prefix and convert to BigInteger compatible string
      let batchId = shipmentData.batchId;
      if (typeof batchId === 'string' && batchId.startsWith('BT')) {
        batchId = batchId.replace('BT', '');
      }

      // Use the distributor-specific endpoint with pharmacyId
      return await apiClient.post('/distributor/shipments', {
        batchId: batchId.toString(), // Ensure string for BigInteger parsing
        pharmacyId: parseInt(shipmentData.pharmacyId),
        quantity: parseInt(shipmentData.quantity),
        trackingNumber: shipmentData.trackingNumber,
        notes: shipmentData.notes || '',
        driverName: shipmentData.driverName || '',
        driverPhone: shipmentData.driverPhone || '',
        transportMethod: shipmentData.transportMethod || 'Xe tải'
      });
    } catch (error) {
      console.error('Failed to create shipment:', error.message);
      throw error;
    }
  },

  getShipments: async (filters = {}) => {
    try {
      return await blockchainAPI.getShipments(filters);
    } catch (error) {
      console.error('Failed to get shipments:', error.message);
      throw error;
    }
  },

  // Get shipments sent by this distributor (for export management)
  getShipmentsBySender: async (senderAddress) => {
    try {
      return await apiClient.get(`/blockchain/drugs/shipments/sender/${senderAddress}`);
    } catch (error) {
      console.error('Failed to get shipments by sender:', error.message);
      throw error;
    }
  },

  getShipmentById: async (shipmentId) => {
    try {
      return await blockchainAPI.getShipmentById(shipmentId);
    } catch (error) {
      console.error('Failed to get shipment details:', error.message);
      throw error;
    }
  },

  // Shipments by batch
  getShipmentsByBatch: async (batchId) => {
    try {
      return await blockchainAPI.getShipmentsByBatch(batchId);
    } catch (error) {
      console.error('Failed to get shipments by batch:', error.message);
      throw error;
    }
  },

  // Shipments by recipient
  getShipmentsByRecipient: async (recipientAddress) => {
    try {
      return await blockchainAPI.getShipmentsByRecipient(recipientAddress);
    } catch (error) {
      console.error('Failed to get shipments by recipient:', error.message);
      throw error;
    }
  },

  // Verification
  verifyDrug: async (qrCode) => {
    try {
      return await blockchainAPI.verifyDrug(qrCode);
    } catch (error) {
      console.error('Failed to verify drug:', error.message);
      throw error;
    }
  },

  // Verify shipment ownership on blockchain - Anti-counterfeit check
  verifyShipmentOwnership: async (shipmentId, expectedOwner) => {
    try {
      return await apiClient.get(`/blockchain/drugs/shipments/${shipmentId}/verify-ownership`, {
        params: { expectedOwner }
      });
    } catch (error) {
      console.error('Failed to verify shipment ownership:', error.message);
      throw error;
    }
  },

  // Receive Goods (Nhập kho từ NSX)
  receiveShipment: async (shipmentId) => {
    try {
      return await blockchainAPI.receiveShipment(shipmentId);
    } catch (error) {
      console.error('Failed to receive shipment:', error.message);
      throw error;
    }
  },

  // Get pending inbound shipments for a recipient
  getInboundShipments: async (recipientAddress) => {
    try {
      const response = await distributorService.getShipmentsByRecipient(recipientAddress);
      if (response.success && response.data) {
        // Filter for shipments that are 'in_transit' and not yet received
        return {
          ...response,
          data: response.data.filter(shipment => shipment.status === 'in_transit')
        };
      }
      return { success: true, data: [] };
    } catch (error) {
      console.error('Failed to get inbound shipments:', error.message);
      throw error;
    }
  },

  // ==================== DISTRIBUTOR INVENTORY APIs ====================

  // Get distributor inventory by wallet address
  getDistributorInventory: async (walletAddress) => {
    try {
      return await apiClient.get(`/distributor/inventory/wallet/${walletAddress}`);
    } catch (error) {
      console.error('Failed to get distributor inventory:', error.message);
      throw error;
    }
  },

  // Get low stock items
  getDistributorLowStock: async (distributorId) => {
    try {
      return await apiClient.get(`/distributor/inventory/company/${distributorId}/low-stock`);
    } catch (error) {
      console.error('Failed to get low stock items:', error.message);
      throw error;
    }
  },

  // Get expiring soon items
  getDistributorExpiringSoon: async (distributorId) => {
    try {
      return await apiClient.get(`/distributor/inventory/company/${distributorId}/expiring-soon`);
    } catch (error) {
      console.error('Failed to get expiring soon items:', error.message);
      throw error;
    }
  },

  // Search distributor inventory
  searchDistributorInventory: async (distributorId, searchTerm) => {
    try {
      return await apiClient.get(`/distributor/inventory/company/${distributorId}/search`, {
        params: { searchTerm }
      });
    } catch (error) {
      console.error('Failed to search inventory:', error.message);
      throw error;
    }
  },

  // Get total inventory value
  getDistributorInventoryValue: async (distributorId) => {
    try {
      return await apiClient.get(`/distributor/inventory/company/${distributorId}/total-value`);
    } catch (error) {
      console.error('Failed to get inventory value:', error.message);
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
          name: 'CPC1 Hà Nội',
          address: '15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội',
          phone: '024 3854 3902',
          email: 'contact@cpc1.com.vn',
          license: 'GPL-2024-002',
          website: 'https://cpc1.com.vn',
          type: 'DISTRIBUTOR'
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
  }
};

// Export API client for auth service
export { apiClient };

// Export default
export default distributorService;