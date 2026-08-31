# Memoir — Daftar Pekerjaan (Panduan Lengkap)

Urutan berdasarkan prioritas fungsional. Kerjakan dari atas ke bawah.
Setiap poin ada: lokasi file, fungsi terkait, dan cara exact.

================================================================
## 🔴 WAJIB DULU (app rusak / nge-blokir fitur lain)
================================================================

### 1. #10 — Picker baca seluruh gallery HP (bukan cuma folder app)
- MASALAH: picker sekarang baca cuma folder lokal app (`FileUtils.getMemoirDataDir()` / `getPostsDir()`), makanya "dikit".
- ARH: ganti ke MediaStore API:
  ```kotlin
  val uri = Media.EXTERNAL_CONTENT_URI
  contentResolver.query(uri, arrayOf(Media._ID, Media.DATA, Media.MIME_TYPE), null, null, "${Media.DATE_ADDED} DESC")
  ```
  Atau paling gampang pakai PhotoPicker:
  ```kotlin
  registerForActivityResult(PickMultipleVisualMedia()) { uris -> ... }
  ```
  Ini baca SEMUA image/video di HP kayak gallery bawaan.

### 2. #8 — Add post bisa pilih video
- MASALAH: picker filter `image/*` doang.
- ARH: MIME jadi `image/*,video/*` atau pakai `PickMultipleVisualMedia()` (otomatis baca dua-duanya).
- Cek `MediaView` udah render video (`VideoPlayerView` ada).

### 3. #7 — Bisa campur foto+video dalam 1 post
- MASALAH: di picker ada guard `if (selected.any{isVideo} && new.isImage) return`.
- ARH: hapus guard itu biar bebas campur. Atau lock ke tipe pertama dipilih.

### 4. #9 — Video gak auto-play semua pas add post (berat)
- MASALAH: tiap video di grid preview instantiate `ExoPlayer` sendiri → berat parah.
- ARH: jangan bikin player per item. Cuma tampilin thumbnail (frame pertama via `MediaMetadataRetriever`) + icon play. Play cuma kalau di-tap. Atau 1 shared player buat preview hidup (cuma 1 yang playing).

### 5. #13 — Crop beneran ke-save (bukan full-size)
- MASALAH: hasil crop gak dipakai, yang disave `mediaPaths` asli (full-size).
- ARH:
  - Pas crop selesai, `bitmap.compress(Bitmap.CompressFormat.JPEG, 85, FileOutputStream(croppedFile))` ke `FileUtils.getPostsDir()/cropped_xxx.jpg`.
  - Update `mediaPaths[index] = croppedFile.absolutePath` SEBELUM `insertPost`/`updatePost`.
  - Cek `onActivityResult`/`ActivityResultLauncher` crop → return URI hasil → assign balik ke list media.
  - Edit post: hapus file crop lama kalau diganti.

================================================================
## 🟠 FITUR INTI
================================================================

### 6. #5 (revisi) — Edit post dari titik tiga (⋮) di card
- TRIGGER: `FeedPostCard` ada `DropdownMenu`/`ModalBottomSheet` dari tombol ⋮ → opsi "Edit".
- ARH:
  - Buka sheet edit (reuse `AddPostBottomSheet` mode edit ATAU bikin `EditPostDialog`).
  - Prefill dari `PostEntity`: caption, location, linkedAlbumId (spinner album), createdAt (DatePicker), mediaPaths (thumbnail yg bisa dihapus/ditambah).
  - Save: `repository.updatePost(post.copy(caption=..., location=..., linkedAlbumId=..., createdAt=..., mediaPaths=...))`.
  - Edit gambar: hapus file lama di storage, tulis baru dari picker, update path.

### 7. #14 — SMB Explorer + folder `Memoir (user)` + export/import
- ARH:
  - Layar SMB Explorer pakai `SmbHelper.listSubfoldersInShare()` + `listMediaFiles()`.
  - EXPORT: pilih folder → bikin `Memoir (nama_user)` via `diskShare.mkdir()` → `syncMemoirFolderToSmb(remoteBaseFolder="Memoir (nama_user)")`.
  - IMPORT: pilih folder `Memoir (nama_user)` → `importMemoirFolderFromSmb()` (baca `post.json` per folder, reconstruct PostEntity, anti-duplikat).
  - WAJIB inisiasi: sebelum export/import, harus ada `SourceConnectionEntity` status ONLINE + folder dipilih. Kalau belum → tombol disabled + pesan "Pilih folder SMB dulu".

### 8. #11 — Add story kayak IG (foto baru + gallery + text)
- ARH: `StoryCreatorDialog` 2 tahap:
  - (a) capture kamera ATAU pick gallery (FAB kanan bawah).
  - (b) preview full-screen + tombol "Add Text" (overlay, pilih warna/style).
  - Save ke `StoryEntity` (`caption`, `textColor`, `textStyle` udah ada di entity).

### 9. #1 + #2 — Story 7 hari + filter expired di home
- #1: `Entities.kt:30` `expiresAt = createdAt + 24*60*60*1000L` → ganti `7*24*60*60*1000L`.
- Pastiin `deleteExpiredStories()` (repository) dipanggil di `MemoirViewModel.init{}` atau scheduler pas app dibuka.
- #2: di `HomeScreen` sebelum tampil story: `stories.filter { it.expiresAt > System.currentTimeMillis() }` — BUANG SEBELUM di-shuffle.

