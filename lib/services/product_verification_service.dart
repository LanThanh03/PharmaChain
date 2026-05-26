import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config/api_config.dart';

/// Service để verify sản phẩm theo item code
class ProductVerificationService {
  static final ProductVerificationService instance = ProductVerificationService._();
  ProductVerificationService._();

  /// Verify product bằng item code (QR scan)
  Future<ProductVerificationResult> verifyProduct(String itemCode) async {
    try {
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final uri = Uri.parse('${ApiConfig.baseUrl}/api/public/verify/batch/$itemCode?t=$timestamp');
      print('Calling verify API: $uri');

      final response = await http
          .get(uri, headers: {'Content-Type': 'application/json'})
          .timeout(const Duration(seconds: 30));

      print('Response status: ${response.statusCode}');

      if (response.statusCode == 200) {
        final jsonResponse = json.decode(response.body);
        // /batch/ endpoint wraps: { success: true, data: {...} }
        if (jsonResponse['success'] == true && jsonResponse['data'] != null) {
          return ProductVerificationResult.fromJson(jsonResponse['data']);
        } else {
          return ProductVerificationResult.fromJson(jsonResponse);
        }
      } else {
        return ProductVerificationResult.error(
            'Không thể xác thực sản phẩm (Mã lỗi: ${response.statusCode})');
      }
    } on TimeoutException {
      return ProductVerificationResult.error(
          'Quá thời gian chờ. Backend đang bận, vui lòng thử lại sau.');
    } catch (e) {
      print('Verify error: $e');
      final msg = e.toString().contains('SocketException') ||
              e.toString().contains('Connection refused') ||
              e.toString().contains('Failed host lookup')
          ? 'Không kết nối được backend.\nKiểm tra IP (${ApiConfig.baseUrl}) và WiFi.'
          : 'Lỗi kết nối: ${e.toString()}';
      return ProductVerificationResult.error(msg);
    }
  }

  /// Report suspicious product
  Future<bool> reportSuspiciousProduct({
    required String itemCode,
    required String reason,
    String? description,
    String? reporterEmail,
  }) async {
    try {
      final uri = Uri.parse('${ApiConfig.baseUrl}/api/public/verify/report');
      final response = await http.post(
        uri,
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'itemCode': itemCode,
          'reason': reason,
          'description': description,
          'reporterEmail': reporterEmail,
        }),
      ).timeout(const Duration(seconds: 30));
      return response.statusCode == 200;
    } catch (e) {
      print('Error reporting product: $e');
      return false;
    }
  }
}

// ============================================================
// MODELS
// ============================================================

/// Kết quả xác thực sản phẩm
class ProductVerificationResult {
  final bool success;
  final bool isAuthentic;
  final String verificationResult; // AUTHENTIC, COUNTERFEIT, EXPIRED, RECALLED, ERROR
  final String message;
  final ProductInfo? productInfo;
  final List<JourneyStep> ownershipHistory;
  final int journeySteps;
  final bool blockchainVerified;
  final String recallStatus;
  final int scanCount;
  final DateTime? lastScanned;
  final PharmacyInstructions? pharmacyInstructions;

  ProductVerificationResult({
    required this.success,
    required this.isAuthentic,
    required this.verificationResult,
    required this.message,
    this.productInfo,
    this.ownershipHistory = const [],
    this.journeySteps = 0,
    this.blockchainVerified = false,
    this.recallStatus = '',
    this.scanCount = 0,
    this.lastScanned,
    this.pharmacyInstructions,
  });

