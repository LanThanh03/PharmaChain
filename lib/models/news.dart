class News {
  final String id;
  final String title;
  final String summary;
  final String link;
  final String imageUrl;
  final DateTime publishedDate;
  final String category;

  News({
    required this.id,
    required this.title,
    required this.summary,
    required this.link,
    required this.imageUrl,
    required this.publishedDate,
    required this.category,
  });

  factory News.fromJson(Map<String, dynamic> json) {
    return News(
      id: json['id'] ?? '',
      title: json['title'] ?? '',
      summary: json['summary'] ?? '',
      link: json['link'] ?? '',
      imageUrl: json['image_url'] ?? '',
      publishedDate: DateTime.tryParse(json['published_date'] ?? '') ?? DateTime.now(),
      category: json['category'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'summary': summary,
      'link': link,
      'image_url': imageUrl,
      'published_date': publishedDate.toIso8601String(),
      'category': category,
    };
  }

  // Helper method to get relative time
  String get timeAgo {
    final difference = DateTime.now().difference(publishedDate);
    
    if (difference.inDays > 0) {
      return '${difference.inDays} ngày trước';
    } else if (difference.inHours > 0) {
      return '${difference.inHours} giờ trước';
    } else if (difference.inMinutes > 0) {
      return '${difference.inMinutes} phút trước';
    } else {
      return 'Vừa xong';
    }
  }

  // Helper method to get short summary
  String get shortSummary {
    if (summary.length <= 150) return summary;
    return '${summary.substring(0, 150)}...';
  }
}
