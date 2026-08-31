# Memoir — Daftar Pekerjaan 2 (Tambahan)

11 pekerjaan tambahan dari sesi berikutnya. SUDAH DIURUTKAN dari yang wajib dulu (bug/blocker) → fitur → polishing.
Semua ada lokasi file + fungsi + arah. GAK dikerjakan, cuma panduan.

================================================================
## 🔴 P0 — BUG / NGE-BLOKIR (harus dulu)
================================================================

### 1. #25 (task lama #25) — Perbaiki logic crop, canvas, dll di Add Postingan
- LOKASI: `ui/components/InstagramImageEditorCanvas.kt` (fungsi `InstagramImageEditorCanvas`, line ~41), `AddPostBottomSheet.kt`.
- FAKTA KODE (pasca strip comment): `InstagramImageEditorCanvas` return transform via `onTransformChange(scale, rotation, offsetX, offsetY, vw, vh)` (line 48, 72-73). Transform disimpan ke `mediaTransformsJson` (AddPostBottomSheet line 322/337).
- ARH:
  - Transform (scale/rotation/offset/viewport) SUDAH dikumpul tapi harus di-apply ke bitmap saat publish (crop ke rect hasil transform, rotate, scale).
  - `mediaTransformsJson` (org.json.JSONArray) sudah ada per-media → baca pas simpan, apply ke file.
  - Pastiin canvas gak clip salah, aspect ratio lock jalan.

### 2. #26 — Thumbnail gak nurut crop/zoom/rotate (masih ukuran asli)
- STATUS: ✅ SUDAH DIKERJAIN MORO (logic apply transform ada di `FileUtils.processAndSaveMediaInFolder`).
- FAKTA KODE (pasca rubah Moro): `AddPostBottomSheet.kt:275` panggil `FileUtils.processAndSaveMediaInFolder(...)` → di `FileUtils.kt:196-259` apply `Matrix.postRotate(rotation)`, `postScale(scale)`, `postTranslate(offsetX,offsetY)` → `Bitmap.createBitmap` sesuai `targetRatioStr` → compress JPEG 90 → simpan file hasil edit. `mediaTransformsJson` (scale/rotation/offset/vw) disimpan di line 322/337.
- ARH (kalau masih ada sisa bug):
  - Pastiin `mediaPaths` di `post.json` point ke file hasil `processAndSaveMediaInFolder` (bukan original).
  - Cek thumbnail di `MediaView` ambil dari path hasil (bukan `mediaTransformsJson` doang).
  - Kalau masih tampil asli: debug apakah `processAndSaveMediaInFolder` dipanggil untuk SEMUA media (foto+video) atau cuma foto. Video gak bisa di-crop via bitmap → butuh handle terpisah (thumbnail frame / overlay).

