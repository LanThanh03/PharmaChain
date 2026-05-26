import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../models/user_profile.dart';
import 'main_screen.dart';

class ProfileSetupScreen extends StatefulWidget {
  const ProfileSetupScreen({super.key});

  @override
  State<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends State<ProfileSetupScreen> {
  final _formKey = GlobalKey<FormState>();
  bool _isLoading = false;

  // Medical History
  final _chronicDiseasesController = TextEditingController();
  final _allergiesController = TextEditingController();
  final _medicationsController = TextEditingController();

  // Lifestyle
  SmokingStatus? _smokingStatus;
  DrinkingFrequency? _drinkingFrequency;

  // Common chronic diseases for suggestions
  final List<String> _commonDiseases = [
    'Tăng huyết áp',
    'Tiểu đường type 1',
    'Tiểu đường type 2',
    'Hen suyễn',
    'Bệnh tim mạch',
    'Cholesterol cao',
    'Viêm khớp',
    'Loãng xương',
    'Trầm cảm',
    'Lo âu',
    'Béo phì',
    'Bệnh thận',
    'Bệnh gan',
    'Ung thư',
  ];

  @override
  void dispose() {
    _chronicDiseasesController.dispose();
    _allergiesController.dispose();
    _medicationsController.dispose();
    super.dispose();
  }

  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
    });

    try {
      final currentProfile = AuthService.instance.currentProfile;
      final user = AuthService.instance.currentUser!;

      // Parse chronic diseases from text
      final chronicDiseases = _chronicDiseasesController.text
          .split(',')
          .map((e) => e.trim())
          .where((e) => e.isNotEmpty)
          .toList();

      // Parse allergies from text
      final allergies = _allergiesController.text
          .split(',')
          .map((e) => e.trim())
          .where((e) => e.isNotEmpty)
          .toList();

      // Parse medications from text
      final medications = _medicationsController.text
          .split(',')
          .map((e) => e.trim())
          .where((e) => e.isNotEmpty)
          .toList();

      final updatedProfile = (currentProfile ?? UserProfile(userId: user.id)).copyWith(
        medicalHistory: chronicDiseases.join(', '),
        allergies: allergies.join(', '),
        currentMedications: medications.join(', '),
        smokingStatus: _smokingStatus?.name,
        drinkingStatus: _drinkingFrequency?.name,
        updatedAt: DateTime.now(),
      );

      final result = await AuthService.instance.saveProfile(updatedProfile);

      if (result.isSuccess && mounted) {
        // Show success message
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Hồ sơ y tế đã được lưu thành công!'),
            backgroundColor: Colors.green,
          ),
        );

        // Navigate to main screen
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => const MainScreen()),
        );
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Có lỗi xảy ra khi lưu hồ sơ'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }

    setState(() {
      _isLoading = false;
    });
  }

  void _skipForNow() {
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (context) => const MainScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        automaticallyImplyLeading: false,
        title: const Text(
          'Thiết lập hồ sơ y tế',
          style: TextStyle(
            color: Colors.black87,
            fontWeight: FontWeight.w600,
          ),
        ),
        actions: [
          TextButton(
            onPressed: _skipForNow,
            child: Text(
              'Bỏ qua',
              style: TextStyle(
                color: Theme.of(context).primaryColor,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Header
              Container(
                padding: const EdgeInsets.all(20.0),
                decoration: BoxDecoration(
                  color: Colors.blue.shade50,
                  borderRadius: BorderRadius.circular(16.0),
                  border: Border.all(color: Colors.blue.shade200),
                ),
                child: Column(
                  children: [
                    Icon(
                      Icons.medical_information_outlined,
                      color: Colors.blue.shade700,
                      size: 48.0,
                    ),
                    const SizedBox(height: 16.0),
                    Text(
                      'Thông tin sức khỏe của bạn',
                      style: TextStyle(
                        fontSize: 20.0,
                        fontWeight: FontWeight.bold,
                        color: Colors.blue.shade900,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8.0),
                    Text(
                      'Cung cấp thông tin này sẽ giúp AI đưa ra các gợi ý chính xác và an toàn hơn cho riêng bạn.',
                      style: TextStyle(
                        fontSize: 16.0,
                        color: Colors.blue.shade700,
                        height: 1.4,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 32.0),

              // Medical History Section
              _buildSectionHeader(
                'Tiền sử y tế',
                'Những thông tin này rất quan trọng cho việc chẩn đoán',
                Icons.history_edu,
              ),

              const SizedBox(height: 16.0),

              // Chronic Diseases
              _buildMedicalField(
                controller: _chronicDiseasesController,
                label: 'Bệnh nền/Bệnh mãn tính',
                hint: 'Ví dụ: Tăng huyết áp, Tiểu đường type 2, Hen suyễn',
                helperText: 'Nhập các bệnh, cách nhau bởi dấu phẩy',
                suggestions: _commonDiseases,
              ),

              const SizedBox(height: 16.0),

              // Allergies
              _buildMedicalField(
                controller: _allergiesController,
                label: 'Dị ứng',
                hint: 'Ví dụ: Penicillin, Tôm cua, Phấn hoa',
                helperText: 'Nhập các chất gây dị ứng, cách nhau bởi dấu phẩy',
                icon: Icons.warning_amber_outlined,
              ),

              const SizedBox(height: 16.0),

              // Current Medications
              _buildMedicalField(
                controller: _medicationsController,
                label: 'Thuốc đang sử dụng thường xuyên',
                hint: 'Ví dụ: Aspirin 100mg, Metformin 500mg',
                helperText: 'Nhập tên thuốc và li용량, cách nhau bởi dấu phẩy',
                icon: Icons.medication_outlined,
              ),

              const SizedBox(height: 32.0),

              // Lifestyle Section
              _buildSectionHeader(
                'Lối sống & Thói quen',
                'Thông tin này giúp đánh giá nguy cơ mắc bệnh',
                Icons.favorite_border,
              ),

              const SizedBox(height: 16.0),

              // Smoking Status
              _buildChoiceField(
                label: 'Hút thuốc lá',
                value: _smokingStatus,
                items: SmokingStatus.values,
                itemLabels: {
                  SmokingStatus.never: 'Không bao giờ',
                  SmokingStatus.former: 'Đã từng hút',
                  SmokingStatus.current: 'Đang hút thuốc',
                },
                onChanged: (value) {
                  setState(() {
                    _smokingStatus = value;
                  });
                },
                icon: Icons.smoke_free_outlined,
              ),

              const SizedBox(height: 16.0),

              // Drinking Frequency
              _buildChoiceField(
                label: 'Uống rượu bia',
                value: _drinkingFrequency,
                items: DrinkingFrequency.values,
                itemLabels: {
                  DrinkingFrequency.never: 'Không bao giờ',
                  DrinkingFrequency.rarely: 'Hiếm khi',
                  DrinkingFrequency.weekly: 'Hàng tuần',
                  DrinkingFrequency.daily: 'Hàng ngày',
                },
                onChanged: (value) {
                  setState(() {
                    _drinkingFrequency = value;
                  });
                },
                icon: Icons.local_drink_outlined,
              ),

              const SizedBox(height: 32.0),

              // Privacy Notice
              Container(
                padding: const EdgeInsets.all(16.0),
                decoration: BoxDecoration(
                  color: Colors.green.shade50,
                  borderRadius: BorderRadius.circular(12.0),
                  border: Border.all(color: Colors.green.shade200),
                ),
                child: Row(
                  children: [
                    Icon(
                      Icons.security_outlined,
                      color: Colors.green.shade700,
                      size: 20.0,
                    ),
                    const SizedBox(width: 12.0),
                    Expanded(
                      child: Text(
                        'Thông tin y tế của bạn được mã hóa và bảo mật tuyệt đối. Chúng tôi cam kết không chia sẻ với bên thứ ba.',
                        style: TextStyle(
                          color: Colors.green.shade700,
                          fontSize: 14.0,
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24.0),

              // Save Button
              ElevatedButton(
                onPressed: _isLoading ? null : _saveProfile,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).primaryColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16.0),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.0),
                  ),
                ),
                child: _isLoading
                  ? const SizedBox(
                      height: 20.0,
                      width: 20.0,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.0,
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                      ),
                    )
                  : const Text(
                      'Lưu hồ sơ và tiếp tục',
                      style: TextStyle(
                        fontSize: 16.0,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
              ),

              const SizedBox(height: 16.0),

              // Skip Button
              TextButton(
                onPressed: _skipForNow,
                child: Text(
                  'Tôi sẽ cập nhật sau',
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 16.0,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, String subtitle, IconData icon) {
    return Row(
      children: [
        Icon(
          icon,
          color: Theme.of(context).primaryColor,
          size: 24.0,
        ),
        const SizedBox(width: 12.0),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 18.0,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
              ),
              Text(
                subtitle,
                style: TextStyle(
                  fontSize: 14.0,
                  color: Colors.grey.shade600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMedicalField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required String helperText,
    List<String>? suggestions,
    IconData? icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextFormField(
          controller: controller,
          maxLines: 3,
          decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            helperText: helperText,
            helperMaxLines: 2,
            prefixIcon: Icon(icon ?? Icons.medical_services_outlined),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12.0),
            ),
            alignLabelWithHint: true,
          ),
        ),
        if (suggestions != null) ...[
          const SizedBox(height: 8.0),
          Wrap(
            spacing: 8.0,
            runSpacing: 4.0,
            children: suggestions.take(6).map((suggestion) {
              return ActionChip(
                label: Text(
                  suggestion,
                  style: const TextStyle(fontSize: 12.0),
                ),
                onPressed: () {
                  final current = controller.text;
                  if (current.isEmpty) {
                    controller.text = suggestion;
                  } else {
                    controller.text = '$current, $suggestion';
                  }
                },
                backgroundColor: Colors.grey.shade100,
                side: BorderSide(color: Colors.grey.shade300),
              );
            }).toList(),
          ),
        ],
      ],
    );
  }

  Widget _buildChoiceField<T>({
    required String label,
    required T? value,
    required List<T> items,
    required Map<T, String> itemLabels,
    required ValueChanged<T?> onChanged,
    IconData? icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            if (icon != null) ...[
              Icon(
                icon,
                color: Colors.grey.shade600,
                size: 20.0,
              ),
              const SizedBox(width: 8.0),
            ],
            Text(
              label,
              style: const TextStyle(
                fontSize: 16.0,
                fontWeight: FontWeight.w500,
                color: Colors.black87,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8.0),
        Wrap(
          spacing: 12.0,
          children: items.map((item) {
            final isSelected = value == item;
            return ChoiceChip(
              label: Text(itemLabels[item] ?? ''),
              selected: isSelected,
              onSelected: (_) => onChanged(item),
              backgroundColor: Colors.grey.shade100,
              selectedColor: Theme.of(context).primaryColor.withOpacity(0.2),
              labelStyle: TextStyle(
                color: isSelected ? Theme.of(context).primaryColor : Colors.grey.shade700,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
              ),
              side: BorderSide(
                color: isSelected ? Theme.of(context).primaryColor : Colors.grey.shade300,
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}
