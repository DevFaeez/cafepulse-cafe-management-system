# CafePulse sales forecasting

Open [CafePulse](http://localhost:5174/manager) as a manager and select **Forecast**.

## Simulated menu history and forecasts

- **Forecast scenario** defaults to **CafePulse menu**. Choose a date, weather, traffic and any events or promotions, then select **Generate forecast**. Estimates are net units for all 52 sample products, with a category filter.
- **Simulated history** shows 365 completed Kuala Lumpur calendar days. Filter by product and period, or download the complete history as CSV or JSON. Promotions, weather, closures, unavailable products, refunds and sales amounts are simulated. Each of the 18,980 product-day rows is labelled in the CSV. The net amount is gross less discounts and refunds.
- The simulator uses a stable synthetic profile per sample product. It freezes sample names and prices independently from current catalogue changes and generates reproducible rows for each calendar date. Holiday and event flags are fictional. The snapshot ends yesterday.
- A Ridge regression per product learns the relationship between net units and the 21 scenario inputs. The evaluation trains on the earliest 80% and compares the newest 20% with a same-weekday average. It excludes closures and product stockouts. Future forecasts are fitted on all eligible simulated days.
- The **Original eight-drink model**, **Product mapping**, **Actual observations** and **Training data** remain available for the original forecast workflow. Demo data stays separate from live orders, stock, payments, sales reports and actual observations.

## Original eight-drink workflow

Log masuk sebagai pengurus dan buka [CafePulse → Ramalan](http://localhost:5174/manager). Model yang sedang digunakan ialah model **Gradient Boosting** daripada eksperimen jualan simulasi. Ketepatan untuk operasi kafe memerlukan penilaian dengan jualan sebenar.

## Jana dan baca ramalan

1. Buka **Senario ramalan**. Pilih tarikh, cuaca untuk tiga slot masa dan tahap trafik. Tarikh menentukan hari biasa atau hujung minggu. Model asal menggunakan Sabtu dan Ahad sebagai hujung minggu.
2. Tandakan cuti, acara dan promosi yang relevan. Model menerima 21 input yang disahkan di pelayan Java dan Python. Input ini menguji senario yang dimasukkan, dan promosi dalam borang ramalan ialah andaian senario.
3. Tekan **Jana ramalan**. Jadual memaparkan anggaran cawan untuk lapan minuman asal. **Belum dipetakan** menunjukkan minuman tanpa produk CafePulse yang ditetapkan. **Tidak dijual** menunjukkan produk dipadankan yang tidak tersedia. Kedua-dua keadaan masih menunjukkan anggaran model untuk senario itu.
4. Gunakan **Padanan produk** untuk memautkan setiap minuman yang sepadan kepada ID produk berunit **Cawan**. Nama atau harga menu boleh berubah sambil mengekalkan ID produk. Padankan hanya minuman yang benar-benar sama. Latte biasa dan Americano dalam menu ujian tidak dipadankan secara automatik.
5. Jika servis Python terganggu, skrin ramalan memberi mesej untuk cuba lagi. Kiosk dan aliran bayaran menghubungi aplikasi Java secara berasingan.

## Kumpul data sebenar

Selepas operasi harian, buka **Catatan sebenar** dan rekod keadaan yang berlaku. Tandakan **Kedai tutup** atau produk yang kehabisan atau tidak dijual pada tarikh tersebut. Gunakan **Buka catatan** untuk menyemak atau mengubah catatan lama. Satu catatan per tarikh menyimpan masa, pengguna dan senarai ID produk yang dijejak ketika catatan mula dibuat. Simpan padanan produk sebelum mula mengumpul catatan berkaitan.

Tab **Data latihan** menggabungkan catatan dengan kuantiti **dibayar** dan **dipulangkan** daripada laporan jualan harian. Rekod tiada jualan bagi produk yang dijejak pada hari buka dan tersedia dianggap sifar. Hari tanpa catatan operasi tiada dalam eksport. Hari tutup dan produk tidak tersedia kekal ditanda dan dikecualikan ketika model dinilai. Pemetaan mengikut ID produk, jadi nama boleh dikemas kini tanpa memecahkan siri.

## Sediakan servis Python setempat

Dari folder projek yang mempunyai `MachineLearning`, jalankan:

```sh
docker build --pull=false -t cafepulse-forecast:phase7 MachineLearning
docker run --rm cafepulse-forecast:phase7 python -m unittest interface.test_forecast
docker run -d --name cafepulse-forecast --restart unless-stopped \
  -p 127.0.0.1:5001:5000 cafepulse-forecast:phase7
curl -fsS http://127.0.0.1:5001/health
```

Jika Docker Desktop tiada dalam PATH, gunakan `/Applications/Docker.app/Contents/Resources/bin/docker`. Bekas ini menggunakan Python 3.11, scikit-learn 1.6.1 dan NumPy 2.2.6 yang dipasangkan dengan versi tetap. Java menggunakan `http://127.0.0.1:5001` secara lalai. `CAFE_FORECAST_URL` boleh digunakan untuk alamat lain. Port 5000 pada Mac ini sedang digunakan oleh Control Center, jadi servis diterbitkan pada 5001.

## Nilai jualan sebenar

Muat turun JSON daripada **Data latihan**, kemudian jalankan penilaian:

```sh
docker run --rm -i cafepulse-forecast:phase7 \
  python -m interface.evaluate_actual /dev/stdin < cafepulse-data-ramalan.json
```

Penilaian dibuat mengikut urutan masa bagi setiap ID produk. Ia menggunakan sekurang-kurangnya 21 hari latihan dan 7 hari ujian yang layak, dengan sekurang-kurangnya 20% pemerhatian terkini disimpan untuk ujian. Jumlah 28 hari ialah lantai semakan awal, dan lebih banyak data diperlukan untuk menilai variasi operasi. Anggaran asas ialah purata jualan pada hari minggu yang sama daripada data latihan. Skor **MAE** ialah purata beza mutlak dalam cawan per hari, dengan nilai lebih rendah menunjukkan ralat yang lebih kecil.

Skrip melaporkan `insufficient_data` sehingga data mencukupi. Ia menghasilkan ukuran ralat bagi model dan anggaran asas tanpa menukar model yang sedang digunakan. Gunakan keputusan itu untuk menentukan sama ada ramalan sesuai bagi perancangan stok sebenar.

## Keputusan semakan

- Servis Docker memuatkan model tersimpan dan menghasilkan lapan angka yang sama dengan notebook asal. Senario contoh menganggarkan 265 cawan Iced Caramel Macchiato dan 369 cawan Matcha Latte.
- Ujian Java, Python dan Chrome mengesahkan pengesahan input, capaian pengurus, padanan produk, penyimpanan catatan hari tutup atau kehabisan stok, serta muat turun eksport yang sepadan dengan kuantiti bayaran ujian.
- Ujian sengaja menggunakan alamat servis ramalan yang tidak tersedia. Ramalan memberi mesej ralat dan tempahan serta bayaran masih berjaya.
- Penilaian mengikut masa dan perbandingan MAE dengan purata hari sepadan diuji menggunakan data rekaan. Prestasi untuk jualan sebenar menunggu catatan operasi dan bayaran sebenar yang mencukupi.
