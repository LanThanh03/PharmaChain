import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../services/product_verification_service.dart';
import 'product_verification_result_screen.dart';
import 'scan_history_screen.dart';

/// Màn hình quét QR sản phẩm (dùng mobile_scanner)
class ProductVerificationScreen extends StatefulWidget {
  final bool isActive;

  const ProductVerificationScreen({super.key, this.isActive = true});

  @override
  State<ProductVerificationScreen> createState() =>
      _ProductVerificationScreenState();
}

class _ProductVerificationScreenState extends State<ProductVerificationScreen>
    with WidgetsBindingObserver {
  final MobileScannerController controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.normal,
    facing: CameraFacing.back,
    torchEnabled: false,
  );
  bool isVerifying = false;
  String? lastScannedCode;
  String? scannedItemCode;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // Start scanner after build if active
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (widget.isActive) {
        controller.start();
      }
    });
  }

  @override
  void didUpdateWidget(ProductVerificationScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isActive != oldWidget.isActive) {
      if (widget.isActive && !isVerifying) {
        controller.start();
      } else {
        controller.stop();
      }
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    controller.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    switch (state) {
      case AppLifecycleState.paused:
      case AppLifecycleState.inactive:
        controller.stop();
        break;
      case AppLifecycleState.resumed:
        if (widget.isActive && !isVerifying) {
          controller.start();
        }
        break;
      default:
        break;
    }
  }

  Future<void> _onDetect(BarcodeCapture capture) async {
    final List<Barcode> barcodes = capture.barcodes;

    if (barcodes.isEmpty || isVerifying) return;

    final String? code = barcodes.first.rawValue;

    if (code == null || code == lastScannedCode) return;

    String itemCode = code;
    if (code.contains('/verify/')) {
      itemCode = code.split('/verify/').last;
    }

    setState(() {
      lastScannedCode = code;
      scannedItemCode = itemCode;
      isVerifying = true;
    });

    print('🔍 QR Scanned: $code');

    try {
      print('📦 Verifying item code: $itemCode');

      // Verify product
      final result = await ProductVerificationService.instance.verifyProduct(
        itemCode,
      );

      print('✅ Verification result: ${result.verificationResult}');

      // Save to history
      await saveScanToHistory(
        ScanHistoryItem(
          itemCode: itemCode,
          productName: result.productInfo?.name,
          batchNumber: result.productInfo?.batchNumber,
          manufacturer: result.productInfo?.manufacturer,
          isAuthentic: result.verificationResult == 'AUTHENTIC',
          isSold: result.productInfo?.isSold ?? false,
          scannedAt: DateTime.now(),
        ),
      );

      if (!mounted) {
        print('⚠️ Widget not mounted, skipping navigation');
        return;
      }

      // Stop scanner before navigate
      await controller.stop();

      print('🚀 Navigating to result screen...');

      // Navigate to result screen
      final navigationResult = await Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => ProductVerificationResultScreen(result: result),
        ),
      );

      print('⬅️ Returned from result screen: $navigationResult');

      // Reset when coming back
      if (mounted) {
        setState(() {
          isVerifying = false;
          lastScannedCode = null;
          scannedItemCode = null;
        });
        await controller.start();
        print('🔄 Scanner restarted');
      }
    } catch (e, stackTrace) {
      print('❌ Verification error: $e');
      print('Stack trace: $stackTrace');

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Lỗi xác thực: $e'),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 3),
        ),
      );

      setState(() {
        isVerifying = false;
        lastScannedCode = null;
        scannedItemCode = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        title: const Text(
          'Quét QR Sản phẩm',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 18,
            color: Colors.white,
          ),
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        foregroundColor: Colors.white,
        centerTitle: true,
        actions: [
          Container(
            margin: const EdgeInsets.only(right: 16, top: 8, bottom: 8),
            decoration: BoxDecoration(
              color: Colors.black.withOpacity(0.3),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Colors.white.withOpacity(0.2)),
            ),
            child: IconButton(
              icon: const Icon(Icons.history, size: 22),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => const ScanHistoryScreen(),
                  ),
                );
              },
              tooltip: 'Lịch sử quét',
            ),
          ),
        ],
      ),
      body: Stack(
        children: [
          // Camera scanner
          MobileScanner(controller: controller, onDetect: _onDetect),

          // Overlay với khung quét
          ClipRect(
            child: CustomPaint(painter: ScannerOverlay(), child: Container()),
          ),

          // Decoration Corners (Top Left)
          Positioned(
            top: 100,
            left: 20,
            child: Icon(
              Icons.qr_code_scanner,
              color: Colors.white.withOpacity(0.1),
              size: 100,
            ),
          ),

          // Instructions
          Positioned(
            bottom: 120,
            left: 0,
            right: 0,
            child: Center(
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 14,
                ),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.6),
                  borderRadius: BorderRadius.circular(30),
                  border: Border.all(
                    color: const Color(0xFF10B981).withOpacity(0.5),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF10B981).withOpacity(0.2),
                      blurRadius: 20,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.document_scanner,
                      color: Color(0xFF10B981),
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    const Text(
                      'Đặt mã QR vào trong khung',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Loading indicator
          if (isVerifying)
            Container(
              color: Colors.black.withOpacity(0.7),
              child: Center(
                child: Container(
                  padding: const EdgeInsets.all(32),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    boxShadow: [
                      BoxShadow(
                        color: const Color(0xFF10B981).withOpacity(0.3),
                        blurRadius: 30,
                        spreadRadius: 5,
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const CircularProgressIndicator(
                        valueColor: AlwaysStoppedAnimation<Color>(
                          Color(0xFF10B981),
                        ),
                        strokeWidth: 3,
                      ),
                      const SizedBox(height: 20),
                      const Text(
                        'Đang xác thực...',
                        style: TextStyle(
                          color: Color(0xFF1E3A5F),
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      if (scannedItemCode != null) ...[
                        const SizedBox(height: 8),
                        Text(
                          'Mã SP: $scannedItemCode',
                          style: const TextStyle(
                            color: Colors.black54,
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'scan_flash',
        onPressed: () => controller.toggleTorch(),
        backgroundColor: const Color(0xFF10B981),
        child: ValueListenableBuilder(
          valueListenable: controller,
          builder: (context, value, child) {
            final isTorchOn = value.torchState == TorchState.on;
            return Icon(isTorchOn ? Icons.flash_off : Icons.flash_on);
          },
        ),
      ),
    );
  }
}

/// Custom painter for scanner overlay
class ScannerOverlay extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final double scanAreaSize = size.width * 0.7;
    final double left = (size.width - scanAreaSize) / 2;
    final double top = (size.height - scanAreaSize) / 2;

    // Draw semi-transparent background
    final backgroundPaint = Paint()..color = Colors.black.withOpacity(0.2);

    canvas.drawRect(
      Rect.fromLTWH(0, 0, size.width, size.height),
      backgroundPaint,
    );

    // Clear the scan area (make it transparent so camera can be seen)
    // final clearPaint = Paint()..blendMode = BlendMode.clear;

    // canvas.drawRect(
    //   Rect.fromLTWH(left, top, scanAreaSize, scanAreaSize),
    //   clearPaint,
    // );

    // Remove solid border and replace with soft glowing corners
    final double cornerRadius = 24.0;
    final double cornerLength = 40.0;

    final cornerPaint = Paint()
      ..color = const Color(0xFF10B981)
      ..strokeWidth = 6
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke;

    final path = Path();

    // Top Left
    path.moveTo(left, top + cornerLength);
    path.quadraticBezierTo(left, top, left + cornerRadius, top);
    path.lineTo(left + cornerLength, top);

    // Top Right
    path.moveTo(left + scanAreaSize - cornerLength, top);
    path.lineTo(left + scanAreaSize - cornerRadius, top);
    path.quadraticBezierTo(
      left + scanAreaSize,
      top,
      left + scanAreaSize,
      top + cornerLength,
    );

    // Bottom Left
    path.moveTo(left, top + scanAreaSize - cornerLength);
    path.quadraticBezierTo(
      left,
      top + scanAreaSize,
      left + cornerRadius,
      top + scanAreaSize,
    );
    path.lineTo(left + cornerLength, top + scanAreaSize);

    // Bottom Right
    path.moveTo(left + scanAreaSize - cornerLength, top + scanAreaSize);
    path.lineTo(left + scanAreaSize - cornerRadius, top + scanAreaSize);
    path.quadraticBezierTo(
      left + scanAreaSize,
      top + scanAreaSize,
      left + scanAreaSize,
      top + scanAreaSize - cornerLength,
    );

    // Add glowing effect to corners
    final glowPaint = Paint()
      ..color = const Color(0xFF10B981).withOpacity(0.3)
      ..strokeWidth = 14
      ..strokeCap = StrokeCap.round
      ..style = PaintingStyle.stroke
      ..maskFilter = const MaskFilter.blur(BlurStyle.normal, 8);

    canvas.drawPath(path, glowPaint);
    canvas.drawPath(path, cornerPaint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}
