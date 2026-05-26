import axios from 'axios';

// Create axios instance with base configuration
const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Resolve image URL — đảm bảo ảnh upload từ server luôn dùng đúng host.
 * Trường hợp 1: URL ngoài (https://cms-prod...) → giữ nguyên
 * Trường hợp 2: URL localhost (http://localhost:8080/uploads/...) → thay bằng host thực của backend
 * Trường hợp 3: Đường dẫn tương đối (/uploads/...) → ghép với base URL backend
 */
export function resolveImageUrl(imageUrl) {
  if (!imageUrl) return null;

  // URL bên ngoài (CDN, fptcloud, v.v.) — không thay đổi
  if (imageUrl.startsWith('http') && !imageUrl.includes('localhost') && !imageUrl.includes('127.0.0.1')) {
    return imageUrl;
  }

  // Lấy base URL của backend (bỏ phần /api)
  const backendBase = API_BASE_URL.replace(/\/api\/?$/, '');

  // URL chứa localhost hoặc 127.0.0.1 — thay bằng host thực của backend
  if (imageUrl.includes('localhost') || imageUrl.includes('127.0.0.1')) {
    // Giữ lại phần path sau host:port
    const urlObj = new URL(imageUrl);
    return backendBase + urlObj.pathname;
  }

  // Đường dẫn tương đối như /uploads/xxx.jpg
  if (imageUrl.startsWith('/')) {
    return backendBase + imageUrl;
  }

  return imageUrl;
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // Increased for blockchain operations
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('pharmacy_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle API responses
apiClient.interceptors.response.use(
  (response) => {
    let data = response.data;

    // If data is a string, try to parse it as JSON
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data);
      } catch (e) {
        console.warn('Failed to parse response as JSON:', e);
      }
    }

    // If data has ApiResponse structure, return it directly
    if (data && typeof data === 'object' && 'success' in data) {
      return data;
    }

    // Otherwise wrap in success response
    return { success: true, data: data, message: 'Success' };
  },
  (error) => {
    console.error('API Error:', error);

    const errorMessage = error.response?.data?.message ||
      error.message ||
      'Có lỗi xảy ra khi kết nối với server';

    return Promise.reject(new Error(errorMessage));
  }
);

// Pharmacy service for hiệu thuốc
export const pharmacyService = {
  // Get available batches (inventory) - NOW USING pharmacy_inventory
  getInventory: async () => {
    try {
      const walletAddress = localStorage.getItem('walletAddress');
      if (!walletAddress) {
        console.warn('No wallet address found');
        return { success: true, data: [] };
      }

      const response = await apiClient.get(`/pharmacy/inventory/wallet/${walletAddress}`);
      console.log('Inventory API response:', response);

      // Parse string response if needed
      let parsedResponse = response;
      if (typeof response.data === 'string') {
        try {
          parsedResponse = JSON.parse(response.data);
        } catch (e) {
          console.error('Failed to parse response:', e);
          return { success: true, data: [] };
        }
      }

      // Extract data array from nested ApiResponse structure
      const inventoryData = parsedResponse.data?.data || parsedResponse.data || [];
      if (!Array.isArray(inventoryData)) {
        console.error('Inventory data is not an array:', inventoryData);
        return { success: true, data: [] };
      }

      // Transform pharmacy inventory to frontend format
      const inventory = inventoryData.map(raw => {
        const toNumber = (v, fallback = 0) => {
          const n = typeof v === 'number' ? v : parseFloat(v);
          return Number.isFinite(n) ? n : fallback;
        };
        const item = raw || {};
        const quantity = toNumber(item.quantity);
        const availableQuantity = toNumber(item.availableQuantity);
        const reservedQuantity = toNumber(item.reservedQuantity);
        const soldQuantity = toNumber(item.soldQuantity);
        const minStock = toNumber(item.minStockLevel, 20);
        const maxStock = toNumber(item.maxStockLevel, 500);
        const unitPrice = toNumber(item.costPrice, getEstimatedPrice(item.drugName));
        const retailPrice = toNumber(item.retailPrice);
        const totalValue = toNumber(item.totalValue, quantity * unitPrice);
        const profitMargin = toNumber(item.profitMargin);
        return {
          id: item.id,
          name: item.drugName,
          batchCode: item.batchNumber,
          category: getCategoryFromDrugName(item.drugName),
          manufacturer: item.manufacturer || 'N/A',
          currentStock: quantity,
          availableQuantity,
          reservedQuantity,
          soldQuantity,
          minStock,
          maxStock,
          unitPrice,
          retailPrice,
          totalValue,
          profitMargin,
          expiryDate: item.expiryDate ? String(item.expiryDate).split('T')[0] : '',
          manufactureDate: item.manufactureDate ? String(item.manufactureDate).split('T')[0] : '',
          location: item.shelfLocation || 'Kệ chính',
          lastUpdated: item.updatedAt || item.createdAt,
          status: item.status ? String(item.status).toLowerCase() : 'in_stock',
          storageConditions: item.storageConditions || 'Nơi khô ráo, tránh ánh sáng',
          batchNumber: item.batchNumber,
          qrCode: item.qrCode,
          isVerified: !!item.isVerified,
          isFeatured: !!item.isFeatured,
          isOnSale: !!item.isOnSale,
          requiresPrescription: !!item.requiresPrescription,
          // Additional pharmacy-specific fields
          receivedDate: item.receivedDate,
          receivedQuantity: toNumber(item.receivedQuantity),
          firstSaleDate: item.firstSaleDate,
          lastSaleDate: item.lastSaleDate,
          averageDailySales: toNumber(item.averageDailySales),
          daysOfSupply: toNumber(item.daysOfSupply)
        };
      });
      return { success: true, data: inventory };
    } catch (error) {
      console.error('Failed to get inventory:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  // Get medications from backend
  getMedications: async () => {
    try {
      return await apiClient.get('/medications');
    } catch (error) {
      console.error('Failed to get medications:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  // Get pharmacies
  getPharmacies: async () => {
    try {
      return await apiClient.get('/pharmacies');
    } catch (error) {
      console.error('Failed to get pharmacies:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  // Dashboard stats
  getDashboardStats: async () => {
    try {
      const inventoryResponse = await pharmacyService.getInventory();
      if (inventoryResponse.success) {
        const inventory = inventoryResponse.data;
        const lowStockItems = inventory.filter(item => item.currentStock <= item.minStock);
        const expiringItems = inventory.filter(item => {
          if (!item.expiryDate) return false;
          const expiryDate = new Date(item.expiryDate);
          const thirtyDaysFromNow = new Date();
          thirtyDaysFromNow.setDate(thirtyDaysFromNow.getDate() + 30);
          return expiryDate <= thirtyDaysFromNow;
        });

        return {
          success: true,
          data: {
            pendingReceive: 0, // TODO: Implement shipments tracking
            totalInventory: inventory.reduce((sum, item) => sum + item.currentStock, 0),
            lowStockItems: lowStockItems.length,
            expiringItems: expiringItems.length,
            inventory: inventory
          }
        };
      }
      return { success: false, data: null };
    } catch (error) {
      console.error('Failed to get dashboard stats:', error);
      return { success: false, data: null, message: error.message };
    }
  },

  // Get batch information by batch code/QR code
  getBatchInfo: async (batchCode) => {
    try {
      const response = await apiClient.get(`/blockchain/drugs/batch/${batchCode}`);
      if (response.success && response.data) {
        const batch = response.data;
        return {
          success: true,
          data: {
            id: batch.id,
            name: batch.drugName,
            manufacturer: batch.manufacturer || 'N/A',
            batchNumber: batch.batchNumber,
            quantity: batch.quantity,
            expireDate: batch.expiryDate ? batch.expiryDate.split(' ')[0] : '',
            manufactureDate: batch.manufactureTimestamp ? batch.manufactureTimestamp.split(' ')[0] : '',
            qrCode: batch.qrCode,
            transactionHash: batch.transactionHash,
            currentOwner: batch.currentOwner || 'N/A',
            storageConditions: batch.storageConditions || 'Nơi khô ráo, tránh ánh sáng'
          }
        };
      }
      return { success: false, data: null, message: 'Không tìm thấy thông tin lô hàng' };
    } catch (error) {
      console.error('Failed to get batch info:', error);
      throw error;
    }
  },

  // Get shipment by tracking code or shipment ID
  getShipmentInfo: async (trackingCode) => {
    try {
      // Try to get from blockchain shipments first
      const response = await apiClient.get(`/blockchain/shipments/${trackingCode}`);
      if (response.success && response.data) {
        return { success: true, data: response.data };
      }

      return { success: false, data: null, message: 'Không tìm thấy thông tin lô hàng' };
    } catch (error) {
      console.error('Failed to get shipment info:', error);
      throw error;
    }
  },

  // Get shipments by batch identifier (Số lô or Batch ID)
  // ✅ UPDATED: Now supports searching by both batchNumber (BT202512121857) and batchId (blockchain ID)
  getShipmentsByBatch: async (identifier) => {
    try {
      let searchTerm = String(identifier).trim();

      // Remove common prefixes
      if (searchTerm.toUpperCase().startsWith('SHIP-')) {
        searchTerm = searchTerm.substring(5);
      } else if (searchTerm.toUpperCase().startsWith('BATCH-')) {
        searchTerm = searchTerm.substring(6);
      }

      console.log('🔍 getShipmentsByBatch: Searching for:', searchTerm, '(original:', identifier, ')');

      // ✅ Use smart search API that supports both batchNumber and batchId
      // This API will try batchNumber first, then batchId
      const response = await apiClient.get(`/blockchain/drugs/batches/search/${encodeURIComponent(searchTerm)}/shipments`);

      console.log('📦 Smart search response:', response);
      return response;

    } catch (error) {
      console.error('Failed to get shipments by batch:', error.message);

      // Fallback: Try the old API with numeric batch ID
      try {
        const numericId = String(identifier).trim();
        if (/^\d+$/.test(numericId)) {
          console.log('📍 Fallback: Trying numeric batch ID:', numericId);
          const fallbackResponse = await apiClient.get(`/blockchain/drugs/batches/${numericId}/shipments`);
          return fallbackResponse;
        }
      } catch (fallbackError) {
        console.error('Fallback also failed:', fallbackError.message);
      }

      throw error;
    }
  },

  // Confirm receipt of goods and update blockchain ownership
  confirmReceiveGoods: async (data) => {
    try {
      const response = await apiClient.post('/blockchain/shipments/confirm-receipt', data);
      if (response.success) {
        return {
          success: true,
          data: {
            transactionHash: response.data.transactionHash,
            shipmentId: data.shipmentId,
            confirmedAt: new Date().toISOString()
          },
          message: 'Đã xác nhận nhận hàng và cập nhật blockchain'
        };
      }
      return { success: false, message: response.message || 'Không thể xác nhận nhận hàng' };
    } catch (error) {
      console.error('Failed to confirm receipt:', error);
      throw error;
    }
  },

  // Create new shipment (for distributors)
  createShipment: async (shipmentData) => {
    try {
      const response = await apiClient.post('/blockchain/shipments/create', shipmentData);
      if (response.success) {
        return {
          success: true,
          data: response.data,
          message: 'Đã tạo lô hàng thành công'
        };
      }
      return { success: false, message: response.message || 'Không thể tạo lô hàng' };
    } catch (error) {
      console.error('Failed to create shipment:', error);
      throw error;
    }
  },

  // Verify batch authenticity for counter sales
  verifyBatchAuthenticity: async (batchCode) => {
    try {
      const response = await apiClient.get(`/public/verify/batch/${batchCode}`);
      console.log('Verify batch response:', response);

      // API returns { success: true, data: { verified, batch, blockchain, ... } }
      if (response.success && response.data && response.data.verified) {
        const batchInfo = response.data.batch;
        const blockchainInfo = response.data.blockchain;

        return {
          success: true,
          data: {
            verified: true,
            batch: {
              drugName: batchInfo.drugName,
              batchNumber: batchInfo.batchNumber,
              manufacturer: batchInfo.manufacturer,
              manufactureTimestamp: batchInfo.manufactureTimestamp,
              expiryDate: batchInfo.expiryDate,
              quantity: batchInfo.quantity,
              status: batchInfo.status,
              storageConditions: batchInfo.storageConditions,
              qrCode: batchInfo.qrCode,
              transactionHash: batchInfo.transactionHash,
              blockNumber: batchInfo.blockNumber,
              // Item-specific info (if available)
              itemCode: batchInfo.itemCode,
              currentOwner: batchInfo.currentOwner,
              // ✅ FIX: Product detail fields needed for image display
              imageUrl: batchInfo.imageUrl,
              description: batchInfo.description,
              activeIngredient: batchInfo.activeIngredient,
              dosage: batchInfo.dosage || batchInfo.drugDosage,
              soldAt: batchInfo.soldAt
            },
            blockchain: {
              verified: blockchainInfo?.verified || false,
              transactionHash: blockchainInfo?.transactionHash || batchInfo.transactionHash,
              blockNumber: blockchainInfo?.blockNumber || batchInfo.blockNumber || 'N/A',
              timestamp: blockchainInfo?.timestamp || batchInfo.createdAt
            },
            // ✅ NEW: Pass through traceabilityHistory from API for full TX tracking
            traceabilityHistory: response.data.traceabilityHistory || []
          }
        };
      }

      // Check for partial success (data exists but not verified)
      if (response.data) {
        return {
          success: false,
          data: { verified: false },
          message: response.data.message || 'Không thể xác thực lô hàng'
        };
      }

      return { success: false, data: { verified: false }, message: 'Không thể xác thực lô hàng' };
    } catch (error) {
      console.error('Failed to verify batch:', error);
      throw error;
    }
  },

  // ✅ NEW: Sell individual product item at pharmacy counter
  sellItem: async (itemCode, pharmacyId, pharmacyName, buyerInfo) => {
    try {
      const params = new URLSearchParams();
      if (pharmacyName) params.append('pharmacyName', pharmacyName);
      if (buyerInfo) params.append('buyerInfo', buyerInfo);

      const url = `/pharmacy/inventory/company/${pharmacyId}/item/${encodeURIComponent(itemCode)}/sell${params.toString() ? '?' + params.toString() : ''}`;
      const response = await apiClient.post(url);
      console.log('Sell item response:', response);
      return response;
    } catch (error) {
      console.error('Failed to sell item:', error);
      throw error;
    }
  },

  // ✅ NEW: Check item sale status
  getItemStatus: async (itemCode, pharmacyId) => {
    try {
      const response = await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/item/${encodeURIComponent(itemCode)}/status`);
      return response;
    } catch (error) {
      console.error('Failed to get item status:', error);
      throw error;
    }
  },

  // ✅ NEW: Report item as damaged
  reportDamagedItem: async (itemCode, payload) => {
    try {
      const response = await apiClient.post(`/product-items/${encodeURIComponent(itemCode)}/report-damage`, payload);
      return response;
    } catch (error) {
      console.error('Failed to report damaged item:', error);
      throw error;
    }
  },

  // ✅ NEW: Upload image file
  uploadImage: async (file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await apiClient.post('/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response;
    } catch (error) {
      console.error('Failed to upload image:', error);
      throw error;
    }
  },

  // Get list of pharmacies (for distributors)
  getPharmacies: async () => {
    try {
      const response = await apiClient.get('/pharmacies');
      return response;
    } catch (error) {
      console.error('Failed to get pharmacies:', error);
      throw error;
    }
  },

  // Get pending shipments for current pharmacy
  getPendingShipments: async () => {
    try {
      // Get current user's wallet address
      const userData = localStorage.getItem('pharmacy_user');
      if (!userData) {
        throw new Error('User not authenticated');
      }

      let user;
      try {
        user = JSON.parse(userData);
      } catch (e) {
        console.warn('Invalid pharmacy_user JSON');
        throw new Error('User not authenticated');
      }
      if (!user.walletAddress) {
        throw new Error('User wallet address not found');
      }

      // Get shipments specifically for this wallet address
      const response = await apiClient.get(`/blockchain/drugs/shipments/recipient/${user.walletAddress}`);
      return response;
    } catch (error) {
      console.error('Failed to get pending shipments:', error);
      throw error;
    }
  },

  // Get shipment by ID
  getShipmentById: async (shipmentId) => {
    try {
      const response = await apiClient.get(`/blockchain/drugs/shipments/${shipmentId}`);
      return response;
    } catch (error) {
      console.error('Failed to get shipment details:', error);
      throw error;
    }
  },

  // Verify shipment ownership on blockchain
  verifyShipmentOwnership: async (shipmentId, expectedOwner) => {
    try {
      const response = await apiClient.get(`/blockchain/drugs/shipments/${shipmentId}/verify-ownership`, {
        params: { expectedOwner }
      });
      return response;
    } catch (error) {
      console.error('Failed to verify shipment ownership:', error);
      throw error;
    }
  },

  // Receive shipment (confirm receipt on blockchain)
  receiveShipment: async (shipmentId) => {
    try {
      const response = await apiClient.post(`/blockchain/drugs/shipments/${shipmentId}/receive`);
      return response;
    } catch (error) {
      console.error('Failed to receive shipment:', error);
      throw error;
    }
  },

  // Get all shipments (for distributor/manufacturer to manage)
  getAllShipments: async () => {
    try {
      const response = await apiClient.get('/blockchain/drugs/shipments');
      return response;
    } catch (error) {
      console.error('Failed to get all shipments:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  // ==================== PHARMACY INVENTORY APIs ====================

  // Get pharmacy inventory by wallet address
  getPharmacyInventory: async (walletAddress) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/wallet/${walletAddress}`);
    } catch (error) {
      console.error('Failed to get pharmacy inventory:', error);
      throw error;
    }
  },

  // Get low stock items
  getPharmacyLowStock: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/low-stock`);
    } catch (error) {
      console.error('Failed to get low stock items:', error);
      throw error;
    }
  },

  // Get items need reorder
  getPharmacyNeedReorder: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/need-reorder`);
    } catch (error) {
      console.error('Failed to get items need reorder:', error);
      throw error;
    }
  },

  // Get expiring soon items
  getPharmacyExpiringSoon: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/expiring-soon`);
    } catch (error) {
      console.error('Failed to get expiring soon items:', error);
      throw error;
    }
  },

  // Search pharmacy inventory
  searchPharmacyInventory: async (pharmacyId, searchTerm) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/search`, {
        params: { searchTerm }
      });
    } catch (error) {
      console.error('Failed to search inventory:', error);
      throw error;
    }
  },

  // Get featured products
  getPharmacyFeatured: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/featured`);
    } catch (error) {
      console.error('Failed to get featured products:', error);
      throw error;
    }
  },

  // Get products on sale
  getPharmacyOnSale: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/on-sale`);
    } catch (error) {
      console.error('Failed to get products on sale:', error);
      throw error;
    }
  },

  // Get inventory values (cost, retail, profit)
  getPharmacyInventoryValues: async (pharmacyId) => {
    try {
      return await apiClient.get(`/pharmacy/inventory/company/${pharmacyId}/values`);
    } catch (error) {
      console.error('Failed to get inventory values:', error);
      throw error;
    }
  },

  // Record a sale
  recordSale: async (pharmacyId, batchId, quantity) => {
    try {
      return await apiClient.post(`/pharmacy/inventory/company/${pharmacyId}/batch/${batchId}/sale`, null, {
        params: { quantity }
      });
    } catch (error) {
      console.error('Failed to record sale:', error);
      throw error;
    }
  },

  // ==================== DISPENSE WITH INSTRUCTIONS APIs ====================

  /**
   * Dispense item with detailed usage instructions
   * @param {Object} request - Dispense request with instructions
   * @param {string} request.itemCode - Product item code
   * @param {string} request.customerName - Customer name (optional)
   * @param {string} request.customerPhone - Customer phone (optional)
   * @param {string} request.dosage - Dosage info, e.g., "1 viên"
   * @param {number} request.frequency - Times per day (1, 2, 3, 4)
   * @param {string} request.mealRelation - BEFORE, AFTER, WITH, ANY
   * @param {string} request.specificTimes - Comma-separated times, e.g., "08:00,12:00,20:00"
   * @param {number} request.durationDays - Number of days to take medication
   * @param {string} request.specialNotes - Special notes from pharmacist
   * @param {string} request.pharmacistName - Pharmacist name
   * @param {number} request.salePrice - Sale price
   */
  dispenseWithInstructions: async (request) => {
    try {
      const pharmacyId = localStorage.getItem('pharmacy_company_id') || '1';
      const pharmacyName = localStorage.getItem('pharmacy_company_name') || 'Pharmacy'; // ASCII only for HTTP headers

      const response = await apiClient.post('/pharmacy/dispense-with-instructions', request, {
        headers: {
          'X-Pharmacy-Id': pharmacyId,
          'X-Pharmacy-Name': pharmacyName
        }
      });

      console.log('Dispense with instructions response:', response);
      return response;
    } catch (error) {
      console.error('Failed to dispense with instructions:', error);
      throw error;
    }
  },

  /**
   * Get dispense instruction by item code (for checking before sale)
   */
  getDispenseInstruction: async (itemCode) => {
    try {
      const response = await apiClient.get(`/pharmacy/dispense-with-instructions/by-item/${encodeURIComponent(itemCode)}`);
      return response;
    } catch (error) {
      console.error('Failed to get dispense instruction:', error);
      throw error;
    }
  },

  /**
   * Get all dispense instructions for a customer by phone
   */
  getDispenseInstructionsByCustomer: async (phone) => {
    try {
      const response = await apiClient.get(`/pharmacy/dispense-with-instructions/by-customer`, {
        params: { phone }
      });
      return response;
    } catch (error) {
      console.error('Failed to get customer instructions:', error);
      throw error;
    }
  },

  /**
   * Get today's dispense statistics
   */
  getTodayDispenseStats: async () => {
    try {
      const pharmacyId = localStorage.getItem('pharmacy_company_id') || '1';
      const response = await apiClient.get('/pharmacy/dispense-with-instructions/stats/today', {
        headers: {
          'X-Pharmacy-Id': pharmacyId
        }
      });
      return response;
    } catch (error) {
      console.error('Failed to get dispense stats:', error);
      throw error;
    }
  }
};

