import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import '../models/user.dart';
import '../models/user_profile.dart';
import '../config/api_config.dart';
import 'login_screen.dart';
import 'profile_detail_screen.dart';
import 'server_settings_screen.dart';
import 'test_notification_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  User? get currentUser => AuthService.instance.currentUser;
  UserProfile? get currentProfile => AuthService.instance.currentProfile;

  Future<void> _logout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Đăng xuất'),
        content: const Text('Bạn có chắc chắn muốn đăng xuất?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Hủy'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Đăng xuất'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await AuthService.instance.logout();
      if (mounted) {
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (context) => const LoginScreen()),
          (route) => false,
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey.shade50,
      appBar: AppBar(
        title: const Text(
          'Hồ sơ',
          style: TextStyle(fontWeight: FontWeight.w600, color: Colors.black87),
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, color: Colors.black87),
            onPressed: _logout,
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // Profile Header
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24.0),
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
                children: [
                  // Avatar
                  Container(
                    width: 80.0,
                    height: 80.0,
                    decoration: BoxDecoration(
                      color: Theme.of(context).primaryColor.withOpacity(0.1),
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: Theme.of(context).primaryColor.withOpacity(0.3),
                        width: 2.0,
                      ),
                    ),
                    child: Icon(
                      Icons.person,
                      size: 40.0,
                      color: Theme.of(context).primaryColor,
                    ),
                  ),
                  const SizedBox(height: 16.0),

                  // Name
                  Text(
                    currentUser?.name ??
                        currentUser?.email.split('@')[0] ??
                        'Người dùng',
                    style: const TextStyle(
                      fontSize: 20.0,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 4.0),

                  // Email
                  Text(
                    currentUser?.email ?? 'Chưa đăng nhập',
                    style: TextStyle(
                      fontSize: 14.0,
                      color: Colors.grey.shade600,
                    ),
                  ),
                  const SizedBox(height: 16.0),

                  // View Detail Button
                  ElevatedButton(
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (context) => const ProfileDetailScreen(),
                        ),
                      );
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Theme.of(context).primaryColor,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 24.0,
                        vertical: 12.0,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(24.0),
                      ),
                    ),
                    child: const Text(
                      'Xem chi tiết hồ sơ',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24.0),

            // Health Stats
            _buildSectionCard(
              title: 'Thông tin sức khỏe',
              icon: Icons.bar_chart,
              items: [
                _buildStatItem(
                  'Tuổi',
                  currentProfile?.age?.toString() ?? 'Chưa cập nhật',
                  Icons.cake,
                ),
                _buildStatItem(
                  'Giới tính',
                  currentProfile?.genderDisplay ?? 'Chưa cập nhật',
                  Icons.person,
                ),
                _buildStatItem(
                  'Tỉnh/Thành',
                  currentProfile?.province ?? 'Chưa cập nhật',
                  Icons.location_on,
                ),
                _buildStatItem(
                  'Hút thuốc',
                  currentProfile?.smokingDisplay ?? 'Chưa cập nhật',
                  Icons.smoke_free,
                ),
                _buildStatItem(
                  'Rượu bia',
                  currentProfile?.drinkingDisplay ?? 'Chưa cập nhật',
                  Icons.local_drink,
                ),
              ],
            ),

            // const SizedBox(height: 16.0),

            // // Quick Actions
            // _buildSectionCard(
            //   title: 'Thao tác nhanh',
            //   icon: Icons.flash_on,
            //   items: [
            //     _buildActionItem(
            //       'Lịch sử chẩn đoán',
            //       'Xem lại các cuộc trò chuyện với Dia5',
            //       Icons.history,
            //       () {
            //         // TODO: Navigate to chat history
            //       },
            //     ),
            //     _buildActionItem(
            //       'Bài viết đã lưu',
            //       'Danh sách tin tức y tế bạn đã bookmark',
            //       Icons.bookmark,
            //       () {
            //         // TODO: Navigate to saved articles
            //       },
            //     ),
            //     _buildActionItem(
            //       'Cài đặt thông báo',
            //       'Quản lý thông báo về tin tức y tế mới',
            //       Icons.notifications,
            //       () {
            //         // TODO: Navigate to notification settings
            //       },
            //     ),
            //   ],
            // ),
            // const SizedBox(height: 16.0),

            // // Developer / Network Settings
            // _buildSectionCard(
            //   title: 'Cài đặt kết nối',
            //   icon: Icons.settings_ethernet_rounded,
            //   items: [
            //     _buildActionItem(
            //       'Cài đặt Server',
            //       ApiConfig.baseUrlSync,
            //       Icons.dns_rounded,
            //       () async {
            //         await Navigator.of(context).push(
            //           MaterialPageRoute(
            //             builder: (context) => const ServerSettingsScreen(),
            //           ),
            //         );
            //         // Reload URL hiện tại sau khi quay lại
            //         setState(() {});
            //       },
            //     ),
            //     const Divider(indent: 20, endIndent: 20, height: 1),
            //     _buildActionItem(
            //       'Test Thông Báo (Debug)',
            //       'Mô phỏng thông báo uống thuốc',
            //       Icons.science_outlined,
            //       () {
            //         Navigator.of(context).push(
            //           MaterialPageRoute(
            //             builder: (context) => const TestNotificationScreen(),
            //           ),
            //         );
            //       },
            //     ),
            //   ],
            // ),
            const SizedBox(height: 16.0),

            // App Info
            _buildSectionCard(
              title: 'Thông tin ứng dụng',
              icon: Icons.info,
              items: [
                _buildActionItem(
                  'Về Dia5',
                  'Tìm hiểu về trợ lý AI chẩn đoán y tế',
                  Icons.medical_services,
                  () {
                    _showAboutDialog();
                  },
                ),
                _buildActionItem(
                  'Chính sách bảo mật',
                  'Cam kết bảo vệ thông tin cá nhân của bạn',
                  Icons.privacy_tip,
                  () {
                    // TODO: Show privacy policy
                  },
                ),
                _buildActionItem(
                  'Điều khoản sử dụng',
                  'Quy định và điều kiện sử dụng ứng dụng',
                  Icons.description,
                  () {
                    // TODO: Show terms of service
                  },
                ),
                _buildActionItem('Phiên bản', 'v1.0.0', Icons.update, null),
              ],
            ),

            const SizedBox(height: 32.0),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionCard({
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
                Icon(icon, color: Theme.of(context).primaryColor, size: 24.0),
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
          ...items,
        ],
      ),
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon) {
    return Padding(
      padding: const EdgeInsets.only(left: 20.0, right: 20.0, bottom: 16.0),
      child: Row(
        children: [
          Icon(icon, color: Colors.grey.shade600, size: 20.0),
          const SizedBox(width: 12.0),
          Expanded(
            child: Text(
              label,
              style: TextStyle(fontSize: 16.0, color: Colors.grey.shade700),
            ),
          ),
          Text(
            value,
            style: const TextStyle(
              fontSize: 16.0,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionItem(
    String title,
    String subtitle,
    IconData icon,
    VoidCallback? onTap,
  ) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.only(left: 20.0, right: 20.0, bottom: 16.0),
        child: Row(
          children: [
            Icon(icon, color: Colors.grey.shade600, size: 20.0),
            const SizedBox(width: 12.0),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16.0,
                      fontWeight: FontWeight.w500,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 2.0),
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
            if (onTap != null)
              Icon(
                Icons.arrow_forward_ios,
                color: Colors.grey.shade400,
                size: 16.0,
              ),
          ],
        ),
      ),
    );
  }

  void _showAboutDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Row(
          children: [
            Icon(Icons.medical_services, color: Theme.of(context).primaryColor),
            const SizedBox(width: 8.0),
            const Text('Về Dia5'),
          ],
        ),
        content: const Text(
          'Dia5 là trợ lý AI chẩn đoán y tế được phát triển để hỗ trợ người dùng '
          'phân tích triệu chứng và đưa ra chẩn đoán sơ bộ.\n\n'
          'Lưu ý: Dia5 chỉ hỗ trợ chẩn đoán sơ bộ và không thay thế ý kiến '
          'của bác sĩ chuyên khoa.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }
}
