import React, { useState, useEffect } from 'react';
import { Users, Building, Edit, Save, AlertCircle, CheckCircle } from 'lucide-react';
import distributorService from '../services/apiService';
import './AccountManagement.css';

const AccountManagement = () => {
    const [companyInfo, setCompanyInfo] = useState({
        name: '',
        address: '',
        phone: '',
        email: '',
        license: '',
        website: ''
    });

    const [employees] = useState([
        {
            id: 1,
            name: 'Lê Văn C',
            position: 'Giám đốc Phân phối',
            email: 'c.le@cpc1.com',
            role: 'admin',
            status: 'active'
        },
        {
            id: 2,
            name: 'Phạm Thị D',
            position: 'Quản lý Kho',
            email: 'd.pham@cpc1.com',
            role: 'manager',
            status: 'active'
        }
    ]);

    const [isEditingCompany, setIsEditingCompany] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    useEffect(() => {
        fetchCompanyInfo();
    }, []);

    const fetchCompanyInfo = async () => {
        try {
            setLoading(true);
            const companyId = localStorage.getItem('companyId') || '2';
            const response = await distributorService.getCompanyInfo(companyId);

            if (response.success && response.data) {
                setCompanyInfo({
                    name: response.data.name || 'CPC1 Hà Nội',
                    address: response.data.address || '',
                    phone: response.data.phone || '',
                    email: response.data.email || '',
                    license: response.data.license || '',
                    website: response.data.website || ''
                });
            }
        } catch (err) {
            console.error('Error fetching company info:', err);
            // Set default values if API fails
            setCompanyInfo({
                name: 'CPC1 Hà Nội',
                address: '15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội',
                phone: '024 3854 3902',
                email: 'contact@cpc1.com.vn',
                license: 'GPL-2024-002',
                website: 'https://cpc1.com.vn'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleSaveCompany = async () => {
        try {
            setLoading(true);
            setError(null);
            setSuccess(null);

            const companyId = localStorage.getItem('companyId') || '2';
            const response = await distributorService.updateCompanyInfo(companyId, companyInfo);

            if (response.success) {
                setSuccess('Cập nhật thông tin công ty thành công!');
                setIsEditingCompany(false);
                setTimeout(() => setSuccess(null), 3000);
            } else {
                setError(response.message || 'Không thể cập nhật thông tin');
            }
        } catch (err) {
            console.error('Error updating company info:', err);
            setError('Lỗi khi cập nhật thông tin: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="account-management">
            <div className="page-header">
                <h1>
                    <Users className="page-icon" />
                    Quản lý Tài khoản
                </h1>
                <p>Cập nhật thông tin công ty và quản lý tài khoản nhân viên</p>
            </div>

            {error && (
                <div className="alert alert-error">
                    <AlertCircle size={20} />
                    {error}
                </div>
            )}

            {success && (
                <div className="alert alert-success">
                    <CheckCircle size={20} />
                    {success}
                </div>
            )}

            {/* Company Information */}
            <div className="section-card">
                <div className="section-header">
                    <h3>
                        <Building size={24} />
                        Thông tin Công ty
                    </h3>
                    <button
                        onClick={() => isEditingCompany ? handleSaveCompany() : setIsEditingCompany(true)}
                        className="btn btn-primary"
                        disabled={loading}
                    >
                        {isEditingCompany ? <Save size={16} /> : <Edit size={16} />}
                        {loading ? 'Đang lưu...' : (isEditingCompany ? 'Lưu' : 'Chỉnh sửa')}
                    </button>
                </div>

                <div className="company-form">
                    <div className="form-row">
                        <div className="form-group">
                            <label>Tên công ty</label>
                            <input
                                type="text"
                                value={companyInfo.name}
                                onChange={(e) => setCompanyInfo({ ...companyInfo, name: e.target.value })}
                                disabled={!isEditingCompany}
                                className="form-input"
                                placeholder="CPC1 Hà Nội"
                            />
                        </div>
                        <div className="form-group">
                            <label>Số giấy phép</label>
                            <input
                                type="text"
                                value={companyInfo.license}
                                onChange={(e) => setCompanyInfo({ ...companyInfo, license: e.target.value })}
                                disabled={!isEditingCompany}
                                className="form-input"
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label>Địa chỉ</label>
                        <input
                            type="text"
                            value={companyInfo.address}
                            onChange={(e) => setCompanyInfo({ ...companyInfo, address: e.target.value })}
                            disabled={!isEditingCompany}
                            className="form-input"
                            placeholder="15 Phùng Hưng, Phúc La, Hà Đông, Hà Nội"
                        />
                    </div>

                    <div className="form-row">
                        <div className="form-group">
                            <label>Số điện thoại</label>
                            <input
                                type="tel"
                                value={companyInfo.phone}
                                onChange={(e) => setCompanyInfo({ ...companyInfo, phone: e.target.value })}
                                disabled={!isEditingCompany}
                                className="form-input"
                            />
                        </div>
                        <div className="form-group">
                            <label>Email</label>
                            <input
                                type="email"
                                value={companyInfo.email}
                                onChange={(e) => setCompanyInfo({ ...companyInfo, email: e.target.value })}
                                disabled={!isEditingCompany}
                                className="form-input"
                            />
                        </div>
                    </div>

                    <div className="form-group">
                        <label>Website</label>
                        <input
                            type="url"
                            value={companyInfo.website}
                            onChange={(e) => setCompanyInfo({ ...companyInfo, website: e.target.value })}
                            disabled={!isEditingCompany}
                            className="form-input"
                        />
                    </div>
                </div>
            </div>

            {/* Employees */}
            {/* <div className="section-card">
                <div className="section-header">
                    <h3>
                        <Users size={24} />
                        Nhân viên ({employees.length})
                    </h3>
                </div>

                <div className="employees-table">
                    <table>
                        <thead>
                            <tr>
                                <th>Họ tên</th>
                                <th>Chức vụ</th>
                                <th>Email</th>
                                <th>Vai trò</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            {employees.map(employee => (
                                <tr key={employee.id}>
                                    <td className="employee-name">{employee.name}</td>
                                    <td>{employee.position}</td>
                                    <td>{employee.email}</td>
                                    <td>
                                        <span className={`role-badge role-${employee.role}`}>
                                            {employee.role === 'admin' ? 'Quản trị' : 'Nhân viên'}
                                        </span>
                                    </td>
                                    <td>
                                        <span className={`status-badge status-${employee.status}`}>
                                            {employee.status === 'active' ? 'Hoạt động' : 'Tạm khóa'}
                                        </span>
                                    </td>
                                    <td>
                                        <button className="btn btn-outline">
                                            <Edit size={14} />
                                            Sửa
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div> */}
        </div>
    );
};

export default AccountManagement;
