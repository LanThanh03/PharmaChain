import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

/// Timeout chung cho các API medication (30s cho blockchain operations)
const _kMedicationTimeout = Duration(seconds: 30);

/// Service for managing user medications and reminders
class MedicationService {
  static String get baseUrl => '${ApiConfig.baseUrl}/api';

  /// Get dispense instruction by item code (after scanning QR)
  Future<Map<String, dynamic>> getDispenseInstruction(String itemCode) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications/dispense/$itemCode'),
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? '',
        };
      } else {
        return {
          'success': false,
          'message': 'Không tìm thấy thông tin thuốc',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi kết nối: ${e.toString()}',
      };
    }
  }

  /// Add medication to user's record
  Future<Map<String, dynamic>> addMedication({
    required int userId,
    String? dispenseInstructionId,
    required String drugName,
    String? batchNumber,
    String? expiryDate,
    required String dosage,
    required int frequency,
    required String mealRelation,
    required String reminderTimes,
    required String startDate,
    required String endDate,
    String? pharmacyName,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/app/medications/add-manual'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'userId': userId,
          'dispenseInstructionId': dispenseInstructionId,
          'drugName': drugName,
          'batchNumber': batchNumber,
          'expiryDate': expiryDate,
          'dosage': dosage,
          'frequency': frequency,
          'mealRelation': mealRelation,
          'reminderTimes': reminderTimes,
          'startDate': startDate,
          'endDate': endDate,
          'pharmacyName': pharmacyName,
          'itemCode': null, // optional
          'manufacturer': null, // optional
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
          'message': data['message'] ?? 'Đã thêm thuốc thành công',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể thêm thuốc',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Get all active medications for user
  Future<Map<String, dynamic>> getActiveMedications(int userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications?userId=$userId'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_kMedicationTimeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'] ?? [],
          'message': data['message'] ?? '',
        };
      } else {
        return {
          'success': false,
          'data': [],
          'message': 'Không thể tải danh sách thuốc (Mã: ${response.statusCode})',
        };
      }
    } on TimeoutException {
      return {
        'success': false,
        'data': [],
        'message': 'Quá thời gian chờ. Kiểm tra kết nối đến server ${ApiConfig.baseUrl}',
      };
    } catch (e) {
      return {
        'success': false,
        'data': [],
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Get scheduled reminders for a specific record
  Future<Map<String, dynamic>> getRemindersByRecordId(int recordId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications/records/$recordId/reminders'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_kMedicationTimeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'] ?? [],
        };
      } else {
        return {
          'success': false,
          'data': [],
        };
      }
    } on TimeoutException {
      return {
        'success': false,
        'data': [],
        'message': 'Quá thời gian chờ. Kiểm tra kết nối đến server',
      };
    } catch (e) {
      return {
        'success': false,
        'data': [],
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Get medication history
  Future<Map<String, dynamic>> getMedicationHistory(int userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications/history?userId=$userId'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_kMedicationTimeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'] ?? [],
        };
      } else {
        return {
          'success': false,
          'data': [],
        };
      }
    } on TimeoutException {
      return {
        'success': false,
        'data': [],
        'message': 'Quá thời gian chờ. Kiểm tra kết nối đến server ${ApiConfig.baseUrl}',
      };
    } catch (e) {
      return {
        'success': false,
        'data': [],
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Get today's reminders
  Future<Map<String, dynamic>> getTodayReminders(int userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications/reminders/today?userId=$userId'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_kMedicationTimeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'] ?? [],
        };
      } else {
        return {
          'success': false,
          'data': [],
        };
      }
    } on TimeoutException {
      return {
        'success': false,
        'data': [],
        'message': 'Quá thời gian chờ. Kiểm tra kết nối đến server ${ApiConfig.baseUrl}',
      };
    } catch (e) {
      return {
        'success': false,
        'data': [],
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Mark reminder as taken
  Future<Map<String, dynamic>> markReminderAsTaken(
      int reminderId, String? notes) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/app/medications/reminders/$reminderId/taken'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'notes': notes}),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'message': data['message'] ?? 'Đã đánh dấu đã uống',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể cập nhật',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Mark reminder as skipped
  Future<Map<String, dynamic>> markReminderAsSkipped(
      int reminderId, String? notes) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/app/medications/reminders/$reminderId/skip'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'reason': notes}),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'message': data['message'] ?? 'Đã đánh dấu bỏ qua',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể cập nhật',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Update reminder times
  Future<Map<String, dynamic>> updateReminderTimes(
      int recordId, String newTimes) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/app/medications/$recordId/update-times'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({'times': newTimes}),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'message': data['message'] ?? 'Đã cập nhật giờ uống',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể cập nhật',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Mark reminder action by time
  Future<Map<String, dynamic>> markReminderActionByTime({
    required int recordId,
    required String scheduledDate, // YYYY-MM-DD
    required String scheduledTime, // HH:mm
    required String action, // "TAKEN" or "SKIPPED"
    String? notes,
  }) async {
    try {
      final response = await http.put(
        Uri.parse('$baseUrl/app/medications/records/$recordId/reminders/action'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'scheduledDate': scheduledDate,
          'scheduledTime': scheduledTime,
          'action': action,
          'notes': notes,
        }),
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'message': data['message'] ?? 'Đã cập nhật',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể cập nhật (Mã: ${response.statusCode})',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Get adherence statistics
  Future<Map<String, dynamic>> getAdherenceStats(int userId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/app/medications/summary/today?userId=$userId'),
        headers: {'Content-Type': 'application/json'},
      ).timeout(_kMedicationTimeout);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'data': data['data'],
        };
      } else {
        return {
          'success': false,
          'data': null,
        };
      }
    } on TimeoutException {
      return {
        'success': false,
        'data': null,
        'message': 'Quá thời gian chờ. Kiểm tra kết nối đến server ${ApiConfig.baseUrl}',
      };
    } catch (e) {
      return {
        'success': false,
        'data': null,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }

  /// Stop medication
  Future<Map<String, dynamic>> stopMedication(int recordId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/app/medications/$recordId/stop'),
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return {
          'success': data['success'] ?? false,
          'message': data['message'] ?? 'Đã dừng thuốc',
        };
      } else {
        return {
          'success': false,
          'message': 'Không thể dừng thuốc',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi: ${e.toString()}',
      };
    }
  }
}
