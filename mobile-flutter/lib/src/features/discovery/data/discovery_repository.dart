import 'package:shared_preferences/shared_preferences.dart';

import '../domain/movie.dart';
import 'discovery_api.dart';

class DiscoveryRepository {
  DiscoveryRepository({DiscoveryApi? api}) : _api = api ?? DiscoveryApi();

  static const _recentSearchesKey = 'recent_searches';
  final DiscoveryApi _api;

  Future<List<Movie>> fetchTrending() => _api.fetchTrending();

  Future<List<Movie>> searchRecommendations(String query) {
    return _api.searchRecommendations(query);
  }

  Future<List<Movie>> smartSearch(String query) => _api.smartSearch(query);

  Future<List<String>> loadRecentSearches() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getStringList(_recentSearchesKey) ?? <String>[];
  }

  Future<void> saveRecentSearch(String query) async {
    final trimmed = query.trim();
    if (trimmed.isEmpty) return;

    final prefs = await SharedPreferences.getInstance();
    final current = prefs.getStringList(_recentSearchesKey) ?? <String>[];
    final next = [
      trimmed,
      ...current.where((item) => item != trimmed),
    ].take(6).toList();
    await prefs.setStringList(_recentSearchesKey, next);
  }
}
