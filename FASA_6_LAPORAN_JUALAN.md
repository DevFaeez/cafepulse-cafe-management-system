# Fasa 6 CafePulse · Laporan jualan

Log masuk sebagai pengurus di [CafePulse setempat](http://localhost:5174/manager), kemudian buka tab **Laporan jualan**. Pilih tarikh mula dan tamat, pilih **Harian**, **Mingguan** atau **Bulanan**, kemudian tekan **Papar laporan**. Kedua-dua tarikh dikira dalam julat. Laporan menerima sehingga 366 hari bagi satu permintaan.

## Cara baca angka

- **Jualan kasar** menjumlahkan subtotal tempahan yang bayaran sahnya direkod dalam julat. Harga dan caj variasi ialah angka yang disimpan bersama tempahan.
- **Diskaun** menjumlahkan promosi dan nilai tebus mata bagi bayaran dalam julat. **Bayaran diterima** ialah jumlah sebenar yang direkod oleh staf. Tempahan percuma yang disahkan muncul sebagai satu tempahan dengan bayaran RM0.
- **Pulangan** muncul pada tarikh pulangan direkod dalam CafePulse. **Jualan bersih** ialah bayaran diterima ditolak pulangan. Contohnya bayaran RM12 hari Rabu dan pulangan RM12 hari Khamis menghasilkan +RM12 pada Rabu dan −RM12 pada Khamis untuk transaksi itu.
- **Tempahan dibayar**, **Tempahan dipulangkan** dan **Tempahan batal** dikira mengikut tarikh tindakan masing-masing. Tempahan yang masih menunggu bayaran belum masuk jualan. Rekod bayaran yang dibatalkan untuk pembetulan dikeluarkan daripada kiraan.
- **Cawan**, **Item makanan** dan unit lain menunjukkan unit bersih selepas pulangan. Produk yang belum dikenal pasti unitnya muncul dalam **Belum dikelaskan**. Di tab **Produk**, pilih **Ubah produk → Unit laporan jualan** untuk menetapkannya. Pesanan baharu menyimpan unit jualan pada baris pesanan. Pesanan lama tanpa unit khusus menggunakan unit semasa produk.

Tarikh perniagaan bermula tengah malam waktu **Asia/Kuala_Lumpur**. Minggu bermula Isnin. Kumpulan pertama dan terakhir dalam graf mengikut tarikh sebenar yang dipilih, walaupun julat hanya meliputi sebahagian minggu atau bulan. Pilih bar dalam graf atau guna **Butiran tempoh** untuk semak pecahan.

**Produk terlaris** disusun mengikut kuantiti bersih dan memaparkan bilangan dibayar serta dipulangkan. ID produk kekal sama apabila namanya berubah. Laporan menggunakan nama menu semasa sebagai label.

## Sejarah harian untuk Fasa 7

Tekan **Muat turun sejarah produk** untuk mendapatkan fail JSON berdasarkan julat laporan yang sedang dipaparkan. Pengurus juga boleh menggunakan `GET /api/admin/reports/product-history?from=YYYY-MM-DD&to=YYYY-MM-DD`. Setiap baris mengandungi `date`, `productId`, `productName`, `salesUnit`, `paidQuantity`, `refundedQuantity` dan `netQuantity`.

Gunakan `productId` sebagai pengenal produk dalam modul ramalan. Untuk anggaran permintaan, gunakan kuantiti dibayar dan simpan kuantiti dipulangkan sebagai lajur berasingan. Hari atau produk tanpa peristiwa tiada baris dalam eksport. Isikan sifar bagi kombinasi tarikh dan produk yang telah dikenal pasti sebagai tersedia untuk dijual apabila membina siri lengkap. Fail itu memuatkan ringkasan transaksi, tanpa maklumat pelanggan.

## Ulang ujian

Jalankan `mvnw clean verify` dan `npm run build`. Untuk semakan transaksi mengikut tarikh, gunakan satu MySQL ujian kosong dan jalankan pelayan dengan profil `dev-seed`. Tetapkan `CAFEPULSE_CHECK_API` dan `CAFE_DEV_SEED_PASSWORD`. Jalankan `python3 dev/check-sales.py seed` dan hantar SQL yang terhasil ke pangkalan data ujian yang sama untuk menetapkan masa peristiwa. Kemudian jalankan `python3 dev/check-sales.py verify`. Ujian ini mencipta tempahan, promosi, bayaran, pembatalan dan pulangan rekaan.

## Keputusan semakan

- MySQL ujian merangkumi bayaran penuh, diskaun, tempahan percuma, pembatalan tanpa bayaran, pembetulan rekod bayaran dan pulangan pada hari berikutnya. Untuk julat ujian, jualan kasar **RM66.00** tolak diskaun **RM7.40** dan pulangan **RM12.00** memberi jualan bersih **RM46.60**.
- Bayaran pada 23:59:59.999999 dan 00:00:01 waktu Malaysia masuk pada hari yang berlainan. Jumlah harian, mingguan dan bulanan sepadan bagi julat sama.
- Chrome mengesahkan unit jualan boleh dikemas kini pada produk, penapis dan butiran trend boleh dipilih, produk terlaris disusun dengan betul, dan fail JSON boleh dimuat turun. ID Latte kekal sama selepas nama menu ditukar.
- Akses laporan dan eksport diuji untuk pengurus, staf dan kiosk. Transaksi ujian menggunakan MySQL sementara.
