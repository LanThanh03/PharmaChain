import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// Generic QR Scanner Screen (dùng mobile_scanner)
class QRScannerScreen extends StatefulWidget {
  final Function(String) onQRScanned;
  final String title;

  const QRScannerScreen({
    super.key,
    required this.onQRScanned,
    this.title = 'Quét mã QR',
  });

  @override
  State<QRScannerScreen> createState() => _QRScannerScreenState();
}

class _QRScannerScreenState extends State<QRScannerScreen> {
  final MobileScannerController controller = MobileScannerController();
  bool hasScanned = false;

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (hasScanned) return;

    final List<Barcode> barcodes = capture.barcodes;
    if (barcodes.isEmpty) return;

    final String? code = barcodes.first.rawValue;
    if (code == null) return;

    setState(() {
      hasScanned = true;
    });

    controller.stop();
    widget.onQRScanned(code);
    // Chỉ pop nếu màn hình này được mở bằng push. Khi dùng trong tab, không pop để tránh màn hình đen
    if (Navigator.of(context).canPop()) {
      Navigator.of(context).pop(code);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: Colors.blue,
        foregroundColor: Colors.white,
      ),
      body: Stack(
        children: [
          // Camera scanner
          MobileScanner(
            controller: controller,
            onDetect: _onDetect,
          ),

          // Overlay
          CustomPaint(
            painter: ScannerOverlay(),
            child: Container(),
          ),

          // Instructions
        Positioned(
            bottom: 100,
            left: 0,
            right: 0,
          child: Container(
            padding: const EdgeInsets.all(16),
              color: Colors.black54,
            child: const Text(
                'Đặt mã QR trong khung để quét',
              style: TextStyle(
                color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ],
      ),
      floatingActionButton: FloatingActionButton(
        heroTag: 'qr_scanner_flash',
        onPressed: () => controller.toggleTorch(),
        child: const Icon(Icons.flash_on),
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

    // Dùng saveLayer để BlendMode.clear hoạt động đúng trên mọi thiết bị
    final overlayRect = Rect.fromLTWH(0, 0, size.width, size.height);
    canvas.saveLayer(overlayRect, Paint());

    // Vẽ nền mờ
    final backgroundPaint = Paint()..color = Colors.black54;
    canvas.drawRect(overlayRect, backgroundPaint);

    // Đục lỗ khu vực quét để hiển thị camera
    final clearPaint = Paint()..blendMode = BlendMode.clear;
    canvas.drawRect(
      Rect.fromLTWH(left, top, scanAreaSize, scanAreaSize),
      clearPaint,
    );

    canvas.restore();

    // Draw border
    final borderPaint = Paint()
      ..color = Colors.green
      ..strokeWidth = 4
      ..style = PaintingStyle.stroke;

    canvas.drawRect(
      Rect.fromLTWH(left, top, scanAreaSize, scanAreaSize),
      borderPaint,
    );

    // Draw corner indicators
    final cornerPaint = Paint()
      ..color = Colors.green
      ..strokeWidth = 6
      ..style = PaintingStyle.stroke;

    const double cornerLength = 30;

    // Corners (same as product_verification_screen)
    canvas.drawLine(Offset(left, top), Offset(left + cornerLength, top), cornerPaint);
    canvas.drawLine(Offset(left, top), Offset(left, top + cornerLength), cornerPaint);
    canvas.drawLine(Offset(left + scanAreaSize, top), Offset(left + scanAreaSize - cornerLength, top), cornerPaint);
    canvas.drawLine(Offset(left + scanAreaSize, top), Offset(left + scanAreaSize, top + cornerLength), cornerPaint);
    canvas.drawLine(Offset(left, top + scanAreaSize), Offset(left + cornerLength, top + scanAreaSize), cornerPaint);
    canvas.drawLine(Offset(left, top + scanAreaSize), Offset(left, top + scanAreaSize - cornerLength), cornerPaint);
    canvas.drawLine(Offset(left + scanAreaSize, top + scanAreaSize), Offset(left + scanAreaSize - cornerLength, top + scanAreaSize), cornerPaint);
    canvas.drawLine(Offset(left + scanAreaSize, top + scanAreaSize), Offset(left + scanAreaSize, top + scanAreaSize - cornerLength), cornerPaint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}
