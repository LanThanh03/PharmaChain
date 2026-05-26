import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/drug_verification_result.dart';

/// Service for Java Spring Boot Backend
/// Xử lý blockchain functions: verify, batch, shipment
/// Cũng xử lý: users, auth, medical AI, etc.
class BlockchainApiService {
  // Import config
  static const String baseUrl = 'http://localhost:8080'; // Java Spring Boot backend

  final http.Client _client;

  BlockchainApiService({http.Client? client}) : _client = client ?? http.Client();

  /// Verify drug by QR code (Public endpoint - no auth required)
  Future<DrugVerificationResult> verifyDrug(String qrCode) async {
    try {
      final response = await _client.post(
        Uri.parse('$baseUrl/api/blockchain/public/verify'),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: jsonEncode({
          'qrCode': qrCode,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        
        if (data['success'] == true) {
          return DrugVerificationResult.fromJson(data['data']);
        } else {
          throw Exception(data['message'] ?? 'Verification failed');
        }
      } else {
        throw Exception('Server error: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Failed to verify drug: $e');
    }
  }

  /// Get all batches (requires authentication in production)
  Future<List<Map<String, dynamic>>> getAllBatches({String? token}) async {
    try {
      final headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };
      
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }

      final response = await _client.get(
        Uri.parse('$baseUrl/api/blockchain/batches'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          return List<Map<String, dynamic>>.from(data['data']);
        } else {
          throw Exception(data['message'] ?? 'Failed to get batches');
        }
      } else {
        throw Exception('Server error: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Failed to get batches: $e');
    }
  }

  /// Get batch by ID
  Future<Map<String, dynamic>> getBatchById(String batchId, {String? token}) async {
    try {
      final headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };
      
      if (token != null) {
        headers['Authorization'] = 'Bearer $token';
      }

      final response = await _client.get(
        Uri.parse('$baseUrl/api/blockchain/batches/$batchId'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          return data['data'];
        } else {
          throw Exception(data['message'] ?? 'Batch not found');
        }
      } else {
        throw Exception('Server error: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Failed to get batch: $e');
    }
  }

  /// Health check
  Future<bool> healthCheck() async {
    try {
      final response = await _client.get(
        Uri.parse('$baseUrl/health'),
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  void dispose() {
    _client.close();
  }
}

