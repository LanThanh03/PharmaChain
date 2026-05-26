enum MessageSender { user, bot }

class Message {
  final String id;
  final String content;
  
  // Getter for backward compatibility
  String get text => content;
  bool get isBot => sender == MessageSender.bot;
  final MessageSender sender;
  final DateTime timestamp;
  final bool isThinking;

  Message({
    required this.id,
    required this.content,
    required this.sender,
    required this.timestamp,
    this.isThinking = false,
  });

  // Create a thinking message for bot
  factory Message.thinking() {
    return Message(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      content: "Dia5 đang suy nghĩ...",
      sender: MessageSender.bot,
      timestamp: DateTime.now(),
      isThinking: true,
    );
  }

  // Create a user message
  factory Message.user(String content) {
    return Message(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      content: content,
      sender: MessageSender.user,
      timestamp: DateTime.now(),
    );
  }

  // Create a bot message
  factory Message.bot(String content) {
    return Message(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      content: content,
      sender: MessageSender.bot,
      timestamp: DateTime.now(),
    );
  }

  // Copy with method for updating message content (useful for streaming)
  Message copyWith({
    String? id,
    String? content,
    MessageSender? sender,
    DateTime? timestamp,
    bool? isThinking,
  }) {
    return Message(
      id: id ?? this.id,
      content: content ?? this.content,
      sender: sender ?? this.sender,
      timestamp: timestamp ?? this.timestamp,
      isThinking: isThinking ?? this.isThinking,
    );
  }
}