  factory ProductVerificationResult.fromJson(Map<String, dynamic> json) {
    // Parse journey steps
    List<JourneyStep> history = [];

    // 1. Ưu tiên traceabilityHistory (format mới từ backend)
    final traceList = json['traceabilityHistory'] as List<dynamic>? ?? [];
    if (traceList.isNotEmpty) {
      for (var e in traceList) {
        history.add(JourneyStep.fromTraceabilityEvent(e as Map<String, dynamic>));
      }
    } else {
      // 2. Fallback: ownershipHistory + blockchainShipmentHistory
      final ownerList = json['ownershipHistory'] as List<dynamic>? ?? [];
      for (var e in ownerList) {
        history.add(JourneyStep.fromJson(e as Map<String, dynamic>));
      }
      final bcList = json['blockchainShipmentHistory'] as List<dynamic>? ?? [];
      for (var e in bcList) {
        history.add(JourneyStep.fromBlockchainCheckpoint(e as Map<String, dynamic>));
      }

      // 3. Nếu vẫn rỗng, build từ productInfo
      if (history.isEmpty) {
        final pInfo = json['productInfo'] ?? json['batch'];
        if (pInfo != null) {
          history.add(JourneyStep(
            stage: 'Sản xuất',
            company: pInfo['manufacturer']?.toString(),
            address: 'Nhà máy sản xuất',
            timestamp: _parseTimestamp(pInfo['manufactureDate'] ?? pInfo['manufactureTimestamp']),
            verified: true,
            icon: '🏭',
            txHash: null,
          ));
          if (json['blockchainVerified'] == true || json['blockchain']?['verified'] == true) {
            history.add(JourneyStep(
              stage: 'Xác thực Blockchain',
              company: 'Hệ thống Blockchain',
              address: 'Blockchain Network',
              timestamp: DateTime.now(),
              verified: true,
              icon: '🔗',
              txHash: null,
            ));
          }
        }
      }
    }

    history.sort((a, b) => a.timestamp.compareTo(b.timestamp));

    return ProductVerificationResult(
      success: json['success'] ?? json['verified'] ?? false,
      isAuthentic: json['isAuthentic'] ?? json['verified'] ?? false,
      verificationResult: json['verificationResult'] ?? 'UNKNOWN',
      message: json['message'] ?? '',
      productInfo: (json['productInfo'] != null || json['batch'] != null)
          ? ProductInfo.fromJson(json)
          : null,
      ownershipHistory: history,
      journeySteps: history.isNotEmpty ? history.length : (json['journeySteps'] ?? 0),
      blockchainVerified: json['blockchainVerified'] ??
          (json['blockchain']?['verified'] ?? false),
      recallStatus: json['recallStatus']?.toString() ?? '',
      scanCount: json['scanCount'] ?? 0,
      lastScanned: json['lastScanned'] != null
          ? _parseTimestamp(json['lastScanned'])
          : null,
      pharmacyInstructions: json['pharmacyInstructions'] != null
          ? PharmacyInstructions.fromJson(json['pharmacyInstructions'])
          : null,
    );
  }

  factory ProductVerificationResult.error(String message) {
    return ProductVerificationResult(
      success: false,
      isAuthentic: false,
      verificationResult: 'ERROR',
      message: message,
    );
  }

  bool get isError => verificationResult == 'ERROR';
  bool get isCounterfeit => verificationResult == 'COUNTERFEIT';
  bool get isExpired => verificationResult == 'EXPIRED';
  bool get isRecalled => verificationResult == 'RECALLED';

  static DateTime _parseTimestamp(dynamic value) {
    if (value == null) return DateTime.now();
    if (value is int) {
      return value > 1000000000000
          ? DateTime.fromMillisecondsSinceEpoch(value)
          : DateTime.fromMillisecondsSinceEpoch(value * 1000);
    }
    if (value is String) return DateTime.tryParse(value) ?? DateTime.now();
    return DateTime.now();
  }
}

/// Thông tin sản phẩm
class ProductInfo {
  final String itemCode;
  final String name;
  final String? activeIngredient;
  final String? dosage;
  final String? unit;
  final String? category;
  final String? description;       // Mô tả từ nhà sản xuất
  final String? usage;             // Công dụng
  final String? sideEffects;       // Tác dụng phụ
  final String? contraindications; // Chống chỉ định
  final String? packaging;         // Quy cách đóng gói
  final String? registrationNumber;// Số đăng ký lưu hành
  final String manufacturer;
  final String batchNumber;
  final DateTime manufactureDate;
  final DateTime expiryDate;
  final String? storageConditions;
  final String expiryStatus; // VALID, EXPIRING_SOON, EXPIRED
  final int daysUntilExpiry;
  // Trạng thái item: MANUFACTURED, AT_WAREHOUSE, AT_DISTRIBUTOR, AT_PHARMACY, SOLD, RECALLED
  final String currentStatus;
  final DateTime? soldAt;
  final String? imageUrl;

  ProductInfo({
    required this.itemCode,
    required this.name,
    this.activeIngredient,
    this.dosage,
    this.unit,
    this.category,
    this.description,
    this.usage,
    this.sideEffects,
    this.contraindications,
    this.packaging,
    this.registrationNumber,
    required this.manufacturer,
    required this.batchNumber,
    required this.manufactureDate,
    required this.expiryDate,
    this.storageConditions,
    required this.expiryStatus,
    required this.daysUntilExpiry,
    this.currentStatus = 'UNKNOWN',
    this.soldAt,
    this.imageUrl,
  });