### 3. #27 — Perbaiki multi-media di postingan (tampilan)
- LOKASI: `FeedPostCard.kt`, `MediaView.kt`, `AddPostBottomSheet.kt` (multi-select).
- ARH:
  - Post multi-media tampil sebagai carousel/pager (swipe antar media).
  - Indikator dot jumlah media.
  - Campur foto+video jalan (lihat #7 task lama).
  - Aspect ratio tiap media dihormati (jangan paksa sama semua).

================================================================
## 🟠 P1 — PROFILE & EDIT FOTO
================================================================

### 4. #21 — Perbaiki page Edit Foto (zoom/crop) biar sesuai tema & lebih bagus
- LOKASI: `ProfileDialogs.kt` → `UCropContract` (ucrop `.withAspectRatio(1f,1f).withMaxResultSize(1000,1000)`).
- ARH:
  - Pakai `UCrop` dengan theme app (warna accent) biar matching.
  - Izinin zoom/pinch + grid 3x3 + rotasi.
  - Hasil compress ke `FileUtils.getMemoirDataDir()/profile_xxx.jpg` (JPEG 85).
  - Tampilin preview circular (CircleShape) di profile.

### 5. #19 — Perbaiki page Edit Profile
- LOKASI: `ui/screens/profile/ProfileDialogs.kt` (ada `UCropContract`, dialog profile), `ui/screens/ProfileScreen.kt`.
- ARH:
  - Pastiin `EditProfileDialog` ada field: nama, bio (username), foto profil.
  - Simpan ke `UserPreferencesRepository` (DataStore).
  - Validasi: nama gak boleh kosong, bio max N char.
  - Preview update langsung (gak perlu restart).

### 6. #20 — Tombol "Ubah" di section profile → langsung ke page Edit Profile
- LOKASI: `ui/screens/ProfileScreen.kt` (section "Edit Profile" / tombol ubah).
- ARH:
  - Cari tombol "Ubah"/"Edit" → `onClick` langsung panggil `EditProfileDialog(show=true)` atau navigate ke route edit profile.
  - Jangan cuma buka settings umum — harus spesifik ke edit profile.

================================================================
## 🟡 P2 — UI / LAYOUT
================================================================

### 7. #24 — Tombol silang (hapus) media di Add Post pindah biar gak kehapus gak sengaja
- LOKASI: `ui/components/AddPostBottomSheet.kt` (grid preview media, line ~844 `Box clickable onSelectMediaItem`).
- ARH:
  - Tombol "X" jangan di tengah/gara gampang ke-klik. Pindah ke **pojok kanan atas** item + kasih margin.
  - Atau tambah konfirmasi: "Hapus media ini?" sebelum remove.
  - `selectedMedia` list di ViewModel → `removeAt(index)` cuma kalau tombol X beneran di-klik.

### 8. #23 — Perbaiki UI Add Story (tata letak & fungsi)
- LOKASI: `ui/components/StoryCreatorDialog.kt` (891 line), `StoriesTray.kt`.
- ARH:
  - Tombol **Gallery + Kamera** pindah ke **pojok kanan bawah** (icon terpisah, bukan di panel atas).
  - FAB publish (panah) sudah benar di kanan bawah → pisahin dari tombol media.
  - Pastiin flow: foto baru (kamera) → preview → pilih gallery (kanan bawah) → add text.
  - Cek `StoryCreatorContent` line 501+ buat posisi exact tombol saat ini.

### 9. #22 — Explore urutan random tiap buka
- LOKASI: `ui/screens/ExploreScreen.kt`, `MemoirViewModel` (list explore).
- ARH:
  - Saat screen dibuka (`LaunchedEffect(Unit)` atau `onResume`), `list = allItems.shuffled()`.
  - Jangan pakai `ORDER BY RANDOM()` SQL (berat). Shuffle di memory tiap masuk screen.
  - Simpan di State biar gak reshuffle tiap recomposition.

================================================================
## 🎬 P3 — ANIMASI (polishing terakhir)
================================================================

### 10. #28 — Perbaiki animasi pindah page
- LOKASI: `ui/screens/MainAppScreens.kt` (`AnimatedNavHost`), `FeedPostCard.kt` (transition).
- ARH:
  - Pakai `MaterialSharedAxis` / `slideInHorizontally + slideOutHorizontally` konsisten.
  - Jangan campur `Crossfade` + `slide` (bikin pusing). Pilih 1 gaya.
  - Duration ~300ms, ease-out.
  - Pastiin `EnterTransition`/`ExitTransition` di-set di `NavHost` composable.

### 11. #29 — Perbaiki animasi geser media di multi-media postingan (beda rasio)
- LOKASI: `FeedPostCard.kt` (pager media), `MediaView.kt`.
- ARH:
  - Geser antar media (pager) halus walau rasio beda (1:1, 4:5, 9:16).
  - Jangan hardcode height — pakai `fillMaxWidth().aspectRatio(media.ratio)` per page.
  - Animate `pager` dengan `rememberPagerState` + `HorizontalPager` (bawaan).
  - Hindari jump/lag pas ganti rasio.

================================================================
## URUTAN EKSEKUSI:
`1(#25 crop logic) → 2(#26 thumbnail) → 3(#27 multi-media) → 4(#21 edit foto) → 5(#19 edit profile) → 6(#20 tombol ubah) → 7(#24 tombol silang) → 8(#23 add story) → 9(#22 explore random) → 10(#28 anim page) → 11(#29 anim geser)`
================================================================
