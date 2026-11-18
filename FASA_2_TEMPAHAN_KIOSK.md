# Fasa 2 CafePulse · Tempahan kiosk

Buka [CafePulse setempat](http://localhost:5174) dan log masuk sebagai `kiosk@cafepulse.example`. Gunakan kata laluan dalam `CAFE_DEV_SEED_PASSWORD` daripada salinan kerja. Rujuk [panduan persediaan](FASA_0_PERSEDIAAN_TEMPATAN.md) dan [panduan akses](FASA_1_AKSES_DAN_SESI.md) jika perkhidmatan setempat belum berjalan.

## Cuba satu tempahan

1. Cari **Latte** atau buka kategori **Minuman Ujian**. Tekan Latte, pilih variasi **Besar +RM2**, dan tekan **Tambah ke troli**.
2. Tekan **Tambah kuantiti Latte** supaya kuantiti menjadi dua. **Ubah pilihan** menyunting variasi atau nota untuk baris itu. Ikon tong sampah membuang baris tersebut.
3. Masukkan nombor telefon. Fasa ini menggunakan nombor wajib supaya setiap tempahan kekal dipautkan kepada rekod pelanggan dan mata ganjaran sedia ada. Untuk operasi setempat, guna nombor rekaan seperti `0100000202`.
4. Tekan **Hantar tempahan**. Selepas pelayan mengesahkan simpanan, kiosk menunjukkan nombor giliran dan jumlah disahkan **RM24.00**. Tekan **Tempahan baharu** untuk kembali ke troli kosong.
5. Jika pelayar tak mendapat pengesahan, tekan **Hantar semula**. Kiosk menyimpan percubaan itu dalam sesi tab supaya muat semula masih menggunakan pengenal yang sama. Gunakan tab dan sesi kiosk yang sama untuk menyelesaikannya. Jika pelayan memulangkan ralat pada butiran seperti produk habis, troli dibuka semula untuk dibetulkan.

## Apa yang disimpan

- Pelayan mengira jumlah daripada harga semasa produk dan variasi. Troli memaparkan anggaran sebelum dihantar. Pangkalan data menyimpan harga asas dan caj variasi yang dikenakan, kuantiti, nota, status `ORDER_PENDING`, tarikh perniagaan dan nombor giliran.
- Nombor pelanggan dinormalkan, termasuk format `+60`. Untuk tempahan yang sah, mata sedia ada direkod sekali bersama tempahan. Kiraan promosi, diskaun dan tebus mata ialah kerja Fasa 4.
- Nombor giliran bermula semula mengikut tarikh perniagaan **Asia/Kuala_Lumpur**. Pelayan menggunakan satu kunci transaksi dalam MySQL supaya beberapa kiosk yang menghantar serentak tidak berkongsi nombor giliran untuk tempahan berbeza. Nombor dari tarikh sebelumnya kekal bersama rekodnya.
- Pemanggil API perlu menghantar UUID melalui pengepala `Idempotency-Key` pada `POST /api/order/create`. Gunakan satu UUID bagi satu percubaan tempahan dan guna semula UUID serta butiran yang sama ketika mencuba semula. UUID yang sama bagi akaun kiosk yang sama memulangkan rekod asal. Penggunaan UUID yang sama dengan butiran lain menghasilkan 409. Respons kepada kiosk mengandungi nombor giliran, status dan jumlah. Rekod perhubungan pelanggan kekal di pelayan.
- Fasa 3 akan menyambungkan tempahan tersimpan ini ke dashboard staf, bayaran dan tindakan **Siap**.

## Ulang ujian

`mvnw clean verify` menjalankan ujian Spring Boot dan semakan nombor giliran merentas dua tarikh. `npm run build` menyemak binaan React. Skrip `dev/check-checkout.py` menjalankan ujian API dengan dua proses Spring Boot yang berkongsi satu pangkalan data MySQL ujian kosong. Jalankan proses pertama dengan profil `dev-seed` untuk menu dan akaun ujian. Selepas ia sedia, jalankan proses kedua dengan skema yang sama dan `--spring.jpa.hibernate.ddl-auto=validate`. Tetapkan `CAFEPULSE_CHECK_API` dan `CAFEPULSE_CHECK_SECOND_API` kepada alamat dua proses. Muatkan kata laluan akaun ujian melalui persekitaran sebelum menjalankan skrip. Skrip ini mencipta rekod dan menggunakan MySQL sementara khusus untuk ujian.

## Keputusan semakan

- Binaan frontend dan dua ujian Maven lulus. Nombor pada tarikh ujian pertama ialah 1 dan 2. Tarikh berikutnya bermula semula pada 1.
- API menolak troli kosong, telefon atau kuantiti tidak sah, variasi daripada produk lain, pilihan variasi berganda dan produk habis.
- Lapan penghantaran serentak dengan satu UUID melalui dua proses pelayan menghasilkan satu tempahan RM24 dan satu nombor giliran. Empat tempahan berbeza yang turut dihantar serentak mendapat nombor berturutan.
- Dalam Chrome, carian, kategori, suntingan variasi, kuantiti dan pembuangan item diuji. Sambungan diputuskan selepas MySQL menyimpan dua Latte Besar. Muat semula dan percubaan semula memaparkan nombor yang sama serta RM24. MySQL mengesahkan satu rekod untuk tempahan tersebut bersama harga asas RM10, tambahan RM2, kuantiti dua dan nota.
- Ujian yang mencipta tempahan dijalankan pada MySQL sementara.
