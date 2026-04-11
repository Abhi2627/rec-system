import 'package:flutter/material.dart';

import '../domain/movie.dart';
import 'discovery_controller.dart';
import 'movie_detail_page.dart';

class SavedMoviesPage extends StatefulWidget {
  const SavedMoviesPage({super.key, required this.controller});

  final DiscoveryController controller;

  @override
  State<SavedMoviesPage> createState() => _SavedMoviesPageState();
}

class _SavedMoviesPageState extends State<SavedMoviesPage> {
  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_onControllerChanged);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onControllerChanged);
    super.dispose();
  }

  void _onControllerChanged() {
    if (mounted) {
      setState(() {});
    }
  }

  void _openMovie(Movie movie) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => MovieDetailPage(
          movie: movie,
          isSaved: widget.controller.isSaved(movie.id),
          onToggleSaved: () => widget.controller.toggleSaved(movie),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final movies = widget.controller.savedMovies;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            colors: [Color(0xFFE8E9EE), Color(0xFFF7E3C8)],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
        child: SafeArea(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            children: [
              Text(
                'Saved Movies',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: const Color(0xFF1A1A1A),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Your personal shortlist lives here, even when discovery results change.',
                style: Theme.of(
                  context,
                ).textTheme.bodyLarge?.copyWith(color: const Color(0xFF4C4C4C)),
              ),
              const SizedBox(height: 20),
              if (movies.isEmpty)
                const _EmptySavedCard()
              else
                ...movies.map(
                  (movie) => Padding(
                    padding: const EdgeInsets.only(bottom: 14),
                    child: _SavedMovieCard(
                      movie: movie,
                      onOpen: () => _openMovie(movie),
                      onRemove: () => widget.controller.toggleSaved(movie),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SavedMovieCard extends StatelessWidget {
  const _SavedMovieCard({
    required this.movie,
    required this.onOpen,
    required this.onRemove,
  });

  final Movie movie;
  final VoidCallback onOpen;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(24),
        onTap: onOpen,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      movie.title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 10),
                    if (movie.releaseDate.isNotEmpty)
                      Text(
                        movie.releaseDate,
                        style: Theme.of(context).textTheme.labelLarge,
                      ),
                    if (movie.overview.isNotEmpty) ...[
                      const SizedBox(height: 10),
                      Text(
                        movie.overview,
                        maxLines: 3,
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
              IconButton(
                onPressed: onRemove,
                tooltip: 'Remove from saved',
                icon: const Icon(Icons.bookmark_remove_outlined),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptySavedCard extends StatelessWidget {
  const _EmptySavedCard();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Text(
          'You have not saved any movies yet. Bookmark titles from discovery to build your list.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
      ),
    );
  }
}
