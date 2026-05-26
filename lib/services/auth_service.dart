import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import '../models/user_profile.dart';
import 'api_service.dart';

class AuthService {
  static const String _userKey = 'current_user';
  static const String _profileKey = 'user_profile';
  
  static AuthService? _instance;
  static AuthService get instance => _instance ??= AuthService._();
  AuthService._();

  final ApiService _apiService = ApiService.instance;
  
  User? _currentUser;
  UserProfile? _currentProfile;

  User? get currentUser => _currentUser;
  UserProfile? get currentProfile => _currentProfile;
  bool get isLoggedIn => _currentUser != null;

  // Initialize auth service
  Future<void> initialize() async {
    await _loadUserFromStorage();
    await _loadProfileFromStorage();
  }

  // Login with email and password
  Future<AuthResult> login(String email, String password) async {
    try {
      final response = await _apiService.post(
        '/auth/login',
        {
          'email': email,
          'password': password,
        },
        requireAuth: false,
        fromJson: (data) => AuthResponseData.fromJson(data),
      );
      
      if (response.isSuccess && response.data != null) {
        final authData = response.data!;
        
        // Save token
        await _apiService.saveAuthToken(authData.accessToken);
        
        // Save user data
        _currentUser = User(
          id: authData.user.id,
          email: authData.user.email,
          name: authData.user.name,
          isProfileComplete: authData.user.isProfileComplete,
          isActive: authData.user.isActive,
        );
        await _saveUserToStorage(_currentUser!);
        
        // Load user profile if exists
        if (authData.user.isProfileComplete) {
          await _loadUserProfile(_currentUser!.id);
        }
        
        return AuthResult.success();
      } else {
        return AuthResult.error(response.error ?? 'Đăng nhập thất bại');
      }
    } catch (e) {
      print('Login error: $e');
      return AuthResult.error('Đã có lỗi xảy ra khi đăng nhập');
    }
  }

  // Register new user
  Future<AuthResult> register(String email, String password, String confirmPassword, String? name) async {
    try {
      print('Attempting to register user: $email');
      
      final response = await _apiService.post(
        '/auth/register',
        {
          'email': email,
          'password': password,
          'confirmPassword': confirmPassword,
          'name': name ?? '',
        },
        requireAuth: false,
        fromJson: (data) => AuthResponseData.fromJson(data),
      );
      
      if (response.isSuccess && response.data != null) {
        final authData = response.data!;
        
        // Save token
        await _apiService.saveAuthToken(authData.accessToken);
        
        // Save user data
        _currentUser = User(
          id: authData.user.id,
          email: authData.user.email,
          name: authData.user.name,
          isProfileComplete: authData.user.isProfileComplete,
          isActive: authData.user.isActive,
        );
        await _saveUserToStorage(_currentUser!);
        
        print('Register successful for user: ${_currentUser!.email}');
        return AuthResult.success();
      } else {
        print('Register failed: ${response.error}');
        return AuthResult.error(response.error ?? 'Đăng ký thất bại');
      }
    } catch (e) {
      print('Register error: $e');
      return AuthResult.error('Đã có lỗi xảy ra khi đăng ký');
    }
  }

  // Save user profile
  Future<AuthResult> saveProfile(UserProfile profile) async {
    try {
      final response = await _apiService.put(
        '/users/profile',
        profile.toJson(),
      );
      
      if (response.isSuccess) {
        _currentProfile = profile;
        await _saveProfileToStorage(profile);
        
        // Update user's profile complete status
        if (_currentUser != null && profile.isComplete) {
          _currentUser = User(
            id: _currentUser!.id,
            email: _currentUser!.email,
            name: _currentUser!.name,
            isProfileComplete: true,
            isActive: _currentUser!.isActive,
          );
          await _saveUserToStorage(_currentUser!);
        }
        
        return AuthResult.success();
      } else {
        return AuthResult.error(response.error ?? 'Lưu profile thất bại');
      }
    } catch (e) {
      print('Save profile error: $e');
      return AuthResult.error('Đã có lỗi xảy ra khi lưu profile');
    }
  }