  factory ProductInfo.fromJson(Map<String, dynamic> json) {
    // Backend wraps in 'productInfo' or 'batch' key
    final info = (json['productInfo'] ?? json['batch'] ?? json) as Map<String, dynamic>;

    DateTime? soldAt;
    if (info['soldAt'] != null) {
      try {
        soldAt = DateTime.parse(info['soldAt'].toString());
      } catch (_) {}
    }

    DateTime manufactureDate;
    try {
      manufactureDate = DateTime.parse(
          (info['manufactureDate'] ?? info['manufactureTimestamp']).toString());
    } catch (_) {
      manufactureDate = DateTime.now();
    }

    DateTime expiryDate;
    try {
      expiryDate = DateTime.parse(info['expiryDate'].toString());
    } catch (_) {
      expiryDate = DateTime.now().add(const Duration(days: 365));
    }

    return ProductInfo(
      itemCode: info['itemCode']?.toString() ?? '',
      name: info['name']?.toString() ?? info['drugName']?.toString() ?? '',
      activeIngredient: info['activeIngredient']?.toString(),
      dosage: info['dosage']?.toString(),
      unit: info['unit']?.toString(),
      category: info['category']?.toString(),
      description: info['description']?.toString(),
      usage: info['usage']?.toString(),
      sideEffects: info['sideEffects']?.toString(),
      contraindications: info['contraindications']?.toString(),
      packaging: info['packaging']?.toString(),
      registrationNumber: info['registrationNumber']?.toString()
          ?? info['registrationNo']?.toString(),
      manufacturer: info['manufacturer']?.toString() ?? '',
      batchNumber: info['batchNumber']?.toString() ?? info['batchId']?.toString() ?? '',
      manufactureDate: manufactureDate,
      expiryDate: expiryDate,
      storageConditions: info['storageConditions']?.toString(),
      expiryStatus: info['expiryStatus']?.toString() ?? 'UNKNOWN',
      daysUntilExpiry: DateTime(expiryDate.year, expiryDate.month, expiryDate.day)
          .difference(DateTime(DateTime.now().year, DateTime.now().month, DateTime.now().day))
          .inDays,
      // status field từ backend: item.getCurrentStatus().name()
      currentStatus: info['status']?.toString() ?? info['currentStatus']?.toString() ?? 'UNKNOWN',
      soldAt: soldAt,
      imageUrl: _resolveImageUrl(info['imageUrl']?.toString() ?? info['image_url']?.toString()),
    );
  }

  /// Resolve image URL: thay localhost/127.0.0.1 bằng IP backend thực
  /// vì ảnh upload lưu với URL localhost không truy cập được từ thiết bị Android
  static String? _resolveImageUrl(String? url) {
    if (url == null || url.isEmpty) return null;

    final backendBase = ApiConfig.baseUrlSync; // vd: http://192.168.0.103:8080

    // URL bên ngoài (CDN, fptcloud...) — giữ nguyên
    if (url.startsWith('http') &&
        !url.contains('localhost') &&
        !url.contains('127.0.0.1')) {
      return url;
    }

    // URL localhost/127.0.0.1 → thay bằng backend IP thực
    if (url.contains('localhost') || url.contains('127.0.0.1')) {
      final uri = Uri.tryParse(url);
      if (uri != null) {
        return '$backendBase${uri.path}';
      }
    }

    // Đường dẫn tương đối /uploads/...
    if (url.startsWith('/')) {
      return '$backendBase$url';
    }

    return url;
  }

  bool get isValid => expiryStatus == 'VALID' && daysUntilExpiry > 30;
  bool get isExpiringSoon => expiryStatus == 'EXPIRING_SOON' || (daysUntilExpiry > 0 && daysUntilExpiry <= 30);
  bool get isExpired => expiryStatus == 'EXPIRED' || daysUntilExpiry <= 0;

  /// Kiểm tra đã bán chưa
  bool get isSold => currentStatus == 'SOLD';

  /// Text hiển thị trạng thái
  String get saleStatusText {
    switch (currentStatus) {
      case 'SOLD':          return 'Đã bán';
      case 'AT_PHARMACY':   return 'Tại nhà thuốc (Chưa bán)';
      case 'AT_DISTRIBUTOR':return 'Tại nhà phân phối';
      case 'AT_WAREHOUSE':  return 'Tại kho';
      case 'MANUFACTURED':  return 'Mới sản xuất';
      case 'RECALLED':      return 'Đã thu hồi';
      default:              return 'Không xác định ($currentStatus)';
    }
  }

  /// Chỉ sản phẩm ĐÃ BÁN mới được thêm vào tủ thuốc
  bool get canAddToMedicineCabinet => isSold;
}

/// Bước trong hành trình chuỗi cung ứng
class JourneyStep {
  final String stage;
  final String? company;
  final String? address;
  final DateTime timestamp;
  final bool verified;
  final String icon;
  final String? txHash;

  JourneyStep({
    required this.stage,
    this.company,
    this.address,
    required this.timestamp,
    required this.verified,
    required this.icon,
    this.txHash,
  });

