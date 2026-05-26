import 'package:shared_preferences/shared_preferences.dart';
import 'dart:io';

/// API Configuration - Dynamic Server Discovery
/// Hỗ trợ tự động phát hiện IP server, thay đổi URL qua Settings
class ApiConfig {
  static const String _serverUrlKey = 'server_base_url';
  static const int _port = 8080;
  static const int _connectTimeoutMs = 2000;

  /// Default fallback URLs theo thứ tự ưu tiên
  static const List<String> _fallbackUrls = [
    'http://10.10.33.186:8080', // ✅ IP WiFi của máy (ưu tiên cao nhất)
    // 'http://10.10.33.223:8080', // IP WiFi cũ (backup)
    // 'http://192.168.110.35:8080', // IP WiFi khác
    'http://192.168.1.6:8080',   // WiFi thường gặp (slot 1)
    'http://192.168.1.100:8080', // WiFi thường gặp (slot 2)
    'http://192.168.1.105:8080',
    'http://192.168.0.100:8080',
    'http://192.168.0.105:8080',
    'http://10.0.2.2:8080',      // Android Emulator
    'http://10.0.0.2:8080',
    'http://172.20.10.2:8080',   // iPhone Hotspot
    'http://127.0.0.1:8080',     // localhost (chỉ dùng khi chạy trên máy tính)
    'http://localhost:8080',     // localhost alias (KHÔNG dùng trên Chrome/Android)
  ];

  // Cache in memory để tránh đọc SharedPreferences mỗi request
  static String? _cachedBaseUrl;

  /// Lấy base URL hiện tại (đọc từ cache hoặc SharedPreferences)
  static Future<String> getBaseUrl() async {
    if (_cachedBaseUrl != null) return _cachedBaseUrl!;

    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getString(_serverUrlKey);
    if (saved != null && saved.isNotEmpty) {
      _cachedBaseUrl = saved;
      return saved;
    }

    // Chưa có URL đã lưu → dùng fallback đầu tiên
    _cachedBaseUrl = _fallbackUrls.first;
    return _cachedBaseUrl!;
  }

  /// Cập nhật URL server và lưu vào SharedPreferences
  static Future<void> setBaseUrl(String url) async {
    // Chuẩn hoá: bỏ trailing slash
    final cleaned = url.trimRight().replaceAll(RegExp(r'/+$'), '');
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_serverUrlKey, cleaned);
    _cachedBaseUrl = cleaned;
  }

  /// Xoá URL đã lưu (reset về auto-detect)
  static Future<void> clearSavedUrl() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_serverUrlKey);
    _cachedBaseUrl = null;
  }

  /// Tự động phát hiện server: thử lần lượt từng URL trong danh sách
  /// Trả về URL đầu tiên phản hồi được, hoặc null nếu không có
  static Future<String?> autoDiscoverServer({
    void Function(String url, bool success)? onTry,
  }) async {
    final candidates = List<String>.from(_fallbackUrls);

    // Thêm URL đang dùng vào đầu danh sách (ưu tiên thử trước)
    final current = _cachedBaseUrl;
    if (current != null && !candidates.contains(current)) {
      candidates.insert(0, current);
    }

    for (final url in candidates) {
      final reachable = await _ping(url);
      onTry?.call(url, reachable);
      if (reachable) {
        await setBaseUrl(url);
        return url;
      }
    }
    return null;
  }

  /// Thử kết nối một URL bằng cách mở TCP socket (hoạt động trên web + mobile)
  static Future<bool> _ping(String baseUrl) async {
    try {
      final uri = Uri.parse(baseUrl);
      final host = uri.host;
      final port = uri.port > 0 ? uri.port : 8080;

      final socket = await Socket.connect(
        host,
        port,
        timeout: const Duration(milliseconds: _connectTimeoutMs),
      );
      socket.destroy();
      return true;
    } catch (_) {
      return false;
    }
  }

  /// Danh sách các URL để hiện trong UI Settings
  static List<String> get fallbackUrls => List.unmodifiable(_fallbackUrls);

  // ── Convenience getters (không thể dùng await ở top-level, nên dùng sync cache) ──

  /// Lấy URL đồng bộ từ cache (gọi getBaseUrl() trước ở khởi động app)
  static String get baseUrlSync => _cachedBaseUrl ?? _fallbackUrls.first;

  static String get javaBackendUrl => baseUrlSync;
  static String get blockchainBackendUrl => baseUrlSync;
  static String get baseUrl => baseUrlSync;

  // Endpoints
  static String get userLogin => '$javaBackendUrl/api/auth/login';
  static String get userRegister => '$javaBackendUrl/api/auth/register';
  static String get userProfile => '$javaBackendUrl/api/users/profile';
  static String get medicalAI => '$javaBackendUrl/api/ai/diagnose';
  static String get appointments => '$javaBackendUrl/api/appointments';

  static String get blockchainVerify =>
      '$blockchainBackendUrl/api/blockchain/public/verify';
  static String get blockchainBatches =>
      '$blockchainBackendUrl/api/blockchain/batches';
  static String get blockchainShipments =>
      '$blockchainBackendUrl/api/blockchain/distributor/shipment';
  static String get blockchainHealth => '$blockchainBackendUrl/health';
}
