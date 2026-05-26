import 'package:flutter/material.dart';
import 'screens/main_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/login_screen.dart';
import 'services/auth_service.dart';
import 'services/notification_service.dart';
import 'config/api_config.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Load server URL vào cache (đọc từ SharedPreferences)
  await ApiConfig.getBaseUrl();

  // ✅ Tự động phát hiện server khi khởi động (tránh dùng IP cũ bị timeout)
  try {
    final discovered = await ApiConfig.autoDiscoverServer()
        .timeout(const Duration(seconds: 8)); // Giới hạn 8s cho auto-discover
    if (discovered != null) {
      print('✅ Auto-discovered server: $discovered');
    } else {
      print('⚠️ Auto-discover failed, using cached URL: ${ApiConfig.baseUrl}');
    }
  } catch (_) {
    print('⚠️ Auto-discover timeout, using cached URL: ${ApiConfig.baseUrl}');
  }

  // Initialize notifications
  final notificationService = NotificationService();
  await notificationService.initialize();
  await notificationService.requestPermissions();

  await AuthService.instance.initialize();
  runApp(const DoAnTotNghiepBCApp());
}

class DoAnTotNghiepBCApp extends StatelessWidget {
  const DoAnTotNghiepBCApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Đồ Án Tốt Nghiệp - Blockchain Drug Management',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        // Medical theme with blue color scheme
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1976D2), // Medical blue
          brightness: Brightness.light,
        ),
        primaryColor: const Color(0xFF1976D2),
        useMaterial3: true,
        
        // AppBar theme
        appBarTheme: const AppBarTheme(
          elevation: 0,
          backgroundColor: Colors.white,
          foregroundColor: Colors.black87,
          titleTextStyle: TextStyle(
            fontSize: 18.0,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        
        // Text theme
        textTheme: const TextTheme(
          bodyLarge: TextStyle(
            fontSize: 16.0,
            height: 1.4,
            color: Colors.black87,
          ),
          bodyMedium: TextStyle(
            fontSize: 14.0,
            height: 1.4,
            color: Colors.black87,
          ),
        ),
        
        // Icon theme
        iconTheme: const IconThemeData(
          color: Color(0xFF1976D2),
        ),
        
        // Input decoration theme
        inputDecorationTheme: InputDecorationTheme(
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(24.0),
            borderSide: BorderSide(color: Colors.grey.shade300),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(24.0),
            borderSide: BorderSide(color: Colors.grey.shade300),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(24.0),
            borderSide: const BorderSide(color: Color(0xFF1976D2)),
          ),
          filled: true,
          fillColor: Colors.grey.shade50,
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 20.0,
            vertical: 12.0,
          ),
        ),
      ),
      routes: {
        '/profile': (context) => const ProfileScreen(),
        '/login': (context) => const LoginScreen(),
      },
      // Tự động vào MainScreen nếu đã đăng nhập, ngược lại vào LoginScreen
      home: AuthService.instance.isLoggedIn ? const MainScreen() : const LoginScreen(),
    );
  }
}
