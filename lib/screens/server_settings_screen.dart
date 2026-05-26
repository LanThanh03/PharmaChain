import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import '../config/api_config.dart';

class ServerSettingsScreen extends StatefulWidget {
  const ServerSettingsScreen({super.key});

  @override
  State<ServerSettingsScreen> createState() => _ServerSettingsScreenState();
}

class _ServerSettingsScreenState extends State<ServerSettingsScreen> {
  final _controller = TextEditingController();
  String _currentUrl = '';
  bool _isDiscovering = false;
  bool _isTesting = false;
  bool? _testResult;
  String _statusMessage = '';
  final Map<String, bool?> _probeResults = {}; // null=chưa thử, true=ok, false=lỗi

  @override
  void initState() {
    super.initState();
    _loadCurrentUrl();
  }

  Future<void> _loadCurrentUrl() async {
    final url = await ApiConfig.getBaseUrl();
    if (!mounted) return;
    setState(() {
      _currentUrl = url;
      _controller.text = url;
    });
  }

  Future<void> _save() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    await ApiConfig.setBaseUrl(text);
    if (!mounted) return;
    setState(() => _currentUrl = ApiConfig.baseUrlSync);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('✅ Đã lưu địa chỉ server!'),
        backgroundColor: Color(0xFF2E7D32),
        duration: Duration(seconds: 2),
      ),
    );
  }

  Future<void> _testConnection() async {
    final url = _controller.text.trim();
    if (url.isEmpty) return;

    setState(() {
      _isTesting = true;
      _testResult = null;
      _statusMessage = 'Đang kiểm tra kết nối...';
    });

    try {
      // Chỉ cần mở socket để kiểm tra
      final parsed = Uri.parse(url);
      final socket = await Socket.connect(
        parsed.host,
        parsed.port > 0 ? parsed.port : 8080,
        timeout: const Duration(seconds: 3),
      );
      socket.destroy();

      if (!mounted) return;
      setState(() {
        _testResult = true;
        _statusMessage = '✅ Kết nối thành công đến $url';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _testResult = false;
        _statusMessage = '❌ Không thể kết nối: ${e.toString().split(":").first}';
      });
    } finally {
      if (mounted) setState(() => _isTesting = false);
    }
  }

  Future<void> _autoDiscover() async {
    setState(() {
      _isDiscovering = true;
      _statusMessage = 'Đang tự động tìm kiếm server...';
      _probeResults.clear();
      _testResult = null;
    });

    final found = await ApiConfig.autoDiscoverServer(
      onTry: (url, success) {
        if (mounted) {
          setState(() {
            _probeResults[url] = success;
            _statusMessage = 'Đang thử: $url';
          });
        }
      },
    );

    if (!mounted) return;
    if (found != null) {
      setState(() {
        _currentUrl = found;
        _controller.text = found;
        _testResult = true;
        _statusMessage = '✅ Tìm thấy server tại: $found';
      });
    } else {
      setState(() {
        _testResult = false;
        _statusMessage = '❌ Không tìm thấy server nào. Hãy nhập IP thủ công.';
      });
    }
    setState(() => _isDiscovering = false);
  }

  void _selectUrl(String url) {
    setState(() {
      _controller.text = url;
      _testResult = null;
      _statusMessage = '';
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final primary = const Color(0xFF1976D2);

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          'Cài đặt Server',
          style: TextStyle(fontWeight: FontWeight.w700),
        ),
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        elevation: 0,
        actions: [
          TextButton.icon(
            onPressed: _save,
            icon: const Icon(Icons.save_rounded),
            label: const Text('Lưu'),
            style: TextButton.styleFrom(foregroundColor: primary),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // ──── Thông báo hướng dẫn ────
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFE3F2FD),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFF90CAF9)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.info_outline_rounded,
                          color: Color(0xFF1976D2), size: 20),
                      const SizedBox(width: 8),
                      const Text(
                        'Hướng dẫn',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF1976D2),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    '• Máy tính và điện thoại phải cùng mạng WiFi\n'
                    '• Tìm IP máy tính: Mở CMD → gõ ipconfig → xem "IPv4 Address"\n'
                    '• Thường có dạng: 192.168.x.x hoặc 10.0.x.x\n'
                    '• Ví dụ: http://192.168.1.6:8080',
                    style: TextStyle(
                      fontSize: 13,
                      color: Color(0xFF1565C0),
                      height: 1.5,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // ──── IP hiện tại đang dùng ────
            Text(
              'Địa chỉ server đang dùng',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade600,
                letterSpacing: 0.8,
              ),
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Row(
                children: [
                  const Icon(Icons.link_rounded, color: Color(0xFF1976D2), size: 18),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _currentUrl,
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // ──── Input thủ công ────
            Text(
              'Nhập địa chỉ server',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade600,
                letterSpacing: 0.8,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: InputDecoration(
                      hintText: 'http://192.168.1.x:8080',
                      prefixIcon: const Icon(Icons.dns_rounded),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide(color: Colors.grey.shade300),
                      ),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide(color: Colors.grey.shade300),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: const BorderSide(color: Color(0xFF1976D2), width: 2),
                      ),
                      filled: true,
                      fillColor: Colors.white,
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    ),
                    keyboardType: TextInputType.url,
                    style: const TextStyle(fontFamily: 'monospace', fontSize: 13),
                    onChanged: (_) => setState(() {
                      _testResult = null;
                      _statusMessage = '';
                    }),
                  ),
                ),
                const SizedBox(width: 8),
                // Nút Test kết nối
                Container(
                  decoration: BoxDecoration(
                    color: _testResult == true
                        ? const Color(0xFF2E7D32)
                        : _testResult == false
                            ? const Color(0xFFC62828)
                            : primary,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: IconButton(
                    onPressed: _isTesting ? null : _testConnection,
                    icon: _isTesting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              color: Colors.white,
                              strokeWidth: 2.5,
                            ),
                          )
                        : Icon(
                            _testResult == true
                                ? Icons.check_rounded
                                : _testResult == false
                                    ? Icons.close_rounded
                                    : Icons.wifi_find_rounded,
                            color: Colors.white,
                          ),
                    tooltip: 'Kiểm tra kết nối',
                  ),
                ),
              ],
            ),

            // Status message
            if (_statusMessage.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: _testResult == true
                      ? const Color(0xFFE8F5E9)
                      : _testResult == false
                          ? const Color(0xFFFFEBEE)
                          : const Color(0xFFFFF3E0),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  _statusMessage,
                  style: TextStyle(
                    fontSize: 12,
                    color: _testResult == true
                        ? const Color(0xFF2E7D32)
                        : _testResult == false
                            ? const Color(0xFFC62828)
                            : const Color(0xFFE65100),
                  ),
                ),
              ),
            ],
            const SizedBox(height: 16),

            // ──── Nút Auto-Discover ────
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _isDiscovering ? null : _autoDiscover,
                icon: _isDiscovering
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2.5,
                        ),
                      )
                    : const Icon(Icons.travel_explore_rounded),
                label: Text(
                  _isDiscovering ? 'Đang tìm kiếm...' : 'Tự động tìm Server',
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  elevation: 0,
                ),
              ),
            ),

            // Probe results khi đang auto-discover
            if (_probeResults.isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey.shade200),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Kết quả tìm kiếm:',
                      style: TextStyle(
                          fontWeight: FontWeight.w600,
                          color: Colors.grey.shade700,
                          fontSize: 12),
                    ),
                    const SizedBox(height: 6),
                    ..._probeResults.entries.map((e) => Padding(
                          padding: const EdgeInsets.symmetric(vertical: 2),
                          child: Row(
                            children: [
                              Icon(
                                e.value == true
                                    ? Icons.check_circle_rounded
                                    : Icons.cancel_rounded,
                                color: e.value == true
                                    ? const Color(0xFF2E7D32)
                                    : Colors.red.shade300,
                                size: 16,
                              ),
                              const SizedBox(width: 6),
                              Expanded(
                                  child: Text(e.key,
                                      style: const TextStyle(
                                          fontFamily: 'monospace',
                                          fontSize: 12))),
                            ],
                          ),
                        )),
                  ],
                ),
              ),
            ],

            const SizedBox(height: 24),

            // ──── Danh sách URL phổ biến ────
            Text(
              'Địa chỉ phổ biến',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Colors.grey.shade600,
                letterSpacing: 0.8,
              ),
            ),
            const SizedBox(height: 8),
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Column(
                children: [
                  _buildPresetTile(
                    title: 'Android Emulator',
                    subtitle: 'Dùng khi chạy bằng AVD (emulator)',
                    url: 'http://10.0.2.2:8080',
                    icon: Icons.phone_android_rounded,
                  ),
                  const Divider(height: 1, indent: 56),
                  _buildPresetTile(
                    title: 'iPhone Hotspot',
                    subtitle: 'Dùng khi chia sẻ mạng từ iPhone',
                    url: 'http://172.20.10.2:8080',
                    icon: Icons.wifi_tethering_rounded,
                  ),
                  const Divider(height: 1, indent: 56),
                  _buildPresetTile(
                    title: 'WiFi (192.168.1.x)',
                    subtitle: 'Router thông thường, IP .6',
                    url: 'http://192.168.1.6:8080',
                    icon: Icons.router_rounded,
                  ),
                  const Divider(height: 1, indent: 56),
                  _buildPresetTile(
                    title: 'WiFi (192.168.1.x)',
                    subtitle: 'Router thông thường, IP .100',
                    url: 'http://192.168.1.100:8080',
                    icon: Icons.router_rounded,
                  ),
                  const Divider(height: 1, indent: 56),
                  _buildPresetTile(
                    title: 'Localhost (Web/Desktop)',
                    subtitle: 'Chỉ dùng khi chạy trên máy tính',
                    url: 'http://127.0.0.1:8080',
                    icon: Icons.computer_rounded,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // ──── Nút Reset ────
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () async {
                  await ApiConfig.clearSavedUrl();
                  await _loadCurrentUrl();
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Đã đặt lại về mặc định'),
                      duration: Duration(seconds: 2),
                    ),
                  );
                },
                icon: const Icon(Icons.restart_alt_rounded),
                label: const Text('Đặt lại mặc định'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.red.shade600,
                  side: BorderSide(color: Colors.red.shade300),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  Widget _buildPresetTile({
    required String title,
    required String subtitle,
    required String url,
    required IconData icon,
  }) {
    final isSelected = _controller.text.trim() == url;
    return InkWell(
      onTap: () => _selectUrl(url),
      borderRadius: BorderRadius.circular(12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: isSelected
                    ? const Color(0xFF1976D2).withOpacity(0.12)
                    : Colors.grey.shade100,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(icon,
                  color: isSelected
                      ? const Color(0xFF1976D2)
                      : Colors.grey.shade600,
                  size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                      color:
                          isSelected ? const Color(0xFF1976D2) : Colors.black87,
                    ),
                  ),
                  Text(
                    subtitle,
                    style: TextStyle(
                        fontSize: 11, color: Colors.grey.shade500),
                  ),
                  Text(
                    url,
                    style: TextStyle(
                      fontSize: 11,
                      fontFamily: 'monospace',
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              const Icon(Icons.check_circle_rounded,
                  color: Color(0xFF1976D2), size: 20),
          ],
        ),
      ),
    );
  }
}
