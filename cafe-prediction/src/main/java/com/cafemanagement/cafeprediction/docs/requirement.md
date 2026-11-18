# 📌 Cafe Management System – Requirement Specification

## 🎯 Objective
Membangunkan sistem pengurusan kafe dengan 3 jenis pengguna:
1. **Customer (Pembeli)** – masukkan order sendiri & join loyalty system.
2. **Cafe Staff (Preparer)** – menyediakan order & lapor stok.
3. **Manager/Admin** – analisis jualan, urus stok, harga, promosi & forecasting.

---

## 👥 User Roles & Functionalities

### 🧑‍💻 1. Customer (Pembeli)
- **Pilih menu & key-in order sendiri**
    - Customer masukkan order melalui kiosk.
    - Order dihantar ke staff untuk diproses.

- **Dapat nombor order automatik**
    - Untuk tracking di skrin besar atau resit.

- **Select promo yang tersedia**
    - Customer boleh pilih promo yang aktif (contoh: *Buy 1 Free 1*).

- **Masukkan nombor telefon untuk kumpul point**
    - Point dimasukkan dalam sistem loyalty.

- **Redeem point**
    - Point boleh ditukar kepada diskaun / free drink semasa buat order.

---

### 👨‍🍳 2. Cafe Staff (Preparer)
- **Terima order real-time**
    - Order baru masuk terus muncul dalam dashboard staff.

- **Update status order**
    - *Prepare* → sedang disediakan.
    - *Completed/Delivered* → siap & dihantar.

- **Tengok history order**
    - Senarai order yang sudah disiapkan.

- **Report stock kepada manager bila berkurang**
    - Untuk elakkan bahan habis secara tiba-tiba.
    - Contoh: bila susu tinggal < 5 liter → notifikasi ke manager.

---

### 🧑‍💼 3. Manager / Admin
- **Lihat laporan harian (daily sales)**
    - Jumlah jualan harian + jumlah cup/item terjual.

- **Analisis & chart**
    - Sales trend, top selling product, monthly/weekly graph.

- **Prediction & Forecasting**
    - Integrasi model ramalan jualan.
    - Boleh tambah manual info harian (contoh: hujan, cuti umum) untuk refine forecast.

- **Manage User & Role**
    - Tambah staff baru, deactivate, reset password.

- **Buy new stock**
    - Rekod pembelian bahan mentah dalam sistem.
    - Update quantity secara automatik bila stock masuk.

- **Create new promo**
    - Buat promosi baru (contoh: 20% diskaun, combo set).

- **Manage minimum stock level**
    - Set minimum threshold (contoh: gula minimum 2kg).
    - Bila bawah threshold → sistem auto alert.

- **Tukar harga produk**
    - Update harga minuman/makanan bila perlu.
