import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/config/app_config.dart';
import '../domain/movie.dart';

class DiscoveryApi {
  DiscoveryApi({http.Client? client}) : _client = client ?? http.Client();

  final http.Client _client;

  Uri _uri(String path, [Map<String, String>? queryParameters]) {
    return Uri.parse(
      '${AppConfig.apiBaseUrl}$path',
    ).replace(queryParameters: queryParameters);
  }

  Future<List<Movie>> fetchTrending() async {
    final response = await _client.get(_uri('/api/discovery/trending'));

    if (response.statusCode != 200) {
      throw Exception('Trending request failed: ${response.body}');
    }

    final decoded = jsonDecode(response.body) as List<dynamic>;
    return decoded
        .map((item) => Movie.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<Movie>> searchRecommendations(String query) async {
    final response = await _client.get(
      _uri('/api/discovery/search', {'q': query}),
    );

    if (response.statusCode != 200) {
      throw Exception('Search request failed: ${response.body}');
    }

    final decoded = jsonDecode(response.body) as Map<String, dynamic>;
    final recommendations = decoded['recommendations'] as List<dynamic>? ?? [];
    return recommendations
        .map((item) => Movie.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<List<Movie>> smartSearch(String query) async {
    final response = await _client.get(
      _uri('/api/discovery/smart-search', {'q': query}),
    );

    if (response.statusCode != 200) {
      throw Exception('Smart search request failed: ${response.body}');
    }

    final decoded = jsonDecode(response.body) as Map<String, dynamic>;
    final results = decoded['results'] as List<dynamic>? ?? [];
    return results
        .map((item) => Movie.fromJson(item as Map<String, dynamic>))
        .toList();
  }
}
