# Fasa 0 CafePulse · Persediaan setempat

Panduan ini menyediakan frontend React, pelayan Java dan satu MySQL khusus untuk CafePulse. Gunakan salinan kerja pada storan dalaman Mac supaya pemasangan npm boleh menggunakan pautan fail dan membina pakej untuk Mac. Folder projek pada ESSD ialah sumber kod asal.

## Keperluan

- JDK 17 dan Node.js yang serasi dengan fail kunci frontend. Binaan ini disahkan dengan Temurin 17.0.21 dan Node.js 26.8.2.
- Docker Desktop yang sedang berjalan. Pangkalan data menggunakan imej `mysql:8.4`.
- Python 3 untuk semakan HTTP. Ujian Fasa 0 tidak memerlukan persekitaran Python untuk model ramalan.
- Akses repositori npm dan Maven untuk pemasangan pertama.

## 1. Sediakan salinan kerja dan konfigurasi

Jalankan blok ini dari root projek CafePulse pada ESSD. Pilih direktori kosong pada storan dalaman jika menyediakan salinan baharu. Arahan ini hanya menyalin sumber dan fail yang diperlukan untuk Fasa 0.

```sh
local_project="$HOME/CafePulse-local"
mkdir -p "$local_project/cafe-prediction" "$local_project/cafe-prediction-frontend" \
  "$local_project/cafe-prediction-database" "$local_project/dev"
rsync -r --exclude=target --exclude=.idea --exclude='._*' cafe-prediction/ "$local_project/cafe-prediction/"
rsync -r --exclude=node_modules --exclude=dist --exclude='._*' cafe-prediction-frontend/ "$local_project/cafe-prediction-frontend/"
rsync -r --exclude='._*' cafe-prediction-database/ "$local_project/cafe-prediction-database/"
rsync -r --exclude='._*' dev/ "$local_project/dev/"
cp .gitignore "$local_project/"
cd "$local_project"

sh dev/prepare-env.sh
set -a
. ./.runtime/dev.env
set +a
```

Skrip menjana kata laluan MySQL, kunci token dan kata laluan akaun ujian dalam `.runtime/dev.env`. Fail tersebut hanya boleh dibaca oleh pemilik dan dikecualikan daripada Git. Simpan fail yang sama untuk pangkalan data itu kerana menukar nilai selepas MySQL membuat pengguna memerlukan perubahan kata laluan dalam MySQL juga.

Port setempat yang digunakan dalam semakan ini:

| Komponen | Alamat |
|---|---|
| Frontend React | `http://localhost:5174` |
| API Spring Boot | `http://localhost:8081` |
| MySQL 8.4 | `127.0.0.1:3307`, skema `cafe_management_dev` |

Jika port berubah, kemas kini URL MySQL atau port Spring dalam `.runtime/dev.env`. Tukar `CAFE_CORS_ORIGIN` untuk sepadan dengan alamat frontend. Untuk alamat API selain port 8081, tetapkan `VITE_API_BASE_URL` sebelum membina atau menjalankan frontend.

## 2. Jalankan MySQL

Jalankan dari salinan kerja. Gunakan `/Applications/Docker.app/Contents/Resources/bin/docker` jika arahan `docker` tiada dalam PATH.

```sh
docker run -d --name cafepulse-mysql-dev \
  --publish 127.0.0.1:3307:3306 \
  --env MYSQL_ROOT_PASSWORD --env MYSQL_DATABASE --env MYSQL_USER --env MYSQL_PASSWORD \
  --volume cafepulse-mysql-dev:/var/lib/mysql \
  mysql:8.4
```

Gunakan `docker start cafepulse-mysql-dev` pada sesi berikutnya. Tunggu sehingga arahan semakan ini memaparkan nama skema dan versi. Imej Docker mewujudkan skema dan pengguna aplikasi daripada fail persekitaran. Skrip `setup.sql` juga boleh mewujudkan skema dengan akaun pentadbir MySQL.

```sh
docker exec cafepulse-mysql-dev sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" -D "$MYSQL_DATABASE" -N -e "SELECT DATABASE(), VERSION();"'
```

## 3. Bina, uji dan jalankan Spring Boot

Dalam terminal pelayan, mula dari root salinan kerja. Tetapkan `JAVA_HOME` kepada pemasangan JDK 17. Di Mac dengan JDK yang didaftarkan, gunakan arahan berikut. Untuk pemasangan JDK tempatan, gunakan path ke `Contents/Home` JDK itu.

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
"$JAVA_HOME/bin/java" -version
set -a
. ./.runtime/dev.env
set +a

