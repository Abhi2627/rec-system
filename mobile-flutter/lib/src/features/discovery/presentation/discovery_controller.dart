import 'package:flutter/foundation.dart';

import '../data/discovery_repository.dart';
import '../domain/movie.dart';

enum DiscoveryMode { trending, semanticSearch, smartSearch }

class DiscoveryController extends ChangeNotifier {
  DiscoveryController({DiscoveryRepository? repository})
    : _repository = repository ?? DiscoveryRepository();

  final DiscoveryRepository _repository;

  DiscoveryMode mode = DiscoveryMode.trending;
  bool loading = true;
  String? error;
  List<Movie> movies = const [];
  List<Movie> savedMovies = const [];
  List<String> recentQueries = const [];
  String lastQuery = '';

  Future<void> initialize() async {
    recentQueries = await _repository.loadRecentSearches();
    savedMovies = await _repository.loadSavedMovies();
    notifyListeners();
    await loadTrending();
  }

  Future<void> loadTrending() async {
    mode = DiscoveryMode.trending;
    loading = true;
    error = null;
    notifyListeners();

    try {
      movies = await _repository.fetchTrending();
    } catch (err) {
      error = err.toString();
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> runSearch(DiscoveryMode nextMode, String query) async {
    final trimmedQuery = query.trim();
    if (trimmedQuery.isEmpty) {
      error = 'Enter a search prompt first.';
      notifyListeners();
      return;
    }

    mode = nextMode;
    lastQuery = trimmedQuery;
    loading = true;
    error = null;
    notifyListeners();

    try {
      await _repository.saveRecentSearch(trimmedQuery);
      recentQueries = await _repository.loadRecentSearches();
      movies = nextMode == DiscoveryMode.smartSearch
          ? await _repository.smartSearch(trimmedQuery)
          : await _repository.searchRecommendations(trimmedQuery);
    } catch (err) {
      error = err.toString();
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> refresh() async {
    if (mode == DiscoveryMode.trending) {
      await loadTrending();
      return;
    }

    await runSearch(mode, lastQuery);
  }

  bool isSaved(int movieId) => savedMovies.any((movie) => movie.id == movieId);

  Future<void> toggleSaved(Movie movie) async {
    await _repository.toggleSavedMovie(movie);
    savedMovies = await _repository.loadSavedMovies();
    notifyListeners();
  }
}
