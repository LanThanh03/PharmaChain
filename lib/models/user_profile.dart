enum Gender { male, female, other }

enum BloodType { 
  A_POSITIVE, A_NEGATIVE, 
  B_POSITIVE, B_NEGATIVE, 
  AB_POSITIVE, AB_NEGATIVE, 
  O_POSITIVE, O_NEGATIVE 
}

enum EducationLevel { 
  PRIMARY, SECONDARY, HIGH_SCHOOL, 
  UNIVERSITY, POSTGRADUATE 
}

enum SmokingStatus { never, former, current }

enum DrinkingFrequency { never, rarely, weekly, daily }

enum ExerciseFrequency { none, rare, weekly, daily }

enum ExerciseIntensity { light, moderate, vigorous }

enum DietType { omnivore, vegetarian, vegan, keto, paleo, other }

enum SleepQuality { poor, fair, good, excellent }

enum StressLevel { low, moderate, high, severe }

enum MentalHealthStatus { excellent, good, fair, poor }

enum WorkEnvironment { office, outdoor, industrial, medical, other }

enum PhysicalDemands { sedentary, light, moderate, heavy }

class UserProfile {
  final String userId;
  final String? name;
  final int? birthYear;
  final int? birthMonth;
  final Gender? gender;
  final int? heightCm;
  final double? weightKg;
  final BloodType? bloodType;
  final int? provinceId;
  final String? occupation;
  final EducationLevel? educationLevel;
  
  // Medical info as strings (matching backend)
  final String? medicalHistory;
  final String? allergies;
  final String? currentMedications;
  
  // Smoking lifestyle
  final String? smokingStatus;
  final int? smokingStartAge;
  final int? smokingQuitAge;
  final int? cigarettesPerDay;
  final int? yearsSmoked;
  
  // Alcohol lifestyle
  final String? drinkingStatus; // alcohol_frequency in DB
  final double? alcoholUnitsPerWeek;
  final String? alcoholTypePreference;
  
  // Exercise lifestyle
  final String? exerciseFrequency;
  final String? exerciseIntensity;
  final int? exerciseDurationMinutes;
  final String? exerciseTypes;
  
  // Diet lifestyle
  final String? dietType;
  final int? mealsPerDay;
  final double? waterIntakeLiters;
  
  // Sleep lifestyle
  final double? sleepHoursAverage;
  final String? sleepQuality;
  final String? sleepDisorders;
  
  // Mental health
  final String? stressLevel;
  final String? mentalHealthStatus;
  
  // Work environment
  final String? workEnvironment;
  final bool? chemicalExposure;
  final String? physicalDemands;
  
  final DateTime? updatedAt;

  UserProfile({
    required this.userId,
    this.name,
    this.birthYear,
    this.birthMonth,
    this.gender,
    this.heightCm,
    this.weightKg,
    this.bloodType,
    this.provinceId,
    this.occupation,
    this.educationLevel,
    this.medicalHistory,
    this.allergies,
    this.currentMedications,
    this.smokingStatus,
    this.smokingStartAge,
    this.smokingQuitAge,
    this.cigarettesPerDay,
    this.yearsSmoked,
    this.drinkingStatus,
    this.alcoholUnitsPerWeek,
    this.alcoholTypePreference,
    this.exerciseFrequency,
    this.exerciseIntensity,
    this.exerciseDurationMinutes,
    this.exerciseTypes,
    this.dietType,
    this.mealsPerDay,
    this.waterIntakeLiters,
    this.sleepHoursAverage,
    this.sleepQuality,
    this.sleepDisorders,
    this.stressLevel,
    this.mentalHealthStatus,
    this.workEnvironment,
    this.chemicalExposure,
    this.physicalDemands,
    this.updatedAt,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      userId: json['userId'] ?? '',
      name: json['name'],
      birthYear: json['birthYear'],
      birthMonth: json['birthMonth'],
      gender: json['gender'] != null ? _parseGender(json['gender']) : null,
      heightCm: json['heightCm'],
      weightKg: json['weightKg']?.toDouble(),
      bloodType: json['bloodType'] != null ? _parseBloodType(json['bloodType']) : null,
      provinceId: json['provinceId'],
      occupation: json['occupation'],
      educationLevel: json['educationLevel'] != null ? _parseEducationLevel(json['educationLevel']) : null,
      medicalHistory: json['medicalHistory'],
      allergies: json['allergies'],
      currentMedications: json['currentMedications'],
      smokingStatus: json['smokingStatus'],
      smokingStartAge: json['smokingStartAge'],
      smokingQuitAge: json['smokingQuitAge'],
      cigarettesPerDay: json['cigarettesPerDay'],
      yearsSmoked: json['yearsSmoked'],
      drinkingStatus: json['alcoholFrequency'], // Backend uses alcoholFrequency
      alcoholUnitsPerWeek: json['alcoholUnitsPerWeek']?.toDouble(),
      alcoholTypePreference: json['alcoholTypePreference'],
      exerciseFrequency: json['exerciseFrequency'],
      exerciseIntensity: json['exerciseIntensity'],
      exerciseDurationMinutes: json['exerciseDurationMinutes'],
      exerciseTypes: json['exerciseTypes'],
      dietType: json['dietType'],
      mealsPerDay: json['mealsPerDay'],
      waterIntakeLiters: json['waterIntakeLiters']?.toDouble(),
      sleepHoursAverage: json['sleepHoursAverage']?.toDouble(),
      sleepQuality: json['sleepQuality'],
      sleepDisorders: json['sleepDisorders'],
      stressLevel: json['stressLevel'],
      mentalHealthStatus: json['mentalHealthStatus'],
      workEnvironment: json['workEnvironment'],
      chemicalExposure: json['chemicalExposure'],
      physicalDemands: json['physicalDemands'],
      updatedAt: json['updatedAt'] != null 
        ? DateTime.tryParse(json['updatedAt'])
        : null,
    );
  }

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{};
    
