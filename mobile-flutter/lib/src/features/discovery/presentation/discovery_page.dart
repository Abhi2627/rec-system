import 'package:flutter/material.dart';

import '../domain/movie.dart';
import 'discovery_controller.dart';
import 'movie_detail_page.dart';

class DiscoveryPage extends StatefulWidget {
  const DiscoveryPage({super.key});

  @override
  State<DiscoveryPage> createState() => _DiscoveryPageState();
}

class _DiscoveryPageState extends State<DiscoveryPage> {
  late final DiscoveryController _controller;
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller = DiscoveryController()..addListener(_onControllerChanged);
    _controller.initialize();
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_onControllerChanged)
      ..dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onControllerChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> _runSearch(DiscoveryMode mode) async {
    await _controller.runSearch(mode, _searchController.text);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFF7E3C8), Color(0xFFE8E9EE)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: RefreshIndicator(
            onRefresh: _controller.refresh,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
              children: [
                Text(
                  'Movie Discovery',
                  style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: const Color(0xFF1A1A1A),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Trending titles, semantic recommendations, and TMDB reranked search in one client.',
                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                    color: const Color(0xFF4C4C4C),
                  ),
                ),
                const SizedBox(height: 20),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        TextField(
                          controller: _searchController,
                          textInputAction: TextInputAction.search,
                          onSubmitted: (_) =>
                              _runSearch(DiscoveryMode.semanticSearch),
                          decoration: const InputDecoration(
                            hintText:
                                'Try: space adventure, emotional sci-fi...',
                            prefixIcon: Icon(Icons.search),
                          ),
                        ),
                        const SizedBox(height: 14),
                        Wrap(
                          spacing: 10,
                          runSpacing: 10,
                          children: [
                            FilledButton(
                              onPressed: _controller.loading
                                  ? null
                                  : () => _runSearch(
                                      DiscoveryMode.semanticSearch,
                                    ),
                              child: const Text('AI Search'),
                            ),
                            FilledButton.tonal(
                              onPressed: _controller.loading
                                  ? null
                                  : () => _runSearch(DiscoveryMode.smartSearch),
                              child: const Text('Smart Search'),
                            ),
                            OutlinedButton(
                              onPressed: _controller.loading
                                  ? null
                                  : _controller.loadTrending,
                              child: const Text('Trending'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                if (_controller.recentQueries.isNotEmpty) ...[
                  const SizedBox(height: 14),
                  Text(
                    'Recent searches',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: _controller.recentQueries.map((query) {
                      return ActionChip(
                        label: Text(query),
                        onPressed: () {
                          _searchController.text = query;
                          _runSearch(DiscoveryMode.semanticSearch);
                        },
                      );
                    }).toList(),
                  ),
                ],
                if (_controller.savedMovieIds.isNotEmpty) ...[
                  const SizedBox(height: 18),
                  Text(
                    'Saved movies in this session',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: _controller.movies
                        .where((movie) => _controller.isSaved(movie.id))
                        .map(
                          (movie) => ActionChip(
                            avatar: const Icon(Icons.bookmark, size: 18),
                            label: Text(movie.title),
                            onPressed: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (_) => MovieDetailPage(
                                    movie: movie,
                                    isSaved: _controller.isSaved(movie.id),
                                    onToggleSaved: () =>
                                        _controller.toggleSaved(movie),
                                  ),
                                ),
                              );
                            },
                          ),
                        )
                        .toList(),
                  ),
                ],
                const SizedBox(height: 18),
                _SectionHeader(mode: _controller.mode),
                const SizedBox(height: 12),
                if (_controller.loading)
                  const Padding(
                    padding: EdgeInsets.only(top: 32),
                    child: Center(child: CircularProgressIndicator()),
                  )
                else if (_controller.error != null)
                  _ErrorCard(message: _controller.error!)
                else if (_controller.movies.isEmpty)
                  const _EmptyCard()
                else
                  ..._controller.movies.map(
                    (movie) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: _MovieCard(
                        movie: movie,
                        isSaved: _controller.isSaved(movie.id),
                        onToggleSaved: () => _controller.toggleSaved(movie),
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (_) => MovieDetailPage(
                                movie: movie,
                                isSaved: _controller.isSaved(movie.id),
                                onToggleSaved: () =>
                                    _controller.toggleSaved(movie),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.mode});

  final DiscoveryMode mode;

  @override
  Widget build(BuildContext context) {
    final label = switch (mode) {
      DiscoveryMode.trending => 'Trending this week',
      DiscoveryMode.semanticSearch => 'AI recommendations',
      DiscoveryMode.smartSearch => 'TMDB reranked results',
    };

    return Text(
      label,
      style: Theme.of(
        context,
      ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
    );
  }
}

class _MovieCard extends StatelessWidget {
  const _MovieCard({
    required this.movie,
    required this.isSaved,
    required this.onTap,
    required this.onToggleSaved,
  });

  final Movie movie;
  final bool isSaved;
  final VoidCallback onTap;
  final VoidCallback onToggleSaved;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(24),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      movie.title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: onToggleSaved,
                    tooltip: isSaved ? 'Remove from saved' : 'Save movie',
                    icon: Icon(
                      isSaved ? Icons.bookmark : Icons.bookmark_border,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 10,
                runSpacing: 8,
                children: [
                  if (movie.releaseDate.isNotEmpty)
                    _MetaChip(label: movie.releaseDate),
                  if (movie.voteAverage > 0)
                    _MetaChip(
                      label: 'Rating ${movie.voteAverage.toStringAsFixed(1)}',
                    ),
                  if (movie.score != null)
                    _MetaChip(
                      label: 'Score ${movie.score!.toStringAsFixed(3)}',
                    ),
                ],
              ),
              if (movie.overview.isNotEmpty) ...[
                const SizedBox(height: 12),
                Text(
                  movie.overview,
                  maxLines: 4,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    height: 1.4,
                    color: const Color(0xFF505050),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _MetaChip extends StatelessWidget {
  const _MetaChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: const Color(0xFFF2E7D7),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: Theme.of(
          context,
        ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w600),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: const Color(0xFFFFF1EC),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Text(
          message,
          style: Theme.of(
            context,
          ).textTheme.bodyMedium?.copyWith(color: const Color(0xFF7A2E1C)),
        ),
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Text(
          'No movies found for this request yet.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ),
    );
  }
}
