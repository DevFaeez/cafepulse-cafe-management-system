# Pelan pembangunan CafePulse

CafePulse ialah sistem kiosk untuk pelanggan, dashboard kerja untuk staf dan panel pengurusan untuk pemilik kafe. Pelan ini bermula dengan satu tempahan sebenar yang boleh diselesaikan dari kiosk hingga ke staf. Selepas transaksi itu stabil, barulah data yang terhasil digunakan untuk promosi, inventori, laporan dan ramalan.

## 1. Urutan kerja

| Fasa | Kerja | Hasil sebelum bergerak |
|---|---|---|
| 0 | Sediakan persekitaran dan data ujian | Frontend, pelayan dan MySQL boleh dijalankan. Ada menu contoh dan pangkalan data ujian. |
| 1 | Betulkan akses dan peranan | Kiosk, staf dan pengurus hanya boleh buat tindakan yang dibenarkan untuk peranan masing-masing. |
| 2 | Siapkan menu, troli dan penghantaran tempahan | Pelanggan boleh pesan dua latte dan dapat nombor giliran. Tempahan tersimpan dalam MySQL. |
| 3 | Hubungkan dashboard staf, bayaran dan sejarah | Staf nampak tempahan baharu, rekod bayaran dan tandakan tempahan siap. |
| 4 | Siapkan panel pengurus, promosi dan mata ganjaran | Pengurus boleh urus operasi. Harga, diskaun dan mata dikira serta direkod di pelayan. |
| 5 | Bina inventori berkuantiti | Staf boleh rekod stok. Pengurus boleh lihat baki dan amaran stok rendah. |
| 6 | Bina laporan jualan | Jumlah jualan dan bilangan item boleh dipadankan dengan transaksi sebenar. |
| 7 | Sambungkan dan sahkan ramalan | Pengurus boleh dapat anggaran cawan mengikut minuman. Keputusan simulasi dan penilaian dengan data kafe dilabel jelas. |
| 8 | Jalankan ujian penerimaan dan rintis | Aliran sebenar lulus diuji. Cara pemasangan, sandaran dan pemulihan boleh diulang. |

**Sasaran pertama ialah penghujung Fasa 3.** Pada tahap itu, CafePulse mesti boleh merekod satu tempahan dan bayaran, kemudian membolehkan staf menyiapkannya. Fasa 4 dan 5 boleh bergerak serentak selepas itu jika ada kapasiti. Siapkan laporan daripada rekod transaksi sebelum gunakan data projek sendiri untuk menilai ramalan. Keputusan operasi yang masih memerlukan input pemilik disenaraikan pada fasa yang terlibat.

## 2. Titik mula projek

- [Skrin utama React](cafe-prediction-frontend/src/App.jsx) membuka paparan pelanggan. Kod paparan staf ada tetapi belum diaktifkan dalam aliran normal.
- [Pelayan Spring Boot](cafe-prediction/pom.xml) menggunakan Java 17 dalam konfigurasi projek. Kodnya sudah mengurus akaun, menu, tempahan, mata dan status stok. [Keperluan asal](cafe-prediction/src/main/java/com/cafemanagement/cafeprediction/docs/requirement.md) menerangkan peranan pelanggan, staf dan pengurus.
- [Fail persediaan MySQL](cafe-prediction-database/setup.sql) masih kosong. Pelayan dikonfigurasi untuk mengemas kini jadual melalui Hibernate, tetapi pangkalan data MySQL perlu tersedia dahulu.
- [Model dan API Python](MachineLearning/interface/prediction_interface.py) berada dalam modul berasingan. Model yang tersimpan menggunakan data jualan simulasi dan belum dipanggil oleh aplikasi.
- Semakan terdahulu belum mengesahkan aplikasi berjalan hujung ke hujung. Pakej frontend dalam ESSD mengandungi binaan Windows, cubaan bina pelayan secara luar talian kekurangan kebergantungan, dan pakej Python untuk ramalan belum tersedia dalam persekitaran semakan.

