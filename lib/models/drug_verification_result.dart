class DrugVerificationResult {
  final bool isAuthentic;
  final String drugName;
  final String manufacturer;
  final String batchNumber;
  final String expiryDate;
  final String currentLocation;
  final List<String> supplyChainHistory;
  final String errorMessage;
  final String blockchainHash;
  final DateTime verifiedAt;

  DrugVerificationResult({
    required this.isAuthentic,
    this.drugName = '',
    this.manufacturer = '',
    this.batchNumber = '',
    this.expiryDate = '',
    this.currentLocation = '',
    this.supplyChainHistory = const [],
    this.errorMessage = '',
    this.blockchainHash = '',
    DateTime? verifiedAt,
  }) : verifiedAt = verifiedAt ?? DateTime.now();

  // Factory constructors for different result types
  factory DrugVerificationResult.authentic({
    required String drugName,
    required String manufacturer,
    required String batchNumber,
    required String expiryDate,
    required String currentLocation,
    required List<String> supplyChainHistory,
    required String blockchainHash,
  }) {
    return DrugVerificationResult(
      isAuthentic: true,
      drugName: drugName,
      manufacturer: manufacturer,
      batchNumber: batchNumber,
      expiryDate: expiryDate,
      currentLocation: currentLocation,
      supplyChainHistory: supplyChainHistory,
      blockchainHash: blockchainHash,
    );
  }

  factory DrugVerificationResult.counterfeit({
    required String errorMessage,
  }) {
    return DrugVerificationResult(
      isAuthentic: false,
      errorMessage: errorMessage,
    );
  }

  factory DrugVerificationResult.error(String errorMessage) {
    return DrugVerificationResult(
      isAuthentic: false,
      errorMessage: errorMessage,
    );
  }

  // JSON serialization
  factory DrugVerificationResult.fromJson(Map<String, dynamic> json) {
    return DrugVerificationResult(
      isAuthentic: json['isAuthentic'] ?? false,
      drugName: json['drugName'] ?? '',
      manufacturer: json['manufacturer'] ?? '',
      batchNumber: json['batchNumber'] ?? '',
      expiryDate: json['expiryDate'] ?? '',
      currentLocation: json['currentLocation'] ?? '',
      supplyChainHistory: (json['supplyChainHistory'] as List<dynamic>?)
          ?.map((e) => e.toString())
          .toList() ?? [],
      errorMessage: json['errorMessage'] ?? '',
      blockchainHash: json['blockchainHash'] ?? '',
      verifiedAt: json['verifiedAt'] != null 
          ? DateTime.parse(json['verifiedAt'])
          : DateTime.now(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'isAuthentic': isAuthentic,
      'drugName': drugName,
      'manufacturer': manufacturer,
      'batchNumber': batchNumber,
      'expiryDate': expiryDate,
      'currentLocation': currentLocation,
      'supplyChainHistory': supplyChainHistory,
      'errorMessage': errorMessage,
      'blockchainHash': blockchainHash,
      'verifiedAt': verifiedAt.toIso8601String(),
    };
  }
}
