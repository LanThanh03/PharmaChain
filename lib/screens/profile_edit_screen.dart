import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../services/auth_service.dart';
import '../models/user_profile.dart';

class ProfileEditScreen extends StatefulWidget {
  const ProfileEditScreen({super.key});

  @override
  State<ProfileEditScreen> createState() => _ProfileEditScreenState();
}

class _ProfileEditScreenState extends State<ProfileEditScreen> {
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;

  // Controllers
  final _nameController = TextEditingController();
  final _heightController = TextEditingController();
  final _weightController = TextEditingController();
  final _occupationController = TextEditingController();
  final _medicalHistoryController = TextEditingController();
  final _allergiesController = TextEditingController();
  final _medicationsController = TextEditingController();

  // Selected values
  int? _selectedBirthMonth;
  BloodType? _selectedBloodType;
  EducationLevel? _selectedEducationLevel;
  String? _selectedSmokingStatus;
  String? _selectedDrinkingStatus;

  @override
  void initState() {
    super.initState();
    _loadCurrentProfile();
  }

  void _loadCurrentProfile() {
    final profile = AuthService.instance.currentProfile;
    if (profile != null) {
      _nameController.text = profile.name ?? '';
      _heightController.text = profile.heightCm?.toString() ?? '';
      _weightController.text = profile.weightKg?.toString() ?? '';
      _occupationController.text = profile.occupation ?? '';
      _medicalHistoryController.text = profile.medicalHistory ?? '';
      _allergiesController.text = profile.allergies ?? '';
      _medicationsController.text = profile.currentMedications ?? '';
      
      _selectedBirthMonth = profile.birthMonth;
      _selectedBloodType = profile.bloodType;
      _selectedEducationLevel = profile.educationLevel;
      _selectedSmokingStatus = profile.smokingStatus;
      _selectedDrinkingStatus = profile.drinkingStatus;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _heightController.dispose();
    _weightController.dispose();
    _occupationController.dispose();
    _medicalHistoryController.dispose();
    _allergiesController.dispose();
    _medicationsController.dispose();
    super.dispose();
  }

  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      final currentProfile = AuthService.instance.currentProfile;
      if (currentProfile == null) {
        throw Exception('Không tìm thấy thông tin người dùng');
      }

      final updatedProfile = currentProfile.copyWith(
        name: _nameController.text.trim().isEmpty ? null : _nameController.text.trim(),
        birthMonth: _selectedBirthMonth,
        heightCm: _heightController.text.trim().isEmpty ? null : int.tryParse(_heightController.text.trim()),
        weightKg: _weightController.text.trim().isEmpty ? null : double.tryParse(_weightController.text.trim()),
        bloodType: _selectedBloodType,
        occupation: _occupationController.text.trim().isEmpty ? null : _occupationController.text.trim(),
        educationLevel: _selectedEducationLevel,
        medicalHistory: _medicalHistoryController.text.trim().isEmpty ? null : _medicalHistoryController.text.trim(),
        allergies: _allergiesController.text.trim().isEmpty ? null : _allergiesController.text.trim(),
        currentMedications: _medicationsController.text.trim().isEmpty ? null : _medicationsController.text.trim(),
        smokingStatus: _selectedSmokingStatus,
        drinkingStatus: _selectedDrinkingStatus,
        updatedAt: DateTime.now(),
      );

      final result = await AuthService.instance.saveProfile(updatedProfile);

      if (result.isSuccess && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Cập nhật hồ sơ thành công!'),
            backgroundColor: Colors.green,
          ),
        );
        Navigator.of(context).pop();
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi cập nhật: ${result.error ?? "Không xác định"}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi không xác định: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: const Text(
          'Chỉnh sửa hồ sơ',
          style: TextStyle(
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
        actions: [
          TextButton(
            onPressed: _isLoading ? null : _saveProfile,
            child: Text(
              'Lưu',
              style: TextStyle(
                color: _isLoading ? Colors.grey : Theme.of(context).primaryColor,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    // Basic Information
                    _buildSection(
                      title: 'Thông tin cơ bản',
                      children: [
                        _buildTextField(
                          controller: _nameController,
                          label: 'Tên',
                          icon: Icons.person,
                        ),
                        const SizedBox(height: 16),
                        _buildDropdown<int>(
                          label: 'Tháng sinh',
                          icon: Icons.cake,
                          value: _selectedBirthMonth,
                          items: List.generate(12, (index) => index + 1)
                              .map((month) => DropdownMenuItem(
                                    value: month,
                                    child: Text('Tháng $month'),
                                  ))
                              .toList(),
                          onChanged: (value) => setState(() => _selectedBirthMonth = value),
                        ),
                      ],
                    ),

                    const SizedBox(height: 24),

                    // Physical Information
                    _buildSection(
                      title: 'Thông tin sức khỏe',
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: _buildTextField(
                                controller: _heightController,
                                label: 'Chiều cao (cm)',
                                icon: Icons.height,
                                keyboardType: TextInputType.number,
                                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: _buildTextField(
                                controller: _weightController,
                                label: 'Cân nặng (kg)',
                                icon: Icons.fitness_center,
                                keyboardType: TextInputType.number,
                                inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        _buildDropdown<BloodType>(
                          label: 'Nhóm máu',
                          icon: Icons.bloodtype,
                          value: _selectedBloodType,
                          items: BloodType.values
                              .map((type) => DropdownMenuItem(
                                    value: type,
                                    child: Text(type.name.replaceAll('_', ' ')),
                                  ))
                              .toList(),
                          onChanged: (value) => setState(() => _selectedBloodType = value),
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _occupationController,
                          label: 'Nghề nghiệp',
                          icon: Icons.work,
                        ),
                        const SizedBox(height: 16),
                        _buildDropdown<EducationLevel>(
                          label: 'Trình độ học vấn',
                          icon: Icons.school,
                          value: _selectedEducationLevel,
                          items: EducationLevel.values
                              .map((level) => DropdownMenuItem(
                                    value: level,
                                    child: Text(_getEducationLevelName(level)),
                                  ))
                              .toList(),
                          onChanged: (value) => setState(() => _selectedEducationLevel = value),
                        ),
                      ],
                    ),

                    const SizedBox(height: 24),

                    // Medical History
                    _buildSection(
                      title: 'Tiền sử y tế',
                      children: [
                        _buildTextField(
                          controller: _medicalHistoryController,
                          label: 'Tiền sử bệnh',
                          icon: Icons.medical_services,
                          maxLines: 3,
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _allergiesController,
                          label: 'Dị ứng',
                          icon: Icons.warning,
                          maxLines: 2,
                        ),
                        const SizedBox(height: 16),
                        _buildTextField(
                          controller: _medicationsController,
                          label: 'Thuốc đang sử dụng',
                          icon: Icons.medication,
                          maxLines: 2,
                        ),
                      ],
                    ),

                    const SizedBox(height: 24),

                    // Lifestyle
                    _buildSection(
                      title: 'Lối sống',
                      children: [
                        _buildDropdown<String>(
                          label: 'Tình trạng hút thuốc',
                          icon: Icons.smoke_free,
                          value: _selectedSmokingStatus,
                          items: ['never', 'former', 'current']
                              .map((status) => DropdownMenuItem(
                                    value: status,
                                    child: Text(_getSmokingStatusName(status)),
                                  ))
                              .toList(),
                          onChanged: (value) => setState(() => _selectedSmokingStatus = value),
                        ),
                        const SizedBox(height: 16),
                        _buildDropdown<String>(
                          label: 'Tần suất uống rượu',
                          icon: Icons.local_drink,
                          value: _selectedDrinkingStatus,
                          items: ['never', 'rarely', 'weekly', 'daily']
                              .map((frequency) => DropdownMenuItem(
                                    value: frequency,
                                    child: Text(_getDrinkingFrequencyName(frequency)),
                                  ))
                              .toList(),
                          onChanged: (value) => setState(() => _selectedDrinkingStatus = value),
                        ),
                      ],
                    ),

                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildSection({required String title, required List<Widget> children}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20.0),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16.0),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10.0,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: 18.0,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).primaryColor,
            ),
          ),
          const SizedBox(height: 16),
          ...children,
        ],
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    int maxLines = 1,
    TextInputType? keyboardType,
    List<TextInputFormatter>? inputFormatters,
  }) {
    return TextFormField(
      controller: controller,
      maxLines: maxLines,
      keyboardType: keyboardType,
      inputFormatters: inputFormatters,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: BorderSide(color: Theme.of(context).primaryColor),
        ),
      ),
    );
  }

  Widget _buildDropdown<T>({
    required String label,
    required IconData icon,
    required T? value,
    required List<DropdownMenuItem<T>> items,
    required void Function(T?) onChanged,
  }) {
    return DropdownButtonFormField<T>(
      value: value,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12.0),
          borderSide: BorderSide(color: Theme.of(context).primaryColor),
        ),
      ),
      items: items,
      onChanged: onChanged,
    );
  }

  String _getEducationLevelName(EducationLevel level) {
    switch (level) {
      case EducationLevel.PRIMARY:
        return 'Tiểu học';
      case EducationLevel.SECONDARY:
        return 'Trung học cơ sở';
      case EducationLevel.HIGH_SCHOOL:
        return 'Trung học phổ thông';
      case EducationLevel.UNIVERSITY:
        return 'Đại học';
      case EducationLevel.POSTGRADUATE:
        return 'Sau đại học';
    }
  }

  String _getSmokingStatusName(String status) {
    switch (status) {
      case 'never':
        return 'Không bao giờ';
      case 'former':
        return 'Đã bỏ';
      case 'current':
        return 'Hiện tại';
      default:
        return status;
    }
  }

  String _getDrinkingFrequencyName(String frequency) {
    switch (frequency) {
      case 'never':
        return 'Không bao giờ';
      case 'rarely':
        return 'Hiếm khi';
      case 'weekly':
        return 'Hàng tuần';
      case 'daily':
        return 'Hàng ngày';
      default:
        return frequency;
    }
  }
}
