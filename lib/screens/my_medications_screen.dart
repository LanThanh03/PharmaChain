import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../services/medication_service.dart';
import '../models/medication_models.dart';
import 'medication_detail_screen.dart';
import 'add_medication_screen.dart';

/// Screen to display and manage user's medications
class MyMedicationsScreen extends StatefulWidget {
  final int userId;

  const MyMedicationsScreen({Key? key, required this.userId}) : super(key: key);

  @override
  MyMedicationsScreenState createState() => MyMedicationsScreenState();
}

class MyMedicationsScreenState extends State<MyMedicationsScreen>
    with SingleTickerProviderStateMixin, AutomaticKeepAliveClientMixin {
  final MedicationService _medicationService = MedicationService();
  late TabController _tabController;

  List<MedicationRecord> _activeMedications = [];
  List<MedicationRecord> _historyMedications = [];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    print('📋 MyMedicationsScreen initialized with userId: ${widget.userId}');
    loadMedications();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> loadMedications() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      print('📋 Loading medications for userId: ${widget.userId}');

      // Load active medications
      final activeResult = await _medicationService.getActiveMedications(
        widget.userId,
      );

      print(
        '📋 Active medications result: success=${activeResult['success']}, data count=${(activeResult['data'] as List?)?.length ?? 0}',
      );

      if (activeResult['success']) {
        final dataList = activeResult['data'] as List;
        print('📋 Active medications data: $dataList');
        setState(() {
          _activeMedications = dataList
              .map((json) => MedicationRecord.fromJson(json))
              .toList();
        });
      } else {
        setState(() {
          _errorMessage =
              activeResult['message'] != null &&
                  activeResult['message'].toString().isNotEmpty
              ? activeResult['message']
              : 'Không thể tải danh sách thuốc';
        });
        return; // Dừng nếu có lỗi
      }

      // Load history
      final historyResult = await _medicationService.getMedicationHistory(
        widget.userId,
      );
      if (historyResult['success']) {
        setState(() {
          _historyMedications = (historyResult['data'] as List)
              .map((json) => MedicationRecord.fromJson(json))
              .toList();
        });
      } else {
        setState(() {
          _errorMessage =
              historyResult['message'] != null &&
                  historyResult['message'].toString().isNotEmpty
              ? historyResult['message']
              : 'Không thể tải lịch sử thuốc';
        });
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

  @override
  Widget build(BuildContext context) {
    super.build(context); // Required for AutomaticKeepAliveClientMixin
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        title: const Text(
          'Thuốc của tôi',
          style: TextStyle(color: Colors.white),
        ),
        backgroundColor: const Color(0xFF284C7B),
        elevation: 0,
        foregroundColor: Colors.white,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(70),
          child: Container(
            margin: const EdgeInsets.fromLTRB(16, 8, 16, 16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.12),
              borderRadius: BorderRadius.circular(30),
            ),
            padding: const EdgeInsets.all(4),
            child: TabBar(
              controller: _tabController,
              indicator: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(26),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.1),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              indicatorSize: TabBarIndicatorSize.tab,
              labelColor: const Color(0xFF284C7B),
              unselectedLabelColor: Colors.white.withOpacity(0.8),
              dividerColor: Colors.transparent,
              labelStyle: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
              tabs: const [
                Tab(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.medical_services, size: 20),
                      SizedBox(width: 8),
                      Text('Đang dùng'),
                    ],
                  ),
                ),
                Tab(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.history, size: 20),
                      SizedBox(width: 8),
                      Text('Lịch sử'),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null
          ? _buildErrorView()
          : TabBarView(
              controller: _tabController,
              children: [_buildActiveMedicationsTab(), _buildHistoryTab()],
            ),
      floatingActionButton: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(30),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF10B981).withOpacity(0.3),
              blurRadius: 12,
              offset: const Offset(0, 6),
            ),
          ],
        ),
        child: FloatingActionButton.extended(
          heroTag: 'add_medication',
          onPressed: () async {
            final result = await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) =>
                    AddMedicationScreen(userId: widget.userId),
              ),
            );
            if (result == true) {
              loadMedications();
            }
          },
          icon: const Icon(Icons.add, color: Colors.white),
          label: const Text(
            'Thêm thuốc',
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
          ),
          backgroundColor: const Color(0xFF10B981),
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(30),
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
          ElevatedButton(
            onPressed: loadMedications,
            child: const Text('Thử lại'),
          ),
        ],
      ),
    );
  }

  Widget _buildActiveMedicationsTab() {
    if (_activeMedications.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: const Color(0xFF284C7B).withOpacity(0.05),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.medical_services_outlined,
                size: 64,
                color: Color(0xFF284C7B),
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Chưa có thuốc nào',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: const Color(0xFF284C7B),
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Quét QR hoặc thêm thuốc thủ công',
              style: TextStyle(fontSize: 14, color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: loadMedications,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _activeMedications.length,
        itemBuilder: (context, index) {
          return _buildMedicationCard(_activeMedications[index]);
        },
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_historyMedications.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.grey.withOpacity(0.08),
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.history_rounded,
                size: 64,
                color: Colors.grey[400],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Chưa có lịch sử',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.grey[600],
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: _historyMedications.length,
      itemBuilder: (context, index) {
        return _buildMedicationCard(
          _historyMedications[index],
          isHistory: true,
        );
      },
    );
  }

  Widget _buildMedicationCard(
    MedicationRecord medication, {
    bool isHistory = false,
  }) {
    final daysRemaining = medication.getDaysRemaining();
    final isExpired = medication.isExpired();

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () async {
            final result = await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) =>
                    MedicationDetailScreen(medication: medication),
              ),
            );
            if (result == true) {
              loadMedications();
            }
          },
          borderRadius: BorderRadius.circular(16),
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: isHistory
                    ? Colors.grey.withOpacity(0.2)
                    : const Color(0xFF10B981).withOpacity(0.3),
                width: 1,
              ),
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: isHistory
                    ? [Colors.white, Colors.grey[50]!]
                    : [Colors.white, const Color(0xFFF0FDF4)],
              ),
            ),
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (medication.imageUrl != null &&
                        medication.imageUrl!.isNotEmpty)
                      Hero(
                        tag: 'med_img_${medication.id}',
                        child: Container(
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(12),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withOpacity(0.05),
                                blurRadius: 4,
                                offset: const Offset(0, 2),
                              ),
                            ],
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(12),
                            child: CachedNetworkImage(
                              imageUrl: medication.imageUrl!,
                              width: 60,
                              height: 60,
                              fit: BoxFit.cover,
                              placeholder: (context, url) => Container(
                                color: Colors.grey[100],
                                child: const Center(
                                  child: SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Color(0xFF10B981),
                                    ),
                                  ),
                                ),
                              ),
                              errorWidget: (context, url, error) => Container(
                                color: const Color(0xFF10B981).withOpacity(0.1),
                                child: const Icon(
                                  Icons.medication,
                                  color: Color(0xFF10B981),
                                  size: 28,
                                ),
                              ),
                            ),
                          ),
                        ),
                      )
                    else
                      Hero(
                        tag: 'med_icon_${medication.id}',
                        child: Container(
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF34D399), Color(0xFF10B981)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(14),
                            boxShadow: [
                              BoxShadow(
                                color: const Color(0xFF10B981).withOpacity(0.3),
                                blurRadius: 8,
                                offset: const Offset(0, 3),
                              ),
                            ],
                          ),
                          child: const Icon(
                            Icons.medication,
                            color: Colors.white,
                            size: 30,
                          ),
                        ),
                      ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            medication.drugName,
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.w800,
                              color: Color(0xFF1E3A5F),
                              letterSpacing: -0.5,
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 6),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFF284C7B).withOpacity(0.08),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              medication.dosage,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: Color(0xFF284C7B),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (isExpired)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFEE2E2),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: const Color(0xFFFCA5A5)),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.warning_amber_rounded,
                              color: Color(0xFFDC2626),
                              size: 14,
                            ),
                            SizedBox(width: 4),
                            Text(
                              'Hết hạn',
                              style: TextStyle(
                                color: Color(0xFFDC2626),
                                fontSize: 12,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                Divider(color: Colors.grey.withOpacity(0.2), height: 1),
                const SizedBox(height: 12),

                // Details Grid
                Row(
                  children: [
                    Expanded(
                      child: _buildInfoItem(
                        Icons.sync,
                        '${medication.frequency} lần/ngày',
                        const Color(0xFF3B82F6),
                      ),
                    ),
                    Expanded(
                      child: _buildInfoItem(
                        Icons.restaurant,
                        medication.getMealRelationText(),
                        const Color(0xFFF59E0B),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: _buildInfoItem(
                        Icons.access_time_filled,
                        medication.getReminderTimesList().join(', '),
                        const Color(0xFF10B981),
                      ),
                    ),
                    if (medication.pharmacyName != null)
                      Expanded(
                        child: _buildInfoItem(
                          Icons.local_pharmacy,
                          medication.pharmacyName!,
                          const Color(0xFF8B5CF6),
                        ),
                      )
                    else
                      const Expanded(child: SizedBox()),
                  ],
                ),

                if (!isHistory) ...[
                  const SizedBox(height: 12),
                  Divider(color: Colors.grey.withOpacity(0.2), height: 1),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: const Color(0xFF284C7B).withOpacity(0.1),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: const Icon(
                          Icons.calendar_month_rounded,
                          size: 16,
                          color: Color(0xFF284C7B),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        daysRemaining > 0
                            ? 'Còn $daysRemaining ngày'
                            : 'Đã hết liệu trình',
                        style: TextStyle(
                          fontSize: 14,
                          color: daysRemaining > 0
                              ? const Color(0xFF10B981)
                              : Colors.orange,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildInfoItem(IconData icon, String text, Color color) {
    return Row(
      children: [
        Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(6),
          ),
          child: Icon(icon, size: 14, color: color),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Color(0xFF4B5563),
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }
}
