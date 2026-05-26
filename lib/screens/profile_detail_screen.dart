import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../models/user.dart';
import '../models/user_profile.dart';
import 'profile_edit_screen.dart';

class ProfileDetailScreen extends StatefulWidget {
  const ProfileDetailScreen({super.key});

  @override
  State<ProfileDetailScreen> createState() => _ProfileDetailScreenState();
}

class _ProfileDetailScreenState extends State<ProfileDetailScreen> {
  User? get currentUser => AuthService.instance.currentUser;
  UserProfile? get currentProfile => AuthService.instance.currentProfile;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    if (mounted) {
      setState(() => _isLoading = true);
    }
    
    try {
      await AuthService.instance.getCurrentUserInfo();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi tải thông tin: $e'),
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
          'Chi tiết hồ sơ',
          style: TextStyle(
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black87),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const ProfileEditScreen(),
                ),
              ).then((_) => _loadProfile());
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadProfile,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    // Basic Information
                    _buildInfoSection(
                      title: 'Thông tin cơ bản',
                      icon: Icons.person,
                      items: [
                        _buildInfoRow('Tên', currentUser?.name ?? 'Chưa cập nhật'),
                        _buildInfoRow('Email', currentUser?.email ?? 'Chưa cập nhật'),
                        _buildInfoRow('Năm sinh', currentProfile?.birthYear?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tháng sinh', currentProfile?.birthMonth?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tuổi', currentProfile?.age?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Giới tính', currentProfile?.genderDisplay ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tỉnh/Thành', currentProfile?.province ?? 'Chưa cập nhật'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Physical Information
                    _buildInfoSection(
                      title: 'Thông tin sức khỏe',
                      icon: Icons.fitness_center,
                      items: [
                        _buildInfoRow('Chiều cao (cm)', currentProfile?.heightCm?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Cân nặng (kg)', currentProfile?.weightKg?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Nhóm máu', currentProfile?.bloodType?.name.replaceAll('_', ' ') ?? 'Chưa cập nhật'),
                        _buildInfoRow('Nghề nghiệp', currentProfile?.occupation ?? 'Chưa cập nhật'),
                        _buildInfoRow('Trình độ học vấn', currentProfile?.educationLevel?.name ?? 'Chưa cập nhật'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Medical History
                    _buildInfoSection(
                      title: 'Tiền sử y tế',
                      icon: Icons.medical_services,
                      items: [
                        _buildInfoRow('Tiền sử bệnh', currentProfile?.medicalHistory ?? 'Không có'),
                        _buildInfoRow('Dị ứng', currentProfile?.allergies ?? 'Không có'),
                        _buildInfoRow('Thuốc đang dùng', currentProfile?.currentMedications ?? 'Không có'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Lifestyle Information
                    _buildInfoSection(
                      title: 'Lối sống',
                      icon: Icons.local_activity,
                      items: [
                        _buildInfoRow('Hút thuốc', currentProfile?.smokingDisplay ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tuổi bắt đầu hút', currentProfile?.smokingStartAge?.toString() ?? 'Không áp dụng'),
                        _buildInfoRow('Tuổi bỏ thuốc', currentProfile?.smokingQuitAge?.toString() ?? 'Không áp dụng'),
                        _buildInfoRow('Số điếu/ngày', currentProfile?.cigarettesPerDay?.toString() ?? 'Không áp dụng'),
                        _buildInfoRow('Số năm hút', currentProfile?.yearsSmoked?.toString() ?? 'Không áp dụng'),
                        _buildDivider(),
                        _buildInfoRow('Uống rượu bia', currentProfile?.drinkingDisplay ?? 'Chưa cập nhật'),
                        _buildInfoRow('Số đơn vị/tuần', currentProfile?.alcoholUnitsPerWeek?.toString() ?? 'Không áp dụng'),
                        _buildInfoRow('Loại đồ uống ưa thích', currentProfile?.alcoholTypePreference ?? 'Không có'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Exercise & Health
                    _buildInfoSection(
                      title: 'Tập luyện & Sức khỏe',
                      icon: Icons.directions_run,
                      items: [
                        _buildInfoRow('Tần suất tập', currentProfile?.exerciseFrequency ?? 'Chưa cập nhật'),
                        _buildInfoRow('Cường độ tập', currentProfile?.exerciseIntensity ?? 'Chưa cập nhật'),
                        _buildInfoRow('Thời gian tập (phút)', currentProfile?.exerciseDurationMinutes?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Loại bài tập', currentProfile?.exerciseTypes ?? 'Chưa cập nhật'),
                        _buildDivider(),
                        _buildInfoRow('Chế độ ăn', currentProfile?.dietType ?? 'Chưa cập nhật'),
                        _buildInfoRow('Số bữa/ngày', currentProfile?.mealsPerDay?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Lượng nước/ngày (lít)', currentProfile?.waterIntakeLiters?.toString() ?? 'Chưa cập nhật'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Sleep & Mental Health
                    _buildInfoSection(
                      title: 'Giấc ngủ & Sức khỏe tinh thần',
                      icon: Icons.bedtime,
                      items: [
                        _buildInfoRow('Giờ ngủ trung bình', currentProfile?.sleepHoursAverage?.toString() ?? 'Chưa cập nhật'),
                        _buildInfoRow('Chất lượng giấc ngủ', currentProfile?.sleepQuality ?? 'Chưa cập nhật'),
                        _buildInfoRow('Rối loạn giấc ngủ', currentProfile?.sleepDisorders ?? 'Không có'),
                        _buildDivider(),
                        _buildInfoRow('Mức độ căng thẳng', currentProfile?.stressLevel ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tình trạng sức khỏe tâm thần', currentProfile?.mentalHealthStatus ?? 'Chưa cập nhật'),
                      ],
                    ),

                    const SizedBox(height: 16),

                    // Work Environment
                    _buildInfoSection(
                      title: 'Môi trường làm việc',
                      icon: Icons.work,
                      items: [
                        _buildInfoRow('Môi trường làm việc', currentProfile?.workEnvironment ?? 'Chưa cập nhật'),
                        _buildInfoRow('Yêu cầu thể chất', currentProfile?.physicalDemands ?? 'Chưa cập nhật'),
                        _buildInfoRow('Tiếp xúc hóa chất', currentProfile?.chemicalExposure == true ? 'Có' : currentProfile?.chemicalExposure == false ? 'Không' : 'Chưa cập nhật'),
                      ],
                    ),

                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildInfoSection({
    required String title,
    required IconData icon,
    required List<Widget> items,
  }) {
    return Container(
      width: double.infinity,
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
          // Section Header
          Padding(
            padding: const EdgeInsets.all(20.0),
            child: Row(
              children: [
                Icon(
                  icon,
                  color: Theme.of(context).primaryColor,
                  size: 24.0,
                ),
                const SizedBox(width: 12.0),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 18.0,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
          
          // Section Items
          Padding(
            padding: const EdgeInsets.only(left: 20.0, right: 20.0, bottom: 20.0),
            child: Column(children: items),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: TextStyle(
                fontSize: 16.0,
                color: Colors.grey.shade700,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          const SizedBox(width: 16.0),
          Expanded(
            flex: 3,
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 16.0,
                fontWeight: FontWeight.w400,
                color: Colors.black87,
              ),
              textAlign: TextAlign.right,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDivider() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Divider(
        color: Colors.grey.shade200,
        thickness: 1,
      ),
    );
  }
}
