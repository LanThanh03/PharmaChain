import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Sidebar from './Sidebar';
import Header from './Header';
import MainContent from './MainContent';

// Distributor screens
import DistributorDashboard from '../distributor/DistributorDashboard';
import CreateShipment from '../distributor/CreateShipment';
import ManageShipments from '../distributor/ManageShipments';

// Pharmacy screens
import PharmacyDashboard from '../pharmacy/PharmacyDashboard';
import ReceiveGoods from '../pharmacy/ReceiveGoods';
import ManageInventory from '../pharmacy/ManageInventory';

import './Dashboard.css';

function Dashboard() {
  const { user } = useAuth();

  return (
    <div className="dashboard">
      <Sidebar />
      <div className="dashboard-main">
        <Header />
        <MainContent>
          <Routes>
            {user.role === 'distributor' ? (
              <>
                <Route path="/" element={<DistributorDashboard />} />
                <Route path="/create-shipment" element={<CreateShipment />} />
                <Route path="/manage-shipments" element={<ManageShipments />} />
              </>
            ) : (
              <>
                <Route path="/" element={<PharmacyDashboard />} />
                <Route path="/receive-goods" element={<ReceiveGoods />} />
                <Route path="/manage-inventory" element={<ManageInventory />} />
              </>
            )}
          </Routes>
        </MainContent>
      </div>
    </div>
  );
}

export default Dashboard;

