# Memoir

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?logo=android&logoColor=white)](https://github.com/okkysatria/Memoir/releases/download/v1.0/Memoir.apk)

Jurnal visual offline pribadi bergaya Instagram. Simpan momen sebagai post, story, dan album, dengan dukungan penyimpanan lokal, SMB (NAS), dan cloud.

## Fitur

### Feed & Story
- Feed postingan bergaya Instagram (gambar & video).
- Stories tray dengan viewer & creator dialog.
- Komentar per postingan (CommentsBottomSheet).
- Profil pengguna dengan dialog settings.

### Album & Penyimpanan
- Album lokal, SMB, dan cloud.
- `AlbumListScreen` / `AlbumDetailScreen` untuk manajemen album.
- `ManageConnectionsScreen` untuk hubungkan server SMB / cloud.

### Backup
- `BackupRepository` + `BackupScreen` untuk cadangkan/restor data.
- Worker sinkronisasi SMB (`SmbSyncWorker`, `SmbSyncScheduler`).

### Widget
- App Widget home screen (`MemoirWidget`) via Glance API.

### Keamanan
- Biometric unlock (`BiometricHelper`).
- Permission handling (`PermissionsSetupScreen`, `PermissionRationaleCard`).

## Tech Stack
- **Language**: Kotlin + Coroutines/Flow
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room
- **Image/Video**: Coil, ExoPlayer/Media3
- **SMB**: JCIFS-NG
- **DI**: Hilt
- **Build**: Gradle 8+ (KSP, Roborazzi, Secrets Plugin)
- **Min SDK**: 24 · **Target SDK**: 35

## Project Structure
```
app/src/main/java/com/example/
├── data/
│   ├── backup/      Model backup
│   ├── database/    Entities, DAO, Converters, MemoirDatabase
│   └── repository/  Backup / Memoir / UserPreferences repositories
├── glance/          App Widget (Glance)
├── ui/
│   ├── components/  BottomSheet, FeedPostCard, Story, MediaView, dll
│   ├── screens/     Home, Explore, Activity, Profile, Album, Backup, Settings
│   ├── theme/       Color, Theme, Type
│   └── viewmodel/   MemoirViewModel
├── utils/           Biometric, File, Responsive, SmbHelper
└── worker/          SmbSyncWorker, SmbSyncScheduler
```

## Build & Run
1. Buka di Android Studio (File → Open → folder `memoir`).
2. Buat file `.env` di root, isi: `GEMINI_API_KEY=...` (lihat `.env.example`).
3. Jalankan di emulator / device.

## License
MIT.