cd cafe-prediction
sh ./mvnw clean verify
"$JAVA_HOME/bin/java" -jar target/cafe-prediction-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev-seed
```

Hibernate mewujudkan jadual dalam skema kosong. Profil `dev-seed` memasukkan kategori **Minuman Ujian**, **Latte RM10**, **Americano RM8**, variasi **Besar +RM2** untuk Latte serta tiga akaun berikut. Ulang mula menggunakan pangkalan data sama untuk menyemak bahawa rekod contoh yang sedia ada tidak diduplikasi.

| Akaun | Peranan |
|---|---|
| `manager@cafepulse.example` | `ROLE_MANAGER` |
| `staff@cafepulse.example` | `ROLE_STAFF` |
| `kiosk@cafepulse.example` | `ROLE_CUSTOMER` |

Semua akaun ujian menggunakan nilai `CAFE_DEV_SEED_PASSWORD` daripada fail persekitaran. Pelayan menyimpan kata laluan akaun sebagai hash BCrypt. Tiada rekod pelanggan diperlukan untuk semakan menu. Untuk ujian checkout pada fasa seterusnya, gunakan nombor rekaan.

## 4. Bina dan jalankan frontend

Dalam terminal berasingan dari root salinan kerja:

```sh
set -a
. ./.runtime/dev.env
set +a
cd cafe-prediction-frontend
npm ci --no-audit --no-fund
npm run build
npm run dev -- --host localhost --port 5174 --strictPort
```

Buka `http://localhost:5174`. Pada Fasa 0, frontend boleh dimuatkan. Ia masih memerlukan token untuk meminta menu. Skrin log masuk dan cara kiosk mendapatkan sesi ialah kerja Fasa 1.

## 5. Sahkan perkhidmatan

Dalam terminal ketiga dari root salinan kerja:

```sh
set -a
. ./.runtime/dev.env
set +a
python3 dev/check-local.py
```

Semakan ini memeriksa frontend boleh dimuatkan, tiga akaun boleh log masuk, API menu menolak permintaan tanpa token, asal frontend dibenarkan, dua produk dipulangkan melalui akaun pengurus dan tambahan Latte bernilai RM2. Ujian Maven juga memeriksa konteks Spring boleh bermula dengan MySQL.

Untuk menguji dari skema baharu, jalankan satu bekas MySQL ujian berasingan dengan nama dan port lain atau port sementara Docker. Mulakan pelayan dengan URL pangkalan data itu dan jalankan semakan yang sama. Semak menu dan tiga akaun wujud selepas permulaan pertama. Mulakan semula pelayan dengan profil `dev-seed` dan semak bilangan kategori, produk, variasi dan akaun kekal sama. Gunakan bekas ujian yang berasingan daripada bekas berdata pembangunan.

Hentikan frontend dan Spring Boot menggunakan Ctrl+C dalam terminal masing-masing. Gunakan `docker stop cafepulse-mysql-dev` untuk menghentikan MySQL sambil mengekalkan pangkalan data untuk sesi berikutnya.

## Keputusan semakan pada 15 November 2026

- Frontend berjaya dibina daripada fail kunci dan memulangkan HTTP 200 pada port 5174.
- Maven Wrapper berjaya menjalankan `clean verify` menggunakan JDK 17. Ujian sedia ada lulus dengan satu ujian dan sifar kegagalan.
- Spring Boot pada port 8081 disambungkan ke MySQL 8.4.13. Ketiga-tiga akaun ujian berjaya log masuk. Dengan token pengurus, API memulangkan dua produk dan variasi Latte +RM2. Permintaan menu tanpa token ditolak dan asal frontend setempat dibenarkan.
- Ulangan bermula dengan sifar jadual pada bekas MySQL berasingan. Permulaan pertama dan kedua masing-masing menghasilkan satu kategori menu, dua produk, satu kategori variasi, satu variasi dan tiga akaun, tanpa pendua.
- Salinan aktif pada Mac ini berada dalam folder `CafePulse-local` di ruang kerja `Thread Affiliate`. Untuk menggunakan JDK yang dipasang dalam salinan ini, jalankan `export JAVA_HOME="$(pwd)/.runtime/jdk17/Contents/Home"` dari root salinan kerja.
