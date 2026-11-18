# CafePulse — Cafe Management System

CafePulse brings self-service ordering, staff order queues, inventory recipes, loyalty rewards, sales reports and sales forecasting into one web application.

## Features

- Customer kiosk with category filters, product options, a cart and queue numbers.
- Staff dashboard for recording counter payments and preparing orders.
- Manager tools for products, staff accounts, promotions and customer loyalty.
- Inventory receipts, usage, waste, adjustments and recipe deductions.
- Sales reports with date filters, refunds and product quantities.
- Forecast scenarios for 52 sample menu products, with downloadable simulated sales history.

The sample catalogue contains 52 products, 50 options and 97 recipe ingredients. Sample sales, opening quantities and forecasting results are demonstration data. Counter payments are recorded by staff after they receive or verify payment.

## Stack

- React, Vite, Material UI and Redux Toolkit.
- Java 17, Spring Boot, Spring Security, Spring Data JPA and MySQL 8.4.
- Python 3.11, Flask, NumPy, pandas and scikit-learn.
- Docker for local MySQL and the forecasting service.

## Local setup

Install JDK 17, Node.js with npm, Docker and OpenSSL.

### Generate local configuration

```sh
sh dev/prepare-env.sh
set -a
. .runtime/dev.env
set +a
```

The script generates random database passwords, a JWT secret and a development account password in `.runtime/dev.env`. Keep this file private. The same configuration is needed in each terminal that starts a service.

### Start MySQL

```sh
docker run -d --name cafepulse-mysql-dev \
  --publish 127.0.0.1:3307:3306 \
  --env MYSQL_ROOT_PASSWORD --env MYSQL_DATABASE --env MYSQL_USER --env MYSQL_PASSWORD \
  --volume cafepulse-mysql-dev:/var/lib/mysql \
  mysql:8.4
```

Wait for MySQL to be ready. On subsequent runs, start the existing container with `docker start cafepulse-mysql-dev`.

### Start the forecast service

```sh
docker build -t cafepulse-forecast:menu-simulation MachineLearning
docker run -d --name cafepulse-forecast --restart unless-stopped \
  -p 127.0.0.1:5001:5000 cafepulse-forecast:menu-simulation
```

### Start the Java API

After loading `.runtime/dev.env`, run:

```sh
cd cafe-prediction
sh ./mvnw clean verify
java -jar target/cafe-prediction-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev-seed
```

The API listens on `http://localhost:8081`. The `dev-seed` profile creates these local demonstration accounts, using `CAFE_DEV_SEED_PASSWORD` from your generated configuration.

| Email | Workspace |
|---|---|
| `kiosk@cafepulse.example` | Customer kiosk |
| `staff@cafepulse.example` | Staff dashboard |
| `manager@cafepulse.example` | Manager console |

For a store installation, use a dedicated database and the first-manager setup described in [the operations guide](docs/OPERATIONS.md).

### Start the frontend

From another terminal, load `.runtime/dev.env`, then run:

```sh
cd cafe-prediction-frontend
npm ci
npm run dev -- --host localhost --port 5174 --strictPort
```

Open `http://localhost:5174`.

### Import the sample menu and recipes

With the Java API running and local configuration loaded, run from the repository root:

```sh
python3 dev/import-cafe-menu.py --check --with-inventory
python3 dev/import-cafe-menu.py --with-inventory
```

Managers can review recipe quantities and record opening ingredient counts in Inventory. See [the menu and inventory guide](MENU_CAFEPULSE.md) for details.

## Forecasting

The menu forecast uses a Ridge regression per product with 21 scenario inputs. It evaluates 365 days of simulated history in chronological order, reserving the latest 20% for testing and comparing against a same-weekday average. The original eight-drink Gradient Boosting model and experimental notebooks are also included.

Run the Python checks with:

```sh
docker run --rm cafepulse-forecast:menu-simulation python -m unittest interface.test_forecast
```

Model evaluation describes performance on simulated data. See [the forecast guide](docs/FORECASTING.md) for the workflow and limitations.

## Data and credentials

Local environment files, Docker database content, database backups, logs, dependency folders and build outputs are excluded from Git. MySQL data lives in its Docker volume. To move current operational data, use the backup and restore scripts described in [the backup and recovery guide](docs/OPERATIONS.md#back-up-and-restore).

The notebook outputs are cleared. The bundled spreadsheets contain synthetic sales used for model experiments. The application loads the trusted model bundled in `MachineLearning/model`.

## Project layout

| Folder | Purpose |
|---|---|
| `cafe-prediction-frontend` | React web application |
| `cafe-prediction` | Spring Boot API |
| `cafe-prediction-database` | Development database setup |
| `MachineLearning` | Forecast service, models and research notebooks |
| `dev` | Environment setup, sample catalogue and integration checks |

Menu photography credits and asset sources are listed in [the menu guide](MENU_CAFEPULSE.md).
