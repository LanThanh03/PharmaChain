import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import '../models/medication_models.dart';
import 'medication_service.dart';

@pragma('vm:entry-point')
void notificationTapBackground(NotificationResponse notificationResponse) async {
  print('Notification action tapped: ${notificationResponse.actionId}');
  
  if (notificationResponse.payload != null) {
    try {
      final payloadData = jsonDecode(notificationResponse.payload!);
      final recordId = payloadData['recordId'];
      final scheduledDate = payloadData['scheduledDate'];
      final scheduledTime = payloadData['scheduledTime']; 
      final drugName = payloadData['drugName'];
      final medicationService = MedicationService();

      if (notificationResponse.actionId == NotificationService.actionTaken) {
        await medicationService.markReminderActionByTime(
          recordId: recordId,
          scheduledDate: scheduledDate,
          scheduledTime: scheduledTime,
          action: 'TAKEN',
          notes: 'Đã uống qua thông báo',
        );
        print('Đã đánh dấu uống thuốc: $drugName');
      } else if (notificationResponse.actionId == NotificationService.actionSkip) {
        await medicationService.markReminderActionByTime(
          recordId: recordId,
          scheduledDate: scheduledDate,
          scheduledTime: scheduledTime,
          action: 'SKIPPED',
          notes: 'Bỏ qua qua thông báo',
        );
        print('Đã đánh dấu bỏ qua: $drugName');
      } else if (notificationResponse.actionId == NotificationService.actionSnooze) {
        // Reschedule in 10 minutes
        await NotificationService().snoozeReminder(
          recordId: recordId,
          drugName: drugName,
          payload: notificationResponse.payload!,
        );
      }
    } catch (e) {
      print('Error parsing notification payload: $e');
    }
  }
}

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();

  factory NotificationService() {
    return _instance;
  }

  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();

  static const String actionTaken = 'action_taken';
  static const String actionSnooze = 'action_snooze';
  static const String actionSkip = 'action_skip';

  Future<void> initialize() async {
    tz.initializeTimeZones();
    tz.setLocalLocation(tz.getLocation('Asia/Ho_Chi_Minh'));

    const AndroidInitializationSettings initializationSettingsAndroid =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initializationSettings =
        InitializationSettings(
      android: initializationSettingsAndroid,
    );

    await _notificationsPlugin.initialize(
      initializationSettings,
      onDidReceiveNotificationResponse: (NotificationResponse response) {
        // Handle foreground/background tap
        if (response.actionId != null) {
            notificationTapBackground(response);
        }
      },
      onDidReceiveBackgroundNotificationResponse: notificationTapBackground,
    );
  }

  Future<void> requestPermissions() async {
    if (!kIsWeb && Platform.isAndroid) {
      final AndroidFlutterLocalNotificationsPlugin? androidImplementation =
          _notificationsPlugin.resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>();

      await androidImplementation?.requestNotificationsPermission();
      await androidImplementation?.requestExactAlarmsPermission();
    }
  }

  Future<void> scheduleMedicationReminder(MedicationReminder reminder) async {
    final now = DateTime.now();
    DateTime scheduledDateTime = reminder.getScheduledDateTime();
    
    // Add 1 minute delay according to latest requirements
    DateTime notificationTime = scheduledDateTime.add(const Duration(minutes: 1));

    // Don't schedule if it's in the past
    if (notificationTime.isBefore(now)) {
      print('⏭️ [SCHEDULE] Bỏ qua (đã qua): ${reminder.drugName} '
          '- Giờ hẹn: $scheduledDateTime '
          '- Giờ thông báo (+ 1 phút): $notificationTime '
          '- Hiện tại: $now');
      return;
    }

    final id = reminder.id > 0 ? reminder.id : (reminder.recordId * 1000 + scheduledDateTime.hour * 60 + scheduledDateTime.minute);

    print('📅 [SCHEDULE] Đặt lịch: ${reminder.drugName} '
        '- ID thông báo: $id '
        '- Giờ uống: ${reminder.scheduledTime} (${reminder.scheduledDate}) '
        '- Giờ thông báo: $notificationTime '
        '- Còn ${notificationTime.difference(now).inMinutes} phút nữa');

    final payload = jsonEncode({
      'recordId': reminder.recordId,
      'reminderId': reminder.id,
      'scheduledDate': reminder.scheduledDate,
      'scheduledTime': reminder.scheduledTime,
      'drugName': reminder.drugName ?? 'Thuốc',
    });

    await _notificationsPlugin.zonedSchedule(
      id,
      'Đến giờ uống thuốc',
      reminder.drugName ?? 'Vui lòng kiểm tra lịch uống thuốc',
      tz.TZDateTime.from(notificationTime, tz.local),
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'medication_reminders',
          'Nhắc nhở uống thuốc',
          channelDescription: 'Thông báo đến giờ uống thuốc',
          importance: Importance.max,
          priority: Priority.high,
          actions: <AndroidNotificationAction>[
            AndroidNotificationAction(
              actionTaken,
              'Đã uống',
              showsUserInterface: false,
              titleColor: Color(0xFF10B981),
            ),
            AndroidNotificationAction(
              actionSnooze,
              'Nhắc lại sau 10 phút',
              showsUserInterface: false,
            ),
            AndroidNotificationAction(
              actionSkip,
              'Bỏ qua',
              showsUserInterface: false,
              titleColor: Color(0xFFEF4444),
            ),
          ],
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      payload: payload,
    );
  }

  Future<void> snoozeReminder({
    required int recordId,
    required String drugName,
    required String payload,
  }) async {
    final now = DateTime.now();
    final notificationTime = now.add(const Duration(minutes: 10));
    final id = (recordId * 1000 + notificationTime.hour * 60 + notificationTime.minute) % 2147483647;

    await _notificationsPlugin.zonedSchedule(
      id,
      'Đến giờ uống thuốc (Nhắc lại)',
      drugName,
      tz.TZDateTime.from(notificationTime, tz.local),
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'medication_reminders',
          'Nhắc nhở uống thuốc',
          channelDescription: 'Thông báo đến giờ uống thuốc',
          importance: Importance.max,
          priority: Priority.high,
          actions: <AndroidNotificationAction>[
            AndroidNotificationAction(
              actionTaken,
              'Đã uống',
              showsUserInterface: false,
            ),
            AndroidNotificationAction(
              actionSnooze,
              'Nhắc lại sau 10 phút',
              showsUserInterface: false,
            ),
            AndroidNotificationAction(
              actionSkip,
              'Bỏ qua',
              showsUserInterface: false,
            ),
          ],
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      payload: payload,
    );
  }

  Future<void> cancelAllReminders() async {
    await _notificationsPlugin.cancelAll();
  }
}
