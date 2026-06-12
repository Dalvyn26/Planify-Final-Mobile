<div align="center">
  <img src="Screenshot/Banner.jpg" alt="Planify" width="100%" />
</div>

<br/>

<div align="center">

# Planify

**Aplikasi produktivitas Android bergaya 8-bit yang dirancang untuk orang yang serius soal waktu mereka (Asik ee).**

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)](#)
[![Android](https://img.shields.io/badge/Android-API_30%2B-green?style=flat-square&logo=android)](#)
[![Room](https://img.shields.io/badge/Room-SQLite-blue?style=flat-square)](#)
[![MVVM](https://img.shields.io/badge/Architecture-MVVM-purple?style=flat-square)](#)

</div>

---

## Tentang Aplikasi

**Planify** adalah aplikasi *task manager* inovatif berbasis Android native yang mengusung estetika **Retro 8-Bit Brutalist**. Dirancang khusus sebagai proyek akhir semester, aplikasi ini bertujuan menciptakan ruang kerja digital bebas distraksi dengan menyatukan manajemen tugas harian, sesi fokus Pomodoro, sinkronisasi Google Calendar, serta widget cuaca real-time ke dalam satu dashboard *high-contrast* yang interaktif.

Setiap interaksi tombol memicu feedback audio retro 8-bit yang khas, memberikan kepuasan instan layaknya menyelesaikan misi di dalam *game classic*.

---

## Fitur Utama

Berikut adalah modul navigasi & fitur unggulan yang terintegrasi di dalam Planify:

### ⊡ `01` Task Manager 
* **Kontrol Penuh**: Tambah, edit, dan hapus tugas harian Anda secara dinamis.
* **Prioritas Misi**: Tentukan tingkat urgensi tugas menggunakan indikator prioritas `Low`, `Medium`, atau `High`.
* **State Management**: Sistem tracking otomatis yang memisahkan tugas aktif (`Pending`) dan tugas selesai (`Completed`).
* **Offline First**: Semua data tersimpan aman secara lokal tanpa memerlukan koneksi internet melalui database SQLite (Room Persistence Library).

### ⏱ `02` Pomodoro Timer 
* **Sesi Pomodoro**: Siklus fokus terintegrasi (25 menit kerja, diikuti dengan 5 menit istirahat).
* **Retro Progress**: Animasi loading bar pixelated yang estetik saat timer berjalan.
* **Mission Tracker**: Setiap sesi yang sukses diselesaikan otomatis direkam dan dikonversi menjadi poin statistik harian Anda.

### 📅 `03` Google Calendar Sync
* **Real-time Sync**: Tampilkan jadwal acara pribadi dari Google Calendar langsung pada tab kalender aplikasi.
* **Google OAuth**: Integrasi API yang aman dengan Google Sign-In tanpa mengganggu sistem autentikasi lokal aplikasi.

### 🌤 `04` Weather Widget
* **Deteksi Lokasi**: Integrasi GPS Location Services untuk membaca posisi geografis secara otomatis.
* **Informasi Real-Time**: Data cuaca dan suhu ditarik langsung dari OpenWeather API.
* **Smart Cache**: Dilengkapi sistem penyimpanan cache untuk menghemat kuota data seluler Anda.
* **Tombol Refresh**: Tombol refresh manual tersedia untuk memperbarui data cuaca saat terjadi error atau tidak ada jaringan.

### 👤 `05` Profile & Mission Stats
* **Statistik Produktivitas**: Pantau metrik kemajuan Anda seperti *Completion Rate*, *Total Pomodoro*, dan rekor hari beruntun (*Streak*).
* **Custom Avatar**: Ambil gambar langsung dari Galeri HP Anda, dengan pemrosesan *auto-crop* persegi yang selaras dengan tema Brutalist.

### ⚙️ `06` Settings
* **Kustomisasi Durasi**: Atur panjang sesi Pomodoro sesuai preferensi fokus Anda.
* **Manajemen Tema**: Dukungan mode gelap (*Dark Mode*), mode terang (*Light Mode*), atau mengikuti sistem Android (*System Default*).
* **Audio Switch**: Aktifkan atau matikan musik & efek suara retro 8-bit kapan saja.

---

## App Design Preview

<div align="center">
  <img src="Screenshot/PLANIFYmockup.jpg" alt="Planify Mockup Showcase" width="100%" style="border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,0.15);" />
</div>

---

## ✅ Pemenuhan Spesifikasi Teknis

Tabel berikut merangkum pemenuhan seluruh syarat teknis yang diwajibkan dalam project ini:

### 1. Activity — ✅ TERPENUHI

Aplikasi memiliki **7 Activity** yang berbeda, jauh melebihi syarat minimal 2 Activity:

| Activity | Keterangan |
| :--- | :--- |
| `SplashActivity` | **Launcher Activity** — Layar pembuka dengan animasi, cek sesi login |
| `MainActivity` | Activity utama yang menjadi host navigasi Bottom Nav + Fragment |
| `LoginActivity` | Halaman login pengguna dengan autentikasi Room DB |
| `SignupActivity` | Halaman registrasi pengguna baru |
| `EditProfileActivity` | Halaman edit nama, avatar, dan data profil |
| `SettingsActivity` | Halaman pengaturan tema, suara, dan durasi Pomodoro |
| `MissionLogActivity` | Halaman riwayat pencapaian / misi yang telah selesai |

> `SplashActivity` adalah **Launcher Activity** yang didefinisikan dengan `intent-filter ACTION_MAIN + CATEGORY_LAUNCHER` pada `AndroidManifest.xml`.

---

### 2. Intent — ✅ TERPENUHI

`Intent` digunakan secara konsisten untuk berpindah antar Activity di seluruh aplikasi:

| Dari | Ke | Keterangan |
| :--- | :--- | :--- |
| `SplashActivity` | `MainActivity` | Jika sesi login tersimpan |
| `SplashActivity` | `LoginActivity` | Jika belum login |
| `LoginActivity` | `SignupActivity` | Daftar akun baru |
| `LoginActivity` | `MainActivity` | Setelah login berhasil |
| `SignupActivity` | `LoginActivity` | Kembali ke login |
| `SignupActivity` | `MainActivity` | Setelah daftar berhasil |
| `ProfileFragment` | `SettingsActivity` | Buka halaman pengaturan |
| `ProfileFragment` | `EditProfileActivity` | Edit profil pengguna |
| `ProfileFragment` | `MissionLogActivity` | Lihat riwayat misi |

---

### 3. RecyclerView — ✅ TERPENUHI

`RecyclerView` digunakan di **4 tempat** berbeda dengan adapter yang masing-masing terpisah:

| Adapter | Digunakan di | Data yang Ditampilkan |
| :--- | :--- | :--- |
| `TaskAdapter` | `TaskFragment` & `HomeFragment` | Daftar tugas aktif dan selesai (multi-viewtype) |
| `CalendarDayAdapter` | `CalendarFragment` | Grid hari-hari dalam satu bulan |
| `CalendarAgendaAdapter` | `CalendarFragment` | Daftar agenda/event dari Google Calendar |
| *(Mission Log Adapter)* | `MissionLogActivity` | Riwayat pencapaian/misi |

---

### 4. Fragment & Navigation Component — ✅ TERPENUHI

Aplikasi memiliki **5 Fragment** yang dikelola sepenuhnya oleh Android Navigation Component:

| Fragment | Fungsi |
| :--- | :--- |
| `HomeFragment` | Dashboard utama (cuaca, statistik, tugas hari ini) |
| `TaskFragment` | Manajemen tugas lengkap dengan swipe-to-delete |
| `PomodoroFragment` | Timer Pomodoro dengan countdown & progress bar |
| `CalendarFragment` | Kalender bulanan + sinkronisasi Google Calendar |
| `ProfileFragment` | Profil pengguna, statistik XP, dan navigasi menu |

Navigasi antar Fragment diatur melalui `nav_graph.xml` menggunakan `NavController` dan `NavigationUI.setupWithNavController()` yang terhubung ke `BottomNavigationView`.

---

### 5. Background Thread — ✅ TERPENUHI

Semua operasi berat dijalankan di latar belakang menggunakan **`ExecutorService`** dan **`Handler`**:

| Kelas | Implementasi | Operasi yang Dijalankan |
| :--- | :--- | :--- |
| `TaskRepository` | `Executors.newFixedThreadPool(4)` + `Handler(Looper.getMainLooper())` | Insert, Update, Delete, Query task dari Room DB |
| `CalendarRepository` | `ExecutorService` + `Handler mainHandler` | Fetch, Create, Delete event Google Calendar API |
| `EditProfileActivity` | `Executors.newSingleThreadExecutor()` | Simpan & ambil data profil dari Room DB |
| `LoginActivity` | `Executors.newSingleThreadExecutor()` | Verifikasi kredensial dari Room DB |
| `SignupActivity` | `Executors.newSingleThreadExecutor()` | Insert user baru ke Room DB |
| `SplashActivity` | `Handler(Looper.getMainLooper())` | Delay animasi splash screen |

Hasil dari thread background dikembalikan ke Main Thread melalui `mainHandler.post()` untuk update UI.

---

### 6. Networking (Retrofit) — ✅ TERPENUHI

Aplikasi mengintegrasikan **Retrofit 2 + OkHttp 3** untuk mengambil data dari REST API:

**Library yang digunakan:**
- `Retrofit 2` — HTTP client untuk Android
- `OkHttp 3` — HTTP client layer dengan `HttpLoggingInterceptor`
- `Gson Converter` — Parsing response JSON otomatis

**API yang diintegrasikan:**

| API | Service | Endpoint | Data yang Ditampilkan |
| :--- | :--- | :--- | :--- |
| **OpenWeatherMap API** | `WeatherApiService` | `GET data/2.5/weather` | Suhu, kondisi cuaca, ikon cuaca di `HomeFragment` |
| **Google Calendar API** | `CalendarApiService` | Google API HTTP | Event kalender di `CalendarFragment` |

**Tombol Refresh saat gagal jaringan:** ✅
- Di `HomeFragment`, terdapat `btnRefreshWeather` yang memanggil `weatherViewModel.forceRefresh()` untuk memperbarui data cuaca secara manual.
- `NetworkUtils.isConnected()` digunakan untuk mendeteksi status koneksi jaringan sebelum melakukan request.
- Jika tidak ada jaringan, pesan error ditampilkan via `tvWeatherError` dan data cuaca terakhir dari cache `SharedPreferences` tetap ditampilkan.

---

### 7. Local Data Persistent — ✅ TERPENUHI

Aplikasi menggunakan **dua mekanisme** penyimpanan data lokal:

#### a) Room Database (SQLite)
Room digunakan untuk menyimpan data terstruktur secara persisten:

| Entity | DAO | Data yang Disimpan |
| :--- | :--- | :--- |
| `Task` | `TaskDao` | Tugas (judul, prioritas, status, deadline, dll) |
| `User` | `UserDao` | Data akun pengguna (nama, email, password hash) |

Seluruh data tugas tersedia **offline** — pengguna tetap bisa melihat, menambah, dan menyelesaikan tugas meski tanpa koneksi internet.

#### b) SharedPreferences
`SharedPreferences` digunakan untuk menyimpan data sesi dan preferensi pengguna:

| Key | Data yang Disimpan |
| :--- | :--- |
| `user_id`, `user_name`, `user_email` | Sesi login aktif |
| `user_xp_<id>`, `total_pomodoros` | Poin XP & statistik produktivitas |
| `weather_cache_*` | Cache data cuaca (hemat data seluler) |
| `theme_preference` | Preferensi tema (Dark/Light/System) |
| `sound_enabled` | Status on/off efek suara |
| `pomodoro_duration` | Durasi timer Pomodoro yang dikustomisasi |

#### c) Dua Tema (Dark / Light) ✅
Aplikasi mendukung **tiga pilihan tema** yang bisa diatur dari halaman Settings:
- **Dark Mode** — Menggunakan `values-night/themes.xml` dan `values-night/colors.xml`
- **Light Mode** — Menggunakan `values/themes.xml` dan `values/colors.xml`
- **System Default** — Mengikuti pengaturan sistem Android secara otomatis

Theme diimplementasikan menggunakan `Theme.MaterialComponents.DayNight.NoActionBar` dengan override resource di folder `values-night/`.

---

## Tech Stack & Arsitektur

Konstruksi teknis Planify dibangun menggunakan standar pengembangan Android modern dan stabil:

| Komponen | Implementasi Teknologi |
| :--- | :--- |
| **Language** | <code>Java 17</code> (LTS) |
| **Architecture** | <code>MVVM</code> (Model-View-ViewModel) dengan <code>LiveData</code> & <code>ViewModel</code> |
| **User Interface** | <code>ViewBinding</code>, <code>Material Design 3</code>, Custom Shape & XML Vectors |
| **Database** | <code>Room DB</code> (SQLite Wrapper), <code>SharedPreferences</code> |
| **Networking** | <code>Retrofit 2</code>, <code>OkHttp 3</code> (untuk REST API) |
| **Google Services** | <code>Play Services Location</code>, <code>Google Calendar API</code>, <code>Google Sign-In</code> |
| **Visual Effects** | <code>Facebook Shimmer</code> (efek loading skeleton), Custom bitmap cropping |
| **Audio Engine** | <code>SoundPool</code> / <code>MediaPlayer</code> (gaya Retro SoundManager) |

---

## Cara Menjalankan

**Prasyarat:** Android Studio + JDK 17 + device/emulator Android 11+ (API 30+)

```bash
# 1. Clone repo
git clone https://github.com/Dalvyn26/Planify-Final-Mobile.git

# 2. Buka di Android Studio, lalu sync Gradle

# 3. Buat file local.properties di root project, tambahkan:
WEATHER_API_KEY=your_openweather_api_key_here

# 4. Taruh google-services.json ke folder app/
#    (diperlukan untuk fitur Google Calendar & Firebase)

# 5. Run
./gradlew assembleDebug
```

> **Catatan:** Tanpa `google-services.json`, fitur Calendar tidak akan berfungsi. Fitur lain (Task, Pomodoro, Weather, Profile) tetap bisa digunakan secara offline.

---

## Struktur Project

```
app/src/main/java/com/example/planify/
├── data/
│   ├── local/          # Room DAO, Entity, Database
│   ├── remote/         # Retrofit API service & model
│   └── repository/     # TaskRepo, CalendarRepo
├── service/            # PomodoroTimerService (foreground)
├── receiver/           # NotificationReceiver
├── ui/
│   ├── activity/       # Login, Signup, Splash, EditProfile, Settings, MissionLog, Main
│   ├── fragment/       # Home, Task, Pomodoro, Calendar, Profile
│   ├── viewmodel/      # TaskVM, WeatherVM, CalendarVM
│   ├── adapter/        # RecyclerView adapters
│   └── bottomsheet/    # Bottom sheet dialogs
└── utils/              # Constants, NetworkUtils, SoundManager, AvatarUtils, dll
```

---

<sub>Dibuat untuk Final Lab Semester 4 · 2026</sub>
