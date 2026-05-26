import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:provider/provider.dart';
import 'package:timezone/timezone.dart' as tz;
import '../models/medication_models.dart';
import '../services/medication_service.dart';
import '../services/notification_service.dart';
import '../services/auth_service.dart';

class TestNotificationScreen extends StatefulWidget {
  const TestNotificationScreen({Key? key}) : super(key: key);

  @override
  _TestNotificationScreenState createState() => _TestNotificationScreenState();
}

class _TestNotificationScreenState extends State<TestNotificationScreen> {
  final MedicationService _medicationService = MedicationService();
  final NotificationService _notificationService = NotificationService();
  
  List<MedicationRecord> _activeMedications = [];
  MedicationRecord? _selectedMedication;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    // Sử dụng AuthService để lấy userId
    final user = AuthService.instance.currentUser;
    if (user != null) {
      final userId = user.numericId;
      final response = await _medicationService.getActiveMedications(userId);
      if (response['success'] == true) {
        if (mounted) {
          setState(() {
            _activeMedications = (response['data'] as List)
                .map((item) => MedicationRecord.fromJson(item))
                .toList();
            if (_activeMedications.isNotEmpty) {
              _selectedMedication = _activeMedications.first;
            }
          });
        }
      }
    }
    
    if (mounted) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _sendTestNotification(int delaySeconds) async {
    if (_selectedMedication == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn thuốc trước')),
      );
      return;
    }

    final now = DateTime.now();
    final notificationTime = now.add(Duration(seconds: delaySeconds));

    final payload = jsonEncode({
      'recordId': _selectedMedication!.id,
      'reminderId': 999999, // Fake ID
      'scheduledDate': "${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}",
      'scheduledTime': "${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}",
      'drugName': _selectedMedication!.drugName,
    });

    final details = const NotificationDetails(
        android: AndroidNotificationDetails(
          'medication_reminders',
          'Nhắc nhở uống thuốc',
          channelDescription: 'Thông báo đến giờ uống thuốc',
          importance: Importance.max,
          priority: Priority.high,
          actions: <AndroidNotificationAction>[
            AndroidNotificationAction(
              NotificationService.actionTaken,
              'Đã uống',
              showsUserInterface: false,
              titleColor: Color(0xFF10B981), // Fixed format
            ),
            AndroidNotificationAction(
              NotificationService.actionSnooze,
              'Nhắc lại sau 10 phút',
              showsUserInterface: false,
            ),
            AndroidNotificationAction(
              NotificationService.actionSkip,
              'Bỏ qua',
              showsUserInterface: false,
              titleColor: Color(0xFFEF4444), // Fixed format
            ),
          ],
        ),
      );

    final id = DateTime.now().millisecondsSinceEpoch % 100000;

    if (delaySeconds == 0) {
      final plugin = FlutterLocalNotificationsPlugin();
      await plugin.show(
        id,
        'Đến giờ uống thuốc (Test)',
        _selectedMedication!.drugName,
        details,
        payload: payload,
      );
    } else {
      final plugin = FlutterLocalNotificationsPlugin();
      await plugin.zonedSchedule(
        id,
        'Đến giờ uống thuốc (Test)',
        _selectedMedication!.drugName,
        tz.TZDateTime.from(notificationTime, tz.local),
        details,
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        uiLocalNotificationDateInterpretation:
            UILocalNotificationDateInterpretation.absoluteTime,
        payload: payload,
      );
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(delaySeconds == 0 ? 'Đã gửi thông báo' : 'Sẽ gửi thông báo sau $delaySeconds giây')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Test Thông Báo'),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black,
        elevation: 0,
      ),
      body: _isLoading 
        ? const Center(child: CircularProgressIndicator())
        : Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text('Chọn thuốc có trong danh sách để test logic lưu API lúc chọn action trên Notification:', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 10),
                if (_activeMedications.isEmpty)
                  const Text('Không có thuốc nào đang uống.')
                else
                  DropdownButton<MedicationRecord>(
                    isExpanded: true,
                    value: _selectedMedication,
                    items: _activeMedications.map((m) {
                      return DropdownMenuItem(
                        value: m,
                        child: Text(m.drugName),
                      );
                    }).toList(),
                    onChanged: (val) {
                      setState(() {
                        _selectedMedication = val;
                      });
                    },
                  ),
                const SizedBox(height: 30),
                ElevatedButton.icon(
                  onPressed: () => _sendTestNotification(0),
                  icon: const Icon(Icons.notifications_active),
                  label: const Text('Hiện thông báo ngay lập tức'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF10B981),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  onPressed: () => _sendTestNotification(5),
                  icon: const Icon(Icons.timer),
                  label: const Text('Hẹn gửi sau 5 giây'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF3B82F6),
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ],
            ),
          ),
    );
  }
}