## 3. Langkah pelaksanaan

### Fasa 0 — Jadikan projek boleh dijalankan

1. Guna `cafe-prediction-frontend` sebagai frontend utama. Pasang kebergantungan daripada fail kunci dalam persekitaran pembangunan Mac dan sahkan binaannya. Mulakan dengan salinan kerja jika perlu mengasingkan pemasangan sedia ada pada ESSD.
2. Pilih JDK 17 untuk projek Spring Boot dan gunakan Maven Wrapper. Bina pelayan, jalankan ujian sedia ada dan rekod jika ujian memerlukan MySQL atau konfigurasi tambahan.
3. Sediakan pangkalan data pembangunan yang berasingan. Tentukan cara menyediakan jadual secara konsisten. Asingkan konfigurasi sambungan, kata laluan dan kunci token daripada kod. Catat port setempat dan tetapan akses frontend kepada pelayan.
4. Sediakan data ujian yang boleh dimasukkan semula tanpa pendua. Mulakan dengan satu kategori minuman, dua produk, satu variasi bercaj dan akaun staf serta pengurus ujian. Guna nombor telefon rekaan.
5. Catat cara memasang, menjalankan dan menguji frontend, pelayan dan pangkalan data dari salinan kerja yang bersih. Sahkan menu boleh dibaca terus melalui API dengan akses ujian yang dibenarkan.

**Semak sebelum sambung** — Frontend boleh dibina, Spring Boot berjalan, sambungan MySQL berjaya dan menu ujian boleh dibaca. Ulang langkah permulaan pada pangkalan data ujian kosong untuk memastikan ia boleh dihasilkan semula.

### Fasa 1 — Akses kiosk, staf dan pengurus

1. Tetapkan matriks akses sebelum ubah skrin. Kiosk membaca menu dan membuat tempahan. Staf melihat serta memproses tempahan dan mengemas kini stok mengikut kebenaran. Pengurus mengurus konfigurasi operasi, akaun dan laporan.
2. Baiki pendaftaran supaya pemohon tak boleh memilih sendiri peranan staf atau pengurus. Tetapkan peranan di pelayan dan sediakan kaedah terkawal untuk mewujudkan akaun pengurus pertama.
3. Semak peraturan akses pada laluan dan pada kaedah API, termasuk sama ada anotasi semakan peranan benar-benar berjalan. Pisahkan akses baca menu dan variasi untuk kiosk daripada tindakan ubah produk. Pastikan pelanggan tak boleh minta senarai tempahan orang lain.
4. Untuk kiosk fizikal, cadangan awal ialah log masuk staf dan pengurus secara individu, dengan akses minimum yang dihadkan pada kiosk. Sahkan cara kiosk mendapat sesi dan memperbaharuinya. Laksanakan log masuk, log keluar, tamat sesi dan pengendalian token luput untuk skrin yang memerlukannya.
5. Tambah pengesahan input dan pulangan ralat yang jelas bagi akses gagal. Uji menggunakan akaun sebenar bagi setiap peranan dan permintaan tanpa token.

**Semak sebelum sambung** — Kiosk boleh baca menu dan variasi. Staf boleh masuk ke paparan staf. Permintaan menaikkan peranan sendiri atau mengubah menu dengan akses kiosk ditolak oleh pelayan.

### Fasa 2 — Tempahan lengkap di kiosk

