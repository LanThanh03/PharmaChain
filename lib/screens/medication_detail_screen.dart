import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/medication_models.dart';
import '../services/medication_service.dart';
import '../services/notification_service.dart';
import 'package:table_calendar/table_calendar.dart';
import 'drug_ai_chat_screen.dart';

/// Screen to display detailed information about a medication
class MedicationDetailScreen extends StatefulWidget {
  final MedicationRecord medication;

  const MedicationDetailScreen({Key? key, required this.medication})
    : super(key: key);

  @override
  _MedicationDetailScreenState createState() => _MedicationDetailScreenState();
}

class _MedicationDetailScreenState extends State<MedicationDetailScreen> {
  final MedicationService _medicationService = MedicationService();
  List<MedicationReminder> _reminders = [];
  bool _isLoadingReminders = true;
  late List<String> _currentTimes;

  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay = DateTime.now();

  List<MedicationReminder> _getRemindersForDay(DateTime day) {
    return _reminders.where((r) {
      final sched = DateTime.parse(r.scheduledDate);
      return isSameDay(sched, day);
    }).toList();
  }

  @override
  void initState() {
    super.initState();
    _currentTimes = widget.medication.getReminderTimesList();
    _loadReminders();
  }

  Future<void> _editTime(int index, String oldTime) async {
    final timeParts = oldTime.split(':');
    final initialTime = TimeOfDay(
      hour: int.parse(timeParts[0]),
      minute: int.parse(timeParts[1]),
    );

    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
          child: child!,
        );
      },
    );

    if (picked != null) {
      final newTimeStr =
          '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
      if (newTimeStr != oldTime) {
        setState(() {
          _currentTimes[index] = newTimeStr;
          // Sắp xếp lại giờ
          _currentTimes.sort();
        });

        // Gọi API cập nhật
        try {
          final newTimesString = _currentTimes.join(',');

          final result = await _medicationService.updateReminderTimes(
            widget.medication.id,
            newTimesString,
          );

          if (context.mounted) {
            if (result['success'] == true) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Đã cập nhật giờ uống thuốc'),
                  backgroundColor: Color(0xFF10B981),
                ),
              );
              // Tải lại chi tiết reminder
              await _loadReminders();

              // === ĐẶT LẠI LỊCH THÔNG BÁO SAU KHI CẬP NHẬT GIỜ ===
              await _rescheduleNotifications();
            } else {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(result['message'] ?? 'Lỗi khi cập nhật'),
                  backgroundColor: Colors.red,
                ),
              );
            }
          }
        } catch (e) {
          if (context.mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Lỗi ngoại lệ: $e'),
                backgroundColor: Colors.red,
              ),
            );
          }
        }
      }
    }
  }

  Future<void> _loadReminders() async {
    // Assuming backend endpoint /api/app/medications/{recordId}/reminders exists
    // If not, we will display schedule from string text and progress.
    try {
      final response = await _medicationService.getRemindersByRecordId(
        widget.medication.id,
      );
      if (response['success'] == true) {
        setState(() {
          _reminders = (response['data'] as List)
              .map((e) => MedicationReminder.fromJson(e))
              .toList();
          // Lọc lại các reminder để sort
          _reminders.sort(
            (a, b) =>
                a.getScheduledDateTime().compareTo(b.getScheduledDateTime()),
          );
          _isLoadingReminders = false;
        });
      } else {
        setState(() => _isLoadingReminders = false);
      }
    } catch (e) {
      setState(() => _isLoadingReminders = false);
    }
  }

  /// Hủy tất cả notification cũ và đặt lại lịch thông báo mới
  /// dựa trên danh sách _reminders đã được cập nhật từ server
  Future<void> _rescheduleNotifications() async {
    final notificationService = NotificationService();
    final now = DateTime.now();

    // Hủy tất cả notification cũ
    await notificationService.cancelAllReminders();
    print('🔔 [NOTIFICATION] Đã hủy tất cả notification cũ');
    print('🔔 [NOTIFICATION] Thời gian hiện tại: ${now.toString()}');

    // Lọc ra các reminder PENDING trong tương lai và đặt lịch mới
    int scheduledCount = 0;
    int skippedCount = 0;

    for (var reminder in _reminders) {
      final scheduledTime = reminder.getScheduledDateTime();

      if (reminder.status == 'PENDING' && scheduledTime.isAfter(now)) {
        await notificationService.scheduleMedicationReminder(reminder);
        scheduledCount++;
        print(
          '  ✅ Đã đặt lịch: ${reminder.drugName ?? "Thuốc"} '
          '- Giờ: ${reminder.scheduledTime} '
          '- Ngày: ${reminder.scheduledDate} '
          '- ID: ${reminder.id}',
        );
      } else {
        skippedCount++;
        print(
          '  ⏭️ Bỏ qua: ${reminder.drugName ?? "Thuốc"} '
          '- Giờ: ${reminder.scheduledTime} '
          '- Trạng thái: ${reminder.status} '
          '- Đã qua: ${scheduledTime.isBefore(now)}',
        );
      }
    }

    print(
      '🔔 [NOTIFICATION] Tổng kết: Đã đặt $scheduledCount thông báo, bỏ qua $skippedCount',
    );

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            '🔔 Đã cập nhật $scheduledCount thông báo nhắc uống thuốc',
          ),
          backgroundColor: const Color(0xFF3B82F6),
          duration: const Duration(seconds: 3),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        title: const Text('Chi tiết thuốc'),
        backgroundColor: const Color(0xFF284C7B),
        elevation: 0,
        actions: [
          if (widget.medication.isActive)
            IconButton(
              icon: const Icon(Icons.stop_circle_outlined),
              onPressed: () => _showStopDialog(context),
              tooltip: 'Dừng thuốc',
            ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(),
            _buildInfoSection(),
            _buildDrugDetailsSection(),
            _buildScheduleSection(),
            _buildDetailedRemindersSection(),
            _buildPharmacySection(),
            _buildPharmacySection(),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFF1E3A5F), Color(0xFF284C7B)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (widget.medication.imageUrl != null &&
                  widget.medication.imageUrl!.isNotEmpty)
                Hero(
                  tag: 'med_img_${widget.medication.id}',
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.2),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(16),
                      child: CachedNetworkImage(
                        imageUrl: widget.medication.imageUrl!,
                        width: 80,
                        height: 80,
                        fit: BoxFit.cover,
                        placeholder: (context, url) => Container(
                          color: Colors.grey[100],
                          child: const Center(
                            child: SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Color(0xFF10B981),
                              ),
                            ),
                          ),
                        ),
                        errorWidget: (context, url, error) => Container(
                          color: Colors.white.withOpacity(0.2),
                          child: const Icon(
                            Icons.medication,
                            color: Colors.white,
                            size: 40,
                          ),
                        ),
                      ),
                    ),
                  ),
                )
              else
                Hero(
                  tag: 'med_icon_${widget.medication.id}',
                  child: Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.15),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: Colors.white.withOpacity(0.2),
                        width: 1,
                      ),
                    ),
                    child: const Icon(
                      Icons.medication,
                      color: Colors.white,
                      size: 40,
                    ),
                  ),
                ),
              const SizedBox(width: 20),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.medication.drugName,
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.w800,
                        color: Colors.white,
                        letterSpacing: -0.5,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.white.withOpacity(0.2),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        widget.medication.dosage,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          if (widget.medication.isExpired()) ...[
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red[100],
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Row(
                children: [
                  Icon(Icons.warning, color: Colors.red, size: 20),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      '⚠️ Thuốc đã hết hạn sử dụng',
                      style: TextStyle(
                        color: Colors.red,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInfoSection() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF59E0B).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.assignment,
                  color: Color(0xFFF59E0B),
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              const Text(
                'Thông tin chung',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1E3A5F),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          if (widget.medication.batchNumber != null &&
              widget.medication.batchNumber!.isNotEmpty)
            _buildDetailItem(
              Icons.qr_code,
              'Mã sản phẩm / Số lô',
              widget.medication.batchNumber!,
            ),
          if (widget.medication.expiryDate != null)
            _buildDetailItem(
              Icons.event_busy,
              'Hạn sử dụng',
              widget.medication.expiryDate!,
            ),
          _buildDetailItem(
            Icons.event_available,
            'Ngày bắt đầu',
            widget.medication.startDate,
          ),
          _buildDetailItem(
            Icons.event,
            'Ngày kết thúc',
            widget.medication.endDate,
          ),
          _buildDetailItem(
            Icons.hourglass_bottom,
            'Còn lại',
            '${widget.medication.getDaysRemaining()} ngày',
            valueColor: widget.medication.getDaysRemaining() > 0
                ? const Color(0xFF10B981)
                : const Color(0xFFF59E0B),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleSection() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF8B5CF6).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.calendar_month,
                  color: Color(0xFF8B5CF6),
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              const Text(
                'Lịch uống thuốc',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1E3A5F),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _buildDetailItem(
            Icons.repeat,
            'Số lần/ngày',
            '${widget.medication.frequency} lần',
          ),
          _buildDetailItem(
            Icons.restaurant,
            'Thời điểm',
            widget.medication.getMealRelationText(),
          ),
          if (widget.medication.totalDoses != null)
            _buildDetailItem(
              Icons.medication,
              'Tiến độ',
              '${widget.medication.takenDoses ?? 0} / ${widget.medication.totalDoses} liều',
              valueColor: const Color(0xFF10B981),
            ),
          const SizedBox(height: 12),
          const Padding(
            padding: EdgeInsets.only(left: 30),
            child: Text(
              'Giờ uống:',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: Color(0xFF64748B),
              ),
            ),
          ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.only(left: 30),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: _currentTimes.asMap().entries.map((entry) {
                final idx = entry.key;
                final time = entry.value;
                return InkWell(
                  onTap: () => _editTime(idx, time),
                  borderRadius: BorderRadius.circular(20),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xFF10B981).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: const Color(0xFF10B981).withOpacity(0.3),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(
                          Icons.access_time_filled,
                          size: 16,
                          color: Color(0xFF10B981),
                        ),
                        const SizedBox(width: 6),
                        Text(
                          time,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF059669),
                          ),
                        ),
                        const SizedBox(width: 4),
                        const Icon(
                          Icons.edit,
                          size: 12,
                          color: Color(0xFF059669),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailedRemindersSection() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF10B981).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.check_circle_outline,
                  color: Color(0xFF10B981),
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                'Chi tiết tình trạng uống thuốc',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1E3A5F),
                ),
              ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          if (_isLoadingReminders)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(20.0),
                child: CircularProgressIndicator(),
              ),
            )
          else if (_reminders.isEmpty)
            const Text(
              'Chưa có lịch uống thuốc chi tiết.',
              style: TextStyle(color: Colors.grey, fontStyle: FontStyle.italic),
            )
          else
            Column(
              children: [
                TableCalendar<MedicationReminder>(
                  firstDay: DateTime.now().subtract(const Duration(days: 365)),
                  lastDay: DateTime.now().add(const Duration(days: 365)),
                  focusedDay: _focusedDay,
                  calendarFormat: CalendarFormat.week,
                  startingDayOfWeek: StartingDayOfWeek.monday,
                  selectedDayPredicate: (day) => isSameDay(_selectedDay, day),
                  onDaySelected: (selectedDay, focusedDay) {
                    setState(() {
                      _selectedDay = selectedDay;
                      _focusedDay = focusedDay;
                    });
                  },
                  eventLoader: _getRemindersForDay,
                  calendarStyle: const CalendarStyle(
                    todayDecoration: BoxDecoration(
                      color: Color(0xFF93C5FD),
                      shape: BoxShape.circle,
                    ),
                    selectedDecoration: BoxDecoration(
                      color: Color(0xFF10B981),
                      shape: BoxShape.circle,
                    ),
                  ),
                  calendarBuilders: CalendarBuilders(
                    markerBuilder: (context, date, events) {
                      if (events.isEmpty) return const SizedBox();
                      return Positioned(
                        bottom: 1,
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: events.map((event) {
                            return Container(
                              margin: const EdgeInsets.symmetric(
                                horizontal: 0.5,
                              ),
                              width: 5,
                              height: 5,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: event.status == 'TAKEN'
                                    ? const Color(0xFF10B981)
                                    : (event.status == 'SKIPPED'
                                          ? const Color(0xFFEF4444)
                                          : const Color(0xFFF59E0B)),
                              ),
                            );
                          }).toList(),
                        ),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 8),
                if (_getRemindersForDay(_selectedDay ?? _focusedDay).isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 20),
                    child: Text(
                      'Không có lịch uống thuốc trong ngày này',
                      style: TextStyle(color: Colors.grey),
                    ),
                  )
                else
                  ..._getRemindersForDay(_selectedDay ?? _focusedDay).map((
                    reminder,
                  ) {
                    final isTaken = reminder.status == 'TAKEN';
                    final color = Color(
                      int.parse(
                        reminder.getStatusColor().replaceFirst('#', '0xFF'),
                      ),
                    );
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: color.withOpacity(0.1),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              isTaken
                                  ? Icons.check
                                  : (reminder.status == 'SKIPPED'
                                        ? Icons.close
                                        : Icons.schedule),
                              color: color,
                              size: 16,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Giờ uống: ${reminder.scheduledTime}',
                                  style: const TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                if (reminder.takenAt != null)
                                  Text(
                                    'Lúc: ${DateFormat('HH:mm dd/MM/yyyy').format(DateTime.parse(reminder.takenAt!).toLocal())}',
                                    style: TextStyle(
                                      color: Colors.grey[600],
                                      fontSize: 13,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: color.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Text(
                              reminder.getStatusText(),
                              style: TextStyle(
                                color: color,
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
              ],
            ),
        ],
      ),
    );
  }

  Widget _buildDrugDetailsSection() {
    // final hasDetails =
    //     widget.medication.activeIngredient != null ||
    //     widget.medication.description != null ||
    //     widget.medication.category != null;

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF3B82F6).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.info_outline,
                  color: Color(0xFF3B82F6),
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              const Text(
                'Thông tin chi tiết thuốc',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1E3A5F),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          if (widget.medication.category != null &&
              widget.medication.category!.isNotEmpty)
            _buildDetailItem(
              Icons.category,
              'Danh mục',
              widget.medication.category!,
            ),
          if (widget.medication.activeIngredient != null &&
              widget.medication.activeIngredient!.isNotEmpty)
            _buildDetailItem(
              Icons.science,
              'Hoạt chất',
              widget.medication.activeIngredient!,
            ),
          if (widget.medication.drugDosage != null &&
              widget.medication.drugDosage!.isNotEmpty)
            _buildDetailItem(
              Icons.monitor_weight,
              'Hàm lượng',
              widget.medication.drugDosage!,
            ),
          if (widget.medication.unit != null &&
              widget.medication.unit!.isNotEmpty)
            _buildDetailItem(
              Icons.inventory_2,
              'Đóng gói',
              widget.medication.unit!,
            ),
          if (widget.medication.storageConditions != null &&
              widget.medication.storageConditions!.isNotEmpty)
            _buildDetailItem(
              Icons.thermostat,
              'Bảo quản',
              widget.medication.storageConditions!,
            ),
          if (widget.medication.shelfLife != null &&
              widget.medication.shelfLife!.isNotEmpty)
            _buildDetailItem(
              Icons.date_range,
              'Tuổi thọ',
              widget.medication.shelfLife!,
            ),
          if (widget.medication.description != null &&
              widget.medication.description!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFFF8FAFC),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFE2E8F0)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.notes, size: 16, color: Colors.grey[600]),
                      const SizedBox(width: 8),
                      const Text(
                        'Chỉ định / Mô tả:',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Color(0xFF4B5563),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    widget.medication.description!,
                    style: const TextStyle(
                      fontSize: 14,
                      color: Color(0xFF1F2937),
                      height: 1.6,
                    ),
                  ),
                ],
              ),
            ),
          ],
          // === Nút tư vấn AI ===
          const SizedBox(height: 20),
          InkWell(
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) =>
                      DrugAIChatScreen(drugName: widget.medication.drugName),
                ),
              );
            },
            borderRadius: BorderRadius.circular(16),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF6366F1), Color(0xFF8B5CF6)],
                  begin: Alignment.centerLeft,
                  end: Alignment.centerRight,
                ),
                borderRadius: BorderRadius.circular(16),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF6366F1).withOpacity(0.3),
                    blurRadius: 12,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.smart_toy,
                      color: Colors.white,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Tư vấn AI về thuốc này',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Chống chỉ định, tác dụng phụ, tương tác thuốc...',
                          style: TextStyle(color: Colors.white70, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.arrow_forward_ios,
                    color: Colors.white70,
                    size: 16,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailItem(
    IconData icon,
    String label,
    String value, {
    Color? valueColor,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: const Color(0xFF94A3B8)),
          const SizedBox(width: 12),
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: Color(0xFF64748B),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: valueColor ?? const Color(0xFF334155),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPharmacySection() {
    if (widget.medication.pharmacyName == null) return const SizedBox.shrink();

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 15,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF10B981).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.storefront,
                  color: Color(0xFF10B981),
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              const Text(
                'Nơi mua',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1E3A5F),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              const SizedBox(width: 8),
              const Icon(Icons.location_on, color: Color(0xFF94A3B8), size: 18),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  widget.medication.pharmacyName!,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF334155),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _showStopDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Dừng thuốc'),
        content: Text(
          'Bạn có chắc muốn dừng thuốc "${widget.medication.drugName}"?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              final service = MedicationService();
              final result = await service.stopMedication(widget.medication.id);

              if (context.mounted) {
                if (result['success']) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Đã dừng thuốc'),
                      backgroundColor: Color(0xFF10B981),
                    ),
                  );
                  Navigator.pop(context, true);
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(result['message'] ?? 'Có lỗi xảy ra'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              }
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Dừng'),
          ),
        ],
      ),
    );
  }
}
