import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../services/ai_service.dart';

/// AI Chatbot screen for drug consultation
/// Supports two modes:
/// 1. General chat (drugName == null) - generic drug Q&A via AI API
/// 2. Drug-specific (drugName != null) - shows DB info first, then AI for follow-up
class DrugAIChatScreen extends StatefulWidget {
  final String? drugName;

  const DrugAIChatScreen({Key? key, this.drugName}) : super(key: key);

  @override
  _DrugAIChatScreenState createState() => _DrugAIChatScreenState();
}

class _DrugAIChatScreenState extends State<DrugAIChatScreen> {
  final AIService _aiService = AIService();
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  
  final List<ChatMessage> _messages = [];
  bool _isLoading = false;
  bool _hasDbData = false; // Có dữ liệu từ CSDL không

  @override
  void initState() {
    super.initState();
    
    if (widget.drugName != null && widget.drugName!.isNotEmpty) {
      _messages.add(ChatMessage(
        text: '💊 Đang tải thông tin ${widget.drugName} từ cơ sở dữ liệu...',
        isUser: false,
        timestamp: DateTime.now(),
      ));
      _loadDrugFromDb();
    } else {
      _messages.add(ChatMessage(
        text: '👋 Xin chào! Tôi là chuyên gia AI y tế.\n\n'
            'Bạn có thể hỏi tôi về:\n'
            '💊 Thông tin thuốc\n'
            '⚠️ Tác dụng phụ\n'
            '🔄 Tương tác thuốc\n'
            '💡 Cách sử dụng hiệu quả\n\n'
            'Hãy đặt câu hỏi của bạn!',
        isUser: false,
        timestamp: DateTime.now(),
      ));
    }
  }

