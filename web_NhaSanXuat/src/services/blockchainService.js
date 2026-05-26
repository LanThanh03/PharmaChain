import axios from 'axios';

// JAVA SPRING BOOT BACKEND - CHỈ XỬ LÝ BLOCKCHAIN
const BLOCKCHAIN_API_URL = process.env.REACT_APP_BLOCKCHAIN_API_URL || 'http://localhost:8080/api/blockchain';

// Create axios instance for blockchain backend
const blockchainClient = axios.create({
  baseURL: BLOCKCHAIN_API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
blockchainClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('manufacturer_token') || localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor  
blockchainClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Blockchain API Error:', error);
    return Promise.reject(error);
  }
);

// Blockchain Service for Manufacturer
const blockchainService = {
  /**
   * Issue new batch - POST /api/blockchain/manufacturer/batch
   */
  issueBatch: async (batchData) => {
    try {
      const payload = {
        drugName: batchData.productName,
        activeIngredient: batchData.activeIngredient || 'Paracetamol',
        dosage: batchData.dosage || '500mg',
        manufacturerName: batchData.manufacturer,
        registrationNumber: batchData.registrationNumber || 'VD-12345-18',
        quantity: parseInt(batchData.quantity),
        manufactureDate: batchData.manufactureDate || new Date().toISOString().split('T')[0],
        expiryDate: batchData.expiryDate,
      };
      
      console.log('Issuing batch to Golang backend:', payload);
      const response = await blockchainClient.post('/manufacturer/batch', payload);
      
      return {
        success: true,
        data: {
          batchID: response.data.batchID,
          qrCode: response.data.qrCode,
          transactionHash: response.data.transactionHash || 'N/A',
        },
        message: response.message || 'Batch issued successfully'
      };
    } catch (error) {
      console.error('Failed to issue batch:', error);
      throw error;
    }
  },

  /**
   * Get all batches - GET /api/blockchain/batches
   */
  getAllBatches: async () => {
    try {
      const response = await blockchainClient.get('/batches');
      return {
        success: true,
        data: response.data || [],
        message: response.message || 'Success'
      };
    } catch (error) {
      console.error('Failed to get batches:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  /**
   * Get batch by ID - GET /api/blockchain/batches/:id
   */
  getBatchById: async (batchId) => {
    try {
      const response = await blockchainClient.get(`/batches/${batchId}`);
      return {
        success: true,
        data: response.data,
        message: response.message || 'Success'
      };
    } catch (error) {
      console.error('Failed to get batch:', error);
      throw error;
    }
  },

  /**
   * Create shipment - POST /api/blockchain/distributor/shipment
   */
  createShipment: async (shipmentData) => {
    try {
      const payload = {
        batchID: String(shipmentData.batchId),
        to: String(shipmentData.toAddress || shipmentData.pharmacyAddress),
        quantity: Number(shipmentData.quantity),
        trackingNumber: shipmentData.trackingInfo || `TRK-${Date.now()}`,
      };
      
      console.log('Creating shipment to Golang backend:', payload);
      const response = await blockchainClient.post('/distributor/shipment', payload);
      
      return {
        success: true,
        data: {
          shipmentID: response.data.shipmentID,
          transactionHash: response.data.transactionHash || 'N/A',
        },
        message: response.message || 'Shipment created successfully'
      };
    } catch (error) {
      console.error('Failed to create shipment:', error);
      throw error;
    }
  },

  /**
   * Get shipments by batch - GET /api/blockchain/shipments/batch/:batchId
   */
  getShipmentsByBatch: async (batchId) => {
    try {
      const response = await blockchainClient.get(`/shipments/batch/${batchId}`);
      return {
        success: true,
        data: response.data || [],
        message: response.message || 'Success'
      };
    } catch (error) {
      console.error('Failed to get shipments:', error);
      return { success: false, data: [], message: error.message };
    }
  },

  /**
   * Verify drug by QR - POST /api/blockchain/public/verify
   */
  verifyDrug: async (qrCode) => {
    try {
      const response = await blockchainClient.post('/public/verify', { qrCode });
      return {
        success: true,
        data: response.data,
        message: response.message || 'Verification successful'
      };
    } catch (error) {
      console.error('Failed to verify drug:', error);
      throw error;
    }
  },
};

export default blockchainService;

