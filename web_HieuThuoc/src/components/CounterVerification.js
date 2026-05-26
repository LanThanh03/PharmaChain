import React, { useState } from 'react';
import {
  Shield,
  Scan,
  CheckCircle,
  AlertCircle,
  Package,
  Factory,
  Truck,
  Search,
  QrCode,
  Clock,
  MapPin,
  FileText,
  Tag,
  Calendar,
  Thermometer,
  FlaskConical
} from 'lucide-react';
import pharmacyService, { resolveImageUrl } from '../services/apiService';
import SellWithInstructionsModal from './SellWithInstructionsModal';
import './CounterVerification.css';

const CounterVerification = () => {
  const [scanInput, setScanInput] = useState('');
  const [verificationResult, setVerificationResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [recentVerifications, setRecentVerifications] = useState([]);
  const [selling, setSelling] = useState(false);
  const [saleSuccess, setSaleSuccess] = useState(null);
  const [showSellModal, setShowSellModal] = useState(false);

  // Damaged Item States
  const [showDamagedModal, setShowDamagedModal] = useState(false);
  const [damagedReason, setDamagedReason] = useState('');
  const [damagedImage, setDamagedImage] = useState(null);
  const [isReportingDamaged, setIsReportingDamaged] = useState(false);
  const [reportSuccess, setReportSuccess] = useState(null);

  const handleScan = async () => {
    if (!scanInput || !scanInput.trim()) {
      setError('Vui lòng nhập mã QR hoặc mã lô thuốc');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSaleSuccess(null);
      setReportSuccess(null);

      let verificationResult;

      try {
        const response = await pharmacyService.verifyBatchAuthenticity(scanInput);

        if (response.success && response.data.verified) {
          const batch = response.data.batch;
          const blockchain = response.data.blockchain;
          const apiTraceabilityHistory = response.data.traceabilityHistory;

          const pharmacyUser = localStorage.getItem('pharmacy_user');
          let pharmacyName = 'Hiệu thuốc';
          if (pharmacyUser) {
            try {
              const user = JSON.parse(pharmacyUser);
              pharmacyName = user.pharmacyName || 'Hiệu thuốc';
            } catch (e) { }
          }

          let blockchainHistorySteps = [];

          if (apiTraceabilityHistory && apiTraceabilityHistory.length > 0) {
            blockchainHistorySteps = apiTraceabilityHistory.map((step, index) => ({
              step: step.step || index + 1,
              event: step.event,
              actor: step.actor,
              actorType: step.actorType,
              location: step.location || step.toLocation || 'N/A',
              timestamp: step.timestamp,
              txHash: step.txHash,
              blockNumber: step.blockNumber,
              shipmentCode: step.shipmentCode,
              details: step.details
            }));
          } else {
            blockchainHistorySteps = [
              {
                step: 1,
                event: 'Sản xuất',
                actor: batch.manufacturer,
                actorType: 'manufacturer',
                location: 'Nhà máy sản xuất',
                timestamp: batch.manufactureTimestamp || new Date().toISOString(),
                txHash: blockchain.transactionHash,
                details: 'Lô thuốc được sản xuất và ghi nhận lên blockchain'
              }
            ];

            if (batch.status === 'DELIVERED' || batch.status === 'IN_TRANSIT' || batch.currentOwner) {
              blockchainHistorySteps.push({
                step: 2,
                event: 'Vận chuyển & Phân phối',
                actor: 'Nhà phân phối',
                actorType: 'distributor',
                location: 'Trung tâm phân phối',
                timestamp: batch.manufactureTimestamp
                  ? new Date(new Date(batch.manufactureTimestamp).getTime() + 86400000).toISOString()
                  : new Date().toISOString(),
                txHash: null,
                details: 'Lô hàng được vận chuyển'
              });
            }

            blockchainHistorySteps.push({
              step: blockchainHistorySteps.length + 1,
              event: 'Xác thực blockchain',
              actor: 'Hệ thống Blockchain',
              actorType: 'system',
              location: 'Blockchain Network',
              timestamp: blockchain.timestamp || new Date().toISOString(),
              txHash: blockchain.transactionHash,
              details: `Xác thực thành công tại block #${blockchain.blockNumber}`
            });
          }

          blockchainHistorySteps.push({
            step: blockchainHistorySteps.length + 1,
            event: 'Sẵn sàng bán',
            actor: pharmacyName,
            actorType: 'pharmacy',
            location: `Quầy thuốc - ${pharmacyName}`,
            timestamp: new Date().toISOString(),
            txHash: null,
            details: 'Sản phẩm được xác thực và sẵn sàng bán cho khách hàng'
          });

          verificationResult = {
            isAuthentic: true,
            isSold: batch.status === 'SOLD',
            isDamaged: batch.status === 'DAMAGED',
            product: {
              name: batch.drugName,
              batchCode: batch.batchNumber,
              activeIngredient: batch.activeIngredient || batch.drugName.split(' ')[0],
              dosage: batch.dosage || (batch.drugName.includes('mg') ? batch.drugName.match(/\d+mg/)?.[0] || 'N/A' : 'N/A'),
              imageUrl: resolveImageUrl(batch.imageUrl),
              description: batch.description,
              manufacturer: batch.manufacturer,
              manufactureDate: batch.manufactureTimestamp ? batch.manufactureTimestamp.split('T')[0] : 'N/A',
              expiryDate: batch.expiryDate ? batch.expiryDate.split('T')[0] : 'N/A',
              qrCode: batch.qrCode || scanInput,
              storageConditions: batch.storageConditions || 'Nơi khô ráo, tránh ánh sáng trực tiếp',
              itemCode: batch.itemCode,
              currentOwner: batch.currentOwner,
              status: batch.status,
              soldAt: batch.soldAt,
            },
            blockchainHistory: blockchainHistorySteps,
            qualityInfo: {
              qualityGrade: 'A',
              testResults: 'Đạt chuẩn',
              certifications: ['GMP', 'ISO 9001', 'WHO-GMP'],
              batchSize: batch.quantity || 10000,
              productionLine: 'Dây chuyền A'
            },
            warnings: [],
            recommendations: [
              'Bảo quản nơi khô ráo, nhiệt độ dưới 30°C',
              'Tránh ánh sáng trực tiếp',
              'Kiểm tra hạn sử dụng trước khi bán'
            ]
          };

          if (verificationResult.product.expiryDate && verificationResult.product.expiryDate !== 'N/A') {
            const expiryDate = new Date(verificationResult.product.expiryDate);
            const now = new Date();
            const daysToExpiry = Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24));

            if (daysToExpiry < 90) {
              verificationResult.warnings.push({
                type: 'expiry_warning',
                message: `Sản phẩm sẽ hết hạn trong ${daysToExpiry} ngày`,
                severity: daysToExpiry < 30 ? 'high' : 'medium'
              });
            }
          }
        } else {
          verificationResult = {
            isAuthentic: false,
            product: {
              name: 'Không xác định',
              batchCode: scanInput,
              qrCode: scanInput
            },
            warnings: [{
              type: 'authentication_failed',
              message: response.message || 'Không thể xác thực sản phẩm này trên blockchain',
              severity: 'high'
            }],
            recommendations: [
              'Không bán sản phẩm này',
              'Liên hệ nhà cung cấp để kiểm tra',
              'Báo cáo với cơ quan chức năng nếu cần'
            ]
          };
        }
      } catch (apiError) {
        verificationResult = {
          isAuthentic: false,
          product: { name: 'Không xác định', batchCode: scanInput, qrCode: scanInput },
          warnings: [{ type: 'api_error', message: 'Không thể kết nối đến hệ thống xác thực blockchain', severity: 'high' }],
          recommendations: ['Không bán sản phẩm này', 'Kiểm tra kết nối mạng', 'Thử lại sau ít phút']
        };
      }

      setVerificationResult(verificationResult);

      if (verificationResult.isDamaged) {
        setError(`Sản phẩm này đã bị báo hỏng (DAMAGED) và không thể bán!`);
      }

      const newVerification = {
        id: Date.now(),
        productName: verificationResult.product.name,
        batchCode: verificationResult.product.batchCode,
        imageUrl: resolveImageUrl(verificationResult.product.imageUrl),
        verifiedAt: new Date(),
        status: verificationResult.isAuthentic ? 'authentic' : 'suspicious',
        customerType: 'Khách lẻ'
      };
      setRecentVerifications([newVerification, ...recentVerifications.slice(0, 9)]);

    } catch (err) {
      setError('Không thể xác thực sản phẩm: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSell = async () => {
    if (!verificationResult || !verificationResult.isAuthentic) {
      setError('Chỉ có thể bán sản phẩm đã xác thực chính hãng');
      return;
    }

    if (verificationResult.isDamaged) {
      setError('Không thể bán sản phẩm đã báo hỏng!');
      return;
    }

    if (!verificationResult.product.itemCode) {
      setError('Chức năng bán chỉ dành cho hộp thuốc riêng lẻ (có mã sản phẩm)');
      return;
    }

    try {
      setSelling(true);
      setSaleSuccess(null);
      setError(null);

      const pharmacyUser = localStorage.getItem('pharmacy_user');
      let pharmacyId = 1;
      let pharmacyName = 'Hiệu thuốc';

      if (pharmacyUser) {
        try {
          const user = JSON.parse(pharmacyUser);
          pharmacyId = user.pharmacyId || user.companyId || 1;
          pharmacyName = user.pharmacyName || user.name || 'Hiệu thuốc';
        } catch (e) { }
      }

      const response = await pharmacyService.sellItem(verificationResult.product.itemCode, pharmacyId, pharmacyName, null);

      if (response.success) {
        setSaleSuccess({
          itemCode: response.data.itemCode,
          drugName: response.data.drugName,
          soldAt: new Date().toLocaleString('vi-VN'),
          message: response.message || 'Bán hàng thành công!'
        });

        setVerificationResult(prev => ({
          ...prev,
          isSold: true,
          product: { ...prev.product, status: 'SOLD' }
        }));

        setTimeout(() => { handleScan(); }, 3000);
      } else {
        setError(response.message || 'Không thể bán sản phẩm');
      }
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Lỗi khi thực hiện bán hàng';
      setError(errorMessage);

      if (errorMessage.includes('đã được bán') || errorMessage.includes('already sold')) {
        setVerificationResult(prev => {
          if (!prev) return prev;
          return { ...prev, isSold: true, product: { ...prev.product, status: 'SOLD' } };
        });
      }
    } finally {
      setSelling(false);
    }
  };

  const handleImageChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setDamagedImage(e.target.files[0]);
    }
  };

  const submitDamagedReport = async () => {
    if (!damagedReason.trim()) {
      alert('Vui lòng nhập lý do hỏng hóc!');
      return;
    }

    try {
      setIsReportingDamaged(true);
      setError(null);

      let imageUrl = '';
      if (damagedImage) {
        const uploadRes = await pharmacyService.uploadImage(damagedImage);
        if (uploadRes.success) {
          imageUrl = uploadRes.data.url || uploadRes.data;
        }
      }

      const pharmacyUser = localStorage.getItem('pharmacy_user');
      let pharmacyId = 1;

      if (pharmacyUser) {
        try {
          const user = JSON.parse(pharmacyUser);
          pharmacyId = user.pharmacyId || user.companyId || 1;
        } catch (e) { }
      }

      const payload = {
        pharmacyId,
        reason: damagedReason,
        imageUrl: imageUrl
      };

      const itemCode = verificationResult.product.itemCode;
      const res = await pharmacyService.reportDamagedItem(itemCode, payload);

      if (res.success) {
        setReportSuccess({
          itemCode: itemCode,
          timestamp: new Date().toLocaleString('vi-VN'),
          message: 'Đã báo hỏng thành công. Đang cập nhật lên Blockchain!'
        });

        setVerificationResult(prev => ({
          ...prev,
          isDamaged: true,
          product: { ...prev.product, status: 'DAMAGED' }
        }));

        setShowDamagedModal(false);
        setDamagedReason('');
        setDamagedImage(null);

        setTimeout(() => handleScan(), 3500);
      } else {
        alert(res.message || 'Lỗi khi báo hỏng.');
      }
    } catch (err) {
      alert('Không thể báo hỏng sản phẩm: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsReportingDamaged(false);
    }
  };

  const getActorIcon = (actorType) => {
    switch (actorType) {
      case 'manufacturer': return <Factory size={16} />;
      case 'distributor': return <Truck size={16} />;
      case 'pharmacy': return <Package size={16} />;
      case 'system': return <Shield size={16} />;
      default: return <Package size={16} />;
    }
  };

  const getActorColor = (actorType) => {
    switch (actorType) {
      case 'manufacturer': return 'manufacturer';
      case 'distributor': return 'distributor';
      case 'pharmacy': return 'pharmacy';
      case 'system': return 'system';
      default: return 'default';
    }
  };

  const formatDateTime = (dateString) => new Date(dateString).toLocaleString('vi-VN');

  const formatTimeAgo = (timestamp) => {
    const now = new Date();
    const diff = now - timestamp;
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    if (minutes < 60) return `${minutes} phút trước`;
    return `${hours} giờ trước`;
  };

  const isSold = verificationResult?.isSold || verificationResult?.product?.status === 'SOLD';
  const isDamaged = verificationResult?.isDamaged || verificationResult?.product?.status === 'DAMAGED';

  return (
    <div className="counter-verification">
      <div className="page-header">
        <h1>
          <Shield className="page-icon" />
          Xác thực tại quầy
        </h1>
        <p>Quét mã QR để xem thông tin nguồn gốc và hình ảnh sản phẩm trước khi bán</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          {error}
        </div>
      )}

      {/* Scanner Section */}
      <div className="scanner-section">
        <div className="scanner-card">
          <div className="scanner-header">
            <QrCode size={32} />
            <h3>Quét mã xác thực sản phẩm</h3>
            <p>Quét mã QR hoặc nhập mã lô để xác thực nguồn gốc trước khi bán</p>
          </div>
          <div className="scanner-input">
            <div className="input-group">
              <Scan className="input-icon" />
              <input
                type="text"
                value={scanInput}
                onChange={(e) => setScanInput(e.target.value)}
                placeholder="Nhập hoặc quét mã QR / mã lô (VD: BT2024001)"
                className="scan-input"
                onKeyPress={(e) => e.key === 'Enter' && handleScan()}
              />
              <button onClick={handleScan} disabled={loading} className="btn btn-primary scan-btn">
                {loading ? 'Đang xác thực...' : 'Xác thực'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* ══════════════ VERIFICATION RESULT ══════════════ */}
      {verificationResult && (
        <div className="verification-result">

          {/* Sale Success Banner */}
          {saleSuccess && (
            <div className="sale-success-banner">
              <CheckCircle size={28} />
              <div>
                <strong>🎉 {saleSuccess.message}</strong>
                <p>Sản phẩm: {saleSuccess.drugName} · Mã: {saleSuccess.itemCode}</p>
                <p>Thời gian: {saleSuccess.soldAt}</p>
              </div>
            </div>
          )}

          {/* Report Success Banner */}
          {reportSuccess && (
            <div className="sale-success-banner" style={{ backgroundColor: '#fff3cd', color: '#856404', borderLeftColor: '#ffc107' }}>
              <AlertCircle size={28} />
              <div>
                <strong>⚠️ {reportSuccess.message}</strong>
                <p>Mã SP: {reportSuccess.itemCode}</p>
                <p>Thời gian: {reportSuccess.timestamp}</p>
              </div>
            </div>
          )}

          {/* Sold Warning Banner */}
          {isSold && !saleSuccess && !isDamaged && (
            <div className="sold-banner">
              <Package size={22} />
              <div>
                <strong>⚠️ Sản phẩm đã được bán</strong>
                <p>Sản phẩm này đã được bán cho khách hàng trước đó</p>
              </div>
            </div>
          )}

          {/* Damaged Warning Banner */}
          {isDamaged && (
            <div className="sold-banner" style={{ backgroundColor: '#fff5f5', color: '#c53030', borderColor: '#feb2b2' }}>
              <AlertCircle size={22} />
              <div>
                <strong>❌ Sản phẩm đã bị báo hỏng (DAMAGED)</strong>
                <p>Sản phẩm này đã được báo cáo là có lỗi/nứt vỡ và không thể lưu hành.</p>
              </div>
            </div>
          )}

          <div className={`result-card ${verificationResult.isAuthentic && !isDamaged ? 'authentic' : 'suspicious'} ${isSold ? 'sold' : ''} ${isDamaged ? 'damaged-hero' : ''}`}>

            {/* ── HERO STATUS ── */}
            <div className={`result-hero ${verificationResult.isAuthentic && !isDamaged ? 'hero-authentic' : 'hero-suspicious'}`}>
              <div className="hero-left">
                <div className="hero-icon-circle">
                  {verificationResult.isAuthentic && !isDamaged
                    ? <CheckCircle size={44} />
                    : <AlertCircle size={44} />
                  }
                </div>
                <div className="hero-text">
                  <h2>{isDamaged ? 'Sản phẩm Lỗi / Hỏng' : (verificationResult.isAuthentic ? 'Sản phẩm chính hãng' : 'Cần kiểm tra thêm')}</h2>
                  <p>{isDamaged ? 'Đã bị khóa lưu hành trên hệ thống Blockchain' : (verificationResult.isAuthentic
                    ? 'Đã xác thực nguồn gốc trên Blockchain'
                    : 'Không thể xác thực nguồn gốc sản phẩm')
                  }</p>
                  {verificationResult.isAuthentic && (
                    <span className={`sale-status-chip ${isDamaged ? 'chip-damaged' : isSold ? 'chip-sold' : 'chip-available'}`} style={isDamaged ? { backgroundColor: '#fed7d7', color: '#c53030' } : {}}>
                      {isDamaged
                        ? '❌ ĐÃ BÁO HỎNG'
                        : isSold
                          ? `🔴 ĐÃ BÁN${verificationResult.product?.soldAt ? ' · ' + new Date(verificationResult.product.soldAt).toLocaleString('vi-VN') : ''}`
                          : '🟢 CHƯA BÁN — Sẵn sàng bán'
                      }
                    </span>
                  )}
                </div>
              </div>

              {verificationResult.isAuthentic &&
                verificationResult.product?.itemCode &&
                !isSold && (
                  <div className="hero-sell-buttons">
                    <button className="sell-button" onClick={handleSell} disabled={selling}>
                      {selling ? '⏳ Đang xử lý...' : '💊 Bán nhanh'}
                    </button>
                    <button className="sell-button sell-with-instructions" onClick={() => setShowSellModal(true)} disabled={selling}>
                      📋 Bán + Hướng dẫn
                    </button>
                  </div>
                )}
            </div>

            {/* NEW ACTIONS: Báo hỏng button for authentic but NOT sold/damaged items */}
            {verificationResult.isAuthentic &&
              verificationResult.product?.itemCode &&
              !isSold && !isDamaged && (
                <div className="secondary-actions" style={{ display: 'flex', justifyContent: 'flex-end', padding: '0 2rem 1rem 2rem', marginTop: '-1rem' }}>
                  <button className="btn btn-secondary" style={{ backgroundColor: '#fff5f5', color: '#e53e3e', borderColor: '#feb2b2', display: 'flex', alignItems: 'center', gap: '8px' }} onClick={() => setShowDamagedModal(true)}>
                    <AlertCircle size={16} />
                    Báo hỏng / Trả hàng
                  </button>
                </div>
              )}

            {/* ── MAIN: Image + Info ── */}
            <div className="result-main">

              {/* LEFT: Product Image */}
              <div className="product-image-panel">
                {verificationResult.product.imageUrl ? (
                  <div className="product-image-wrapper">
                    <img
                      src={verificationResult.product.imageUrl}
                      alt={verificationResult.product.name}
                      className="product-img"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.style.display = 'none';
                        if (e.target.nextSibling) e.target.nextSibling.style.display = 'flex';
                      }}
                    />
                    <div className="product-img-fallback" style={{ display: 'none' }}>
                      <Package size={60} />
                      <span>Không tải được ảnh</span>
                    </div>
                  </div>
                ) : (
                  <div className="product-img-fallback">
                    <Package size={60} />
                    <span>Không có ảnh sản phẩm</span>
                  </div>
                )}

                <div className="product-name-under-img">
                  <h3>{verificationResult.product.name}</h3>
                  {verificationResult.product.activeIngredient && (
                    <span className="active-ingredient-tag">
                      <FlaskConical size={13} />
                      {verificationResult.product.activeIngredient}
                    </span>
                  )}
                  {verificationResult.product.dosage && (
                    <span className="dosage-tag">{verificationResult.product.dosage}</span>
                  )}
                </div>

                {/* Cert badges under image */}
                <div className="cert-row">
                  {['GMP', 'ISO 9001', 'WHO-GMP'].map(c => (
                    <span key={c} className="cert-badge">{c}</span>
                  ))}
                </div>
              </div>

              {/* RIGHT: Product Details */}
              <div className="product-details-panel">
                <div className="info-section-title">
                  <FileText size={17} />
                  Thông tin sản phẩm
                </div>

                <div className="info-grid-new">
                  <div className="info-card-item">
                    <span className="info-card-label"><Tag size={13} /> Mã lô / Batch</span>
                    <span className="info-card-value mono">{verificationResult.product.batchCode}</span>
                  </div>

                  <div className="info-card-item">
                    <span className="info-card-label"><Factory size={13} /> Nhà sản xuất</span>
                    <span className="info-card-value">{verificationResult.product.manufacturer}</span>
                  </div>

                  <div className="info-card-item">
                    <span className="info-card-label"><Calendar size={13} /> Ngày sản xuất</span>
                    <span className="info-card-value">
                      {verificationResult.product.manufactureDate !== 'N/A'
                        ? new Date(verificationResult.product.manufactureDate).toLocaleDateString('vi-VN')
                        : 'N/A'}
                    </span>
                  </div>

                  <div className="info-card-item">
                    <span className="info-card-label"><Calendar size={13} /> Hạn sử dụng</span>
                    <span className="info-card-value expiry-value">
                      {verificationResult.product.expiryDate !== 'N/A'
                        ? new Date(verificationResult.product.expiryDate).toLocaleDateString('vi-VN')
                        : 'N/A'}
                    </span>
                  </div>

                  <div className="info-card-item full-width">
                    <span className="info-card-label"><Thermometer size={13} /> Bảo quản</span>
                    <span className="info-card-value">{verificationResult.product.storageConditions || 'Nơi khô ráo, tránh ánh sáng'}</span>
                  </div>
                </div>

                {/* Description */}
                {verificationResult.product.description && (
                  <div className="product-description">
                    <div className="desc-label">📋 Mô tả sản phẩm</div>
                    <p>{verificationResult.product.description}</p>
                  </div>
                )}

                {/* Warnings */}
                {verificationResult.warnings && verificationResult.warnings.length > 0 && (
                  <div className="warnings-inline">
                    {verificationResult.warnings.map((w, i) => (
                      <div key={i} className={`warning-item ${w.severity}`}>
                        <AlertCircle size={15} />
                        {w.message}
                      </div>
                    ))}
                  </div>
                )}

                {/* Recommendations */}
                {verificationResult.recommendations && verificationResult.recommendations.length > 0 && (
                  <div className="recommendations-compact">
                    <div className="desc-label">💡 Khuyến nghị</div>
                    <ul className="recommendations-list">
                      {verificationResult.recommendations.map((rec, i) => <li key={i}>{rec}</li>)}
                    </ul>
                  </div>
                )}
              </div>
            </div>

            {/* ── BLOCKCHAIN TIMELINE ── */}
            {verificationResult.blockchainHistory && verificationResult.blockchainHistory.length > 0 && (
              <div className="traceability-section">
                <div className="section-title-row">
                  <Shield size={18} />
                  <h4>Lịch sử truy xuất nguồn gốc</h4>
                  <span className="step-count-badge">{verificationResult.blockchainHistory.length} bước</span>
                </div>
                <div className="blockchain-timeline">
                  {verificationResult.blockchainHistory.map((event, index) => (
                    <div key={index} className="timeline-item">
                      <div className="timeline-step">{event.step}</div>
                      <div className={`timeline-icon ${getActorColor(event.actorType)}`}>
                        {getActorIcon(event.actorType)}
                      </div>
                      <div className="timeline-content">
                        <div className="timeline-header">
                          <span className="event-name">{event.event}</span>
                          <span className="event-time">{formatDateTime(event.timestamp)}</span>
                        </div>
                        <div className="event-actor"><strong>{event.actor}</strong></div>
                        <div className="event-location">
                          <MapPin size={14} />
                          {event.location}
                        </div>
                        <div className="event-details">{event.details}</div>
                        {event.shipmentCode && (
                          <div className="shipment-code">📦 Mã vận đơn: <strong>{event.shipmentCode}</strong></div>
                        )}
                        {event.txHash && (
                          <div className="tx-hash">
                            <span className="tx-label">TX:</span>
                            <span
                              className="tx-value"
                              onClick={() => { navigator.clipboard.writeText(event.txHash); alert('Đã copy TX hash!'); }}
                              title="Click để copy"
                            >
                              {event.txHash.substring(0, 10)}...{event.txHash.substring(event.txHash.length - 8)}
                            </span>
                            <a
                              href={`http://localhost:3000/tx/${event.txHash}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="tx-link"
                              title="Xem trên Blockscout"
                            >
                              🔗
                            </a>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* ── BOTTOM ACTIONS ── */}
            <div className="result-actions">
              <button
                onClick={() => { setScanInput(''); setVerificationResult(null); setError(null); setSaleSuccess(null); setReportSuccess(null); }}
                className="btn btn-secondary"
              >
                🔄 Quét sản phẩm khác
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Recent Verifications */}
      <div className="recent-verifications">
        <div className="recent-header">
          <h3>
            <Clock size={24} />
            Xác thực gần đây ({recentVerifications.length})
          </h3>
        </div>

        <div className="recent-list">
          {recentVerifications.length === 0 ? (
            <div className="no-recent">
              <Search size={48} className="no-data-icon" />
              <h4>Chưa có xác thực nào</h4>
              <p>Các sản phẩm đã xác thực sẽ hiển thị ở đây</p>
            </div>
          ) : (
            recentVerifications.map(verification => (
              <div key={verification.id} className="recent-item">
                {/* Thumbnail */}
                <div className="recent-thumb">
                  {verification.imageUrl ? (
                    <img
                      src={verification.imageUrl}
                      alt={verification.productName}
                      className="recent-thumb-img"
                      onError={(e) => { e.target.onerror = null; e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                    />
                  ) : null}
                  <div className="recent-thumb-fallback" style={{ display: verification.imageUrl ? 'none' : 'flex' }}>
                    <Shield size={18} />
                  </div>
                </div>

                <div className="recent-content">
                  <div className="recent-product">
                    <strong>{verification.productName}</strong>
                    <span className="batch-code">#{verification.batchCode}</span>
                  </div>
                  <div className="recent-meta">
                    <span className="time">{formatTimeAgo(verification.verifiedAt)}</span>
                    <span className="customer-type">{verification.customerType}</span>
                    <span className={`status-badge status-${verification.status}`}>
                      {verification.status === 'authentic' ? 'Chính hãng' : 'Cần kiểm tra'}
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Modal */}
      <SellWithInstructionsModal
        isOpen={showSellModal}
        onClose={() => setShowSellModal(false)}
        itemData={verificationResult ? {
          itemCode: verificationResult.product?.itemCode || scanInput,
          drugName: verificationResult.product?.name,
          batchNumber: verificationResult.product?.batchCode,
          expiryDate: verificationResult.product?.expiryDate,
          manufacturer: verificationResult.product?.manufacturer
        } : null}
        onSuccess={(data) => {
          setSaleSuccess({
            itemCode: data.itemCode,
            drugName: data.drugName,
            soldAt: new Date().toLocaleString('vi-VN'),
            message: 'Bán hàng + Lưu hướng dẫn thành công!'
          });
          setVerificationResult(prev => ({
            ...prev,
            isSold: true,
            product: { ...prev.product, status: 'SOLD' }
          }));
          setTimeout(() => handleScan(), 3000);
        }}
      />

      {/* Damaged Report Modal */}
      {showDamagedModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2><AlertCircle size={24} style={{ color: '#e53e3e', marginRight: '10px' }} />Báo hỏng / Hoàn thuốc</h2>
              <button className="btn-close" onClick={() => setShowDamagedModal(false)} disabled={isReportingDamaged}>&times;</button>
            </div>

            <div className="modal-body" style={{ padding: '20px' }}>
              <div className="alert alert-warning" style={{ fontSize: '0.9rem', marginBottom: '20px' }}>
                Hành động này sẽ đánh dấu vĩnh viễn sản phẩm <strong>{verificationResult?.product?.itemCode}</strong> là HỎNG trên Blockchain và hệ thống. Bạn không thể hoàn tác!
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>Lý do hỏng / Lỗi *</label>
                <textarea
                  className="form-control"
                  rows="3"
                  placeholder="Ví dụ: Lọ thuốc bị vỡ nắp, thuốc bị đổi màu..."
                  value={damagedReason}
                  onChange={(e) => setDamagedReason(e.target.value)}
                  disabled={isReportingDamaged}
                  style={{ width: '100%', padding: '10px', borderRadius: '4px', border: '1px solid #ddd' }}
                />
              </div>

              <div className="form-group" style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>Hình ảnh minh chứng (Tùy chọn)</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  disabled={isReportingDamaged}
                  style={{ width: '100%' }}
                />
                <small style={{ color: '#666', display: 'block', marginTop: '5px' }}>Tải lên hình ảnh hộp thuốc bị lỗi để nộp lưu trữ bằng chứng.</small>
              </div>
            </div>

            <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', padding: '15px 20px', borderTop: '1px solid #eee' }}>
              <button
                className="btn btn-secondary"
                onClick={() => setShowDamagedModal(false)}
                disabled={isReportingDamaged}
              >
                Hủy bỏ
              </button>
              <button
                className="btn btn-primary"
                style={{ backgroundColor: '#e53e3e' }}
                onClick={submitDamagedReport}
                disabled={isReportingDamaged || !damagedReason.trim()}
              >
                {isReportingDamaged ? 'Đang xử lý...' : 'Xác nhận Báo Hỏng'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CounterVerification;
