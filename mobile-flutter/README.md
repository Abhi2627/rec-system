# mobile_flutter

Flutter client for the Rec System project.

## Features

- Trending movies
- AI semantic search
- TMDB smart search
- Saved movies
- Recent searches

## Run

```bash
flutter pub get
flutter run
```

## API Base URL

Default behavior:
- Android emulator: `http://10.0.2.2:8000`
- iOS simulator / local host: `http://127.0.0.1:8000`

Override for a physical device:

```bash
flutter run --dart-define=API_BASE_URL=http://YOUR_MAC_IP:8000
```

## Main App Files

- `lib/src/app.dart`
- `lib/src/features/discovery/`