  // Logout
  Future<void> logout() async {
    // Call logout API
    try {
      await _apiService.post('/auth/logout', {});
    } catch (e) {
      print('Logout API error: $e');
      // Continue with local logout even if API fails
    }
    
    // Clear local data
    _currentUser = null;
    _currentProfile = null;
    
    // Clear storage
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_userKey);
    await prefs.remove(_profileKey);
    await _apiService.clearAuthToken();
  }

  // Private methods
  Future<void> _saveUserToStorage(User user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, json.encode(user.toJson()));
  }

  Future<void> _loadUserFromStorage() async {
    final prefs = await SharedPreferences.getInstance();
    final userJson = prefs.getString(_userKey);
    if (userJson != null) {
      _currentUser = User.fromJson(json.decode(userJson));
    }
  }

  Future<void> _saveProfileToStorage(UserProfile profile) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_profileKey, json.encode(profile.toJson()));
  }

  Future<void> _loadProfileFromStorage() async {
    final prefs = await SharedPreferences.getInstance();
    final profileJson = prefs.getString(_profileKey);
    if (profileJson != null) {
      _currentProfile = UserProfile.fromJson(json.decode(profileJson));
    }
  }

  Future<void> _loadUserProfile(String userId) async {
    try {
      final response = await _apiService.get('/auth/me');
      
      if (response.isSuccess && response.data != null) {
        final userData = response.data as Map<String, dynamic>;
        final profileData = userData['profile'];
        if (profileData != null) {
          _currentProfile = UserProfile.fromJson(profileData);
          await _saveProfileToStorage(_currentProfile!);
        }
      }
    } catch (e) {
      print('Load profile error: $e');
    }
  }

  // Get current user info from backend
  Future<User?> getCurrentUserInfo() async {
    try {
      final response = await _apiService.get('/auth/me');
      
      if (response.isSuccess && response.data != null) {
        final userData = response.data as Map<String, dynamic>;
        final user = User(
          id: userData['id'],
          email: userData['email'],
          name: userData['name'],
          isProfileComplete: userData['isProfileComplete'] ?? false,
          isActive: userData['isActive'] ?? true,
        );
        
        _currentUser = user;
        await _saveUserToStorage(user);
        
        return user;
      }
    } catch (e) {
      print('Get current user error: $e');
    }
    return null;
  }
}

// Auth result wrapper
class AuthResult {
  final bool success;
  final String? error;

  AuthResult._({required this.success, this.error});

  factory AuthResult.success() => AuthResult._(success: true);
  factory AuthResult.error(String error) => AuthResult._(success: false, error: error);

  bool get isSuccess => success;
  bool get isError => !success;
}

// Auth response data models to match backend DTOs
class AuthResponseData {
  final String accessToken;
  final String tokenType;
  final int expiresIn;
  final UserInfoData user;

  AuthResponseData({
    required this.accessToken,
    required this.tokenType,
    required this.expiresIn,
    required this.user,
  });

  factory AuthResponseData.fromJson(Map<String, dynamic> json) {
    return AuthResponseData(
      accessToken: json['accessToken'] ?? '',
      tokenType: json['tokenType'] ?? 'Bearer',
      expiresIn: json['expiresIn'] ?? 86400000,
      user: UserInfoData.fromJson(json['user']),
    );
  }
}

class UserInfoData {
  final String id;
  final String email;
  final String name;
  final bool isProfileComplete;
  final bool isActive;

  UserInfoData({
    required this.id,
    required this.email,
    required this.name,
    required this.isProfileComplete,
    required this.isActive,
  });

  factory UserInfoData.fromJson(Map<String, dynamic> json) {
    return UserInfoData(
      id: json['id'] ?? '',
      email: json['email'] ?? '',
      name: json['name'] ?? '',
      isProfileComplete: json['isProfileComplete'] ?? false,
      isActive: json['isActive'] ?? true,
    );
  }
}