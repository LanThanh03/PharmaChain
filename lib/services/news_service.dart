// import 'dart:convert';
// import 'package:http/http.dart' as http;
import '../models/news.dart';

class NewsService {
  // static const String _baseUrl = 'https://api.example.com'; // Thay đổi thành URL backend thực tế
  
  // Mock data for testing - replace with real API call later
  Future<List<News>> getNews() async {
    // Simulate network delay
    await Future.delayed(const Duration(seconds: 1));
    
    // Mock news data based on Sức khỏe & Đời sống format
    final mockNews = [
      {
        'id': '1',
        'title': 'Phòng ngừa bệnh tim mạch hiệu quả bằng chế độ ăn uống khoa học',
        'summary': 'Các chuyên gia tim mạch khuyến cáo nên xây dựng chế độ ăn uống giàu omega-3, hạn chế muối và đường để giảm nguy cơ mắc bệnh tim mạch. Nghiên cứu mới cho thấy...',
        'link': 'https://suckhoedoisong.vn/phong-ngua-benh-tim-mach-hieu-qua',
        'image_url': 'https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400',
        'published_date': DateTime.now().subtract(const Duration(hours: 2)).toIso8601String(),
        'category': 'Tim mạch',
      },
      {
        'id': '2',
        'title': 'Vaccine COVID-19 mũi 4: Ai nên tiêm và khi nào?',
        'summary': 'Bộ Y tế khuyến cáo những đối tượng có nguy cơ cao như người trên 65 tuổi, người có bệnh nền nên tiêm mũi vaccine COVID-19 thứ 4 để tăng cường miễn dịch...',
        'link': 'https://suckhoedoisong.vn/vaccine-covid-19-mui-4',
        'image_url': 'https://images.unsplash.com/photo-1584515933487-779824d29309?w=400',
        'published_date': DateTime.now().subtract(const Duration(hours: 5)).toIso8601String(),
        'category': 'Vaccine',
      },
      {
        'id': '3',
        'title': 'Triệu chứng đau đầu cảnh báo bệnh nguy hiểm bạn không nên bỏ qua',
        'summary': 'Đau đầu là triệu chứng phổ biến nhưng nếu kèm theo sốt cao, buồn nôn, nhìn mờ có thể là dấu hiệu của những bệnh nghiêm trọng cần được điều trị khẩn cấp...',
        'link': 'https://suckhoedoisong.vn/trieu-chung-dau-dau-canh-bao',
        'image_url': 'https://images.unsplash.com/photo-1594824804732-5eaaea6b8b83?w=400',
        'published_date': DateTime.now().subtract(const Duration(hours: 8)).toIso8601String(),
        'category': 'Thần kinh',
      },
      {
        'id': '4',
        'title': 'Chế độ dinh dưỡng cho người bệnh tiểu đường type 2',
        'summary': 'Người mắc tiểu đường type 2 cần kiểm soát chặt chẽ lượng carbohydrate, tăng cường rau xanh và protein. Chuyên gia dinh dưỡng chia sẻ thực đơn cụ thể...',
        'link': 'https://suckhoedoisong.vn/che-do-dinh-duong-tieu-duong',
        'image_url': 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400',
        'published_date': DateTime.now().subtract(const Duration(days: 1)).toIso8601String(),
        'category': 'Dinh dưỡng',
      },
      {
        'id': '5',
        'title': 'Tầm quan trọng của việc tập thể dục đối với sức khỏe tâm thần',
        'summary': 'Nghiên cứu quốc tế mới nhất chứng minh tập thể dục 30 phút mỗi ngày có thể giảm 40% nguy cơ trầm cảm và lo âu. Các bài tập đơn giản tại nhà...',
        'link': 'https://suckhoedoisong.vn/tap-the-duc-suc-khoe-tam-than',
        'image_url': 'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400',
        'published_date': DateTime.now().subtract(const Duration(days: 1, hours: 3)).toIso8601String(),
        'category': 'Tâm thần',
      },
      {
        'id': '6',
        'title': 'Cách nhận biết và xử lý cơn đau thắt ngực',
        'summary': 'Đau thắt ngực có thể là dấu hiệu cảnh báo đau tim. Bác sĩ tim mạch hướng dẫn cách phân biệt đau thắt ngực do tim và các nguyên nhân khác, cách sơ cứu...',
        'link': 'https://suckhoedoisong.vn/dau-that-nguc-cach-xu-ly',
        'image_url': 'https://images.unsplash.com/photo-1530497610245-94d3c16cda28?w=400',
        'published_date': DateTime.now().subtract(const Duration(days: 2)).toIso8601String(),
        'category': 'Tim mạch',
      },
    ];
    
    return mockNews.map((json) => News.fromJson(json)).toList();
  }
  
  // Real API implementation (commented out for now)
  /*
  Future<List<News>> _callRealAPI() async {
    try {
      final response = await http.get(
        Uri.parse('$_baseUrl/api/news'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer YOUR_API_KEY',
        },
      );
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body) as List;
        return data.map((json) => News.fromJson(json)).toList();
      } else {
        throw Exception('Failed to load news');
      }
    } catch (e) {
      throw Exception('Error fetching news: $e');
    }
  }
  */
  
  // Get news by category
  Future<List<News>> getNewsByCategory(String category) async {
    final allNews = await getNews();
    return allNews.where((news) => news.category.toLowerCase().contains(category.toLowerCase())).toList();
  }
  
  // Search news
  Future<List<News>> searchNews(String query) async {
    final allNews = await getNews();
    return allNews.where((news) => 
      news.title.toLowerCase().contains(query.toLowerCase()) ||
      news.summary.toLowerCase().contains(query.toLowerCase())
    ).toList();
  }
}