1. Sambungkan kategori dan carian kepada senarai menu sebenar. Paparkan produk tersedia, harga, variasi dan caj tambahan. Sediakan keadaan paparan ketika memuatkan, tiada hasil atau pelayan gagal dihubungi.
2. Siapkan troli. Pelanggan boleh tambah kuantiti, tukar variasi atau buang item. Tentukan sama ada nombor telefon wajib atau tempahan tetamu dibenarkan. Selaraskan keputusan itu dengan rekod pelanggan dan pengiraan mata.
3. Sambungkan **Proceed Order** ke API yang sedia ada. Sahkan di pelayan bahawa troli berisi, kuantiti sah, produk masih tersedia dan setiap variasi memang milik produk dipilih. Pelayan mengira jumlah daripada harga semasa dan menyimpan harga yang dikenakan pada tempahan.
4. Simpan nombor giliran, tempahan dan semua item secara konsisten. Semak penjana nombor apabila dua kiosk menghantar serentak dan apabila hari bertukar. Gunakan satu pengenal penghantaran supaya tekan dua kali atau cuba semula selepas sambungan terputus tak mencipta dua tempahan.
5. Paparkan jumlah yang disahkan pelayan dan nombor giliran selepas simpan berjaya. Kosongkan troli selepas pengesahan. Jika permintaan gagal, pertahankan isi troli dan beri cara cuba semula dengan selamat. Paparkan diskaun sebenar apabila fungsi Fasa 4 sudah disambungkan.

**Semak sebelum sambung** — Pesan dua latte RM10 dengan tambahan RM2 setiap cawan. Tanpa diskaun, kiosk dan pangkalan data mesti menunjukkan RM24 serta nombor giliran yang sama. Uji juga troli kosong, variasi daripada produk lain, produk habis dan penghantaran dua kali.

### Fasa 3 — Dashboard staf, bayaran dan sejarah

1. Buka laluan dashboard selepas log masuk staf. Sambungkan senarai tempahan menunggu dan sedang disediakan kepada API. Paparkan nombor giliran, masa, item, kuantiti dan variasi. Gantikan rekod contoh dalam sejarah dengan tempahan sebenar.
2. Mulakan kemas kini automatik dengan semakan senarai secara berkala. Paparkan masa kemas kini terakhir dan keadaan bila sambungan terputus. Nilai WebSocket jika rintis menunjukkan semakan berkala tak cukup pantas.
3. Tambah fungsi kemas kini status di pelayan. Benarkan peralihan yang dipersetujui seperti `PENDING` → `PREPARING` → `COMPLETED`. Jika pembatalan dibenarkan, tentukan siapa boleh buat dan pada status apa. Rekod masa dan staf yang membuat perubahan. Cegah tindakan daripada skrin lapuk menimpa status lebih baharu.
4. Asingkan status bayaran daripada status penyediaan. Cadangan awal ialah staf merekod bayaran secara manual dengan kaedah, jumlah, masa dan identiti staf. Tetapkan sama ada penyediaan boleh bermula sebelum bayaran. Cegah bayaran direkod dua kali. Tentukan cara pembetulan atau pemulangan sebelum aliran itu digunakan.
5. Tentukan cara pelanggan menjejak giliran. Untuk rintis, paparan nombor pada kiosk selepas checkout boleh digunakan. Jika kedai perlukan skrin giliran atau resit, sambungkan kepada nombor tempahan yang sama.
6. Uji aliran pada dua skrin. Buat pesanan di kiosk, semak ketibaan pada dashboard, rekod bayaran, ubah status dan buka sejarah selepas muat semula.

**Semak sebelum sambung** — Dua latte muncul pada dashboard tanpa muat semula manual. Staf boleh rekod bayaran sekali dan tandakan tempahan siap. Kiosk serta staf melihat nombor yang sama dan sejarah kekal selepas muat semula.

### Fasa 4 — Pengurusan menu, promosi dan mata ganjaran