================================================================
## 🟡 POLISHING
================================================================

### 10. #3 — Home post random
- ARH: `MemoirViewModel` map `repository.allPosts` → `.shuffled()` (pakai `mutableStateOf`).
- Shuffle cuma pas data berubah / refresh, simpan di State biar gak reshuffle tiap recomposition.
- JANGAN `ORDER BY RANDOM()` di SQL (berat kalau ribuan row).

### 11. #4 — Pull-to-refresh = reshuffle
- ARH: bungkus `HomeScreen` di `PullToRefreshBox`. `onRefresh` → ViewModel `reshuffle()` assign ulang `posts = posts.shuffled()` + `stories = stories.filter(expired).shuffled()`. Lokal aja, gak network.

### 12. #6 — Caption di atas komentar
- ARH: `CommentsBottomSheet` inject 1 item pseudo `CommentEntity(text=post.caption, isCaption=true)` sebagai teratas. JANGAN simpan ke DB (nanti dobel).

### 13. #12 — Rapiin posisi UI add story
- ARH: `Box(Modifier.fillMaxSize())`:
  - Preview tengah `Modifier.align(Alignment.Center)` + `AspectRatio`.
  - Tombol gallery `Alignment.BottomEnd`, tombol text `Alignment.BottomCenter`.
  - Pakai `Scaffold` + `FloatingActionButton` positioned. `imePadding()` biar gak ketutup keyboard.

### 14. #15 — Bersihin Settings
- ARH: audit `SettingsScreen.kt` + `profile/ProfileDialogs.kt` + `MainAppScreens.kt` (drawer):
  - HAPUS tombol gak guna / "Coming soon" / duplikat.
  - RAPIHIN label ambigu ("Sync" → "Backup ke SMB", "Conn" → "Koneksi SMB").
  - TAMBAH yg kurang: Edit Profil, Kelola SMB, Theme/Dark mode, Hapus Data.
  - GROUPING: Akun / Penyimpanan / Tampilan / Keamanan / Lainnya.

### 15. #16 — Page Edit Profil (pilih foto profile bisa crop/zoom dulu kayak IG)
- ARH:
  - Buka `ProfileDialogs.kt` / `ProfileScreen.kt` → bikin `EditProfileDialog` (nama, bio, foto).
  - Pilih foto: pakai PhotoPicker / `PickVisualMedia()` → dapet URI.
  - CROP + ZOOM: pakai library crop (misal `com.yalantis.ucrop` atau `androidx.activity.result` + `CropImageContract`) dengan mode `CROP_IMAGE_ACTIVITY_REQUEST_CODE` → user bisa zoom/rotate sebelum confirm.
  - Save: hasil crop compress ke file profile (`FileUtils.getMemoirDataDir()/profile_xxx.jpg`), update URI ke `UserPreferencesRepository` / entity profil.
  - JANGAN langsung pakai URI mentah — harus lewat crop dulu (kayak IG: pick → crop circle/square → save).

### 16. #17 — Fitur ganti bahasa (English / Indonesia, default English) + perbaiki penulisan
- ARH:
  - Tambah `res/values/strings.xml` (English, default) + `res/values-in/strings.xml` (Indonesia).
  - Semua teks hardcode di Compose (Text("...")) ganti ke `stringResource(R.string.xxx)` — ini yang dimaksud "perbaiki penulisan semau di projek" (hindari hardcoded, konsisten).
  - Setting bahasa: di `SettingsScreen` tambah dropdown/segmented "Language: English | Indonesia".
  - Simpan pilihan ke `UserPreferencesRepository` (DataStore/Preferences).

  - Default = English (jangan lupa set default di preferences saat first run).
  - Catatan: teks dari DB (caption user) gak diterjemahin — cuma UI string aja.

### 17. #18 — Add post: dropdown album gallery HP + icon pilih sumber media
- ARH:
  - **Dropdown Album (kiri bagian Dropdown Sumber Media gantii ini):** list album gallery HP (pakai MediaStore `BUCKET_DISPLAY_NAME`) → user pilih album, grid langsung filter media album itu. Gak perlu scroll semua media sekaligus. Default = "Semua".
  - **Icon sumber di kanan:** tombol (~FAB/icon di pojok kanan , tombol Tombol Cepat kamera dan galalery hapus ganti ini ) → kalau diklik, muncul dropdown pilihan SUMBER:
    1. Gallery HP (default) → `PickMultipleVisualMedia()` baca semua media (lihat #10).
    2. Kamera langsung → `TakePicture`/`CaptureVideo`.
    3. Folder SMB → `SmbHelper.listMediaFiles()` + `downloadMediaToCache()` (dependency #14).
    4. Cloud storage → `source_connections` type CLOUD.
  - Fokus: Gallery HP (album + semua media) harus jalan dulu (dependency #10). SMB/Cloud nyusul.
  - UI: dropdown album di atas grid, icon sumber di pojok kanan atas sheet.

================================================================
## URUTAN EKSEKUSI:
`1(#10) → 2(#8) → 3(#7) → 4(#9) → 5(#13) → 6(#5) → 7(#14) → 8(#11) → 9(#1+#2) → 10(#3) → 11(#4) → 12(#6) → 13(#12) → 14(#15) → 15(#16) → 16(#17) → 17(#18)`
================================================================
