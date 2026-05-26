import React, { useState, useEffect } from 'react';
import { Shield, Activity, Search, ExternalLink, Database, RefreshCw } from 'lucide-react';
import { adminClient } from '../services/authService';
import './BlockchainHistory.css';

const BlockchainHistory = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchTransactions = () => {
    setLoading(true);
    adminClient.get('/transactions')
      .then(response => {
        if (Array.isArray(response.data)) {
          setTransactions(response.data);
        } else {
          console.error("API did not return an array", response.data);
          setTransactions([]);
        }
        setLoading(false);
      })
      .catch(err => {
        console.error("Error fetching transactions", err);
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  return (
    <div className="admin-page-container">
      <div className="page-header">
        <div className="header-title">
          <Shield size={28} color="#4f46e5" />
          <h2>Lịch sử Giao dịch (Ledger)</h2>
        </div>
        <div className="header-actions">
          <div className="search-bar">
            <Search size={18} color="#6b7280" />
            <input type="text" placeholder="Tìm TxHash, Wallet..." />
          </div>
          <button className="btn-primary" onClick={fetchTransactions} disabled={loading}>
            <RefreshCw size={18} className={loading ? 'spin' : ''} />
            Đồng bộ Sepolia
          </button>
        </div>
      </div>

      <div className="ledger-card">
        <div className="ledger-header">
          <h3>Giao dịch gần đây trên PharmaChain</h3>
          <span className="live-badge"><Activity size={14} className="pulse"/> Live</span>
        </div>

        <table className="admin-table">
          <thead>
            <tr>
              <th>TxHash</th>
              <th>Loại Giao dịch</th>
              <th>Người gửi (From)</th>
              <th>Trạng thái</th>
              <th>Gas Used</th>
              <th>Thời gian</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((tx, idx) => (
              <tr key={idx}>
                <td className="tx-hash">{tx.id}</td>
                <td><span className={`tx-type type-${idx % 3}`}>{tx.type}</span></td>
                <td><span className="wallet-address">{tx.from}</span></td>
                <td>
                  <span className={`status-pill ${tx.status.toLowerCase()}`}>
                    {tx.status}
                  </span>
                </td>
                <td>{tx.gas}</td>
                <td className="time-text">{tx.time}</td>
                <td>
                  {tx.fullHash && tx.fullHash.startsWith('0x') ? (
                    <a href={`https://sepolia.etherscan.io/tx/${tx.fullHash}`} target="_blank" rel="noopener noreferrer" className="btn-icon">
                      <ExternalLink size={18} />
                    </a>
                  ) : (
                    <span className="btn-icon disabled"><ExternalLink size={18} /></span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default BlockchainHistory;