1. Bina panel pengurus untuk tambah atau ubah produk, kategori, variasi, harga dan status jualan menggunakan API sedia ada. Lengkapkan bahagian API yang masih diperlukan. Perubahan harga terpakai pada tempahan baharu dan harga tempahan lama kekal seperti asal.
2. Sediakan pengurusan akaun staf. Pengurus boleh cipta, nyahaktif dan memulakan tetapan semula kata laluan. Tentukan cara sesi akaun yang dinyahaktifkan ditamatkan.
3. Sepakati peraturan promosi, termasuk jenis, waktu aktif, produk layak dan sama ada promosi boleh digabung. Mulakan dengan diskaun peratus atau amaun tetap. Laksanakan tawaran seperti beli satu percuma satu selepas kiraan asas lulus ujian.
4. Sepakati bila mata diperoleh, bagaimana mata ditebus dan apa berlaku bila tempahan dibatalkan atau dibayar balik. Kod sekarang memberi mata semasa tempahan dibuat. Cadangan awal ialah tambah mata selepas bayaran direkod dan simpan sejarah pergerakan mata supaya baki boleh disemak.
5. Minta pelayan semak kelayakan dan kira harga akhir. Simpan diskaun, mata digunakan dan harga akhir bersama tempahan. Pastikan dua penebusan serentak tak boleh melebihi baki dan pembatalan memulihkan mata mengikut peraturan yang dipersetujui.
6. Gantikan nilai promosi dan mata contoh di kiosk dengan ringkasan daripada pelayan. Paparkan sebab yang mudah difahami jika promosi tamat atau mata tak mencukupi.

**Semak sebelum sambung** — Harga di kiosk, rekod tempahan dan jumlah bayaran sepadan. Uji promosi aktif dan tamat, baki mata tak cukup, penebusan serentak, pembatalan serta harga lama yang kekal selepas menu diubah.

### Fasa 5 — Inventori dan amaran stok

1. Tambah kuantiti, unit dan paras minimum bagi setiap bahan. Tetapkan kuantiti awal untuk stok lama yang hanya mempunyai status. Gunakan unit yang konsisten, contohnya liter untuk susu.
2. Rekod pergerakan seperti stok masuk, penggunaan, pelarasan dan pembaziran dengan kuantiti, masa, pengguna dan sebab. Kemas kini baki dan rekod pergerakan dalam operasi yang sama.
3. Sambungkan borang staf dan senarai inventori kepada pelayan. Paparkan baki sebenar serta status yang dikira daripada kuantiti. Pengurus boleh lihat bahan di bawah paras minimum dan terima amaran mengikut kaedah yang dipersetujui.
4. Jika kafe mahu potongan stok automatik, sambungkan produk dan variasi kepada resipi selepas rekod stok manual stabil. Tetapkan bila potongan berlaku dan bila pembatalan perlu mengembalikan stok. Pastikan tindakan yang dihantar semula tidak memotong kali kedua.

**Semak sebelum sambung** — Susu berbaki 4 L dengan paras minimum 5 L muncul dalam senarai stok rendah. Jika staf rekod stok masuk 3 L, baki menjadi 7 L dan sejarah menunjukkan siapa yang merekodnya.

### Fasa 6 — Laporan jualan berdasarkan transaksi

1. Tetapkan definisi jualan, tarikh perniagaan dan zon masa kafe. Cadangan untuk rintis ialah jualan berdasarkan bayaran yang direkod. Bezakan tempahan batal dan rekod pemulangan apabila fungsi itu digunakan.
2. Bina ringkasan harian daripada rekod tempahan, item dan bayaran. Paparkan jualan kasar, diskaun, jualan bersih, bilangan tempahan dan kuantiti produk. Asingkan bilangan cawan daripada item makanan.
3. Tambah penapis tarikh, produk terlaris dan trend mingguan serta bulanan selepas ringkasan harian sepadan dengan rekod asal.
4. Sediakan ringkasan sejarah jualan harian mengikut produk untuk modul ramalan. Pemetaan produk mesti konsisten walaupun nama menu berubah.

**Semak sebelum sambung** — Jumlah laporan bagi satu hari sepadan dengan pengiraan manual daripada transaksi ujian yang meliputi bayaran penuh, diskaun dan pembatalan. Semak juga tempahan di sekitar tengah malam.

### Fasa 7 — Integrasi ramalan jualan

