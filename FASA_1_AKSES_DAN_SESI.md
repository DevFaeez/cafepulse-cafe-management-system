# Fasa 1 CafePulse · Akses dan sesi

Buka [CafePulse setempat](http://localhost:5174) selepas menjalankan tiga perkhidmatan mengikut [panduan Fasa 0](FASA_0_PERSEDIAAN_TEMPATAN.md). Satu skrin log masuk membawa setiap akaun ke ruang yang sepadan.

| Akaun ujian | Paparan | Akses API |
|---|---|---|
| `kiosk@cafepulse.example` | `/kiosk` | Baca menu tersedia, kategori dan variasi yang berkaitan serta hantar tempahan melalui API. |
| `staff@cafepulse.example` | `/staff` | Baca senarai dan butiran tempahan serta baca dan kemas kini stok melalui API. |
| `manager@cafepulse.example` | `/manager` dan `/staff` | Urus produk, kategori, variasi, pelanggan, stok dan akaun staf atau kiosk. Pengurus juga boleh membaca tempahan. |

Akaun ujian menggunakan `CAFE_DEV_SEED_PASSWORD` dalam `.runtime/dev.env` pada salinan kerja. Pengendali kiosk log masuk menggunakan akaun kiosk khusus. Kiosk tiada akses kepada senarai atau butiran tempahan orang lain dan carian rekod pelanggan melalui nombor telefon.

Skrin pengurus memberi akses ke ruang staf. Fungsi panel pengurus akan dilengkapkan pada Fasa 4. Butang penghantaran tempahan kiosk ialah kerja Fasa 2. Dashboard staf masih menggunakan data contoh sehingga Fasa 3.

## Cara sesi berfungsi

- Token akses sah selama 15 minit secara lalai. Aplikasi memperbaharuinya secara automatik apabila menerima respons 401, selagi sesi belum tamat.
- Tempoh maksimum sesi ialah tujuh hari secara lalai. Sesi disimpan dalam tab untuk membolehkan muat semula dan disahkan semula oleh pelayan sebelum ruang kerja dipaparkan.
- Tekan **Log keluar** untuk membatalkan sesi itu pada pelayan. Token akses dan token pembaharuan daripada sesi yang dibatalkan akan ditolak.
- Pelayan menyemak peranan dan status akaun semasa. Kata laluan disimpan sebagai hash BCrypt. Token yang dikeluarkan sebelum perubahan Fasa 1 perlu diganti dengan log masuk baharu.

## Menyediakan pengurus pertama pada pangkalan data baharu

Untuk akaun sebenar pada pangkalan data baharu, gunakan profil `bootstrap-manager` dari terminal yang mempunyai konfigurasi sambungan dan kunci JWT. Jalankan blok ini dari root salinan kerja selepas menyediakan MySQL dan membina pakej Spring Boot. Masukkan kata laluan apabila terminal menunggu input. Kata laluan yang ditaip tidak dipaparkan.

```sh
set -a
. ./.runtime/dev.env
set +a
export CAFE_BOOTSTRAP_MANAGER_EMAIL='pengurus@example.com'
read -rs CAFE_BOOTSTRAP_MANAGER_PASSWORD
export CAFE_BOOTSTRAP_MANAGER_PASSWORD
"$JAVA_HOME/bin/java" -jar cafe-prediction/target/cafe-prediction-0.0.1-SNAPSHOT.jar \
  --server.address=127.0.0.1 --server.port=0 --spring.profiles.active=bootstrap-manager
unset CAFE_BOOTSTRAP_MANAGER_EMAIL CAFE_BOOTSTRAP_MANAGER_PASSWORD
```

Proses tamat selepas semakan atau penciptaan akaun. Jika sudah ada pengurus, akaun baharu tidak dicipta. Tetapkan alamat e-mel sebenar pengurus apabila menggunakan proses itu untuk kedai.

Pengurus boleh menggunakan `POST /auth/signup` untuk mencipta staf dan `POST /auth/kiosk` untuk mencipta kiosk. Kedua-duanya memerlukan token akses pengurus. Badan JSON menerima `username`, `fullname`, `email`, `password` dan `phoneNumber` pilihan. Pelayan memilih peranan berdasarkan endpoint. Antara muka untuk mengurus akaun ialah kerja Fasa 4.

## Ulang semakan akses setempat

Dari root salinan kerja dengan profil `dev-seed` berjalan:

```sh
set -a
. ./.runtime/dev.env
set +a
python3 dev/check-local.py
python3 dev/check-access.py
```

Ujian akses menyemak log masuk gagal, peranan bagi akaun ujian, bacaan menu dan variasi, sekatan perubahan menu dan bacaan tempahan, pembaharuan token serta pembatalan sesi pada pelayan.

## Keputusan semakan

- Chrome berjaya membuka paparan kiosk, staf dan pengurus dengan akaun masing-masing. Ujian merangkumi kata laluan salah, produk dengan dan tanpa variasi, sekatan URL peranan, pembaharuan sesi selepas muat semula dan log keluar.
- Ujian API mengesahkan permintaan kiosk untuk mengubah produk, mendaftar akaun berperanan tinggi, membaca tempahan orang lain atau membuka inventori dipulangkan sebagai 403. Permintaan tanpa sesi sah dipulangkan sebagai 401.
- Pada MySQL baharu, arahan pengurus pertama berjaya sekali. Ulangan mengekalkan satu pengurus. Pengurus itu berjaya mencipta staf dan kiosk, termasuk apabila permintaan cuba menetapkan peranan pengurus. Token akses dengan tempoh ujian yang singkat ditolak selepas luput dan pembaharuan berjaya menggunakan sesi yang sah.
