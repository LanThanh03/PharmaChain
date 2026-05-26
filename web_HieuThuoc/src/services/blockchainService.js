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
    const token = localStorage.getItem('pharmacy_token') || localStorage.getItem('authToken');
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

// Blockchain Service for Pharmacy & Distributor
const blockchainService = {
  /**
   * Create shipment (Distributor) - POST /api/blockchain/distributor/shipment
   */
  createShipment: async (shipmentData) => {
    try {
      const payload = {
        batchID: String(shipmentData.batchId),
        to: String(shipmentData.toAddress || shipmentData.pharmacyAddress),
        quantity: Number(shipmentData.quantity),
        trackingNumber: shipmentData.trackingNumber || `TRK-${Date.now()}`,
      };
      
      console.log('Creating shipment:', payload);
      const response = await blockchainClient.post('/distributor/shipment', payload);
      
      return {
        success: true,
        data: {
          shipmentID: response.data.shipmentID,
          transactionHash: response.data.transactionHash || 'N/A',
        },
        message: response.message || 'Shipment created'
      };
    } catch (error) {
      console.error('Failed to create shipment:', error);
      throw error;
    }
  },

  /**
   * Receive shipment (Pharmacy) - POST /api/blockchain/pharmacy/receive/:id
   */
  receiveShipment: async (shipmentId) => {
    try {
      const response = await blockchainClient.post(`/pharmacy/receive/${shipmentId}`);
      
      return {
        success: true,
        data: {
          shipmentID: shipmentId,
          transactionHash: response.data.transactionHash || 'N/A',
          confirmedAt: new Date().toISOString(),
        },
        message: response.message || 'Shipment received successfully'
      };
    } catch (error) {
      console.error('Failed to receive shipment:', error);
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
   * Verify drug by QR - POST /api/blockchain/public/verify (PUBLIC)
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

