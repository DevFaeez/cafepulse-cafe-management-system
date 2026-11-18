# Fasa 8 CafePulse · Panduan syif rintis dan pemulihan

## 1. Sebelum buka kiosk

1. Mulakan dengan satu kiosk, satu paparan staf dan seorang pengurus yang boleh mengesahkan wang serta menyemak laporan. Pada pemasangan ini, buka [CafePulse](http://localhost:5174) dalam profil pelayar berasingan pada Mac yang menjalankan aplikasi. Frontend, API dan MySQL terhad kepada komputer tersebut.
2. Untuk syif pelanggan, sediakan pangkalan data khas mengikut [Fasa 0](FASA_0_PERSEDIAAN_TEMPATAN.md). Gunakan kata laluan pangkalan data dan kunci sesi tersendiri. Sediakan pengurus pertama menggunakan profil `bootstrap-manager` mengikut [Fasa 1](FASA_1_AKSES_DAN_SESI.md), kemudian jalankan aplikasi dengan konfigurasi pangkalan data itu. Profil `dev-seed` dan akaun `@cafepulse.example` adalah untuk latihan.
3. Pengurus sediakan akaun staf dan kiosk di panel pengurus. Gunakan akaun individu untuk kerja staf dan pengurus. Uji log masuk ketiga-tiga peranan dan pastikan setiap skrin terus membuka ruang kerja yang betul.
4. Semak harga, variasi, status item tersedia, kod promosi, resipi, baki stok pembukaan dan paras minimum. Semak tarikh syif dalam **Laporan jualan**. Aplikasi mengira hari jualan mengikut waktu Kuala Lumpur.
5. Buka **Tempahan** pada skrin staf. Pastikan tiada amaran sambungan. Sediakan rekod tunai atau terminal bayaran untuk semakan di penghujung syif. Buat satu sandaran selepas menyediakan menu dan sebelum menerima pelanggan.

## 2. Semasa syif

1. Pelanggan pilih item di kiosk, semak kuantiti dan jumlah, kemudian tekan **Hantar tempahan**. Gunakan nombor giliran yang dipaparkan untuk mengenal pasti tempahan.
2. Jika kiosk gagal mendapat pengesahan, kekalkan tab yang sama dan gunakan **Hantar semula** selepas sambungan pulih. Jika perlu muat semula halaman, gunakan tab yang sama. Aplikasi menyimpan percubaan asal di tab itu dan menggunakan pengenal yang sama apabila mencuba semula. Semak nombor giliran di dashboard staf sebelum membuat tempahan baharu.
3. Staf semak item, variasi, nota dan jumlah pada kad tempahan. Terima wang atau sahkan transaksi pada terminal atau aplikasi bayaran asal. Tekan **Rekod bayaran**, pilih **Tunai**, **QR** atau **Kad**, semak jumlah diterima dan sahkan. CafePulse mencatat bayaran yang telah diterima.
4. Tekan **Mula sediakan** untuk mula menyediakan minuman dan merekod penggunaan bahan daripada resipi. Tekan **Tandakan Siap** apabila item sedia untuk diserahkan. Jika ada amaran stok rendah, semak bahan secara fizikal dan catat **Stok masuk**, **Pembaziran** atau pelarasan yang berkaitan dalam **Inventori**.
5. Pengurus boleh membatalkan tempahan yang belum dibayar. Untuk pulangan penuh, pulangkan wang melalui kaedah bayaran asal dan catat **Rekod pulangan** bersama sebab. Pilih pemulangan bahan ke stok apabila semua bahan yang digunakan benar-benar dipulangkan.
6. Jika dashboard hilang sambungan, semak masa kemas kini terakhir. Apabila sambungan pulih, cari nombor giliran dan semak **Tempahan** atau **Sejarah** dahulu sebelum mengulangi bayaran, perubahan status atau pulangan. Gunakan bahagian 4 jika amaran berterusan.

## 3. Tutup syif

1. Selesaikan atau serahkan semua tempahan yang masih terbuka. Catat nombor giliran dan tindakan yang tinggal pada rekod serahan syif.
2. Pengurus buka **Laporan jualan** dan pilih tarikh syif. Bandingkan bilangan bayaran serta jumlah mengikut tunai, QR dan kad dengan rekod kaedah bayaran masing-masing. Semak diskaun, pembatalan dan pulangan secara berasingan. Jumlah bersih ialah jumlah bayaran diterima selepas ditolak pulangan.
3. Jika jumlah berbeza, gunakan **Sejarah** untuk semak giliran, kaedah bayaran, sebab pembetulan dan masa tindakan. Catat perbezaan serta puncanya. Semak baki stok akhir yang perlu diserahkan kepada syif berikutnya.
4. Buat sandaran akhir syif dan catat masa, nama fail dan pengurus bertugas. Log keluar pada semua paparan. Sebelum menambah kiosk atau pelanggan, lengkapkan satu syif kedai dengan staf sebenar dan pastikan tempahan, bayaran, laporan dan stok akhir sepadan.

## 4. Jika tempahan berhenti masuk

| Apa yang kelihatan | Cara semak |
|---|---|
| Halaman kiosk atau staf langsung tidak dibuka | Buka semula alamat frontend pada Mac yang menjalankannya. Semak terminal frontend mengikut Fasa 0. |
| Halaman dibuka tetapi log masuk dan semakan tempahan gagal | Semak pelayan CafePulse. `curl -i http://localhost:8081/auth/session` sepatutnya memberi HTTP 401 untuk permintaan tanpa sesi. Tiada respons bermakna pelayan atau alamat perlu diperiksa. |
| Pelayan berjalan tetapi log menunjukkan kegagalan MySQL | Semak Docker Desktop, status bekas pangkalan data dan sambungan menggunakan arahan semakan MySQL dalam Fasa 0. Mulakan semula bekas sedia ada jika ia terhenti, kemudian periksa semula log masuk. |
| Ramalan sahaja tidak tersedia | Semak `http://localhost:5001/health` dan [panduan ramalan](FASA_7_RAMALAN_JUALAN.md). Teruskan menggunakan skrin tempahan, bayaran dan inventori. |

Jika gangguan berlarutan, hentikan penerimaan di kiosk buat sementara. Kekalkan nombor giliran dan bukti bayaran yang telah diterima. Selepas aplikasi pulih, padankan rekod di kiosk, dashboard dan terminal bayaran sebelum sambung.

## 5. Sandarkan dan pulihkan MySQL

Skrip ini menggunakan nama bekas MySQL 8.4 serta pengguna dan skema yang dikonfigurasikan di dalamnya. Jalankan dari direktori projek pada storan dalaman. Jika `docker` tiada dalam PATH pada Mac, tetapkan laluan berikut.

```sh
export DOCKER_CMD=/Applications/Docker.app/Contents/Resources/bin/docker
backup=".runtime/backups/CafePulse-$(date +%Y%m%d-%H%M%S).sql.gz"
bash dev/backup-db.sh cafepulse-mysql-dev "$backup"
gzip -t "$backup"
```

Tukar `cafepulse-mysql-dev` kepada nama bekas yang betul untuk stor operasi. Sandaran ini mengandungi rekod pelanggan dan bayaran serta dibuat dengan akses baca dan tulis untuk pemilik fail sahaja. Simpan di storan yang aksesnya terhad kepada pengurus yang bertanggungjawab. Jadual sesi log masuk dikecualikan. Akaun pengguna dipulihkan daripada sandaran dan semua paparan perlu log masuk semula.

1. Sediakan bekas MySQL berasingan pada port lain mengikut Fasa 0. Pilih nama khas, seperti `cafepulse-mysql-restore`, dan gunakan skema baharu. Tunggu MySQL sedia menerima sambungan. Biarkan aplikasi berhenti pada sambungan ke skema ini supaya ia kekal kosong.
2. Dari direktori projek, jalankan pemulihan dengan nama bekas baharu dan laluan sandaran yang sebenar. Skrip mengesahkan fail dan meminta skema kosong.

   ```sh
   bash dev/restore-db.sh cafepulse-mysql-restore "$backup"
   ```

3. Jalankan salinan CafePulse pada port API berasingan dengan URL MySQL yang menunjuk kepada bekas pemulihan. Mulakan tanpa profil `dev-seed` dan `bootstrap-manager`. Aplikasi menyediakan jadual sesi baharu yang kosong.
4. Log masuk semula sebagai pengurus. Padankan giliran terakhir, status, kaedah dan jumlah bayaran, pulangan, laporan bagi tarikh syif, baki stok dan sejarah pergerakannya dengan rekod akhir syif. Jika sedang memulihkan stor operasi, selesaikan semakan ini sebelum menukar sambungan aplikasi kedai. Kekalkan pangkalan data asal hingga data pulih disahkan.

## 6. Ulang syif simulasi

Gunakan skema MySQL ujian khusus dalam pemasangan bersih dengan profil `dev-seed`. Jalankan `python3 dev/check-local.py` dan `python3 dev/check-access.py` untuk akses, kemudian tetapkan `CAFEPULSE_CHECK_API` ke API ujian dan `CAFEPULSE_PILOT_DATE` ke tarikh hari ini dalam format `YYYY-MM-DD` mengikut waktu Kuala Lumpur. Muatkan `CAFE_DEV_SEED_PASSWORD` daripada konfigurasi ujian. Jalankan `python3 dev/check-pilot.py setup` untuk menyediakan promosi **RINTIS10**, resipi Latte dan stok susu 5.2 L dengan paras minimum 5 L.

| Giliran | Langkah dan hasil jangkaan |
|---|---|
| 1 | Guna nombor rekaan `0100000801` untuk semua giliran. Hantar dua Latte Besar dengan RINTIS10. Putuskan sambungan skrin staf semasa kiosk menghantar. Dalam ujian automatik, jawapan pelayan kepada kiosk juga disekat selepas tempahan disimpan. Selepas percubaan semula, giliran 1 muncul sekali. Rekod QR RM21.60 dan siapkan pesanan. Susu turun kepada 4.7 L dan mencetuskan amaran. Catat 1 L stok masuk dan 0.2 L pembaziran. |
| 2 | Hantar satu Americano dan tebus 10 mata daripada akaun sama. Rekod tunai RM7.00, mula sediakan dan tandakan siap. |
| 3 | Hantar satu Americano tanpa diskaun. Pengurus batalkan sebelum bayaran. |
| 4 | Hantar satu Latte. Rekod tunai RM10.00 dan mula sediakan. Pengurus rekod pulangan penuh RM10.00 dan pemulangan bahan. |

Semak empat giliran selesai diproses, tiga bayaran direkodkan, satu pulangan dan satu pembatalan. Laporan perlu menunjukkan RM38.60 diterima, RM10.00 dipulangkan, RM28.60 jualan bersih dan tiga cawan bersih. Baki susu ialah 5.5 L. Skrip boleh membuat padanan selepas syif dan selepas pemulihan.

```sh
umask 077
python3 dev/check-pilot.py snapshot .runtime/rekod-rintis.json
# Selepas pemulihan dan pelayan ujian kedua bermula pada port 8083
CAFEPULSE_CHECK_API=http://localhost:8083 python3 dev/check-pilot.py restored .runtime/rekod-rintis.json
rm .runtime/rekod-rintis.json
```

Fail perbandingan sementara mengandungi token sesi ujian untuk menyemak token asal ditolak selepas pemulihan. Gunakan pangkalan data simulasi sahaja dan padam fail itu selepas semakan.

## 7. Keputusan pada 16 November 2026

- Pemasangan bersih berjaya membina frontend dan backend. Dua ujian Maven lulus. Semakan akses meliputi log masuk, had kiosk dan staf, pembaharuan token dan log keluar.
- Chrome menjalankan satu syif simulasi dengan akaun berasingan bagi kiosk, staf dan pengurus. Kiosk berjaya menghantar semula tempahan selepas kehilangan jawapan pelayan. Giliran itu direkodkan sekali sahaja. Dashboard staf menyambung semula dan mengambil tempahan yang sama.
- Empat giliran diproses. Tiga bayaran berjumlah RM38.60 direkodkan, satu dibatalkan sebelum bayar dan RM10.00 dipulangkan. Laporan menunjukkan RM28.60 jualan bersih. Susu turun ke 4.7 L, mencetuskan amaran di bawah paras 5 L dan berbaki 5.5 L selepas stok masuk, pembaziran dan pulangan bahan.
- Senarai tempahan staf dan log pelayan yang diperiksa bebas daripada nombor telefon yang digunakan dalam simulasi. Carian pelanggan dibenarkan kepada pengurus dan respons carian meminta pelayar tidak menyimpannya.
- Sandaran syif berjaya dipulihkan ke MySQL kosong. Empat tempahan, rekod bayaran, laporan dan lejar stok sepadan. Pemulihan kedua ke skema yang sudah berisi ditolak. Sesi lama ditolak dan log masuk baharu berjaya.
- Semakan Fasa 2 hingga 7 turut lulus pada pangkalan data ujian, termasuk tempahan dan bayaran serentak, promosi, ganjaran, resipi, laporan dan operasi apabila servis ramalan tidak tersedia. Ujian penciptaan pengurus pertama pada skema kosong dan pembaharuan sesi luput turut lulus.
- Syif ini menggunakan data rekaan pada Mac setempat. Syif rintis dengan staf, pelanggan dan bayaran sebenar masih perlu dijalankan di kedai.
