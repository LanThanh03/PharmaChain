import 'package:flutter/material.dart';
import 'package:device_calendar/device_calendar.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
class CalendarService {
  static final DeviceCalendarPlugin _deviceCalendarPlugin = DeviceCalendarPlugin();
  static bool _initializedTZ = false;

  /// Yêu cầu quyền truy cập lịch và hỏi người dùng có muốn thêm nhắc nhở vào lịch không.
  static Future<bool> promptAndAddReminders({
    required BuildContext context,
    required String drugName,
    required String dosage,
    required String startDateStr,
    required String? endDateStr,
    required String reminderTimesStr, // comma separated e.g., "08:00,12:00"
    required String mealRelation, // e.g., "AFTER", "BEFORE"
    int defaultDurationDays = 7,
  }) async {
    if (!_initializedTZ) {
      tz.initializeTimeZones();
      try {
        // Cố định hoàn toàn múi giờ Việt Nam theo yêu cầu để tránh sai sót
        tz.setLocalLocation(tz.getLocation('Asia/Ho_Chi_Minh'));
      } catch (e) {
        debugPrint('Không thể lấy múi giờ: $e');
      }
      _initializedTZ = true;
    }

    // 1. Check permissions first
    var permissionsGranted = await _deviceCalendarPlugin.hasPermissions();
    if (permissionsGranted.isSuccess && !(permissionsGranted.data ?? false)) {
      permissionsGranted = await _deviceCalendarPlugin.requestPermissions();
      if (!permissionsGranted.isSuccess || !(permissionsGranted.data ?? false)) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Không đủ quyền truy cập lịch. Vui lòng cấp quyền trong Cài đặt.'),
              backgroundColor: Colors.red,
            ),
          );
        }
        return false;
      }
    }

    // 2. Ask user
    if (!context.mounted) return false;
    bool userAgreed = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Thêm vào lịch'),
        content: Text('Bạn có muốn thêm lịch uống thuốc "$drugName" vào ứng dụng Lịch của máy để tự động thông báo không?'),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Bỏ qua', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF10B981)),
            child: const Text('Đồng ý', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    ) ?? false;

    if (!userAgreed) return false;

    // 3. Find writable calendar
    String? selectedCalendarId;
    final calendarsResult = await _deviceCalendarPlugin.retrieveCalendars();
    if (calendarsResult.isSuccess && calendarsResult.data != null) {
       final writableCals = calendarsResult.data!.where((c) => c.isReadOnly == false).toList();
       if (writableCals.isEmpty) {
         if (context.mounted) {
           ScaffoldMessenger.of(context).showSnackBar(
             const SnackBar(content: Text('Không tìm thấy lịch nào có thể ghi trên máy.'), backgroundColor: Colors.red),
           );
         }
         return false;
       }
       // Prioritize local or default calendar, otherwise pick the first one
       selectedCalendarId = writableCals.firstWhere(
         (c) => c.isDefault ?? false,
         orElse: () => writableCals.first,
       ).id;
    } else {
       if (context.mounted) {
         ScaffoldMessenger.of(context).showSnackBar(
           const SnackBar(content: Text('Lỗi khi tải danh sách lịch trên thiết bị.'), backgroundColor: Colors.red),
         );
       }
       return false;
    }

    if (selectedCalendarId == null) return false;

    // 4. Process dates and times
    DateTime startDate;
    try {
      startDate = DateTime.parse(startDateStr);
    } catch (_) {
      startDate = DateTime.now();
    }

    DateTime endDate;
    if (endDateStr != null && endDateStr.isNotEmpty) {
      try {
        endDate = DateTime.parse(endDateStr);
      } catch (_) {
        endDate = startDate.add(Duration(days: defaultDurationDays - 1));
      }
    } else {
      endDate = startDate.add(Duration(days: defaultDurationDays - 1));
    }

    List<String> times = reminderTimesStr.split(',').map((t) => t.trim()).where((t) => t.isNotEmpty).toList();
    if (times.isEmpty) times = ['08:00'];

    String mealInstruction = 'sau ăn';
    if (mealRelation == 'BEFORE') mealInstruction = 'trước ăn';
    else if (mealRelation == 'WITH') mealInstruction = 'trong bữa ăn';
    else if (mealRelation == 'ANY') mealInstruction = 'bất kỳ lúc nào';

    int eventCount = 0;
    
    // We add an event for each day and each time
    try {
      DateTime currentDay = DateTime(startDate.year, startDate.month, startDate.day);
      final stopDay = DateTime(endDate.year, endDate.month, endDate.day);

      while (!currentDay.isAfter(stopDay)) {
        for (String timeStr in times) {
           List<String> parts = timeStr.split(':');
           if (parts.length >= 2) {
             int hour = int.tryParse(parts[0]) ?? 8;
             int minute = int.tryParse(parts[1]) ?? 0;
             
             final nativeDt = DateTime(
               currentDay.year,
               currentDay.month,
               currentDay.day,
               hour,
               minute,
             );
             
             tz.Location location;
             try {
               location = tz.getLocation('Asia/Ho_Chi_Minh');
             } catch (_) {
               location = tz.local;
             }
             
             final eventTime = tz.TZDateTime.from(nativeDt, location);
             
             final Event event = Event(
               selectedCalendarId,
               title: '💊 Uống: $drugName',
               description: 'Uống $dosage $mealInstruction.',
               start: eventTime,
               end: eventTime.add(const Duration(minutes: 10)),
               reminders: [Reminder(minutes: 0)], // Alert exactly at event time
             );
             
             final result = await _deviceCalendarPlugin.createOrUpdateEvent(event);
             if (result != null && result.isSuccess) {
                eventCount++;
             }
           }
        }
        currentDay = currentDay.add(const Duration(days: 1));
      }
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
           SnackBar(
             content: Text('Đã tạo thành công $eventCount lịch nhắc thuốc.'),
             backgroundColor: const Color(0xFF10B981),
           ),
        );
      }
      return true;
    } catch (e) {
      debugPrint('Error adding to calendar: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
           const SnackBar(
             content: Text('Có lỗi xảy ra khi thêm vào lịch.'),
             backgroundColor: Colors.red,
           ),
        );
      }
      return false;
    }
  }
}
