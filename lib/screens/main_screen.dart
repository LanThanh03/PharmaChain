import 'package:flutter/material.dart';
import 'product_verification_screen.dart';
import 'my_medications_screen.dart';
import 'today_reminders_screen.dart';
import 'drug_ai_chat_screen.dart';
import '../services/auth_service.dart';

/// Main screen with bottom navigation bar
class MainScreen extends StatefulWidget {
  const MainScreen({Key? key}) : super(key: key);

  @override
  _MainScreenState createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  final GlobalKey<MyMedicationsScreenState> _medicationsKey =
      GlobalKey<MyMedicationsScreenState>();

  // Lấy userId từ AuthService (đã login), fallback = 1
  int get _userId {
    final user = AuthService.instance.currentUser;
    final id = user?.numericId ?? 1;
    print('🔑 MainScreen userId: ${user?.id} -> hash=$id');
    return id;
  }

  @override
  void initState() {
    super.initState();
    // Khởi tạo các màn hình không có trạng thái động trước
  }

  // Tạo danh sách màn hình động dựa trên _currentIndex
  List<Widget> _buildScreens() {
    final userId = _userId;
    return [
      HomeScreen(onNavigate: _navigateToTab, userId: userId), // Trang chủ
      ProductVerificationScreen(isActive: _currentIndex == 1), // Quét mã
      MyMedicationsScreen(
        key: _medicationsKey,
        userId: userId,
      ), // Thuốc của tôi
      const DrugAIChatScreen(), // Tư vấn AI
    ];
  }

  void _navigateToTab(int index) {
    setState(() {
      _currentIndex = index;
    });
    // Reload medications when switching to medications tab
    if (index == 2) {
      _reloadMedications();
    }
  }

  void _reloadMedications() {
    _medicationsKey.currentState?.loadMedications();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _currentIndex, children: _buildScreens()),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
          // Reload medications when switching to medications tab
          if (index == 2) {
            _reloadMedications();
          }
        },
        type: BottomNavigationBarType.fixed,
        selectedItemColor: const Color(0xFF284C7B),
        unselectedItemColor: Colors.grey[400],
        selectedFontSize: 12,
        unselectedFontSize: 11,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            activeIcon: Icon(Icons.home),
            label: 'Trang chủ',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.qr_code_scanner_outlined),
            activeIcon: Icon(Icons.qr_code_scanner),
            label: 'Quét mã',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.medication_outlined),
            activeIcon: Icon(Icons.medication),
            label: 'Thuốc của tôi',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.chat_outlined),
            activeIcon: Icon(Icons.chat),
            label: 'Tư vấn AI',
          ),
        ],
      ),
    );
  }
}

/// Home screen with quick actions and statistics
class HomeScreen extends StatelessWidget {
  final Function(int) onNavigate;
  final int userId;

  const HomeScreen({Key? key, required this.onNavigate, this.userId = 1})
    : super(key: key);

