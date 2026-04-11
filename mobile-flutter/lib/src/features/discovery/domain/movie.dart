class Movie {
  const Movie({
    required this.id,
    required this.title,
    required this.overview,
    this.posterPath = '',
    this.releaseDate = '',
    this.voteAverage = 0,
    this.score,
  });

  final int id;
  final String title;
  final String overview;
  final String posterPath;
  final String releaseDate;
  final double voteAverage;
  final double? score;

  factory Movie.fromJson(Map<String, dynamic> json) {
    return Movie(
      id: (json['id'] as num?)?.toInt() ?? 0,
      title: (json['title'] as String?) ?? 'Untitled',
      overview: (json['overview'] as String?) ?? '',
      posterPath: (json['poster_path'] as String?) ?? '',
      releaseDate: (json['release_date'] as String?) ?? '',
      voteAverage: (json['vote_average'] as num?)?.toDouble() ?? 0,
      score: (json['score'] as num?)?.toDouble(),
    );
  }
}
