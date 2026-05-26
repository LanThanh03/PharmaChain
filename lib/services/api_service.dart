import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';

class ApiService {
  // Backend API base URL - dùng ApiConfig để dễ thay đổi
  static String get _baseUrl => '${ApiConfig.javaBackendUrl}/api';
  static const String _tokenKey = 'auth_token';
  
  static ApiService? _instance;
  static ApiService get instance => _instance ??= ApiService._();
  ApiService._();

  // Get auth token from storage
  Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString(_tokenKey);
  }

  // Save auth token to storage
  Future<void> _saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_tokenKey, token);
  }

  // Remove auth token from storage
  Future<void> _removeToken() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_tokenKey);
  }

  // Create headers with auth token if available
  Future<Map<String, String>> _getHeaders({bool includeAuth = true}) async {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };

    if (includeAuth) {
      final token = await _getToken();
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }
    }

    return headers;
  }

  // Generic GET request
  Future<ApiResponse<T>> get<T>(
    String endpoint, {
    bool requireAuth = true,
    T Function(dynamic)? fromJson,
  }) async {
    try {
      final headers = await _getHeaders(includeAuth: requireAuth);
      final uri = Uri.parse('$_baseUrl$endpoint');
      
      print('GET: $uri');
      
      final response = await http.get(uri, headers: headers).timeout(
        const Duration(seconds: 30),
        onTimeout: () => throw TimeoutException('Request timeout', const Duration(seconds: 30)),
      );

       return await _handleResponse<T>(response, fromJson, serverReached: true);
    } on SocketException {
      return ApiResponse.error('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.');
    } on TimeoutException {
      return ApiResponse.error('Kết nối timeout. Vui lòng thử lại.');
    } catch (e) {
      print('GET error: $e');
      return ApiResponse.error('Đã có lỗi xảy ra: ${e.toString()}');
    }
  }

  // Generic POST request
  Future<ApiResponse<T>> post<T>(
    String endpoint,
    dynamic body, {
    bool requireAuth = true,
    T Function(dynamic)? fromJson,
  }) async {
    try {
      final headers = await _getHeaders(includeAuth: requireAuth);
      final uri = Uri.parse('$_baseUrl$endpoint');
      
      print('POST: $uri');
      print('Body: ${json.encode(body)}');
      
      final response = await http.post(
        uri,
        headers: headers,
        body: json.encode(body),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () => throw TimeoutException('Request timeout', const Duration(seconds: 30)),
      );

       return await _handleResponse<T>(response, fromJson, serverReached: true);
    } on SocketException {
      return ApiResponse.error('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.');
    } on TimeoutException {
      return ApiResponse.error('Kết nối timeout. Vui lòng thử lại.');
    } catch (e) {
      print('POST error: $e');
      return ApiResponse.error('Đã có lỗi xảy ra: ${e.toString()}');
    }
  }

  // Generic PUT request
  Future<ApiResponse<T>> put<T>(
    String endpoint,
    dynamic body, {
    bool requireAuth = true,
    T Function(dynamic)? fromJson,
  }) async {
    try {
      final headers = await _getHeaders(includeAuth: requireAuth);
      final uri = Uri.parse('$_baseUrl$endpoint');
      
      print('PUT: $uri');
      
      final response = await http.put(
        uri,
        headers: headers,
        body: json.encode(body),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () => throw TimeoutException('Request timeout', const Duration(seconds: 30)),
      );

       return await _handleResponse<T>(response, fromJson, serverReached: true);
    } on SocketException {
      return ApiResponse.error('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.');
    } on TimeoutException {
      return ApiResponse.error('Kết nối timeout. Vui lòng thử lại.');
    } catch (e) {
      print('PUT error: $e');
      return ApiResponse.error('Đã có lỗi xảy ra: ${e.toString()}');
    }
  }

  // Generic DELETE request
  Future<ApiResponse<T>> delete<T>(
    String endpoint, {
    bool requireAuth = true,
    T Function(dynamic)? fromJson,
  }) async {
    try {
      final headers = await _getHeaders(includeAuth: requireAuth);
      final uri = Uri.parse('$_baseUrl$endpoint');
      
      print('DELETE: $uri');
      
      final response = await http.delete(uri, headers: headers).timeout(
        const Duration(seconds: 30),
        onTimeout: () => throw TimeoutException('Request timeout', const Duration(seconds: 30)),
      );

       return await _handleResponse<T>(response, fromJson, serverReached: true);
    } on SocketException {
      return ApiResponse.error('Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.');
    } on TimeoutException {
      return ApiResponse.error('Kết nối timeout. Vui lòng thử lại.');
    } catch (e) {
      print('DELETE error: $e');
      return ApiResponse.error('Đã có lỗi xảy ra: ${e.toString()}');
    }
  }

  // Handle HTTP response
  Future<ApiResponse<T>> _handleResponse<T>(
    http.Response response, 
    T Function(dynamic)? fromJson, {
    bool serverReached = false, // true = request đã đến server thật, false = không kết nối được
  }) async {
    print('Response status: ${response.statusCode}');
    print('Response body: ${response.body}');

    try {
      final jsonResponse = json.decode(response.body);

      if (response.statusCode >= 200 && response.statusCode < 300) {
        // Success response
        if (jsonResponse is Map<String, dynamic>) {
          final success = jsonResponse['success'] ?? true;
          final message = jsonResponse['message'] as String?;
          final data = jsonResponse['data'];

          if (success) {
            T? parsedData;
            if (data != null && fromJson != null) {
              parsedData = fromJson(data);
            } else if (data is T) {
              parsedData = data;
            }
            
            return ApiResponse.success(parsedData, message);
          } else {
            return ApiResponse.error(message ?? 'Unknown error');
          }
        }
        
        // If response is not in expected format, return raw data
        return ApiResponse.success(
          fromJson != null ? fromJson(jsonResponse) : jsonResponse as T?,
        );
      } else {
        // Error response
        String errorMessage = 'Unknown error';
        
        if (jsonResponse is Map<String, dynamic>) {
          errorMessage = jsonResponse['message'] as String? ?? 
                        jsonResponse['error'] as String? ?? 
                        'HTTP ${response.statusCode}';
        }

        // Handle specific status codes
        if (response.statusCode == 401) {
          // ⚠️ Chỉ xóa token nếu request ĐÃ ĐẾN được server (không phải lỗi mạng)
          // Tránh xóa token oan khi server unreachable (SocketException bị catch trước)
          if (serverReached) {
            await _removeToken();
            errorMessage = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
          } else {
            errorMessage = 'Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.';
          }
        } else if (response.statusCode == 403) {
          errorMessage = 'Bạn không có quyền thực hiện thao tác này.';
        } else if (response.statusCode == 404) {
          errorMessage = 'Không tìm thấy tài nguyên yêu cầu.';
        } else if (response.statusCode >= 500) {
          errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
        }

        return ApiResponse.error(errorMessage);
      }
    } catch (e) {
      print('JSON decode error: $e');
      return ApiResponse.error('Lỗi phân tích dữ liệu từ server');
    }
  }

  // Save token from auth response
  Future<void> saveAuthToken(String token) async {
    await _saveToken(token);
  }

  // Clear auth token (for logout)
  Future<void> clearAuthToken() async {
    await _removeToken();
  }

  // Check if user is authenticated
  Future<bool> isAuthenticated() async {
    final token = await _getToken();
    return token != null && token.isNotEmpty;
  }
}

// Generic API Response wrapper
class ApiResponse<T> {
  final bool success;
  final T? data;
  final String? message;
  final String? error;

  ApiResponse._({
    required this.success,
    this.data,
    this.message,
    this.error,
  });

  factory ApiResponse.success(T? data, [String? message]) {
    return ApiResponse._(
      success: true,
      data: data,
      message: message,
    );
  }

  factory ApiResponse.error(String error) {
    return ApiResponse._(
      success: false,
      error: error,
    );
  }

  bool get isSuccess => success;
  bool get isError => !success;
}

// Timeout exception
class TimeoutException implements Exception {
  final String message;
  final Duration duration;

  const TimeoutException(this.message, this.duration);

  @override
  String toString() => 'TimeoutException: $message';
}
