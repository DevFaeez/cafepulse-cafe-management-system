# Fasa 5 CafePulse · Inventori dan resipi

Buka [CafePulse](http://localhost:5174) dan pilih **Inventori**. Akaun pengurus menyediakan tetapan bahan dan resipi. Akaun staf melihat baki serta merekod pergerakan. Lihat [panduan persediaan](FASA_0_PERSEDIAAN_TEMPATAN.md) untuk menjalankan salinan setempat.

## Sediakan inventori

1. Di ruang pengurus, buka **Inventori → Tambah bahan**. Masukkan nama, unit, baki pembukaan hasil kiraan fizikal dan paras minimum. Contohnya **Susu**, **L**, **4** dan **5**.
2. Rekod lama yang hanya menyimpan status dipaparkan sebagai **Belum dikira**. Gunakan **Kira baki pembukaan** untuk memasukkan kuantiti fizikal. Ruang baki kekal kosong sehingga kiraan fizikal direkod.
3. Selepas kiraan pembukaan disimpan, nama dan paras minimum boleh dikemas kini dengan **Tetapan bahan**. Semua pergerakan bagi satu bahan menggunakan unit yang ditetapkan semasa kiraan pembukaan.
4. Pengurus menerima amaran dalam aplikasi apabila bahan berada pada atau di bawah paras minimum. Baki sifar ditanda **Habis**. Senarai inventori dan amaran disemak semula setiap 15 saat semasa ruangan berkaitan dibuka.

## Rekod pergerakan

- **Stok masuk** menambah kuantiti penerimaan. Susu 4 L menjadi 7 L selepas menerima 3 L. Dengan minimum 5 L, susu akan keluar daripada senarai bahan rendah.
- **Penggunaan** dan **Pembaziran** menolak kuantiti yang direkod. Sistem menolak pergerakan yang menyebabkan baki negatif.
- **Pelarasan kiraan** menggunakan baki sebenar selepas kiraan fizikal. Jika seseorang telah mengubah baki sejak borang dibuka, semak baki terkini dan buka semula borang sebelum mengulang kiraan.
- Setiap pergerakan mengandungi kuantiti, baki selepas pergerakan, masa, identiti pengguna dan sebab. **Sejarah stok** memaparkan rekod terbaru dan nombor giliran bagi pergerakan yang berpunca daripada tempahan.
- Jika sambungan terputus sebelum pengesahan, kembali ke **Inventori** dalam tab dan sesi log masuk yang sama dan tekan **Hantar semula**. Percubaan itu menggunakan ID yang sama supaya satu penghantaran menghasilkan satu pergerakan.

## Potongan melalui resipi

1. Di ruang pengurus, buka **Resipi**. Pilih produk dan **Resipi asas**, kemudian masukkan jumlah setiap bahan bagi satu unit jualan. Untuk Latte, contohnya **0.20 L** susu.
2. Pilih variasi **Besar** dan masukkan bahan tambahan bagi satu unit variasi. Tambahan **0.05 L** susu menjadikan penggunaan susu satu Latte Besar **0.25 L**. Dua Latte Besar menggunakan **0.50 L**. Resipi kosong membiarkan bahan bagi produk atau variasi tersebut diurus secara manual.
3. CafePulse menggunakan resipi yang tersedia ketika staf tekan **Mula sediakan** selepas mengesahkan bayaran. Potongan bahan dan perubahan status disimpan sekali dalam transaksi yang sama. Jika satu bahan tidak mencukupi, semua baki dan status kekal seperti sebelum percubaan penyediaan.
4. Pembatalan sebelum penyediaan tidak memerlukan pulangan stok. Bagi tempahan yang sedang disediakan, pengurus boleh memilih **Pulangkan semua bahan resipi yang belum digunakan** ketika merekod pulangan. Sistem memulangkan amaun asal yang dipotong walaupun resipi sudah berubah. Pilihan itu sesuai apabila semua bahan yang dipotong benar-benar boleh dimasukkan semula. Gunakan pelarasan kiraan untuk bahan yang hanya sebahagiannya boleh dipulangkan. Selepas tempahan siap, gunakan pelarasan kiraan bagi sebarang perubahan fizikal.

## Ulang ujian

- Jalankan `mvnw clean verify` dan `npm run build` untuk semak binaan.
- Jalankan `python3 dev/check-inventory.py` pada MySQL sementara yang kosong dengan profil `dev-seed`. Muatkan `CAFE_DEV_SEED_PASSWORD` dan tetapkan `CAFEPULSE_CHECK_API`. Untuk ujian dua pelayan yang berkongsi MySQL sama, tetapkan `CAFEPULSE_CHECK_SECOND_API` juga.
- Skrip mencipta bahan, menambah stok, menghantar tempahan dan merekod bayaran serta pulangan. Gunakan pangkalan data ujian berasingan untuk menjalankannya.

## Keputusan semakan

- Dalam Chrome, susu 4 L dengan minimum 5 L ditanda rendah. Staf merekod 3 L penerimaan. Selepas respons sengaja diputuskan dan pelayar dimuat semula, percubaan yang sama menghasilkan baki 7 L dan satu pergerakan penerimaan dengan identiti staf.
- Resipi Chrome menolak 0.25 L daripada satu Latte Besar. Pulangan yang memilih bahan belum digunakan memulangkan 0.25 L. Sejarah stok menunjukkan nombor giliran yang sama.
- Dua pelayan berkongsi MySQL diuji untuk penghantaran serentak. Penerimaan berulang menghasilkan satu rekod. Dua cubaan mula menyediakan menghasilkan satu potongan resipi. Bahan tidak mencukupi membatalkan keseluruhan potongan. Ujian turut mengesahkan pelarasan lapuk, pembaziran, perubahan resipi selepas potongan dan pulangan wang tanpa pilihan pulangkan bahan.
- Ujian operasi dibuat pada MySQL sementara. Baki pembukaan sebenar perlu dimasukkan oleh pengurus.
