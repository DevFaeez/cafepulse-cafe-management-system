# CafePulse operations

Use the [README](../README.md) to configure the database, API and frontend. Review the [menu and inventory guide](../MENU_CAFEPULSE.md) before taking orders.

## Access and accounts

Customers use the kiosk workspace. Staff process orders, record counter payments and maintain stock. Managers also manage products, accounts, promotions, customer loyalty and reports.

For a store installation, configure a dedicated database and session secret. Create the first manager using the `bootstrap-manager` profile. From the project root, with the database configuration loaded and the Java package built, run the following in Bash. Replace the example email with the manager's address.

~~~sh
export CAFE_BOOTSTRAP_MANAGER_EMAIL='manager@example.com'
read -r -s -p 'Manager password: ' CAFE_BOOTSTRAP_MANAGER_PASSWORD
export CAFE_BOOTSTRAP_MANAGER_PASSWORD
java -jar cafe-prediction/target/cafe-prediction-0.0.1-SNAPSHOT.jar \
  --server.address=127.0.0.1 --server.port=0 --spring.profiles.active=bootstrap-manager
unset CAFE_BOOTSTRAP_MANAGER_EMAIL CAFE_BOOTSTRAP_MANAGER_PASSWORD
~~~

The password must contain at least 12 characters and at most 72 UTF-8 bytes. The process exits after setup. If a manager already exists, the existing account is kept. Start the API normally, using the same database configuration, then create staff and kiosk accounts in **Accounts**.

The `dev-seed` profile creates the demonstration accounts described in the README. Access tokens last 15 minutes by default and refresh automatically within an active session. Select **Sign out** to revoke the session.

## Process an order

1. The customer selects products and options, reviews the cart and selects **Place order**. Identify the order by its queue number.
2. Staff confirm the money received or the transaction on the payment terminal, then use **Record payment** to save the method and amount.
3. Staff select **Start preparing**. CafePulse deducts recipe ingredients and records the preparation status together. A missing opening count or insufficient ingredient balance blocks preparation.
4. Select **Mark as ready** when the order is ready to hand over.
5. Managers can cancel an unpaid order, correct a payment before preparation, or record a full refund. Return unused ingredients to stock only when the physical ingredients can actually be returned.

When a submission is interrupted, keep the same browser tab and use **Resubmit** after the connection returns. Check the staff queue and history before repeating a payment or stock action.

## Start and close a shift

Before opening, confirm menu prices, availability, promotions, recipes, opening counts and minimum levels. Check that the staff dashboard has a working connection and take a database backup.

At closing, compare recorded cash, QR and card payments with their original payment records. Review refunds, cancelled orders, stock movements and the final physical count. Sales dates use **Asia/Kuala_Lumpur**. Take another backup and sign out.

## Back up and restore

From the project root, back up a running MySQL container with the bundled script.

~~~sh
backup=".runtime/backups/CafePulse-$(date +%Y%m%d-%H%M%S).sql.gz"
bash dev/backup-db.sh cafepulse-mysql-dev "$backup"
gzip -t "$backup"
~~~

Use your operating container name when it differs. If Docker is outside the shell PATH on macOS, set `DOCKER_CMD=/Applications/Docker.app/Contents/Resources/bin/docker`.

Backups contain account, customer and payment records. Store them privately. Login sessions are excluded, so users sign in again after restoration.

To restore, create a separate MySQL container with an empty database and keep CafePulse disconnected from that database. Run the following with the actual backup path.

~~~sh
bash dev/restore-db.sh cafepulse-mysql-restore "$backup"
~~~

The script rejects a database that already contains tables. Start the API against the restored database using its configuration. Compare order statuses, payment totals, sales reports and stock balances with the original records before switching the operating installation.

## Connection checks

| Service | Local check |
|---|---|
| Frontend | Open `http://localhost:5174`. |
| API | `curl -i http://localhost:8081/auth/session` returns HTTP 401 without a session. |
| MySQL | Check the configured container with `docker ps -a`. |
| Forecast | `curl -fsS http://localhost:5001/health` returns a health response. |

The default configuration binds services to the local computer. Other devices require an appropriate network configuration.
