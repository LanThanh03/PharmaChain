import 'package:flutter/material.dart';
import '../services/medication_service.dart';
import '../services/calendar_service.dart';

/// Screen to manually add medication
class AddMedicationScreen extends StatefulWidget {
  final int userId;
  final Map<String, dynamic>? dispenseData; // Optional: from QR scan

  const AddMedicationScreen({Key? key, required this.userId, this.dispenseData})
    : super(key: key);

  @override
  _AddMedicationScreenState createState() => _AddMedicationScreenState();
}

class _AddMedicationScreenState extends State<AddMedicationScreen> {
  final _formKey = GlobalKey<FormState>();
  final MedicationService _medicationService = MedicationService();

  // Form controllers
  final _drugNameController = TextEditingController();
  final _batchNumberController = TextEditingController();
  final _dosageController = TextEditingController();
  final _pharmacyNameController = TextEditingController();

  bool _fromQR = false;
  bool _hasPharmacyInstructions = false;

  int _frequency = 3;
  String _mealRelation = 'AFTER';
  DateTime _startDate = DateTime.now();
  DateTime _endDate = DateTime.now().add(const Duration(days: 7));
  DateTime? _expiryDate;
  List<TimeOfDay> _reminderTimes = [
    const TimeOfDay(hour: 8, minute: 0),
    const TimeOfDay(hour: 12, minute: 0),
    const TimeOfDay(hour: 20, minute: 0),
  ];

  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _fromQR = widget.dispenseData != null;
    if (_fromQR) {
      _hasPharmacyInstructions =
          widget.dispenseData!['hasPharmacyInstructions'] == true;
    }
    _loadDispenseData();
  }

  void _loadDispenseData() {
    if (widget.dispenseData != null) {
      final data = widget.dispenseData!;
      _drugNameController.text = data['drugName'] ?? '';
      _batchNumberController.text = data['batchNumber'] ?? '';
      _dosageController.text = data['dosage'] ?? '';
      _pharmacyNameController.text = data['pharmacyName'] ?? '';
      _frequency = data['frequency'] ?? 3;
      _mealRelation = data['mealRelation'] ?? 'AFTER';

      if (data['expiryDate'] != null) {
        try {
          _expiryDate = DateTime.parse(data['expiryDate']);
        } catch (e) {
          // Ignore parse error
        }
      }

      if (data['durationDays'] != null) {
        _endDate = _startDate.add(Duration(days: data['durationDays']));
      }

      if (data['specificTimes'] != null) {
        final String rawStr = (data['specificTimes'] as String)
            .replaceAll('[', '')
            .replaceAll(']', '')
            .replaceAll('"', '')
            .replaceAll("'", "");
        final times = rawStr
            .split(',')
            .where((t) => t.trim().isNotEmpty)
            .toList();
        _reminderTimes = times.map((t) {
          final parts = t.trim().split(':');
          int hour = 8;
          int minute = 0;
          if (parts.isNotEmpty) {
            hour = int.tryParse(parts[0]) ?? 8;
            if (parts.length > 1) {
              minute = int.tryParse(parts[1]) ?? 0;
            }
          }
          return TimeOfDay(hour: hour, minute: minute);
        }).toList();
      }
    }
  }

  @override
  void dispose() {
    _drugNameController.dispose();
    _batchNumberController.dispose();
    _dosageController.dispose();
    _pharmacyNameController.dispose();
    super.dispose();
  }

  Future<void> _submitForm() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
    });

    try {
      final reminderTimesStr = _reminderTimes
          .map(
            (t) =>
                '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}',
          )
          .join(',');

      final result = await _medicationService.addMedication(
        userId: widget.userId,
        drugName: _drugNameController.text,
        batchNumber: _batchNumberController.text.isEmpty
            ? null
            : _batchNumberController.text,
        expiryDate: _expiryDate?.toIso8601String().split('T')[0],
        dosage: _dosageController.text,
        frequency: _frequency,
        mealRelation: _mealRelation,
        reminderTimes: reminderTimesStr,
        startDate: _startDate.toIso8601String().split('T')[0],
        endDate: _endDate.toIso8601String().split('T')[0],
        pharmacyName: _pharmacyNameController.text.isEmpty
            ? null
            : _pharmacyNameController.text,
      );

      if (mounted) {
        if (result['success']) {
          await CalendarService.promptAndAddReminders(
            context: context,
            drugName: _drugNameController.text,
            dosage: _dosageController.text,
            startDateStr: _startDate.toIso8601String().split('T')[0],
            endDateStr: _endDate.toIso8601String().split('T')[0],
            reminderTimesStr: reminderTimesStr,
            mealRelation: _mealRelation,
          );

          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('✅ Đã thêm thuốc thành công'),
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
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Lỗi: ${e.toString()}'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasManufacturerInfo =
        widget.dispenseData != null &&
        ((widget.dispenseData!['description'] != null &&
                widget.dispenseData!['description'].toString().isNotEmpty) ||
            (widget.dispenseData!['usage'] != null &&
                widget.dispenseData!['usage'].toString().isNotEmpty) ||
            (widget.dispenseData!['manufacturer'] != null &&
                widget.dispenseData!['manufacturer'].toString().isNotEmpty));

    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        title: Text(
          widget.dispenseData != null ? 'Thêm thuốc từ QR' : 'Thêm thuốc',
          style: TextStyle(color: Colors.white),
        ),
        backgroundColor: const Color(0xFF284C7B),
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Banner thông tin nhà sản xuất nếu có
            if (hasManufacturerInfo) ...[
              _buildManufacturerInfoCard(),
              const SizedBox(height: 20),
            ],
            _buildSection(
              title: '💊 Thông tin thuốc',
              children: [
                TextFormField(
                  controller: _drugNameController,
                  readOnly: _fromQR,
                  decoration: const InputDecoration(
                    labelText: 'Tên thuốc *',
                    border: OutlineInputBorder(),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Vui lòng nhập tên thuốc';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _dosageController,
                  readOnly: _hasPharmacyInstructions,
                  decoration: const InputDecoration(
                    labelText: 'Liều lượng *',
                    hintText: 'VD: 1 viên, 2 viên, 5ml...',
                    border: OutlineInputBorder(),
                  ),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return 'Vui lòng nhập liều lượng';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _batchNumberController,
                  readOnly: _fromQR,
                  decoration: const InputDecoration(
                    labelText: 'Số lô (tùy chọn)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            _buildSection(
              title: '⏰ Lịch uống',
              children: [
                DropdownButtonFormField<int>(
                  value: _frequency,
                  decoration: const InputDecoration(
                    labelText: 'Số lần/ngày',
                    border: OutlineInputBorder(),
                  ),
                  items: [1, 2, 3, 4].map((freq) {
                    return DropdownMenuItem(
                      value: freq,
                      child: Text('$freq lần/ngày'),
                    );
                  }).toList(),
                  onChanged: _hasPharmacyInstructions
                      ? null
                      : (value) {
                          setState(() {
                            _frequency = value!;
                            _updateReminderTimes();
                          });
                        },
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  value: _mealRelation,
                  decoration: const InputDecoration(
                    labelText: 'Thời điểm uống',
                    border: OutlineInputBorder(),
                  ),
                  items: const [
                    DropdownMenuItem(
                      value: 'BEFORE',
                      child: Text('Trước bữa ăn'),
                    ),
                    DropdownMenuItem(value: 'AFTER', child: Text('Sau bữa ăn')),
                    DropdownMenuItem(
                      value: 'WITH',
                      child: Text('Trong bữa ăn'),
                    ),
                    DropdownMenuItem(
                      value: 'ANY',
                      child: Text('Bất kỳ lúc nào'),
                    ),
                  ],
                  onChanged: _hasPharmacyInstructions
                      ? null
                      : (value) {
                          setState(() {
                            _mealRelation = value!;
                          });
                        },
                ),
                const SizedBox(height: 16),
                const Text(
                  'Giờ uống:',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _reminderTimes.asMap().entries.map((entry) {
                    final index = entry.key;
                    final time = entry.value;
                    return InkWell(
                      onTap: _hasPharmacyInstructions
                          ? null
                          : () => _pickTime(index),
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 12,
                        ),
                        decoration: BoxDecoration(
                          color: _hasPharmacyInstructions
                              ? Colors.grey.withOpacity(0.1)
                              : const Color(0xFF10B981).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: _hasPharmacyInstructions
                                ? Colors.grey.shade400
                                : const Color(0xFF10B981),
                          ),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.access_time,
                              size: 16,
                              color: _hasPharmacyInstructions
                                  ? Colors.grey.shade600
                                  : const Color(0xFF10B981),
                            ),
                            const SizedBox(width: 6),
                            Text(
                              time.format(context),
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                                color: _hasPharmacyInstructions
                                    ? Colors.grey.shade700
                                    : const Color(0xFF10B981),
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ],
            ),
            const SizedBox(height: 24),
            _buildSection(
              title: '📅 Thời gian',
              children: [
                ListTile(
                  title: const Text('Ngày bắt đầu'),
                  subtitle: Text(_formatDate(_startDate)),
                  leading: const Icon(Icons.calendar_today),
                  onTap: _hasPharmacyInstructions
                      ? null
                      : () => _pickDate(isStartDate: true),
                ),
                ListTile(
                  title: const Text('Ngày kết thúc'),
                  subtitle: Text(_formatDate(_endDate)),
                  leading: const Icon(Icons.event),
                  onTap: _hasPharmacyInstructions
                      ? null
                      : () => _pickDate(isStartDate: false),
                ),
                if (_expiryDate != null)
                  ListTile(
                    title: const Text('Hạn sử dụng'),
                    subtitle: Text(_formatDate(_expiryDate!)),
                    leading: const Icon(Icons.warning_amber),
                  ),
              ],
            ),
            const SizedBox(height: 24),
            _buildSection(
              title: '🏪 Thông tin khác',
              children: [
                TextFormField(
                  controller: _pharmacyNameController,
                  decoration: const InputDecoration(
                    labelText: 'Nơi mua (tùy chọn)',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: _isSubmitting ? null : _submitForm,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF10B981),
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: _isSubmitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                      ),
                    )
                  : const Text(
                      'Thêm thuốc',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Widget _buildManufacturerInfoCard() {
    final data = widget.dispenseData!;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF284C7B), Color(0xFF3D6CA5)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.verified, color: Colors.white, size: 20),
              const SizedBox(width: 8),
              const Text(
                'Thông tin từ nhà sản xuất',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                ),
              ),
            ],
          ),
          if (data['manufacturer'] != null &&
              data['manufacturer'].toString().isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.factory, color: Colors.white70, size: 15),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Nhà SX: ${data['manufacturer']}',
                    style: const TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                ),
              ],
            ),
          ],
          if (data['activeIngredient'] != null &&
              data['activeIngredient'].toString().isNotEmpty) ...[
            const SizedBox(height: 6),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.science, color: Colors.white70, size: 15),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Hoạt chất: ${data['activeIngredient']}',
                    style: const TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                ),
              ],
            ),
          ],
          if (data['storageConditions'] != null &&
              data['storageConditions'].toString().isNotEmpty) ...[
            const SizedBox(height: 6),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.ac_unit, color: Colors.white70, size: 15),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Bảo quản: ${data['storageConditions']}',
                    style: const TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                ),
              ],
            ),
          ],
          if (data['usage'] != null && data['usage'].toString().isNotEmpty) ...[
            const Divider(color: Colors.white24, height: 20),
            Text(
              '💊 Công dụng',
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              data['usage'].toString(),
              style: const TextStyle(
                color: Colors.white70,
                fontSize: 12,
                height: 1.4,
              ),
            ),
          ],
          if (data['sideEffects'] != null &&
              data['sideEffects'].toString().isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              '⚠️ Tác dụng phụ',
              style: const TextStyle(
                color: Colors.orangeAccent,
                fontWeight: FontWeight.bold,
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              data['sideEffects'].toString(),
              style: const TextStyle(
                color: Colors.white70,
                fontSize: 12,
                height: 1.4,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildSection({
    required String title,
    required List<Widget> children,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Color(0xFF284C7B),
          ),
        ),
        const SizedBox(height: 16),
        ...children,
      ],
    );
  }

  void _updateReminderTimes() {
    final defaultTimes = {
      1: [const TimeOfDay(hour: 20, minute: 0)],
      2: [
        const TimeOfDay(hour: 8, minute: 0),
        const TimeOfDay(hour: 20, minute: 0),
      ],
      3: [
        const TimeOfDay(hour: 8, minute: 0),
        const TimeOfDay(hour: 12, minute: 0),
        const TimeOfDay(hour: 20, minute: 0),
      ],
      4: [
        const TimeOfDay(hour: 7, minute: 0),
        const TimeOfDay(hour: 12, minute: 0),
        const TimeOfDay(hour: 17, minute: 0),
        const TimeOfDay(hour: 22, minute: 0),
      ],
    };

    setState(() {
      _reminderTimes = defaultTimes[_frequency]!;
    });
  }

  Future<void> _pickTime(int index) async {
    final time = await showTimePicker(
      context: context,
      initialTime: _reminderTimes[index],
    );
    if (time != null) {
      setState(() {
        _reminderTimes[index] = time;
      });
    }
  }

  Future<void> _pickDate({required bool isStartDate}) async {
    final date = await showDatePicker(
      context: context,
      initialDate: isStartDate ? _startDate : _endDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (date != null) {
      setState(() {
        if (isStartDate) {
          _startDate = date;
          if (_endDate.isBefore(_startDate)) {
            _endDate = _startDate.add(const Duration(days: 7));
          }
        } else {
          _endDate = date;
        }
      });
    }
  }

  String _formatDate(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }
}