// Helper functions
function getCategoryFromDrugName(drugName) {
  const categories = {
    'Paracetamol': 'Giảm đau hạ sốt',
    'Ibuprofen': 'Giảm đau hạ sốt',
    'Aspirin': 'Thuốc tim mạch',
    'Amoxicillin': 'Kháng sinh',
    'Vitamin': 'Vitamin & KCS',
    'Metformin': 'Thuốc tiểu đường',
    'Omeprazole': 'Thuốc tiêu hóa',
    'Cetirizine': 'Thuốc dị ứng'
  };

  for (const [key, category] of Object.entries(categories)) {
    if (drugName.toLowerCase().includes(key.toLowerCase())) {
      return category;
    }
  }
  return 'Khác';
}

function getEstimatedPrice(drugName) {
  const prices = {
    'Paracetamol': 500,
    'Ibuprofen': 800,
    'Aspirin': 300,
    'Amoxicillin': 1200,
    'Vitamin': 2000,
    'Metformin': 1500,
    'Omeprazole': 2500,
    'Cetirizine': 1000
  };

  for (const [key, price] of Object.entries(prices)) {
    if (drugName.toLowerCase().includes(key.toLowerCase())) {
      return price;
    }
  }
  return 1000; // Default price
}

function getStockStatus(quantity) {
  if (quantity <= 0) return 'out_of_stock';
  if (quantity <= 50) return 'low_stock';
  if (quantity <= 100) return 'medium_stock';
  return 'good';
}

// Add company management methods to pharmacyService
pharmacyService.getCompanyInfo = async (companyId) => {
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
        name: 'Long Châu',
        address: '379-381 Hai Bà Trưng, Phường 8, Quận 3, TP.HCM',
        phone: '1800 6928',
        email: 'cskh@nhathuoclongchau.com',
        license: 'GPL-2024-003',
        website: 'https://nhathuoclongchau.com',
        type: 'PHARMACY'
      }
    };
  }
};

pharmacyService.updateCompanyInfo = async (companyId, companyData) => {
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
};

export default pharmacyService;