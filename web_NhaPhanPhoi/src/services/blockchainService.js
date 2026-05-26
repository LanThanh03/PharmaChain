import axios from 'axios';

// JAVA SPRING BOOT BACKEND
const BLOCKCHAIN_API_URL = process.env.REACT_APP_BLOCKCHAIN_API_URL || 'http://localhost:8080/api';

const blockchainClient = axios.create({
  baseURL: BLOCKCHAIN_API_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

blockchainClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('distributor_token') || localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

blockchainClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Blockchain API Error:', error);
    return Promise.reject(error);
  }
);

const blockchainService = {
  createShipment: async (shipmentData) => {
    try {
      const payload = {
        batchID: String(shipmentData.batchId),
        to: String(shipmentData.toAddress || shipmentData.pharmacyAddress),
        quantity: Number(shipmentData.quantity),
        trackingNumber: shipmentData.trackingNumber || `TRK-${Date.now()}`,
      };
      
      const response = await blockchainClient.post('/distributor/shipment', payload);
      return {
        success: true,
        data: { shipmentID: response.data.shipmentID },
        message: 'Shipment created'
      };
    } catch (error) {
      throw error;
    }
  },

  getAllBatches: async () => {
    const response = await blockchainClient.get('/batches');
    return { success: true, data: response.data || [] };
  },

  getBatchById: async (batchId) => {
    const response = await blockchainClient.get(`/batches/${batchId}`);
    return { success: true, data: response.data };
  },

  verifyDrug: async (qrCode) => {
    const response = await blockchainClient.post('/public/verify', { qrCode });
    return { success: true, data: response.data };
  },
};

export default blockchainService;