  String get _firstName {
    final name = AuthService.instance.currentUser?.name ?? 'Nhung';
    if (name.trim().isEmpty) return 'Nhung';
    final parts = name.trim().split(' ');
    return parts.last;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        elevation: 0,
        backgroundColor: const Color(0xFF284C7B),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: Colors.blue[300],
                borderRadius: BorderRadius.circular(6),
              ),
              child: const Icon(Icons.widgets, color: Colors.white, size: 20),
            ),
            const SizedBox(width: 8),
            const Text(
              'PharmaLedger',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.w600,
                fontSize: 20,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_none, color: Colors.white),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.person_outline, color: Colors.white),
            onPressed: () {
              Navigator.pushNamed(context, '/profile');
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Greeting section
            Text(
              'Xin chào, $_firstName',
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Color(0xFF284C7B),
              ),
            ),
            const SizedBox(height: 6),
            Text(
              'Quản lý thuốc an toàn',
              style: TextStyle(fontSize: 16, color: Colors.grey[700]),
            ),
            const SizedBox(height: 24),

            // Main Scan Button (Quét mã thuốc)
            Container(
              decoration: BoxDecoration(
                color: const Color(0xFF284C7B),
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF284C7B).withOpacity(0.3),
                    blurRadius: 12,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  onTap: () => onNavigate(1),
                  borderRadius: BorderRadius.circular(16),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 24,
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(
                            Icons.qr_code_scanner,
                            color: Color(0xFF284C7B),
                            size: 32,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Quét mã thuốc',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Xác thực & xem nguồn gốc',
                                style: TextStyle(
                                  color: Colors.white.withOpacity(0.85),
                                  fontSize: 14,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const Icon(
                          Icons.chevron_right,
                          color: Colors.white,
                          size: 28,
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Two Action Cards
            // Column(
            //   children: [
            //     _FeatureCard(
            //       iconWidget: Container(
            //         padding: const EdgeInsets.all(10),
            //         decoration: BoxDecoration(
            //           color: const Color(0xFF4A84F6),
            //           borderRadius: BorderRadius.circular(10),
            //         ),
            //         child: const Icon(Icons.add, color: Colors.white, size: 28),
            //       ),
            //       title: 'Thuốc của tôi',
            //       subtitle: 'Quản lý thuốc',
            //       onTap: () => onNavigate(2),
            //     ),

            //     const SizedBox(height: 16),

            //     _FeatureCard(
            //       iconWidget: const Icon(
            //         Icons.alarm,
            //         color: Color(0xFF10B981),
            //         size: 48,
            //       ),
            //       title: 'Nhắc uống',
            //       subtitle: 'Nhắc uống thuốc',
            //       onTap: () {
            //         Navigator.push(
            //           context,
            //           MaterialPageRoute(
            //             builder: (context) =>
            //                 TodayRemindersScreen(userId: userId),
            //           ),
            //         );
            //       },
            //     ),
            //   ],
            // ),
            Column(
              children: [
                SizedBox(
                  width: double.infinity,
                  child: InkWell(
                    onTap: () => onNavigate(2),
                    borderRadius: BorderRadius.circular(16),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: const [
                          BoxShadow(
                            blurRadius: 8,
                            offset: Offset(0, 2),
                            color: Color(0x14000000),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: const Color(0xFF4A84F6),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: const Icon(
                              Icons.add,
                              color: Colors.white,
                              size: 28,
                            ),
                          ),
                          const SizedBox(width: 16),
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  'Thuốc của tôi',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                SizedBox(height: 4),
                                Text(
                                  'Quản lý thuốc',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.grey,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Icon(Icons.chevron_right, color: Colors.grey),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: InkWell(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) =>
                              TodayRemindersScreen(userId: userId),
                        ),
                      );
                    },
                    borderRadius: BorderRadius.circular(16),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: const [
                          BoxShadow(
                            blurRadius: 8,
                            offset: Offset(0, 2),
                            color: Color(0x14000000),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.alarm,
                            color: Color(0xFF10B981),
                            size: 40,
                          ),
                          const SizedBox(width: 16),
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Text(
                                  'Nhắc uống',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                SizedBox(height: 4),
                                Text(
                                  'Nhắc uống thuốc',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.grey,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Icon(Icons.chevron_right, color: Colors.grey),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),

            // const SizedBox(height: 32),

            // "Thông tin hữu ích" section
            // const Text(
            //   'Thông tin hữu ích',
            //   style: TextStyle(
            //     fontSize: 18,
            //     fontWeight: FontWeight.bold,
            //     color: Color(0xFF284C7B),
            //   ),
            // ),
            // const SizedBox(height: 16),
            // const _InfoCard(
            //   iconWidget: Icon(
            //     Icons.health_and_safety_outlined,
            //     color: Color(0xFF10B981),
            //     size: 28,
            //   ),
            //   title: 'Bảo vệ sức khỏe',
            //   description: 'Xác thực thuốc chính hãng, tránh hàng giả',
            // ),
            // const SizedBox(height: 12),
            // const _InfoCard(
            //   iconWidget: Icon(
            //     Icons.route_outlined,
            //     color: Color(0xFF10B981),
            //     size: 28,
            //   ),
            //   title: 'Theo dõi hành trình',
            //   description: 'Xem đầy đủ quá trình từ sản xuất đến phân phối',
            // ),
          ],
        ),
      ),
    );
  }
}

class _FeatureCard extends StatelessWidget {
  final Widget iconWidget;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _FeatureCard({
    required this.iconWidget,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF4A84F6).withOpacity(0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(16),
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: 16.0,
              vertical: 20.0,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                iconWidget,
                const SizedBox(height: 20),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF284C7B),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  subtitle,
                  style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  final Widget iconWidget;
  final String title;
  final String description;

  const _InfoCard({
    required this.iconWidget,
    required this.title,
    required this.description,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFF10B981).withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: iconWidget,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF284C7B),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: TextStyle(fontSize: 13, color: Colors.grey[600]),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
