# Fasa 3 CafePulse · Dashboard staf dan bayaran

Buka [CafePulse setempat](http://localhost:5174) dalam dua tetingkap berasingan. Log masuk sebagai `kiosk@cafepulse.example` pada satu tetingkap dan `staff@cafepulse.example` pada satu lagi. Gunakan kata laluan ujian `CAFE_DEV_SEED_PASSWORD` daripada salinan kerja. Akaun pengurus boleh membuka dashboard melalui **Buka ruang staf**. Lihat [panduan persediaan](FASA_0_PERSEDIAAN_TEMPATAN.md) dan [panduan akses](FASA_1_AKSES_DAN_SESI.md).

## Proses satu tempahan

1. Di kiosk, hantar dua Latte dengan variasi **Besar +RM2** seperti dalam [panduan tempahan](FASA_2_TEMPAHAN_KIOSK.md). Kiosk memaparkan nombor giliran dan **RM24.00**. Paparan ini menjadi rujukan pelanggan semasa rintis.
2. Di dashboard, buka **Tempahan**. Nombor giliran yang sama muncul di **Menunggu** selepas semakan automatik. Kad menunjukkan masa tempahan, item, kuantiti, variasi, nota dan jumlah. Dashboard menyemak pelayan setiap empat saat apabila respons sebelumnya sudah selesai.
3. Terima bayaran pelanggan, kemudian pilih **Rekod bayaran** dari kad tempahan atau tab **Bayaran**. Pilih **Tunai**, **QR** atau **Kad**. Semak jumlah yang benar-benar diterima dan tekan **Sahkan bayaran**. Kiraan sistem memerlukan jumlah yang sama dengan tempahan. Sebagai contoh, RM23.99 ditolak untuk tempahan RM24.00.
4. Pilih **Mula sediakan**. Kad berpindah ke **Sedang disediakan**. Apabila minuman sedia untuk diserahkan, tekan **Tandakan Siap**.
5. Buka **Sejarah** untuk semak tempahan, rekod bayaran, masa penyediaan bermula, masa siap dan akaun staf yang membuat tindakan. Halaman ini menunjukkan sehingga 20 tempahan setiap halaman, dengan tempahan terbaru dahulu.

Jika sambungan hilang, dashboard menunjukkan amaran dan terus memaparkan data terakhir yang berjaya dibaca bersama masa kemas kini terakhir. Ia cuba menyambung semula secara automatik. Jika tindakan ditolak kerana skrin menunjukkan versi lama, semak rekod yang dimuat semula sebelum cuba lagi. Pelayan membenarkan satu bayaran aktif bagi suatu tempahan. Mencuba tindakan yang sama dari dua skrin serentak akan menerima satu kejayaan dan satu konflik apabila versi tempahan sudah berubah.

## Pembetulan bayaran

Jika rekod bayaran tersilap dan penyediaan masih belum bermula, pengurus boleh buka kad itu dan pilih **Betulkan rekod bayaran**. Masukkan sebab, sahkan pembetulan, dan rekod semula bayaran yang disahkan jika perlu. Sejarah mengekalkan rekod asal, sebab dan identiti pengurus yang membetulkannya.

CafePulse merekod penerimaan wang secara manual. Sebarang pemulangan wang dikendalikan oleh pengurus melalui kaedah bayaran asal dan dicatat dalam rekod operasi kedai. Paparan nombor giliran pelanggan berada pada kiosk selepas tempahan. Aliran aplikasi meliputi **Menunggu → Sedang disediakan → Siap**.

## Ujian semula

Binaan frontend menggunakan `npm run build`. Ujian Spring Boot menggunakan `mvnw clean verify`. Skrip `dev/check-staff-order.py` menguji jumlah bayaran, akses peranan, peralihan status, pembetulan, perubahan serentak dan sejarah. Ia menambah tempahan ujian, jadi gunakan MySQL kosong khusus untuk ujian dan jalankan Spring Boot dengan profil `dev-seed`.

Tetapkan `CAFEPULSE_CHECK_API` kepada alamat API ujian dan muatkan `CAFE_DEV_SEED_PASSWORD` daripada konfigurasi setempat. Untuk menguji dua proses pelayan, tetapkan juga `CAFEPULSE_CHECK_SECOND_API` kepada proses kedua yang menggunakan MySQL ujian yang sama. Kemudian jalankan `python3 dev/check-staff-order.py`.

## Keputusan semakan

- Chrome mengesahkan dua Latte Besar daripada kiosk muncul secara automatik pada dashboard dengan nombor yang sama, nota dan jumlah RM24. Sistem memaparkan data terakhir ketika terputus dan pulih selepas sambungan kembali.
- RM23.99 ditolak. Bayaran QR RM24 kekal selepas muat semula. Tempahan berpindah ke **Sedang disediakan** dan kemudian **Sejarah** dengan akaun serta masa tindakan.
- Pada dua proses pelayan yang berkongsi MySQL, dua permintaan bayaran serentak menghasilkan satu rekod. Skrin dengan versi lama menerima 409. Pembetulan pengurus menyimpan rekod asal dan sebab, manakala permintaan pembetulan selepas penyediaan bermula ditolak.
- MySQL sementara mengesahkan dua tempahan siap, dua bayaran aktif berjumlah RM48 dan satu rekod pembetulan.
