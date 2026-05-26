import 'package:doantotnghiepbc/config/api_config.dart';

/// Model for user medication record
class MedicationRecord {
  final int id;
  final int userId;
  final String? userPhone;
  final int? productItemId;       // Nullable: manual entry may not have productItemId
  final int? dispenseInstructionId;
  final String drugName;
  final String? batchNumber;
  final String? expiryDate;
  final String dosage;
  final int frequency;
  final String mealRelation;
  final String reminderTimes;
  final String startDate;
  final String endDate;
  final bool isActive;
  final bool isCompleted;
  final String? pharmacyName;
  final String? createdAt;
  final String? updatedAt;

  // Additional detail fields mapping from backend ProductItem
  final String? imageUrl;
  final String? description;
  final String? activeIngredient;
  final String? category;
  final String? drugDosage;
  final String? unit;
  final String? storageConditions;
  final String? shelfLife;
  final int? totalDoses;
  final int? takenDoses;
  final int? missedDoses;

  MedicationRecord({
    required this.id,
    required this.userId,
    this.userPhone,
    this.productItemId,             // Now nullable
    this.dispenseInstructionId,
    required this.drugName,
    this.batchNumber,
    this.expiryDate,
    required this.dosage,
    required this.frequency,
    required this.mealRelation,
    required this.reminderTimes,
    required this.startDate,
    required this.endDate,
    required this.isActive,
    required this.isCompleted,
    this.pharmacyName,
    this.createdAt,
    this.updatedAt,
    this.imageUrl,
    this.description,
    this.activeIngredient,
    this.category,
    this.drugDosage,
    this.unit,
    this.storageConditions,
    this.shelfLife,
    this.totalDoses,
    this.takenDoses,
    this.missedDoses,
  });

  factory MedicationRecord.fromJson(Map<String, dynamic> json) {
    // Helper: safely parse bool - backend may return 0/1 or true/false
    bool safeBool(dynamic v, {bool defaultVal = false}) {
      if (v == null) return defaultVal;
      if (v is bool) return v;
      if (v is int) return v != 0;
      if (v is String) return v == 'true' || v == '1';
      return defaultVal;
    }
    // Helper: safely parse int
    int safeInt(dynamic v, {int defaultVal = 0}) {
      if (v == null) return defaultVal;
      if (v is int) return v;
      if (v is double) return v.toInt();
      if (v is String) return int.tryParse(v) ?? defaultVal;
      return defaultVal;
    }
    return MedicationRecord(
      id: safeInt(json['id']),
      userId: safeInt(json['userId']),
      userPhone: json['userPhone']?.toString(),
      productItemId: json['productItemId'] != null ? safeInt(json['productItemId']) : null,
      dispenseInstructionId: json['dispenseInstructionId'] != null ? safeInt(json['dispenseInstructionId']) : null,
      drugName: json['drugName']?.toString() ?? '',
      batchNumber: json['batchNumber']?.toString(),
      expiryDate: json['expiryDate']?.toString(),
      dosage: json['dosage']?.toString() ?? '',
      frequency: safeInt(json['frequency'], defaultVal: 1),
      mealRelation: json['mealRelation']?.toString() ?? 'AFTER',
      reminderTimes: (json['reminderTimes'] is List) 
          ? (json['reminderTimes'] as List).join(',') 
          : json['reminderTimes']?.toString() ?? '',
      startDate: json['startDate']?.toString() ?? '',
      endDate: json['endDate']?.toString() ?? '',
      isActive: safeBool(json['isActive'] ?? json['active'], defaultVal: true),
      isCompleted: safeBool(json['isCompleted'] ?? json['completed']),
      pharmacyName: json['pharmacyName']?.toString(),
      createdAt: json['createdAt']?.toString(),
      updatedAt: json['updatedAt']?.toString(),
      imageUrl: _resolveImageUrl(json['imageUrl']?.toString()),
      description: json['description']?.toString(),
      activeIngredient: json['activeIngredient']?.toString(),
      category: json['category']?.toString(),
      drugDosage: json['drugDosage']?.toString(),
      unit: json['unit']?.toString(),
      storageConditions: json['storageConditions']?.toString(),
      shelfLife: json['shelfLife']?.toString(),
      totalDoses: safeInt(json['totalDoses']),
      takenDoses: safeInt(json['takenDoses']),
      missedDoses: safeInt(json['missedDoses']),
    );
  }

  /// Resolve image URL: thay localhost bằng IP backend thực
  static String? _resolveImageUrl(String? url) {
    if (url == null || url.isEmpty) return null;
    final backendBase = ApiConfig.baseUrlSync;
    if (url.startsWith('http') && !url.contains('localhost') && !url.contains('127.0.0.1')) {
      return url;
    }
    if (url.contains('localhost') || url.contains('127.0.0.1')) {
      final uri = Uri.tryParse(url);
      if (uri != null) return '$backendBase${uri.path}';
    }
    if (url.startsWith('/')) return '$backendBase$url';
    return url;
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'userPhone': userPhone,
      'productItemId': productItemId,
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
      'isActive': isActive,
      'isCompleted': isCompleted,
      'pharmacyName': pharmacyName,
      'createdAt': createdAt,
      'updatedAt': updatedAt,
      'imageUrl': imageUrl,
      'description': description,
      'activeIngredient': activeIngredient,
      'category': category,
      'drugDosage': drugDosage,
      'unit': unit,
      'storageConditions': storageConditions,
      'shelfLife': shelfLife,
    };
  }

  /// Get list of reminder times
  List<String> getReminderTimesList() {
    return reminderTimes.split(',').map((t) => t.trim()).toList();
  }

  /// Get meal relation display text
  String getMealRelationText() {
    switch (mealRelation) {
      case 'BEFORE':
        return 'Trước bữa ăn';
      case 'AFTER':
        return 'Sau bữa ăn';
      case 'WITH':
        return 'Trong bữa ăn';
      case 'ANY':
        return 'Bất kỳ lúc nào';
      default:
        return mealRelation;
    }
  }

  /// Check if medication is expired
  bool isExpired() {
    if (expiryDate == null) return false;
    try {
      final expiry = DateTime.parse(expiryDate!);
      return expiry.isBefore(DateTime.now());
    } catch (e) {
      return false;
    }
  }

  /// Get days remaining
  int getDaysRemaining() {
    try {
      final end = DateTime.parse(endDate);
      final now = DateTime.now();
      return end.difference(now).inDays;
    } catch (e) {
      return 0;
    }
  }
}

