import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/medication_service.dart';
import '../services/notification_service.dart';
import '../models/medication_models.dart';
import 'test_notification_screen.dart';

/// Screen to display today's medication reminders
class TodayRemindersScreen extends StatefulWidget {
  final int userId;

  const TodayRemindersScreen({Key? key, required this.userId})
    : super(key: key);

  @override
  _TodayRemindersScreenState createState() => _TodayRemindersScreenState();
}

class _TodayRemindersScreenState extends State<TodayRemindersScreen> {
  final MedicationService _medicationService = MedicationService();
  List<MedicationReminder> _reminders = [];
  AdherenceStats? _stats;
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      // Load today's reminders
      final remindersResult = await _medicationService.getTodayReminders(
        widget.userId,
      );
      if (remindersResult['success']) {
        setState(() {
          _reminders = (remindersResult['data'] as List)
              .map((json) => MedicationReminder.fromJson(json))
              .toList();
        });
      }

      // Load adherence stats
      final statsResult = await _medicationService.getAdherenceStats(
        widget.userId,
      );
      if (statsResult['success'] && statsResult['data'] != null) {
        setState(() {
          _stats = AdherenceStats.fromJson(statsResult['data']);
        });
      }

      // Schedule notifications for pending reminders today
      if (_reminders.isNotEmpty) {
        final notificationService = NotificationService();
        await notificationService
            .cancelAllReminders(); // Clear old today's notifications
        int count = 0;
        final now = DateTime.now();
        for (var reminder in _reminders) {
          if (reminder.status == 'PENDING' &&
              reminder.getScheduledDateTime().isAfter(now)) {
            await notificationService.scheduleMedicationReminder(reminder);
            count++;
          }
        }
        print('✅ Scheduled $count future notifications for today.');
      }
    } catch (e) {
      setState(() {
        _errorMessage = 'Lỗi tải dữ liệu: ${e.toString()}';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _markAsTaken(MedicationReminder reminder) async {
    final result = await _medicationService.markReminderAsTaken(
      reminder.id,
      null,
    );
    if (result['success']) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('✅ Đã đánh dấu đã uống'),
          backgroundColor: Color(0xFF10B981),
        ),
      );
      _loadData();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['message'] ?? 'Có lỗi xảy ra'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  Future<void> _markAsSkipped(MedicationReminder reminder) async {
    final result = await _medicationService.markReminderAsSkipped(
      reminder.id,
      null,
    );
    if (result['success']) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('⏭️ Đã đánh dấu bỏ qua'),
          backgroundColor: Colors.orange,
        ),
      );
      _loadData();
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['message'] ?? 'Có lỗi xảy ra'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        title: const Text(
          'Nhắc nhở hôm nay',
          style: TextStyle(color: Colors.white),
        ),
        backgroundColor: const Color(0xFF284C7B),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.science),
            tooltip: 'Test thông báo (mock data)',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const TestNotificationScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? _buildErrorView()
          : RefreshIndicator(
              onRefresh: _loadData,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: Column(
                  children: [
                    if (_stats != null) _buildStatsCard(),
                    _buildRemindersList(),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildErrorView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, size: 64, color: Colors.red),
          const SizedBox(height: 16),
          Text(_errorMessage ?? 'Có lỗi xảy ra'),
          const SizedBox(height: 16),
          ElevatedButton(onPressed: _loadData, child: const Text('Thử lại')),
        ],
      ),
    );
  }

  Widget _buildStatsCard() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Color(
              int.parse(_stats!.getAdherenceColor().replaceFirst('#', '0xFF')),
            ),
            Color(
              int.parse(_stats!.getAdherenceColor().replaceFirst('#', '0xFF')),
            ).withOpacity(0.7),
          ],
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Tuân thủ uống thuốc',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  _stats!.getAdherenceLevelText(),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildStatItem(
                '${_stats!.adherenceRate.toStringAsFixed(1)}%',
                'Tỷ lệ',
              ),
              _buildStatItem('${_stats!.takenCount}', 'Đã uống'),
              _buildStatItem('${_stats!.currentStreak}', 'Chuỗi ngày'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String value, String label) {
    return Column(
      children: [
        Text(
          value,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 28,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(color: Colors.white.withOpacity(0.9), fontSize: 14),
        ),
      ],
    );
  }

  Widget _buildRemindersList() {
    if (_reminders.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            Icon(Icons.check_circle_outline, size: 80, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              'Không có nhắc nhở hôm nay',
              style: TextStyle(fontSize: 18, color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    // Group reminders by time
    final now = DateTime.now();
    final upcoming = _reminders
        .where(
          (r) => r.status == 'PENDING' && r.getScheduledDateTime().isAfter(now),
        )
        .toList();
    final overdue = _reminders
        .where(
          (r) =>
              r.status == 'PENDING' && r.getScheduledDateTime().isBefore(now),
        )
        .toList();
    final completed = _reminders
        .where((r) => r.status == 'TAKEN' || r.status == 'SKIPPED')
        .toList();

    return Column(
      children: [
        if (overdue.isNotEmpty) ...[
          _buildSectionHeader('⚠️ Đã quá giờ', overdue.length, Colors.red),
          ...overdue.map((r) => _buildReminderCard(r, isOverdue: true)),
        ],
        if (upcoming.isNotEmpty) ...[
          _buildSectionHeader('⏰ Sắp tới', upcoming.length, Colors.orange),
          ...upcoming.map((r) => _buildReminderCard(r)),
        ],
        if (completed.isNotEmpty) ...[
          _buildSectionHeader(
            '✅ Đã hoàn thành',
            completed.length,
            Colors.green,
          ),
          ...completed.map((r) => _buildReminderCard(r, isCompleted: true)),
        ],
      ],
    );
  }

  Widget _buildSectionHeader(String title, int count, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      margin: const EdgeInsets.only(top: 8),
      color: color.withOpacity(0.1),
      child: Row(
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
            decoration: BoxDecoration(
              color: color,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              '$count',
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReminderCard(
    MedicationReminder reminder, {
    bool isOverdue = false,
    bool isCompleted = false,
  }) {
    final timeFormat = DateFormat('HH:mm');
    final scheduledTime = reminder.getScheduledDateTime();

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: isOverdue ? Border.all(color: Colors.red, width: 2) : null,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(isOverdue ? 0.08 : 0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: Color(
                        int.parse(
                          reminder.getStatusColor().replaceFirst('#', '0xFF'),
                        ),
                      ).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(
                      Icons.medication,
                      color: Color(
                        int.parse(
                          reminder.getStatusColor().replaceFirst('#', '0xFF'),
                        ),
                      ),
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          reminder.drugName ?? 'Thuốc',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF284C7B),
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          reminder.dosage ?? '',
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    timeFormat.format(scheduledTime),
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: isOverdue ? Colors.red : const Color(0xFF284C7B),
                    ),
                  ),
                ],
              ),
              if (reminder.mealRelation != null) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(Icons.restaurant, size: 16, color: Colors.grey[600]),
                    const SizedBox(width: 6),
                    Text(
                      _getMealRelationText(reminder.mealRelation!),
                      style: TextStyle(fontSize: 14, color: Colors.grey[700]),
                    ),
                  ],
                ),
              ],
              if (!isCompleted) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton.icon(
                        onPressed: () => _markAsTaken(reminder),
                        icon: const Icon(Icons.check, size: 20),
                        label: const Text('Đã uống'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF10B981),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _markAsSkipped(reminder),
                        icon: const Icon(Icons.close, size: 20),
                        label: const Text('Bỏ qua'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.grey[700],
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          side: BorderSide(color: Colors.grey[400]!),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(8),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ] else ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Color(
                      int.parse(
                        reminder.getStatusColor().replaceFirst('#', '0xFF'),
                      ),
                    ).withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        reminder.status == 'TAKEN'
                            ? Icons.check_circle
                            : Icons.cancel,
                        size: 16,
                        color: Color(
                          int.parse(
                            reminder.getStatusColor().replaceFirst('#', '0xFF'),
                          ),
                        ),
                      ),
                      const SizedBox(width: 6),
                      Text(
                        reminder.getStatusText(),
                        style: TextStyle(
                          fontSize: 14,
                          color: Color(
                            int.parse(
                              reminder.getStatusColor().replaceFirst(
                                '#',
                                '0xFF',
                              ),
                            ),
                          ),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  String _getMealRelationText(String mealRelation) {
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
}