    // Only include non-null values
    if (name != null) map['name'] = name;
    if (birthYear != null) map['birthYear'] = birthYear;
    if (birthMonth != null) map['birthMonth'] = birthMonth;
    if (gender != null) map['gender'] = gender!.name.toLowerCase(); // Backend expects lowercase
    if (heightCm != null) map['heightCm'] = heightCm;
    if (weightKg != null) map['weightKg'] = weightKg;
    if (bloodType != null) map['bloodType'] = bloodType!.name;
    if (provinceId != null) map['provinceId'] = provinceId;
    if (occupation != null) map['occupation'] = occupation;
    if (educationLevel != null) map['educationLevel'] = educationLevel!.name.toLowerCase();
    if (medicalHistory != null) map['medicalHistory'] = medicalHistory;
    if (allergies != null) map['allergies'] = allergies;
    if (currentMedications != null) map['currentMedications'] = currentMedications;
    
    // Always include default smoking status to satisfy NOT NULL constraint
    map['smokingStatus'] = smokingStatus ?? 'never';
    if (smokingStartAge != null) map['smokingStartAge'] = smokingStartAge;
    if (smokingQuitAge != null) map['smokingQuitAge'] = smokingQuitAge;
    if (cigarettesPerDay != null) map['cigarettesPerDay'] = cigarettesPerDay;
    if (yearsSmoked != null) map['yearsSmoked'] = yearsSmoked;
    
    // Alcohol - backend uses alcoholFrequency
    if (drinkingStatus != null) map['alcoholFrequency'] = drinkingStatus;
    if (alcoholUnitsPerWeek != null) map['alcoholUnitsPerWeek'] = alcoholUnitsPerWeek;
    if (alcoholTypePreference != null) map['alcoholTypePreference'] = alcoholTypePreference;
    
    // Exercise
    if (exerciseFrequency != null) map['exerciseFrequency'] = exerciseFrequency;
    if (exerciseIntensity != null) map['exerciseIntensity'] = exerciseIntensity;
    if (exerciseDurationMinutes != null) map['exerciseDurationMinutes'] = exerciseDurationMinutes;
    if (exerciseTypes != null) map['exerciseTypes'] = exerciseTypes;
    
    // Diet
    if (dietType != null) map['dietType'] = dietType;
    if (mealsPerDay != null) map['mealsPerDay'] = mealsPerDay;
    if (waterIntakeLiters != null) map['waterIntakeLiters'] = waterIntakeLiters;
    
    // Sleep
    if (sleepHoursAverage != null) map['sleepHoursAverage'] = sleepHoursAverage;
    if (sleepQuality != null) map['sleepQuality'] = sleepQuality;
    if (sleepDisorders != null) map['sleepDisorders'] = sleepDisorders;
    
    // Mental health
    if (stressLevel != null) map['stressLevel'] = stressLevel;
    if (mentalHealthStatus != null) map['mentalHealthStatus'] = mentalHealthStatus;
    