/// Model for medication reminder
class MedicationReminder {
  final int id;
  final int recordId;
  final String scheduledDate;
  final String scheduledTime;
  final String status; // PENDING, TAKEN, MISSED, SKIPPED
  final String? takenAt;
  final String? notes;
  final String createdAt;

  // Related medication info (from join)
  final String? drugName;
  final String? dosage;
  final String? mealRelation;

  MedicationReminder({
    required this.id,
    required this.recordId,
    required this.scheduledDate,
    required this.scheduledTime,
    required this.status,
    this.takenAt,
    this.notes,
    required this.createdAt,
    this.drugName,
    this.dosage,
    this.mealRelation,
  });

  factory MedicationReminder.fromJson(Map<String, dynamic> json) {
    int safeInt(dynamic v, {int defaultVal = 0}) {
      if (v == null) return defaultVal;
      if (v is int) return v;
      if (v is double) return v.toInt();
      if (v is String) return int.tryParse(v) ?? defaultVal;
      return defaultVal;
    }
    return MedicationReminder(
      id: safeInt(json['id']),
      recordId: safeInt(json['recordId'] ?? json['medicationRecordId']),
      scheduledDate: json['scheduledDate']?.toString() ?? '',
      scheduledTime: json['scheduledTime']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      takenAt: json['takenAt']?.toString(),
      notes: json['notes']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
      drugName: json['drugName']?.toString(),
      dosage: json['dosage']?.toString(),
      mealRelation: json['mealRelation']?.toString(),
    );
  }

  /// Get full scheduled datetime
  DateTime getScheduledDateTime() {
    try {
      return DateTime.parse('$scheduledDate $scheduledTime');
    } catch (e) {
      return DateTime.now();
    }
  }

  /// Check if reminder is overdue
  bool isOverdue() {
    if (status != 'PENDING') return false;
    return getScheduledDateTime().isBefore(DateTime.now());
  }

  /// Get status display text
  String getStatusText() {
    switch (status) {
      case 'PENDING':
        return 'Chưa uống';
      case 'TAKEN':
        return 'Đã uống';
      case 'MISSED':
        return 'Bỏ lỡ';
      case 'SKIPPED':
        return 'Đã bỏ qua';
      default:
        return status;
    }
  }

  /// Get status color
  String getStatusColor() {
    switch (status) {
      case 'PENDING':
        return isOverdue() ? '#EF4444' : '#F59E0B';
      case 'TAKEN':
        return '#10B981';
      case 'MISSED':
        return '#EF4444';
      case 'SKIPPED':
        return '#6B7280';
      default:
        return '#9CA3AF';
    }
  }
}

/// Model for adherence statistics
class AdherenceStats {
  final int totalReminders;
  final int takenCount;
  final int missedCount;
  final int skippedCount;
  final double adherenceRate;
  final int currentStreak;
  final int longestStreak;

  AdherenceStats({
    required this.totalReminders,
    required this.takenCount,
    required this.missedCount,
    required this.skippedCount,
    required this.adherenceRate,
    required this.currentStreak,
    required this.longestStreak,
  });

  factory AdherenceStats.fromJson(Map<String, dynamic> json) {
    // Backend trả về: totalReminders, takenCount, pendingCount, missedCount, adherencePercentage
    // Hoặc: totalReminders, takenCount, missedCount, skippedCount, adherenceRate (legacy)
    final adherence = (json['adherencePercentage'] ?? json['adherenceRate'] ?? 0.0).toDouble();
    final taken = json['takenCount'] ?? 0;
    final total = json['totalReminders'] ?? 0;
    final missed = json['missedCount'] ?? 0;
    final skipped = json['skippedCount'] ?? 0;
    return AdherenceStats(
      totalReminders: total,
      takenCount: taken,
      missedCount: missed,
      skippedCount: skipped,
      adherenceRate: adherence,
      currentStreak: json['currentStreak'] ?? 0,
      longestStreak: json['longestStreak'] ?? 0,
    );
  }

  /// Get adherence level text
  String getAdherenceLevelText() {
    if (adherenceRate >= 90) return 'Xuất sắc';
    if (adherenceRate >= 75) return 'Tốt';
    if (adherenceRate >= 60) return 'Trung bình';
    return 'Cần cải thiện';
  }

  /// Get adherence color
  String getAdherenceColor() {
    if (adherenceRate >= 90) return '#10B981';
    if (adherenceRate >= 75) return '#3B82F6';
    if (adherenceRate >= 60) return '#F59E0B';
    return '#EF4444';
  }
}