  factory JourneyStep.fromJson(Map<String, dynamic> json) {
    return JourneyStep(
      stage: json['stage']?.toString() ?? '',
      company: json['company']?.toString(),
      address: json['address']?.toString(),
      timestamp: _parseTs(json['timestamp']),
      verified: json['verified'] == true,
      icon: json['icon']?.toString() ?? '•',
      txHash: json['txHash']?.toString(),
    );
  }

  factory JourneyStep.fromBlockchainCheckpoint(Map<String, dynamic> json) {
    String status = json['status']?.toString().toUpperCase() ?? '';
    String stage;
    String icon;
    switch (status) {
      case 'CREATED':
      case 'SHIPPED':   stage = 'Vận chuyển';        icon = '🚚'; break;
      case 'IN_TRANSIT':stage = 'Đang vận chuyển';   icon = '🚛'; break;
      case 'DELIVERED': stage = 'Đã giao hàng';       icon = '📦'; break;
      case 'RECEIVED':  stage = 'Đã nhận hàng';       icon = '✅'; break;
      default:          stage = status.isNotEmpty ? status : 'Cập nhật'; icon = '📍';
    }

    DateTime timestamp;
    if (json['timestampMs'] != null) {
      timestamp = DateTime.fromMillisecondsSinceEpoch((json['timestampMs'] as num).toInt());
    } else {
      timestamp = _parseTs(json['displayTime'] ?? json['timestamp']);
    }

    return JourneyStep(
      stage: stage,
      company: json['fromCompany']?.toString() ?? json['toCompany']?.toString() ?? json['shipmentCode']?.toString(),
      address: json['location']?.toString() ?? json['locationName']?.toString() ?? '',
      timestamp: timestamp,
      verified: true,
      icon: icon,
      txHash: json['txHash']?.toString(),
    );
  }

  factory JourneyStep.fromTraceabilityEvent(Map<String, dynamic> json) {
    String eventType = json['eventType']?.toString().toUpperCase() ?? '';
    String icon;
    switch (eventType) {
      case 'MANUFACTURE': icon = '🏭'; break;
      case 'SHIP':        icon = '🚚'; break;
      case 'RECEIVE':     icon = '✅'; break;
      case 'SALE':        icon = '💊'; break;
      case 'RECALL':      icon = '🚫'; break;
      default:            icon = '📍';
    }
    return JourneyStep(
      stage: json['event']?.toString() ?? eventType,
      company: json['actor']?.toString(),
      address: json['location']?.toString() ?? json['toLocation']?.toString(),
      timestamp: _parseTs(json['timestamp']),
      verified: true,
      icon: icon,
      txHash: json['txHash']?.toString(),
    );
  }

  static DateTime _parseTs(dynamic v) {
    if (v == null) return DateTime.now();
    if (v is int) {
      return v > 1000000000000
          ? DateTime.fromMillisecondsSinceEpoch(v)
          : DateTime.fromMillisecondsSinceEpoch(v * 1000);
    }
    if (v is String) return DateTime.tryParse(v) ?? DateTime.now();
    return DateTime.now();
  }
}

/// Hướng dẫn dùng thuốc từ nhà thuốc
class PharmacyInstructions {
  final String? dosage;
  final int? frequency;
  final String? mealRelation;
  final String? mealRelationDisplay;
  final String? specificTimes;
  final int? durationDays;
  final String? specialNotes;
  final String? warnings;
  final String? pharmacistName;
  final String? customerName;
  final String? customerPhone;
  final DateTime? dispensedAt;

  PharmacyInstructions({
    this.dosage,
    this.frequency,
    this.mealRelation,
    this.mealRelationDisplay,
    this.specificTimes,
    this.durationDays,
    this.specialNotes,
    this.warnings,
    this.pharmacistName,
    this.customerName,
    this.customerPhone,
    this.dispensedAt,
  });

  factory PharmacyInstructions.fromJson(Map<String, dynamic> json) {
    return PharmacyInstructions(
      dosage: json['dosage']?.toString(),
      frequency: json['frequency'] is int ? json['frequency'] : null,
      mealRelation: json['mealRelation']?.toString(),
      mealRelationDisplay: json['mealRelationDisplay']?.toString(),
      specificTimes: json['specificTimes']?.toString(),
      durationDays: json['durationDays'] is int ? json['durationDays'] : null,
      specialNotes: json['specialNotes']?.toString(),
      warnings: json['warnings']?.toString(),
      pharmacistName: json['pharmacistName']?.toString(),
      customerName: json['customerName']?.toString(),
      customerPhone: json['customerPhone']?.toString(),
      dispensedAt: json['dispensedAt'] != null
          ? DateTime.tryParse(json['dispensedAt'].toString())
          : null,
    );
  }
}