1. Sediakan persekitaran Python yang boleh diulang menggunakan versi pakej yang serasi dengan model tersimpan. Sahkan satu input contoh boleh menghasilkan lapan anggaran.
2. Tentukan format 21 input, termasuk jenis hari, tiga slot cuaca, trafik, cuti, acara dan promosi. Semak input sebelum memanggil model. Petakan lapan keluaran model kepada produk yang betul dan tetapkan cara papar jika produk sudah tidak dijual.
3. Sambungkan dashboard pengurus kepada servis ramalan melalui pelayan aplikasi. Sediakan borang untuk satu hari dengan input manual dan paparan anggaran cawan mengikut minuman. Beri mesej jelas jika input tidak sah atau servis tidak dapat dihubungi.
4. Labelkan model sedia ada sebagai ramalan berasaskan data simulasi. Kumpul rekod jualan sebenar dan keadaan harian yang sepadan. Rekod juga hari kedai tutup atau produk kehabisan supaya jualan rendah pada hari tersebut boleh ditafsir dengan betul.
5. Apabila data sebenar mencukupi, latih dan uji mengikut urutan masa. Bandingkan dengan anggaran mudah seperti purata jualan hari minggu yang sama. Laporkan purata ralat dalam cawan bagi setiap produk dan nilai sama ada anggaran itu cukup tepat untuk kegunaan kedai.

**Semak sebelum sambung** — Pengurus boleh hantar senario dan dapat bilangan cawan mengikut minuman. Servis terputus tak menjejaskan checkout. Sebelum angka dipakai untuk merancang belian stok, bandingkan prestasinya dengan rekod sebenar dan anggaran mudah.

### Fasa 8 — Ujian penerimaan dan penggunaan rintis

1. Ulang semua semakan fasa menggunakan pemasangan ujian yang bersih dan akaun setiap peranan. Jalankan satu syif simulasi dengan beberapa tempahan, sambungan terputus, penghantaran berulang, pertukaran status, bayaran dan stok rendah.
2. Semak data sensitif dan akses. Pastikan nombor telefon hanya dipaparkan kepada pengguna yang memerlukannya. Semak konfigurasi, log dan kawalan peranan sebelum digunakan pada rangkaian kedai.
3. Sediakan sandaran pangkalan data, cuba pulihkan sandaran ke pangkalan data ujian, dan catat cara semak servis jika tempahan berhenti masuk. Sediakan arahan ringkas untuk staf mula syif, proses tempahan dan tutup syif.
4. Mulakan rintis dalam skop yang dipersetujui, contohnya satu kiosk dan satu dashboard staf. Bandingkan tempahan, bayaran dan laporan pada akhir syif. Gunakan hasil rintis untuk membetulkan sebarang masalah sebelum penggunaan lebih meluas.

**Semak sebelum buka kepada lebih ramai pelanggan** — Staf boleh jalankan satu syif menggunakan panduan, angka tempahan dan bayaran sepadan, dan sandaran berjaya dipulihkan dalam ujian.

## 4. Tiga tugasan pertama

- [ ] **Hidupkan persekitaran setempat.** Pasang kebergantungan frontend untuk Mac, jalankan binaan Spring Boot dengan JDK 17 dan sediakan MySQL ujian.
- [ ] **Masukkan menu dan akaun ujian.** Sediakan produk latte RM10, variasi tambahan RM2 dan akaun setiap peranan. Sahkan menu boleh dibaca dan setiap peranan mendapat akses yang sepatutnya.
- [ ] **Sambungkan satu tempahan sebenar.** Hantar dua latte dari kiosk, simpan jumlah RM24 dan pulangkan nombor giliran. Selepas itu, gunakan tempahan yang sama untuk menyambung dashboard, bayaran dan status siap.

Gunakan semakan pada penghujung setiap fasa sebagai syarat untuk bergerak ke fasa seterusnya. Jika semakan gagal, baiki langkah yang gagal dan ulang dengan rekod ujian yang sama supaya hasilnya boleh dibandingkan.
