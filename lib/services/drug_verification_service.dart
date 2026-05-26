// import 'dart:convert'; // Unused
import 'api_service.dart';
import '../models/drug_verification_result.dart';

class DrugVerificationService {
  static DrugVerificationService? _instance;
  static DrugVerificationService get instance => _instance ??= DrugVerificationService._();
  DrugVerificationService._();

  final ApiService _apiService = ApiService.instance;

  /// Verify drug authenticity using QR code
  /// This implements the final step of the optimized workflow:
  /// - Checks both database and blockchain for verification
  /// - Returns full supply chain history if authentic
  /// - Warns about counterfeits based on blockchain ownership verification
  Future<DrugVerificationResult> verifyDrug(String qrCode) async {
    if (qrCode.isEmpty) {
      return DrugVerificationResult.error('Mã QR không hợp lệ');
    }

    try {
      // Call the public verification API endpoint
      final response = await _apiService.post(
        '/blockchain/drugs/verify',
        {'qrCode': qrCode},
        fromJson: (data) => _parseVerificationResponse(data),
      );

      if (response.isSuccess && response.data != null) {
        return response.data!;
      } else {
        return DrugVerificationResult.error(
          response.error ?? 'Không thể xác thực sản phẩm'
        );
      }
    } catch (e) {
      print('Drug verification error: $e');
      return DrugVerificationResult.error(
        'Lỗi kết nối khi xác thực: ${e.toString()}'
      );
    }
  }

  /// Parse the verification response from the API
  DrugVerificationResult _parseVerificationResponse(Map<String, dynamic> data) {
    final isValid = data['isValid'] ?? false;
    
    if (!isValid) {
      // Drug is not authentic
      return DrugVerificationResult.counterfeit(
        errorMessage: data['message'] ?? 'Sản phẩm không được xác thực trên blockchain'
      );
    }

    // Drug is authentic - parse the details
    final drugInfo = data['drugInfo'] ?? {};
    final ownershipHistory = data['ownershipHistory'] ?? [];
    
    // Build supply chain history from ownership history
    final supplyChainHistory = <String>[];
    for (final record in ownershipHistory) {
      final timestamp = record['timestamp'] ?? '';
      final from = record['fromAddress'] ?? 'Hệ thống';
      final to = record['toAddress'] ?? 'N/A';
      final action = record['action'] ?? 'Chuyển giao';
      
      // Format the history entry
      final date = timestamp.isNotEmpty 
          ? _formatDate(timestamp)
          : 'Không xác định';
      
      if (action == 'MINT') {
        supplyChainHistory.add('$date: Sản xuất bởi ${_getEntityName(from)}');
      } else if (action == 'TRANSFER') {
        supplyChainHistory.add('$date: Chuyển từ ${_getEntityName(from)} đến ${_getEntityName(to)}');
      }
    }

    // Get current location (last owner in the chain)
    String currentLocation = 'Không xác định';
    if (ownershipHistory.isNotEmpty) {
      final lastRecord = ownershipHistory.last;
      currentLocation = _getEntityName(lastRecord['toAddress'] ?? '');
    }

    return DrugVerificationResult.authentic(
      drugName: drugInfo['name'] ?? 'N/A',
      manufacturer: drugInfo['manufacturer'] ?? 'N/A',
      batchNumber: drugInfo['batchNumber'] ?? 'N/A',
      expiryDate: _formatDate(drugInfo['expiryDate'] ?? ''),
      currentLocation: currentLocation,
      supplyChainHistory: supplyChainHistory,
      blockchainHash: data['transactionHash'] ?? '',
    );
  }

  /// Format date string for display
  String _formatDate(String dateString) {
    if (dateString.isEmpty) return 'N/A';
    
    try {
      final date = DateTime.parse(dateString);
      return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
    } catch (e) {
      return dateString; // Return original if parsing fails
    }
  }

  /// Get human-readable entity name from wallet address
  String _getEntityName(String address) {
    if (address.isEmpty) return 'N/A';
    
    // In a real implementation, you might have a mapping service
    // For now, we'll show a truncated address
    if (address.length > 10) {
      return '${address.substring(0, 6)}...${address.substring(address.length - 4)}';
    }
    return address;
  }

  /// Get verification history for the current user
  Future<List<DrugVerificationResult>> getVerificationHistory() async {
    try {
      final response = await _apiService.get(
        '/blockchain/drugs/verification-history',
        fromJson: (data) => (data as List)
            .map((item) => DrugVerificationResult.fromJson(item))
            .toList(),
      );

      if (response.isSuccess && response.data != null) {
        return response.data!;
      }
    } catch (e) {
      print('Get verification history error: $e');
    }
    return [];
  }

  /// Report a suspected counterfeit drug
  Future<bool> reportCounterfeit({
    required String qrCode,
    required String description,
    String? location,
  }) async {
    try {
      final response = await _apiService.post(
        '/blockchain/drugs/report-counterfeit',
        {
          'qrCode': qrCode,
          'description': description,
          'location': location,
          'reportedAt': DateTime.now().toIso8601String(),
        },
      );

      return response.isSuccess;
    } catch (e) {
      print('Report counterfeit error: $e');
      return false;
    }
  }
}
