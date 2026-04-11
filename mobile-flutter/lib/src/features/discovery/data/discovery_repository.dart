import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../domain/movie.dart';
import 'discovery_api.dart';

class DiscoveryRepository {
  DiscoveryRepository({DiscoveryApi? api}) : _api = api ?? DiscoveryApi();

  static const _recentSearchesKey = 'recent_searches';
  static const _savedMoviesKey = 'saved_movies';
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

  Future<List<Movie>> loadSavedMovies() async {
    final prefs = await SharedPreferences.getInstance();
    final values = prefs.getStringList(_savedMoviesKey) ?? <String>[];
    return values
        .map((item) => Movie.fromJson(jsonDecode(item) as Map<String, dynamic>))
        .toList();
  }

  Future<void> toggleSavedMovie(Movie movie) async {
    final prefs = await SharedPreferences.getInstance();
    final current = await loadSavedMovies();
    final alreadySaved = current.any((item) => item.id == movie.id);

    if (alreadySaved) {
      current.removeWhere((item) => item.id == movie.id);
    } else {
      current.insert(0, movie);
    }

    await prefs.setStringList(
      _savedMoviesKey,
      current.map((item) => jsonEncode(item.toJson())).toList(),
    );
  }
}
