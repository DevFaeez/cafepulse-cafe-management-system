# Fasa 4 CafePulse · Panel pengurus, promosi dan mata ganjaran

Buka [CafePulse setempat](http://localhost:5174) dan log masuk sebagai `manager@cafepulse.example`. Akaun ujian menggunakan nilai `CAFE_DEV_SEED_PASSWORD` daripada salinan kerja. Lihat [panduan persediaan](FASA_0_PERSEDIAAN_TEMPATAN.md) jika perkhidmatan belum berjalan.

## Urus operasi kedai

- **Produk** menambah atau mengubah nama, penerangan, kategori, imej dan harga. Gunakan **Tandakan habis** atau **Aktifkan jualan** untuk mengubah apa yang boleh dipesan. Tempahan terdahulu menyimpan harga yang dikenakan ketika dihantar.
- **Kategori** menambah atau menamakan semula kategori produk.
- **Variasi** mengurus kumpulan variasi. Pilih produk untuk tambah atau ubah nama pilihan, kumpulan atau caj tambahan.
- **Akaun** mencipta akaun staf dan kiosk, menetapkan semula kata laluan dan menyahaktifkan atau mengaktifkan semula akaun. Tetapan semula kata laluan dan penyahaktifan membatalkan semua sesi sedia ada. Sampaikan kata laluan baharu kepada pemilik akaun melalui saluran yang sesuai.
- **Promosi** mencipta, menyunting dan menghidupkan promosi dengan kod, tarikh mula dan tarikh tamat waktu Malaysia. Tetapkan semua produk atau pilih satu produk. Beli satu percuma satu memerlukan satu produk tertentu.
- **Mata ganjaran** mencari baki melalui nombor telefon dan memaparkan sehingga 50 pergerakan terbaru yang direkod sejak Fasa 4.

## Aturan harga dan mata

1. Satu kod promosi boleh digunakan bagi satu tempahan. Diskaun peratus dan amaun tetap hanya dikenakan pada item yang layak. Beli satu percuma satu menolak satu harga asas bagi setiap dua unit produk yang layak. Caj tambahan variasi kekal dikenakan.
2. Selepas promosi, pelanggan boleh menebus dalam gandaan **10 mata = RM1** hingga baki dan jumlah yang dibenarkan. Contohnya dua Latte Besar dengan subtotal **RM24.00**, promosi 10% dan tebusan 10 mata menghasilkan **RM20.60**.
3. Masukkan telefon, kod dan mata dalam troli. Tekan **Semak jumlah dan mata** untuk melihat baki, diskaun dan jumlah akhir. Pengiraan menggunakan harga dan promosi semasa pada pelayan. Jika harga berubah antara semakan dengan penghantaran, kiosk meminta semakan baharu.
4. Mata penebusan ditolak semasa tempahan disimpan. Mata baharu dikreditkan apabila staf merekod bayaran. Aturan mata sedia ada ialah 15 untuk jumlah berbayar hingga RM30, 35 hingga RM50, 70 hingga RM80, kemudian tambahan satu mata bagi setiap RM2 selepas RM80. Jumlah RM0 menggunakan kaedah **Tanpa bayaran** dan memberi sifar mata.
5. Pengurus boleh membatalkan tempahan yang masih menunggu dan belum dibayar. Sistem memulihkan mata penebusan. Jika wang sudah dipulangkan melalui kaedah asal, pengurus boleh memilih **Rekod pulangan** bagi tempahan berbayar untuk merekod pulangan penuh. Mata yang diperoleh ditarik balik dan mata yang digunakan dipulihkan. Jika mata yang ditarik balik sudah digunakan pada tempahan lain, kredit seterusnya menutup baki pelarasan terlebih dahulu.
6. Pembetulan rekod bayaran sebelum penyediaan menarik balik kredit bayaran itu. Mata yang ditebus kekal diperuntukkan kepada tempahan tersebut hingga ia dibayar semula atau dibatalkan. Sistem menyimpan masa, akaun pengurus dan sebab setiap penutupan atau pembetulan dalam sejarah.

Telefon ialah pengenal mata bagi kiosk setempat yang dikendalikan kedai. Pemulangan wang kekal tindakan pengurus melalui kaedah bayaran asal. CafePulse merekod pulangan dan mengurus pelarasan mata.

## Semakan teknikal

- Pelayan menyimpan subtotal, kod promosi, diskaun promosi, nilai tebusan dan jumlah akhir. Penebusan, kredit, pembatalan dan pemulihan mata disusun dalam transaksi MySQL untuk mengelakkan dua tempahan menggunakan mata yang sama.
- Rekod tempahan lama menggunakan kaedah mata sebelum Fasa 4. Bayaran kemudian untuk rekod itu tidak memberikan kredit kedua. Naik taraf skema mengekalkan rekod status dan membolehkan tempahan baharu mendapat sifar mata.
- Jalankan `mvnw clean verify`, `npm run build` dan `python3 dev/check-rewards.py`. Skrip ini mencipta pesanan, promosi dan akaun pada MySQL sementara yang telah dimulakan dengan profil `dev-seed`. Tetapkan `CAFEPULSE_CHECK_API`, `CAFE_DEV_SEED_PASSWORD` dan, untuk ujian dua pelayan, `CAFEPULSE_CHECK_SECOND_API`.

## Keputusan semakan

- Chrome mengesahkan pengurus boleh mencipta kategori, produk dan variasi, mengubah caj variasi, menukar status jualan, menambah akaun dan memutuskan sesi staf melalui tetapan semula kata laluan.
- Kiosk menolak promosi tamat dan memaparkan **RM20.60** bagi dua Latte Besar selepas promosi 10% serta tebusan 10 mata. Resit dan rekod bayaran staf menunjukkan angka yang sama.
- Ujian dua pelayan mengesahkan satu penebusan berjaya apabila dua tempahan bersaing untuk baki mata yang sama. Pembatalan, pulangan, pembetulan bayaran dan baki pelarasan diuji pada MySQL sementara.
- Ujian juga mengesahkan diskaun amaun tetap, beli satu percuma satu, tempahan RM0, harga lama, penamatan sesi lama selepas penetapan semula kata laluan dan kawalan peranan.
