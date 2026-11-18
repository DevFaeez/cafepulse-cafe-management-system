# 📂 Cafe Management System – Entities & Attributes

Dokumen ini mengandungi senarai entity, atribut, dan relationship asas untuk sistem Cafe Management.

---

## 👤 Staff
- user_id: BIGINT **(PK)**
- username: VARCHAR (UQ)
- password: VARCHAR
- full_name: VARCHAR
- phone_number: VARCHAR
- role: VARCHAR (staff / manager)
- email: VARCHAR
- status: VARCHAR (active / inactive)
- created_at: TIMESTAMP

---

## 👥 Customer
- customer_id: BIGINT **(PK)**
- phone_number: VARCHAR (UQ)
- status: VARCHAR (active / inactive)
- created_at: TIMESTAMP
- last_buy_at: TIMESTAMP

---

## 🍵 Product
- product_id: BIGINT **(PK)**
- name: VARCHAR
- description: TEXT
- price: DECIMAL(10,2)
- created_at: TIMESTAMP

---

## 🍵 ProductVariant
- variant_id: BIGINT **(PK)**
- product_id: BIGINT **(FK → Product.product_id)**
- category: VARCHAR (contoh: sugar, size)
- value: VARCHAR (contoh: less sugar, big cup)
- extra_price: DECIMAL(10,2)

---

## 🧾 Order
- order_id: BIGINT **(PK)**
- customer_id: BIGINT **(FK → Customer.customer_id)**
- order_number: VARCHAR (unique, untuk display kepada customer)
- status: VARCHAR (pending, preparing, delivered, completed, cancelled)
- total_amount: DECIMAL(10,2)
- created_at: TIMESTAMP
- completed_at: TIMESTAMP (update bila status = completed)

---

## 🧾 OrderItem
- order_item_id: BIGINT **(PK)**
- order_id: BIGINT **(FK → Order.order_id)**
- product_id: BIGINT **(FK → Product.product_id)**
- quantity: INT
- price: DECIMAL(10,2) (snapshot masa order dibuat)

---

## 🧾 OrderItemVariant
- order_item_variant_id: BIGINT **(PK)**
- order_item_id: BIGINT **(FK → OrderItem.order_item_id)**
- variant_id: BIGINT **(FK → ProductVariant.variant_id)**

---

## 📦 Stock
- stock_id: BIGINT **(PK)**
- stock_name: VARCHAR
- quantity: DECIMAL(10,2)
- unit: VARCHAR (liter, kg, pack, pcs)
- status: VARCHAR (out_of_stock, low_stock, enough_stock)
- updated_at: TIMESTAMP

---

## 🎁 Promo
- promo_id: BIGINT **(PK)**
- name: VARCHAR
- description: TEXT
- discount_type: VARCHAR (percentage / fixed / BOGO)
- discount_value: DECIMAL(10,2)
- start_date: TIMESTAMP
- end_date: TIMESTAMP
- is_active: BOOLEAN

---

## ⭐ LoyaltyPoint
- loyalty_id: BIGINT **(PK)**
- customer_id: BIGINT **(FK → Customer.customer_id)**
- points_balance: INT
- last_updated: TIMESTAMP

--

## Relationship

- CUSTOMER ||--o{ ORDER : places <br>
- ORDER ||--o{ ORDERITEM : contains <br>
- PRODUCT ||--o{ ORDERITEM : "ordered in" <br>
- PRODUCT ||--o{ PRODUCTVARIANT : has <br>
- ORDERITEM ||--o{ ORDERITEMVARIANT : selects <br>
- PRODUCTVARIANT ||--o{ ORDERITEMVARIANT : applied <br>
- CUSTOMER ||--o{ LOYALTYPOINT : earns <br>