  /// Load drug info from DATABASE (no AI API call)
  Future<void> _loadDrugFromDb() async {
    setState(() => _isLoading = true);

    try {
      final result = await _aiService.getDrugDbInfo(widget.drugName!);

      if (result['success'] == true && result['data'] != null) {
        final data = result['data'] as Map<String, dynamic>;
        final found = data['found'] == true;

        if (found) {
          _hasDbData = true;
          final formattedInfo = _formatDbInfo(data);
          final link = data['articleUrl']?.toString();
          setState(() {
            _messages.add(ChatMessage(
              text: formattedInfo,
              isUser: false,
              timestamp: DateTime.now(),
              articleUrl: link,
            ));
            _messages.add(ChatMessage(
              text: '💬 Bạn có thể đặt câu hỏi thêm về thuốc này. '
                  'Các câu hỏi ngoài thông tin trên sẽ được AI tư vấn.',
              isUser: false,
              timestamp: DateTime.now(),
            ));
          });
        } else {
          // Không tìm thấy trong DB → dùng AI
          setState(() {
            _messages.add(ChatMessage(
              text: '📭 Không tìm thấy thông tin **${widget.drugName}** trong cơ sở dữ liệu.\n\n'
                  '🤖 Đang hỏi AI để tư vấn...',
              isUser: false,
              timestamp: DateTime.now(),
            ));
          });
          await _consultAI();
        }
      } else {
        // Lỗi kết nối DB → fallback AI
        setState(() {
          _messages.add(ChatMessage(
            text: '⚠️ Không thể kết nối CSDL: ${result['message']}\n\n'
                '🤖 Đang hỏi AI để tư vấn...',
            isUser: false,
            timestamp: DateTime.now(),
          ));
        });
        await _consultAI();
      }
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(
          text: '❌ Lỗi: ${e.toString()}',
          isUser: false,
          timestamp: DateTime.now(),
          isError: true,
        ));
      });
    } finally {
      setState(() => _isLoading = false);
      _scrollToBottom();
    }
  }

  /// Format DB data into a nice readable message
  String _formatDbInfo(Map<String, dynamic> data) {
    final buffer = StringBuffer();
    buffer.writeln('📋 THÔNG TIN THUỐC: ${data['drugName'] ?? widget.drugName}\n');

    if (data['activeIngredient'] != null && data['activeIngredient'].toString().isNotEmpty) {
      buffer.writeln('🔬 Hoạt chất: ${data['activeIngredient']}');
    }
    if (data['category'] != null && data['category'].toString().isNotEmpty) {
      buffer.writeln('📁 Danh mục: ${data['category']}');
    }
    if (data['dosage'] != null && data['dosage'].toString().isNotEmpty) {
      buffer.writeln('💊 Hàm lượng: ${data['dosage']}');
    }
    if (data['unit'] != null && data['unit'].toString().isNotEmpty) {
      buffer.writeln('📦 Đóng gói: ${data['unit']}');
    }

    if (data['description'] != null && data['description'].toString().isNotEmpty) {
      buffer.writeln('\n📝 Mô tả:\n${data['description']}');
    }
    if (data['indications'] != null && data['indications'].toString().isNotEmpty) {
      buffer.writeln('\n💊 Chỉ định (Vì sao cần uống):\n${data['indications']}');
    }
    if (data['contraindications'] != null && data['contraindications'].toString().isNotEmpty) {
      buffer.writeln('\n🚫 Chống chỉ định:\n${data['contraindications']}');
    }
    if (data['sideEffects'] != null && data['sideEffects'].toString().isNotEmpty) {
      buffer.writeln('\n⚠️ Tác dụng phụ:\n${data['sideEffects']}');
    }
    if (data['drugInteractions'] != null && data['drugInteractions'].toString().isNotEmpty) {
      buffer.writeln('\n🔄 Tương tác thuốc:\n${data['drugInteractions']}');
    }
    if (data['precautions'] != null && data['precautions'].toString().isNotEmpty) {
      buffer.writeln('\n🛡️ Thận trọng khi sử dụng:\n${data['precautions']}');
    }
    if (data['usageInstructions'] != null && data['usageInstructions'].toString().isNotEmpty) {
      buffer.writeln('\n📋 Hướng dẫn sử dụng:\n${data['usageInstructions']}');
    }
    if (data['storageConditions'] != null && data['storageConditions'].toString().isNotEmpty) {
      buffer.writeln('\n🌡️ Bảo quản: ${data['storageConditions']}');
    }

    buffer.writeln('\n📌 Thông tin từ cơ sở dữ liệu hệ thống');
    return buffer.toString().trim().replaceAll('*', '');
  }

  /// Fallback: consult AI when DB has no data
  Future<void> _consultAI() async {
    setState(() => _isLoading = true);
    _scrollToBottom();

    try {
      final result = await _aiService.consultDrug(widget.drugName!);

      if (result['success'] == true) {
        final response = result['reply']?.toString();
        final link = result['articleUrl']?.toString();
        if (response != null && response.isNotEmpty) {
          setState(() {
            _messages.add(ChatMessage(
              text: response,
              isUser: false,
              timestamp: DateTime.now(),
              articleUrl: link,
            ));
          });
        } else {
          setState(() {
            _messages.add(ChatMessage(
              text: '🤔 AI trả lời rỗng. Hãy thử đặt câu hỏi cụ thể.',
              isUser: false,
              timestamp: DateTime.now(),
            ));
          });
        }
      } else {
        setState(() {
          _messages.add(ChatMessage(
            text: '❌ ${result['message'] ?? 'Có lỗi xảy ra khi tư vấn'}',
            isUser: false,
            timestamp: DateTime.now(),
            isError: true,
          ));
        });
      }
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(
          text: '❌ Lỗi: ${e.toString()}',
          isUser: false,
          timestamp: DateTime.now(),
          isError: true,
        ));
      });
    } finally {
      setState(() => _isLoading = false);
      _scrollToBottom();
    }
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  /// Send follow-up message via AI API
  Future<void> _sendMessage() async {
    final message = _messageController.text.trim();
    if (message.isEmpty) return;

    setState(() {
      _messages.add(ChatMessage(
        text: message,
        isUser: true,
        timestamp: DateTime.now(),
      ));
      _isLoading = true;
    });

    _messageController.clear();
    _scrollToBottom();

    try {
      // Build chat history (exclude system messages)
      final history = _messages
          .where((m) => m != _messages.first)
          .map((m) => {
                'role': m.isUser ? 'user' : 'assistant',
                'content': m.text,
              })
          .toList();

      final result = await _aiService.chat(message, history);

      if (result['success'] == true) {
        final response = result['reply']?.toString();
        if (response == null || response.isEmpty) {
          setState(() {
            _messages.add(ChatMessage(
              text: '🤔 AI trả lời rỗng. Thử lại câu hỏi khác.',
              isUser: false,
              timestamp: DateTime.now(),
            ));
          });
        } else {
          setState(() {
            _messages.add(ChatMessage(
              text: response,
              isUser: false,
              timestamp: DateTime.now(),
            ));
          });
        }
      } else {
        setState(() {
          _messages.add(ChatMessage(
            text: '❌ ${result['message'] ?? 'Có lỗi xảy ra'}',
            isUser: false,
            timestamp: DateTime.now(),
            isError: true,
          ));
        });
      }
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(
          text: '❌ Lỗi: ${e.toString()}',
          isUser: false,
          timestamp: DateTime.now(),
          isError: true,
        ));
      });
    } finally {
      setState(() => _isLoading = false);
      _scrollToBottom();
    }
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _askQuickQuestion(String question) {
    _messageController.text = question;
    _sendMessage();
  }

  @override
  Widget build(BuildContext context) {
    final isDrugMode = widget.drugName != null && widget.drugName!.isNotEmpty;
    
    return Scaffold(
      backgroundColor: const Color(0xFFF3F6FA), // Cảm giác sáng, y tế nhưng cao cấp
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(65.0),
        child: AppBar(
          elevation: 0,
          backgroundColor: Colors.transparent,
          flexibleSpace: Container(
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [Color(0xFF1E3A8A), Color(0xFF3B82F6)], // Gradient xanh dương đậm sang sáng
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              boxShadow: [
                BoxShadow(
                  color: Color(0x333B82F6),
                  blurRadius: 10,
                  offset: Offset(0, 4),
                )
              ],
            ),
          ),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
            onPressed: () => Navigator.of(context).pop(),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.hub_rounded, color: Colors.white, size: 20),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      isDrugMode ? 'Tư vấn: ${widget.drugName}' : 'AI Y Tế',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 0.3,
                      ),
                      overflow: TextOverflow.ellipsis, // Cắt bớt nếu chữ quá dài
                    ),
                    const Text(
                      'Luôn sẵn sàng hỗ trợ bạn',
                      style: TextStyle(
                        color: Colors.white70,
                        fontSize: 12,
                      ),
                      overflow: TextOverflow.ellipsis,
                    )
                  ],
                ),
              ),
            ],
          ),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh_rounded, color: Colors.white),
              onPressed: () {
                setState(() {
                  _messages.clear();
                  _hasDbData = false;
                  if (isDrugMode) {
                    _messages.add(ChatMessage(
                      text: 'Tải lại thông tin ${widget.drugName}...',
                      isUser: false,
                      timestamp: DateTime.now(),
                    ));
                    _loadDrugFromDb();
                  } else {
                    _messages.add(ChatMessage(
                      text: '👋 Xin chào! Tôi là chuyên gia AI y tế.\n\n'
                          'Bạn có thể hỏi tôi về:\n'
                          '💊 Thông tin thuốc\n'
                          '⚠️ Tác dụng phụ\n'
                          '🔄 Tương tác thuốc\n'
                          '💡 Cách sử dụng hiệu quả\n\n'
                          'Hãy đặt câu hỏi của bạn!',
                      isUser: false,
                      timestamp: DateTime.now(),
                    ));
                  }
                });
              },
              tooltip: 'Tải lại',
            ),
            const SizedBox(width: 8),
          ],
        ),
      ),
      body: Column(
        children: [
          // Quick questions
          _buildQuickQuestions(),
          
          // Messages list
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                return _buildMessageBubble(_messages[index]);
              },
            ),
          ),

          // Loading indicator
          if (_isLoading)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              alignment: Alignment.centerLeft,
              child: Row(
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    decoration: BoxDecoration(
                      color: const Color(0xFF3B82F6).withOpacity(0.1),
                      shape: BoxShape.circle,
                    ),
                    child: const Center(
                      child: SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF3B82F6)),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    isDrugMode && !_hasDbData
                        ? 'Đang phân tích dữ liệu...'
                        : 'AI đang phân tích...',
                    style: const TextStyle(
                      color: Color(0xFF64748B), 
                      fontSize: 13,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ),
            ),

          // Input field
          _buildInputField(),
        ],
      ),
    );
  }

  Widget _buildQuickQuestions() {
    final isDrugMode = widget.drugName != null && widget.drugName!.isNotEmpty;
    
    final List<String> questions;
    if (isDrugMode) {
      questions = [
        'Tác dụng phụ của ${widget.drugName}?',
        'Uống ${widget.drugName} cùng thuốc khác được?',
        'Khi nào nên ngừng ${widget.drugName}?',
        'Phụ nữ mang thai dùng được không?',
      ];
    } else {
      questions = [
        'Paracetamol dùng làm gì?',
        'Tác dụng phụ thuốc kháng sinh?',
        'Thuốc uống trước hay sau ăn?',
      ];
    }

    return Container(
      height: 48, 
      margin: const EdgeInsets.only(top: 12, bottom: 8),
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: questions.length,
        itemBuilder: (context, index) {
          return Padding(
            padding: const EdgeInsets.only(right: 10),
            child: InkWell(
              onTap: () => _askQuickQuestion(questions[index]),
              borderRadius: BorderRadius.circular(20),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: const Color(0xFFE2E8F0)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.02),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Text(
                  questions[index],
                  style: const TextStyle(
                    color: Color(0xFF3B82F6),
                    fontWeight: FontWeight.w500,
                    fontSize: 13,
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    // Xoá các dấu * khỏi nội dung tin nhắn để UI sạch đẹp hơn
    final cleanText = message.text.replaceAll('*', '');

    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Row(
        mainAxisAlignment: message.isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!message.isUser) ...[
            Container(
              margin: const EdgeInsets.only(right: 8),
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF3B82F6), Color(0xFF2563EB)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF3B82F6).withOpacity(0.3),
                    blurRadius: 6,
                    offset: const Offset(0, 3),
                  )
                ],
              ),
              child: const Icon(Icons.hub_rounded, color: Colors.white, size: 20),
            ),
          ],
          
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
              decoration: BoxDecoration(
                color: message.isUser ? const Color(0xFF0F172A) : Colors.white,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(24),
                  topRight: const Radius.circular(24),
                  bottomLeft: message.isUser ? const Radius.circular(24) : const Radius.circular(6),
                  bottomRight: message.isUser ? const Radius.circular(6) : const Radius.circular(24),
                ),
                boxShadow: [
                  if (!message.isUser)
                    BoxShadow(
                      color: Colors.black.withOpacity(0.04),
                      blurRadius: 15,
                      offset: const Offset(0, 5),
                    ),
                  if (message.isUser)
                    BoxShadow(
                      color: const Color(0xFF0F172A).withOpacity(0.2),
                      blurRadius: 12,
                      offset: const Offset(0, 4),
                    ),
                ],
                border: message.isUser 
                    ? null 
                    : Border.all(color: message.isError ? Colors.red.shade100 : Colors.white, width: 1),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    cleanText,
                    style: TextStyle(
                      color: message.isUser
                          ? Colors.white
                          : message.isError
                              ? const Color(0xFFEF4444)
                              : const Color(0xFF1E293B),
                      fontSize: 15,
                      height: 1.5,
                      letterSpacing: 0.1,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _formatTime(message.timestamp),
                    style: TextStyle(
                      color: message.isUser
                          ? Colors.white54
                          : const Color(0xFF94A3B8),
                      fontSize: 10,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  if (!message.isUser && message.articleUrl != null && message.articleUrl!.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    InkWell(
                      onTap: () async {
                        final parsedUrl = Uri.tryParse(message.articleUrl ?? '');
                        if (parsedUrl != null) {
                          try {
                            await launchUrl(parsedUrl, mode: LaunchMode.externalApplication);
                          } catch (e) {
                            debugPrint('Could not launch $parsedUrl: $e');
                          }
                        }
                      },
                      borderRadius: BorderRadius.circular(8),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                        decoration: BoxDecoration(
                          color: const Color(0xFFEFF6FF),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: const Color(0xFFBFDBFE)),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: const [
                            Icon(Icons.open_in_new, size: 14, color: Color(0xFF2563EB)),
                            SizedBox(width: 6),
                            Flexible(
                              child: Text(
                                'Đọc bài viết chi tiết tại đây',
                                style: TextStyle(
                                  color: Color(0xFF2563EB),
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          
          if (message.isUser) ...[
            Container(
              margin: const EdgeInsets.only(left: 8),
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: const Color(0xFFE2E8F0),
                shape: BoxShape.circle,
                border: Border.all(color: Colors.white, width: 2),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 5,
                    offset: const Offset(0, 2),
                  )
                ],
              ),
              child: const Icon(Icons.person, color: Color(0xFF64748B), size: 20),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInputField() {
    return Container(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 12,
        bottom: MediaQuery.of(context).padding.bottom + 12, // Kéo dãn ở màn có tai thỏ
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(24),
          topRight: Radius.circular(24),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.04),
            blurRadius: 20,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFF1F5F9),
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: const Color(0xFFE2E8F0)),
              ),
              child: TextField(
                controller: _messageController,
                style: const TextStyle(fontSize: 15, color: Color(0xFF1E293B)),
                decoration: InputDecoration(
                  hintText: widget.drugName != null 
                      ? 'Hỏi bệnh về ${widget.drugName}...'
                      : 'Nhập câu hỏi tại đây...',
                  hintStyle: const TextStyle(color: Color(0xFF94A3B8), fontSize: 14),
                  border: InputBorder.none,
                  enabledBorder: InputBorder.none,
                  focusedBorder: InputBorder.none,
                  isDense: true,
                  contentPadding: const EdgeInsets.symmetric(vertical: 12),
                ),
                maxLines: 4,
                minLines: 1,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => _sendMessage(),
              ),
            ),
          ),
          const SizedBox(width: 12),
          InkWell(
            onTap: _isLoading ? null : _sendMessage,
            borderRadius: BorderRadius.circular(24),
            child: Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF3B82F6), Color(0xFF2563EB)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                shape: BoxShape.circle,
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFF3B82F6).withOpacity(0.4),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  )
                ],
              ),
              child: const Icon(Icons.send_rounded, color: Colors.white, size: 22),
            ),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime time) {
    final now = DateTime.now();
    if (time.day == now.day &&
        time.month == now.month &&
        time.year == now.year) {
      return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
    }
    return '${time.day}/${time.month} ${time.hour}:${time.minute}';
  }
}

class ChatMessage {
  final String text;
  final bool isUser;
  final DateTime timestamp;
  final bool isError;
  final String? articleUrl;

  ChatMessage({
    required this.text,
    required this.isUser,
    required this.timestamp,
    this.isError = false,
    this.articleUrl,
  });
}
