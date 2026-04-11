import 'package:flutter/foundation.dart';

import '../data/discovery_api.dart';
import '../domain/movie.dart';

enum DiscoveryMode { trending, semanticSearch, smartSearch }

class DiscoveryController extends ChangeNotifier {
  DiscoveryController({DiscoveryApi? api}) : _api = api ?? DiscoveryApi();

  final DiscoveryApi _api;

  DiscoveryMode mode = DiscoveryMode.trending;
  bool loading = true;
  String? error;
  List<Movie> movies = const [];
  String lastQuery = '';

  Future<void> loadTrending() async {
    mode = DiscoveryMode.trending;
    loading = true;
    error = null;
    notifyListeners();

    try {
      movies = await _api.fetchTrending();
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
      movies = nextMode == DiscoveryMode.smartSearch
          ? await _api.smartSearch(trimmedQuery)
          : await _api.searchRecommendations(trimmedQuery);
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
}
