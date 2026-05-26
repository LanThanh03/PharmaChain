import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/product_verification_service.dart';
import '../services/auth_service.dart';
import '../services/medication_service.dart';
import '../models/medication_models.dart';
import 'add_medication_screen.dart';

/// Màn hình hiển thị kết quả xác thực với timeline hành trình
class ProductVerificationResultScreen extends StatefulWidget {
  final ProductVerificationResult result;

  const ProductVerificationResultScreen({super.key, required this.result});

  @override
  State<ProductVerificationResultScreen> createState() =>
      _ProductVerificationResultScreenState();
}

class _ProductVerificationResultScreenState
    extends State<ProductVerificationResultScreen> {
  // bool _showReportDialog = false; // Unused for now

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        title: const Text(
          'Kết quả xác thực',
          style: TextStyle(color: Colors.white),
        ),
        backgroundColor: const Color(0xFF284C7B),
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          if (!widget.result.isAuthentic)
            IconButton(
              icon: const Icon(Icons.report_problem),
              onPressed: _showReportForm,
              tooltip: 'Báo cáo sản phẩm giả',
            ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            _buildStatusHeader(),
            if (widget.result.isAuthentic &&
                widget.result.productInfo != null) ...[
              _buildProductInfo(),
              _buildJourneyTimeline(),
              _buildPharmacyNotes(),
              _buildAdditionalInfo(),
            ] else ...[
              _buildWarningInfo(),
            ],
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomBar(),
    );
  }

  Widget _buildStatusHeader() {
    Color statusColor;
    IconData statusIcon;
    String statusTitle;
    String statusMessage;

    if (widget.result.isAuthentic) {
      final pInfo = widget.result.productInfo;
      if (pInfo != null && pInfo.isExpired) {
        statusColor = Colors.orange;
        statusIcon = Icons.event_busy;
        statusTitle = 'SẢN PHẨM HẾT HẠN';
        statusMessage = 'Sản phẩm chính hãng nhưng đã hết hạn sử dụng';
      } else if (pInfo != null && pInfo.isExpiringSoon) {
        statusColor = Colors.orange;
        statusIcon = Icons.warning_amber_rounded;
        statusTitle = 'SẢN PHẨM SẮP HẾT HẠN';
        statusMessage =
            'Sản phẩm chính hãng nhưng sắp hết hạn, xin lưu ý trước khi dùng';
      } else {
        statusColor = Colors.green;
        statusIcon = Icons.verified;
        statusTitle = 'SẢN PHẨM CHÍNH HÃNG';
        statusMessage = 'Đã xác thực qua blockchain';
      }
    } else if (widget.result.isCounterfeit) {
      statusColor = Colors.red;
      statusIcon = Icons.dangerous;
      statusTitle = 'CẢNH BÁO: HÀNG GIẢ';
      statusMessage = 'Sản phẩm không có trong hệ thống';
    } else if (widget.result.isExpired) {
      statusColor = Colors.orange;
      statusIcon = Icons.event_busy;
      statusTitle = 'SẢN PHẨM HẾT HẠN';
      statusMessage = 'Không nên sử dụng sản phẩm này';
    } else if (widget.result.isRecalled) {
      statusColor = Colors.red;
      statusIcon = Icons.cancel;
      statusTitle = 'SẢN PHẨM ĐÃ BỊ THU HỒI';
      statusMessage = 'Vui lòng không sử dụng';
    } else {
      statusColor = Colors.grey;
      statusIcon = Icons.help_outline;
      statusTitle = 'KHÔNG XÁC ĐỊNH';
      statusMessage = widget.result.message;
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(32),
      decoration: BoxDecoration(
        color: statusColor.withOpacity(0.1),
        border: Border(
          bottom: BorderSide(color: statusColor.withOpacity(0.3), width: 2),
        ),
      ),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: statusColor,
              shape: BoxShape.circle,
            ),
            child: Icon(statusIcon, size: 64, color: Colors.white),
          ),
          const SizedBox(height: 16),
          Text(
            statusTitle,
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: statusColor,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 8),
          Text(
            statusMessage,
            style: TextStyle(fontSize: 16, color: Colors.grey[700]),
            textAlign: TextAlign.center,
          ),
          if (widget.result.blockchainVerified)
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.link, size: 16, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    'Đã xác minh trên Blockchain',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey[600],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildProductInfo() {
    final product = widget.result.productInfo!;
    final dateFormat = DateFormat('dd/MM/yyyy');

    // Badge màu sắc theo trạng thái
    Color saleColor;
    IconData saleIcon;
    if (product.isSold) {
      saleColor = Colors.green;
      saleIcon = Icons.sell;
    } else if (product.currentStatus == 'AT_PHARMACY') {
      saleColor = Colors.blue;
      saleIcon = Icons.local_pharmacy;
    } else if (product.currentStatus == 'RECALLED') {
      saleColor = Colors.red;
      saleIcon = Icons.cancel;
    } else {
      saleColor = Colors.orange;
      saleIcon = Icons.inventory_2;
    }

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(20),
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
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'THÔNG TIN SẢN PHẨM',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              // Badge trạng thái bán hàng
              Flexible(
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: saleColor.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: saleColor.withOpacity(0.5)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(saleIcon, size: 14, color: saleColor),
                      const SizedBox(width: 4),
                      Flexible(
                        child: Text(
                          product.saleStatusText,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: saleColor,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          if (product.imageUrl != null && product.imageUrl!.isNotEmpty) ...[
            const SizedBox(height: 16),
            Center(
              child: ClipRRect(
                borderRadius: BorderRadius.circular(12),
                child: Image.network(
                  product.imageUrl!,
                  height: 150,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => const Icon(
                    Icons.image_not_supported,
                    size: 50,
                    color: Colors.grey,
                  ),
                ),
              ),
            ),
          ],
          const Divider(height: 24),
          if (product.itemCode.isNotEmpty)
            _buildInfoRow(
              'Mã sản phẩm',
              product.itemCode,
              Icons.qr_code_scanner,
            ),
          _buildInfoRow('Tên sản phẩm', product.name, Icons.medical_services),
          if (product.activeIngredient != null &&
              product.activeIngredient!.isNotEmpty)
            _buildInfoRow(
              'Hoạt chất',
              product.activeIngredient!,
              Icons.science,
            ),
          if (product.dosage != null && product.dosage!.isNotEmpty)
            _buildInfoRow('Liều dùng', product.dosage!, Icons.medication),
          if (product.packaging != null && product.packaging!.isNotEmpty)
            _buildInfoRow(
              'Quy cách đóng gói',
              product.packaging!,
              Icons.inventory,
            ),
          if (product.registrationNumber != null &&
              product.registrationNumber!.isNotEmpty)
            _buildInfoRow(
              'Số đăng ký',
              product.registrationNumber!,
              Icons.badge,
            ),
          _buildInfoRow('Nhà sản xuất', product.manufacturer, Icons.factory),
          _buildInfoRow('Số lô', product.batchNumber, Icons.qr_code),
          _buildInfoRow(
            'Ngày sản xuất',
            dateFormat.format(product.manufactureDate),
            Icons.calendar_today,
          ),
          _buildInfoRow(
            'Hạn sử dụng',
            product.daysUntilExpiry >= 0
                ? '${dateFormat.format(product.expiryDate)} (còn ${product.daysUntilExpiry} ngày)'
                : '${dateFormat.format(product.expiryDate)} (đã quá hạn ${product.daysUntilExpiry.abs()} ngày)',
            Icons.event,
            valueColor: product.isExpired
                ? Colors.red
                : (product.isExpiringSoon ? Colors.orange : Colors.green),
          ),
          if (product.storageConditions != null &&
              product.storageConditions!.isNotEmpty)
            _buildInfoRow(
              'Bảo quản',
              product.storageConditions!,
              Icons.storage,
            ),
          if (product.isSold && product.soldAt != null)
            _buildInfoRow(
              'Ngày bán',
              DateFormat('dd/MM/yyyy HH:mm').format(product.soldAt!),
              Icons.shopping_cart,
              valueColor: Colors.green,
            ),
          // Thông tin mô tả từ nhà sản xuất
          if (product.description != null &&
              product.description!.isNotEmpty) ...[
            const Divider(height: 24),
            const Text(
              'MÔ TẢ SẢN PHẨM',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: Colors.black54,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              product.description!,
              style: const TextStyle(
                fontSize: 14,
                color: Colors.black87,
                height: 1.5,
              ),
            ),
          ],
          if (product.usage != null && product.usage!.isNotEmpty) ...[
            const SizedBox(height: 12),
            _buildExpandableSection('💊 Công dụng', product.usage!),
          ],
          if (product.sideEffects != null &&
              product.sideEffects!.isNotEmpty) ...[
            const SizedBox(height: 8),
            _buildExpandableSection('⚠️ Tác dụng phụ', product.sideEffects!),
          ],
          if (product.contraindications != null &&
              product.contraindications!.isNotEmpty) ...[
            const SizedBox(height: 8),
            _buildExpandableSection(
              '🚫 Chống chỉ định',
              product.contraindications!,
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildExpandableSection(String title, String content) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            content,
            style: const TextStyle(
              fontSize: 13,
              color: Colors.black87,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(
    String label,
    String value,
    IconData icon, {
    Color? valueColor,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 20, color: Colors.grey[600]),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey[600],
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: TextStyle(
                    fontSize: 14,
                    color: valueColor ?? Colors.black87,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildJourneyTimeline() {
    if (widget.result.ownershipHistory.isEmpty) {
      return const SizedBox.shrink();
    }

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      padding: const EdgeInsets.all(20),
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
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.timeline, color: Colors.blue),
              const SizedBox(width: 8),
              const Text(
                'HÀNH TRÌNH SẢN PHẨM',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: Colors.blue.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '${widget.result.journeySteps} bước',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: Colors.blue,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: widget.result.ownershipHistory.length,
            itemBuilder: (context, index) {
              final step = widget.result.ownershipHistory[index];
              final isFirst = index == 0;
              final isLast = index == widget.result.ownershipHistory.length - 1;

              return _buildTimelineItem(step, isFirst, isLast);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildTimelineItem(JourneyStep step, bool isFirst, bool isLast) {
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm');

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Timeline indicator
        Column(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: step.verified ? Colors.green : Colors.grey,
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(step.icon, style: const TextStyle(fontSize: 20)),
              ),
            ),
            if (!isLast)
              Container(
                width: 2,
                height: step.txHash != null ? 80 : 60,
                color: Colors.grey[300],
              ),
          ],
        ),
        const SizedBox(width: 16),
        // Content
        Expanded(
          child: Container(
            padding: const EdgeInsets.only(bottom: 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  step.stage,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 4),
                if (step.company != null)
                  Text(
                    step.company!,
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey[700],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                if (step.address != null)
                  Text(
                    step.address!,
                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                  ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: Colors.grey[500]),
                    const SizedBox(width: 4),
                    Text(
                      dateFormat.format(step.timestamp),
                      style: TextStyle(fontSize: 12, color: Colors.grey[500]),
                    ),
                    const SizedBox(width: 12),
                    if (step.verified)
                      Row(
                        children: [
                          Icon(
                            Icons.check_circle,
                            size: 14,
                            color: Colors.green[600],
                          ),
                          const SizedBox(width: 4),
                          Text(
                            'Đã xác minh',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.green[600],
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                  ],
                ),
                // Show blockchain transaction hash if available
                if (step.txHash != null && step.txHash!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Row(
                      children: [
                        Icon(Icons.link, size: 12, color: Colors.blue[400]),
                        const SizedBox(width: 4),
                        Expanded(
                          child: Text(
                            'TX: ${step.txHash!.length > 20 ? '${step.txHash!.substring(0, 10)}...${step.txHash!.substring(step.txHash!.length - 8)}' : step.txHash!}',
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.blue[400],
                              fontFamily: 'monospace',
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPharmacyNotes() {
    final instructions = widget.result.pharmacyInstructions;

    if (instructions == null) {
      return const SizedBox.shrink();
    }

    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.amber.withOpacity(0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.amber.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.medical_information, color: Colors.amber, size: 20),
              SizedBox(width: 8),
              Text(
                'Hướng dẫn sử dụng từ Dược sĩ',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.amber,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Dosage & Frequency
          if (instructions.dosage != null || instructions.frequency != null)
            _buildInstructionRow(
              Icons.medication,
              'Liều dùng',
              '${instructions.dosage ?? ''} - ${instructions.frequency ?? 0} lần/ngày',
            ),

          // Meal relation
          if (instructions.mealRelationDisplay != null)
            _buildInstructionRow(
              Icons.restaurant,
              'Thời điểm',
              instructions.mealRelationDisplay!,
            ),

          // Specific times
          if (instructions.specificTimes != null &&
              instructions.specificTimes!.isNotEmpty)
            _buildInstructionRow(
              Icons.access_time,
              'Giờ uống',
              instructions.specificTimes!,
            ),

          // Duration
          if (instructions.durationDays != null)
            _buildInstructionRow(
              Icons.calendar_today,
              'Thời gian',
              '${instructions.durationDays} ngày',
            ),

          // Special notes
          if (instructions.specialNotes != null &&
              instructions.specialNotes!.isNotEmpty) ...[
            const SizedBox(height: 12),
            const Divider(),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.note_alt, size: 18, color: Colors.amber),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Ghi chú đặc biệt',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        instructions.specialNotes!,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.black87,
                          height: 1.5,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],

          // Pharmacist info
          if (instructions.pharmacistName != null) ...[
            const SizedBox(height: 12),
            Row(
              children: [
                const Icon(Icons.person, size: 16, color: Colors.grey),
                const SizedBox(width: 4),
                Text(
                  'Dược sĩ: ${instructions.pharmacistName}',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey[600],
                    fontStyle: FontStyle.italic,
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInstructionRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 18, color: Colors.amber[700]),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey[600],
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.black87,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAdditionalInfo() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.blue.withOpacity(0.05),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.blue.withOpacity(0.2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.info_outline, color: Colors.blue, size: 20),
              SizedBox(width: 8),
              Text(
                'Thông tin thêm',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Colors.blue,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _buildAdditionalRow('Số lần quét', '${widget.result.scanCount} lần'),
          if (widget.result.lastScanned != null)
            _buildAdditionalRow(
              'Lần quét cuối',
              DateFormat('dd/MM/yyyy HH:mm').format(widget.result.lastScanned!),
            ),
          _buildAdditionalRow(
            'Trạng thái thu hồi',
            widget.result.recallStatus == 'NOT_RECALLED'
                ? 'Không bị thu hồi'
                : widget.result.recallStatus,
          ),
        ],
      ),
    );
  }

  Widget _buildAdditionalRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(fontSize: 13, color: Colors.grey[700])),
          Text(
            value,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWarningInfo() {
    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.red.withOpacity(0.05),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.red.withOpacity(0.3), width: 2),
      ),
      child: Column(
        children: [
          const Icon(Icons.warning_amber_rounded, size: 64, color: Colors.red),
          const SizedBox(height: 16),
          const Text(
            'KHUYẾN NGHỊ',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.red,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            widget.result.message,
            style: const TextStyle(fontSize: 15, height: 1.5),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 20),
          const Text(
            '• Không sử dụng sản phẩm này\n'
            '• Báo cáo cho nhà thuốc nơi mua\n'
            '• Liên hệ cơ quan y tế địa phương\n'
            '• Báo cáo qua ứng dụng này',
            style: TextStyle(fontSize: 14, height: 1.8),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Add to My Medicines button (only for SOLD products)
            if (widget.result.isAuthentic &&
                widget.result.productInfo != null &&
                widget.result.productInfo!.canAddToMedicineCabinet)
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _addToMyMedicines,
                  icon: const Icon(Icons.add_circle_outline),
                  label: const Text('Thêm vào thuốc của tôi'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                ),
              ),
            // Hiển thị thông báo nếu chưa bán
            if (widget.result.isAuthentic &&
                widget.result.productInfo != null &&
                !widget.result.productInfo!.canAddToMedicineCabinet)
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  vertical: 12,
                  horizontal: 16,
                ),
                decoration: BoxDecoration(
                  color: Colors.blue.withOpacity(0.08),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.blue.withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    Icon(Icons.info_outline, color: Colors.blue[700], size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Chỉ thuốc đã bán mới có thể thêm vào tủ thuốc cá nhân.',
                        style: TextStyle(color: Colors.blue[700], fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
            if (widget.result.isAuthentic && widget.result.productInfo != null)
              const SizedBox(height: 12),
            Row(
              children: [
                if (!widget.result.isAuthentic)
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _showReportForm,
                      icon: const Icon(Icons.report_problem),
                      label: const Text('Báo cáo sản phẩm giả'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 16),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                if (!widget.result.isAuthentic) const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.qr_code_scanner),
                    label: const Text('Quét sản phẩm khác'),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _addToMyMedicines() async {
    final product = widget.result.productInfo!;

    // Lấy user hiện tại từ singleton (đồng bộ)
    final user = AuthService.instance.currentUser;

    if (!mounted) return;

    if (user == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng đăng nhập để thêm thuốc'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    final userId = user.numericId;

    // Check if the medication is already in the cabinet
    try {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(child: CircularProgressIndicator()),
      );

      final medicationService = MedicationService();
      final response = await medicationService.getActiveMedications(userId);

      if (mounted) {
        Navigator.pop(context); // Close loading dialog
      }

      if (response['success'] == true) {
        final activeMedications = (response['data'] as List)
            .map((item) => MedicationRecord.fromJson(item))
            .toList();

        final isDuplicate = activeMedications.any(
          (med) =>
              med.batchNumber == product.itemCode ||
              med.batchNumber == product.batchNumber,
        );

        if (isDuplicate) {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Thuốc này đã có trong danh sách Đang uống của bạn',
              ),
              backgroundColor: Colors.orange,
            ),
          );
          return;
        }
      }
    } catch (e) {
      if (mounted) {
        Navigator.pop(context); // Close loading dialog on error
      }
      print('Error checking duplicates: $e');
      // Continue anyway if check fails
    }

    if (!mounted) return;

    // Chuẩn bị dữ liệu từ thông tin scan, bao gồm thông tin nhà SX
    final dispenseData = <String, dynamic>{
      'drugName': product.name,
      'batchNumber': product.batchNumber,
      'expiryDate': product.expiryDate.toIso8601String().split('T')[0],
      'dosage': product.dosage ?? '',
      'pharmacyName': '',
      // Truyền thêm thông tin mô tả từ nhà sản xuất
      'description': product.description,
      'usage': product.usage,
      'sideEffects': product.sideEffects,
      'storageConditions': product.storageConditions,
      'activeIngredient': product.activeIngredient,
      'manufacturer': product.manufacturer,
      'itemCode': product.itemCode,
    };

    // Nếu có hướng dẫn dược sĩ, ưu tiên dữ liệu đó
    if (widget.result.pharmacyInstructions != null) {
      final instr = widget.result.pharmacyInstructions!;
      dispenseData['dosage'] = instr.dosage ?? product.dosage ?? '';
      dispenseData['frequency'] = instr.frequency ?? 3;
      dispenseData['mealRelation'] = instr.mealRelation ?? 'AFTER';
      dispenseData['durationDays'] = instr.durationDays;
      dispenseData['specificTimes'] = instr.specificTimes;
      dispenseData['hasPharmacyInstructions'] = true;
    }

    if (!mounted) return;
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) =>
            AddMedicationScreen(userId: userId, dispenseData: dispenseData),
      ),
    );
  }

  void _showReportForm() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => _ReportForm(
        itemCode: widget.result.productInfo?.itemCode ?? 'UNKNOWN',
      ),
    );
  }
}

/// Form báo cáo sản phẩm giả
class _ReportForm extends StatefulWidget {
  final String itemCode;

  const _ReportForm({required this.itemCode});

  @override
  State<_ReportForm> createState() => _ReportFormState();
}

class _ReportFormState extends State<_ReportForm> {
  final _formKey = GlobalKey<FormState>();
  final _descriptionController = TextEditingController();
  final _emailController = TextEditingController();
  String _selectedReason = 'COUNTERFEIT';
  bool _isSubmitting = false;

  @override
  void dispose() {
    _descriptionController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Báo cáo sản phẩm',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                'Mã sản phẩm: ${widget.itemCode}',
                style: TextStyle(color: Colors.grey[600]),
              ),
              const SizedBox(height: 20),
              DropdownButtonFormField<String>(
                value: _selectedReason,
                decoration: const InputDecoration(
                  labelText: 'Lý do',
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(
                    value: 'COUNTERFEIT',
                    child: Text('Hàng giả'),
                  ),
                  DropdownMenuItem(value: 'DAMAGED', child: Text('Hư hỏng')),
                  DropdownMenuItem(
                    value: 'SUSPICIOUS',
                    child: Text('Đáng ngờ'),
                  ),
                  DropdownMenuItem(value: 'OTHER', child: Text('Khác')),
                ],
                onChanged: (value) {
                  setState(() {
                    _selectedReason = value!;
                  });
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _descriptionController,
                decoration: const InputDecoration(
                  labelText: 'Mô tả chi tiết',
                  border: OutlineInputBorder(),
                ),
                maxLines: 3,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Vui lòng mô tả chi tiết';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _emailController,
                decoration: const InputDecoration(
                  labelText: 'Email liên hệ (tùy chọn)',
                  border: OutlineInputBorder(),
                ),
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _isSubmitting ? null : _submitReport,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    backgroundColor: Colors.red,
                  ),
                  child: _isSubmitting
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Text('Gửi báo cáo'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _submitReport() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isSubmitting = true;
    });

    try {
      final success = await ProductVerificationService.instance
          .reportSuspiciousProduct(
            itemCode: widget.itemCode,
            reason: _selectedReason,
            description: _descriptionController.text,
            reporterEmail: _emailController.text.isEmpty
                ? null
                : _emailController.text,
          );

      if (mounted) {
        Navigator.of(context).pop();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              success
                  ? 'Đã gửi báo cáo thành công. Cảm ơn bạn!'
                  : 'Không thể gửi báo cáo. Vui lòng thử lại.',
            ),
            backgroundColor: success ? Colors.green : Colors.red,
          ),
        );
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
}
