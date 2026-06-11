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

Planify adalah task manager bergaya **Retro 8-Bit Brutalist** yang dibuat sebagai proyek akhir semester. Tujuannya sederhana: satu aplikasi yang cukup untuk mengelola tugas harian, sesi fokus (Pomodoro), jadwal (Google Calendar), dan cuaca — semua dalam satu tampilan yang tidak membosankan.

Setiap tombol yang ditekan mengeluarkan suara 8-bit. Splash screen punya efek shimmer. Statistik produktivitas tercatat rapi. Dan desainnya sengaja dibuat bold dan kontras supaya tidak tertukar dengan aplikasi lain.

---

## Fitur

**Task Manager**
Tambah, edit, hapus tugas. Ada label prioritas (Low / Medium / High) dan sistem tracking otomatis antara Pending dan Completed. Semua data tersimpan offline via Room (SQLite).

**Pomodoro Timer**
Timer fokus 25 menit dengan sesi istirahat. Loading bar bergaya retro. Setiap sesi yang selesai otomatis masuk ke statistik "Pomodoro Today" di halaman Home dan Mission Stats di Profile.

**Kalender (Google Calendar Sync)**
Tampilkan jadwal langsung dari Google Calendar. Login Google hanya digunakan untuk kalender — tidak memengaruhi akun lokal aplikasi.

**Cuaca Real-Time**
Menggunakan GPS untuk deteksi lokasi, lalu tarik data dari OpenWeather API. Ada sistem cache agar tidak boros data. Ikon cuaca otomatis menyesuaikan kondisi (hujan, mendung, cerah, dll).

**Profile & Mission Stats**
Pantau Completion Rate, total Pomodoro, dan streak harian. Bisa ganti avatar dari galeri — foto otomatis di-crop jadi persegi dan disimpan lokal. Tidak perlu internet.

**Settings**
Ganti durasi Pomodoro, atur tema (Light/Dark/System), dan kontrol suara aplikasi.

---

## Screenshot

<img src="Screenshot/PLANIFYmockup.jpg" alt="Planify Mockup" width="720"/>

---

## Tech Stack

| Layer | Teknologi |
|---|---|
| Bahasa | Java 17 |
| UI | ViewBinding, Material Components, Custom XML |
| Arsitektur | MVVM + LiveData + ViewModel |
| Database | Room (SQLite), SharedPreferences |
| Networking | Retrofit2, OkHttp3 |
| API Eksternal | Google Calendar API, OpenWeather API |
| Layanan Google | Play Services Location, Google Sign-In |
| Lainnya | Facebook Shimmer, BitmapFactory (avatar) |

---

## Cara Menjalankan

**Prasyarat:** Android Studio + JDK 17 + device/emulator Android 11+ (API 30+)

```bash
# 1. Clone repo
git clone https://github.com/username-anda/planify.git

# 2. Buka di Android Studio, lalu sync Gradle

# 3. Buat file local.properties di root project, tambahkan:
WEATHER_API_KEY=your_openweather_api_key_here

# 4. Taruh google-services.json ke folder app/
#    (diperlukan untuk fitur Google Calendar)

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
│   └── repository/     # TaskRepo, WeatherRepo
├── service/            # PomodoroTimerService (foreground)
├── receiver/           # NotificationReceiver
├── ui/
│   ├── activity/       # Login, Signup, Splash, EditProfile, dll
│   ├── fragment/       # Home, Task, Pomodoro, Calendar, Profile
│   ├── viewmodel/      # TaskVM, WeatherVM, CalendarVM
│   ├── adapter/        # RecyclerView adapters
│   └── bottomsheet/    # Bottom sheet dialogs
└── utils/              # Constants, SoundManager, AvatarUtils, dll
```

---

<sub>Dibuat untuk Final Lab Semester 4 · 2026</sub>