    // Work environment
    if (workEnvironment != null) map['workEnvironment'] = workEnvironment;
    if (chemicalExposure != null) map['chemicalExposure'] = chemicalExposure;
    if (physicalDemands != null) map['physicalDemands'] = physicalDemands;
    
    return map;
  }

  static Gender _parseGender(String gender) {
    switch (gender.toUpperCase()) {
      case 'MALE':
        return Gender.male;
      case 'FEMALE':
        return Gender.female;
      case 'OTHER':
        return Gender.other;
      default:
        return Gender.other;
    }
  }

  static BloodType _parseBloodType(String bloodType) {
    switch (bloodType) {
      case 'A_POSITIVE':
        return BloodType.A_POSITIVE;
      case 'A_NEGATIVE':
        return BloodType.A_NEGATIVE;
      case 'B_POSITIVE':
        return BloodType.B_POSITIVE;
      case 'B_NEGATIVE':
        return BloodType.B_NEGATIVE;
      case 'AB_POSITIVE':
        return BloodType.AB_POSITIVE;
      case 'AB_NEGATIVE':
        return BloodType.AB_NEGATIVE;
      case 'O_POSITIVE':
        return BloodType.O_POSITIVE;
      case 'O_NEGATIVE':
        return BloodType.O_NEGATIVE;
      default:
        return BloodType.O_POSITIVE;
    }
  }

  static EducationLevel _parseEducationLevel(String level) {
    switch (level) {
      case 'PRIMARY':
        return EducationLevel.PRIMARY;
      case 'SECONDARY':
        return EducationLevel.SECONDARY;
      case 'HIGH_SCHOOL':
        return EducationLevel.HIGH_SCHOOL;
      case 'UNIVERSITY':
        return EducationLevel.UNIVERSITY;
      case 'POSTGRADUATE':
        return EducationLevel.POSTGRADUATE;
      default:
        return EducationLevel.HIGH_SCHOOL;
    }
  }

  UserProfile copyWith({
    String? userId,
    String? name,
    int? birthYear,
    int? birthMonth,
    Gender? gender,
    int? heightCm,
    double? weightKg,
    BloodType? bloodType,
    int? provinceId,
    String? occupation,
    EducationLevel? educationLevel,
    String? medicalHistory,
    String? allergies,
    String? currentMedications,
    String? smokingStatus,
    int? smokingStartAge,
    int? smokingQuitAge,
    int? cigarettesPerDay,
    int? yearsSmoked,
    String? drinkingStatus,
    double? alcoholUnitsPerWeek,
    String? alcoholTypePreference,
    String? exerciseFrequency,
    String? exerciseIntensity,
    int? exerciseDurationMinutes,
    String? exerciseTypes,
    String? dietType,
    int? mealsPerDay,
    double? waterIntakeLiters,
    double? sleepHoursAverage,
    String? sleepQuality,
    String? sleepDisorders,
    String? stressLevel,
    String? mentalHealthStatus,
    String? workEnvironment,
    bool? chemicalExposure,
    String? physicalDemands,
    DateTime? updatedAt,
  }) {
    return UserProfile(
      userId: userId ?? this.userId,
      name: name ?? this.name,
      birthYear: birthYear ?? this.birthYear,
      birthMonth: birthMonth ?? this.birthMonth,
      gender: gender ?? this.gender,
      heightCm: heightCm ?? this.heightCm,
      weightKg: weightKg ?? this.weightKg,
      bloodType: bloodType ?? this.bloodType,
      provinceId: provinceId ?? this.provinceId,
      occupation: occupation ?? this.occupation,
      educationLevel: educationLevel ?? this.educationLevel,
      medicalHistory: medicalHistory ?? this.medicalHistory,
      allergies: allergies ?? this.allergies,
      currentMedications: currentMedications ?? this.currentMedications,
      smokingStatus: smokingStatus ?? this.smokingStatus,
      smokingStartAge: smokingStartAge ?? this.smokingStartAge,
      smokingQuitAge: smokingQuitAge ?? this.smokingQuitAge,
      cigarettesPerDay: cigarettesPerDay ?? this.cigarettesPerDay,
      yearsSmoked: yearsSmoked ?? this.yearsSmoked,
      drinkingStatus: drinkingStatus ?? this.drinkingStatus,
      alcoholUnitsPerWeek: alcoholUnitsPerWeek ?? this.alcoholUnitsPerWeek,
      alcoholTypePreference: alcoholTypePreference ?? this.alcoholTypePreference,
      exerciseFrequency: exerciseFrequency ?? this.exerciseFrequency,
      exerciseIntensity: exerciseIntensity ?? this.exerciseIntensity,
      exerciseDurationMinutes: exerciseDurationMinutes ?? this.exerciseDurationMinutes,
      exerciseTypes: exerciseTypes ?? this.exerciseTypes,
      dietType: dietType ?? this.dietType,
      mealsPerDay: mealsPerDay ?? this.mealsPerDay,
      waterIntakeLiters: waterIntakeLiters ?? this.waterIntakeLiters,
      sleepHoursAverage: sleepHoursAverage ?? this.sleepHoursAverage,
      sleepQuality: sleepQuality ?? this.sleepQuality,
      sleepDisorders: sleepDisorders ?? this.sleepDisorders,
      stressLevel: stressLevel ?? this.stressLevel,
      mentalHealthStatus: mentalHealthStatus ?? this.mentalHealthStatus,
      workEnvironment: workEnvironment ?? this.workEnvironment,
      chemicalExposure: chemicalExposure ?? this.chemicalExposure,
      physicalDemands: physicalDemands ?? this.physicalDemands,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  bool get isComplete {
    return birthYear != null && 
           gender != null && 
           provinceId != null;
  }

  // Getter for backward compatibility
  int? get age {
    if (birthYear == null) return null;
    return DateTime.now().year - birthYear!;
  }

  String get genderDisplay {
    if (gender == null) return 'Chưa xác định';
    switch (gender!) {
      case Gender.male:
        return 'Nam';
      case Gender.female:
        return 'Nữ';
      case Gender.other:
        return 'Khác';
    }
  }

  String get province {
    // For display purposes, we'll need to map provinceId back to province name
    // This is a temporary solution - ideally should be handled by a service
    if (provinceId == null) return 'Chưa xác định';
    
    final provinceMap = {
      1: 'An Giang', 2: 'Bà Rịa - Vũng Tàu', 3: 'Bạc Liêu', 4: 'Bắc Giang', 5: 'Bắc Kạn',
      6: 'Bắc Ninh', 7: 'Bến Tre', 8: 'Bình Dương', 9: 'Bình Định', 10: 'Bình Phước',
      11: 'Bình Thuận', 12: 'Cà Mau', 13: 'Cao Bằng', 14: 'Cần Thơ', 15: 'Đà Nẵng',
      16: 'Đắk Lắk', 17: 'Đắk Nông', 18: 'Điện Biên', 19: 'Đồng Nai', 20: 'Đồng Tháp',
      21: 'Gia Lai', 22: 'Hà Giang', 23: 'Hà Nam', 24: 'Hà Nội', 25: 'Hà Tĩnh',
      26: 'Hải Dương', 27: 'Hải Phòng', 28: 'Hậu Giang', 29: 'Hòa Bình', 30: 'Hồ Chí Minh',
      31: 'Hưng Yên', 32: 'Khánh Hòa', 33: 'Kiên Giang', 34: 'Kon Tum', 35: 'Lai Châu',
      36: 'Lạng Sơn', 37: 'Lào Cai', 38: 'Lâm Đồng', 39: 'Long An', 40: 'Nam Định',
      41: 'Nghệ An', 42: 'Ninh Bình', 43: 'Ninh Thuận', 44: 'Phú Thọ', 45: 'Phú Yên',
      46: 'Quảng Bình', 47: 'Quảng Nam', 48: 'Quảng Ngãi', 49: 'Quảng Ninh', 50: 'Quảng Trị',
      51: 'Sóc Trăng', 52: 'Sơn La', 53: 'Tây Ninh', 54: 'Thái Bình', 55: 'Thái Nguyên',
      56: 'Thanh Hóa', 57: 'Thừa Thiên Huế', 58: 'Tiền Giang', 59: 'Trà Vinh', 60: 'Tuyên Quang',
      61: 'Vĩnh Long', 62: 'Vĩnh Phúc', 63: 'Yên Bái'
    };
    
    return provinceMap[provinceId] ?? 'Chưa xác định';
  }

  String get smokingDisplay {
    return smokingStatus ?? 'Chưa xác định';
  }

  String get drinkingDisplay {
    return drinkingStatus ?? 'Chưa xác định';
  }
}