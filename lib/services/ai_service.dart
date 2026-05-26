import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

/// Service for AI-powered drug consultation
class AIService {
  static String get _baseUrl => '${ApiConfig.baseUrl}/api/ai/drug';
  static const Duration _timeout = Duration(seconds: 30);

  /// Get drug info from DATABASE only (no AI call - instant)
  /// GET /api/ai/drug/db-info?name=...
  Future<Map<String, dynamic>> getDrugDbInfo(String drugName) async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/db-info?name=${Uri.encodeComponent(drugName)}'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      }
      return {'success': false, 'message': 'Mã lỗi ${response.statusCode}'};
    } on TimeoutException {
      return {'success': false, 'message': 'Không thể kết nối server.'};
    } catch (e) {
      return {'success': false, 'message': 'Lỗi: ${e.toString()}'};
    }
  }

  /// Get comprehensive drug information
  /// GET /api/ai/drug/info?name=...
  Future<Map<String, dynamic>> getDrugInfo(String drugName) async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/info?name=${Uri.encodeComponent(drugName)}'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      }
      return {'success': false, 'message': 'Mã lỗi ${response.statusCode}'};
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi, thử lại sau.'};
    } catch (e) {
      return {'success': false, 'message': 'Lỗi kết nối: ${e.toString()}'};
    }
  }

  /// Ask a question about a drug
  /// POST /api/ai/drug/ask
  Future<Map<String, dynamic>> askAboutDrug(String drugName, String question) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/ask'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'drugName': drugName, 'question': question}),
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      }
      return {'success': false, 'message': 'Không thể trả lời câu hỏi (${response.statusCode})'};
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi, thử lại sau.'};
    } catch (e) {
      return {'success': false, 'message': 'Lỗi: ${e.toString()}'};
    }
  }

  /// Consult about a specific drug (auto-fetches DB context on backend)
  /// POST /api/ai/drug/consult
  Future<Map<String, dynamic>> consultDrug(String drugName) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/consult'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'drugName': drugName}),
      ).timeout(const Duration(seconds: 45)); // Longer timeout for comprehensive response

      if (response.statusCode == 200) {
        final body = json.decode(response.body);
        if (body['success'] == true && body['data'] != null) {
          return {
            'success': true,
            'reply': body['data']['response']?.toString() ?? '',
            'hasDbData': body['data']['hasDbData'] ?? false,
            'articleUrl': body['data']['articleUrl']?.toString(),
            'message': body['message'] ?? '',
          };
        }
        return {
          'success': false,
          'message': body['message'] ?? 'AI không có câu trả lời.',
        };
      }
      try {
        final errBody = json.decode(response.body);
        return {
          'success': false,
          'message': errBody['message'] ?? 'Lỗi ${response.statusCode}',
        };
      } catch (_) {
        return {'success': false, 'message': 'Lỗi ${response.statusCode}'};
      }
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi sau 45s. Thử lại sau.'};
    } catch (e) {
      final msg = e.toString().contains('SocketException') || e.toString().contains('Connection refused')
          ? 'Không kết nối được backend AI.\nKiểm tra IP ${ApiConfig.baseUrl} và WiFi.'
          : 'Lỗi: ${e.toString()}';
      return {'success': false, 'message': msg};
    }
  }

  /// General chat with AI about medications
  /// POST /api/ai/drug/chat
  /// Backend nhận: { message: String, history: [{role, content}] }
  /// Backend trả:  { data: { response: String, userMessage, model } }
  Future<Map<String, dynamic>> chat(
      String message, List<Map<String, String>>? history) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/chat'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'message': message,
          'history': history ?? [],
        }),
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final body = json.decode(response.body);
        if (body['success'] == true && body['data'] != null) {
          return {
            'success': true,
            // Backend trả 'response' field bên trong data
            'reply': body['data']['response']?.toString() ?? '',
            'message': body['message'] ?? '',
          };
        }
        return {
          'success': false,
          'message': body['message'] ?? body['error'] ?? 'AI không có câu trả lời.',
        };
      }
      // Đọc error message từ response body nếu có
      try {
        final errBody = json.decode(response.body);
        return {
          'success': false,
          'message': errBody['message'] ?? errBody['error'] ?? 'Lỗi ${response.statusCode}',
        };
      } catch (_) {
        return {'success': false, 'message': 'Lỗi ${response.statusCode}'};
      }
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi sau 30s. Thử lại sau.'};
    } catch (e) {
      final msg = e.toString().contains('SocketException') || e.toString().contains('Connection refused')
          ? 'Không kết nối được backend AI.\nKiểm tra IP ${ApiConfig.baseUrl} và WiFi.'
          : 'Lỗi: ${e.toString()}';
      return {'success': false, 'message': msg};
    }
  }

  /// Check drug interactions
  /// POST /api/ai/drug/interactions
  Future<Map<String, dynamic>> checkDrugInteractions(List<String> drugNames) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/interactions'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'drugNames': drugNames}),
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      }
      return {'success': false, 'message': 'Không thể kiểm tra tương tác thuốc'};
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi, thử lại sau.'};
    } catch (e) {
      return {'success': false, 'message': 'Lỗi: ${e.toString()}'};
    }
  }

  /// Get medication tips
  /// POST /api/ai/drug/tips
  Future<Map<String, dynamic>> getMedicationTips({
    required String drugName,
    required String dosage,
    required int frequency,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/tips'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'drugName': drugName,
          'dosage': dosage,
          'frequency': frequency,
        }),
      ).timeout(_timeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      }
      return {'success': false, 'message': 'Không thể lấy lời khuyên'};
    } on TimeoutException {
      return {'success': false, 'message': 'AI không phản hồi, thử lại sau.'};
    } catch (e) {
      return {'success': false, 'message': 'Lỗi: ${e.toString()}'};
    }
  }
}
