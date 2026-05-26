class User {
  final String id;
  final String email;
  final String name;
  final DateTime? createdAt;
  final DateTime? lastLoginAt;
  final bool isProfileComplete;
  final bool isActive;

  User({
    required this.id,
    required this.email,
    required this.name,
    this.createdAt,
    this.lastLoginAt,
    this.isProfileComplete = false,
    this.isActive = true,
  });

  /// Provides a consistent numeric ID for backend tables that require a Long ID
  int get numericId {
    if (id.isEmpty) return 1;
    return id.hashCode.abs();
  }

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? '',
      email: json['email'] ?? '',
      name: json['name'] ?? '',
      createdAt: json['createdAt'] != null 
        ? DateTime.tryParse(json['createdAt'])
        : null,
      lastLoginAt: json['lastLoginAt'] != null 
        ? DateTime.tryParse(json['lastLoginAt'])
        : null,
      isProfileComplete: json['isProfileComplete'] ?? false,
      isActive: json['isActive'] ?? true,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      'createdAt': createdAt?.toIso8601String(),
      'lastLoginAt': lastLoginAt?.toIso8601String(),
      'isProfileComplete': isProfileComplete,
      'isActive': isActive,
    };
  }

  User copyWith({
    String? id,
    String? email,
    String? name,
    DateTime? createdAt,
    DateTime? lastLoginAt,
    bool? isProfileComplete,
    bool? isActive,
  }) {
    return User(
      id: id ?? this.id,
      email: email ?? this.email,
      name: name ?? this.name,
      createdAt: createdAt ?? this.createdAt,
      lastLoginAt: lastLoginAt ?? this.lastLoginAt,
      isProfileComplete: isProfileComplete ?? this.isProfileComplete,
      isActive: isActive ?? this.isActive,
    );
  }
}



