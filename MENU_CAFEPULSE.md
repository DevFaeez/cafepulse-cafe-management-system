# CafePulse menu

The sample menu has 52 products across nine categories. Prices are example prices in Malaysian ringgit (RM). Managers can edit them in **Products**, **Categories** and **Options**.

| Category | Products | Examples |
|---|---:|---|
| Breakfast & Brunch | 6 | Kaya Toast, Scrambled Eggs on Sourdough, Banana Pancakes |
| Main Dishes | 7 | Spiced Fried Chicken Nasi Lemak, Chicken Chop, Crispy Chicken Burger |
| Pasta & Pizza | 5 | Prawn Aglio e Olio, Creamy Mushroom Penne, Margherita Pizza |
| Snacks & Sides | 5 | French Fries, Onion Rings, Mushroom Soup |
| Pastries & Bakery | 4 | Butter Croissant, Pain au Chocolat, Apple Danish |
| Desserts | 6 | Basque Cheesecake, Chocolate Brownie, Alcohol-Free Tiramisu |
| Espresso Drinks | 8 | Latte, Americano, Cappuccino, Palm Sugar Latte |
| Tea & Chocolate | 5 | Matcha Latte, Pulled Milk Tea, Chai Latte |
| Cold Drinks | 6 | Fresh Orange Juice, Mint Lemonade, Mango Smoothie |

Selected products offer options such as large size, iced, oat milk, eggs, cheese or ice cream. Sales reports count food items, cups and bottled water under their respective units.

## Import the menu

The full catalogue is in `dev/menu-cafepulse.json`. Run this from the project folder with the local API running.

```sh
set -a
. .runtime/dev.env
set +a
python3 dev/import-cafe-menu.py
```

Set `CAFEPULSE_MANAGER_EMAIL` and `CAFEPULSE_MANAGER_PASSWORD` to use your own manager account. Use `--api` if the API address differs. Use `--check` to validate the catalogue and image files.

Re-running the importer fills in missing products or options. It upgrades recognised sample menu labels to English in place. Existing product IDs, prices, images and availability are preserved during that translation. Text entered by customers or staff stays part of its original record.

## Set up inventory and recipes

Run `python3 dev/import-cafe-menu.py --with-inventory` after updating the CafePulse server. Add `--check` to validate the full catalogue and recipe template without contacting the server. The importer adds 97 ingredients, base recipes for all 52 products and recipes for all 50 menu options. It preserves existing recipes and counted balances when rerun.

1. Open **Manager console → Inventory**. Filter by **Awaiting count**, then use **Set opening balance** for each ingredient. Count stock in the displayed unit and review the suggested minimum against your delivery schedule.
2. Open **Recipes** and review the quantities per product against your kitchen portions. The template tracks bulk ingredients in kilograms or litres and purchased bakery items, selected desserts and bottles by piece. Where a recipe uses a fraction of a batch item, the quantity is expressed as its share of one piece.
3. Review options as well as base recipes. **Large** increases the relevant ingredients, **Iced** adds ice, and **Oat milk** replaces all whole milk in that line, including milk added by **Large**. On Chicken Chop, mushroom sauce replaces black pepper sauce. Egg, cheese and ice cream add-ons deduct their own ingredients and any associated preparation stock.
4. Once the ingredients needed for a product have opening counts, staff can deduct them by selecting **Start preparing** on a paid order. If a required ingredient has no opening count or insufficient stock, preparation is blocked and the whole deduction is rolled back. Review ingredient readiness before serving customers, since the kiosk menu uses product availability settings.
5. Check **Stock history** for linked recipe deductions, adjustments, receipts and waste. A manager can record a full refund and return the original deduction when the unused ingredients actually go back into stock.

The template provides starting quantities and minimums. The kitchen team should confirm them with a batch and portion test. Each ingredient begins with a blank balance until the manager records a physical count.

## Menu photography

CafePulse uses these photographs under the [Unsplash License](https://unsplash.com/license). Products without an image use a category icon until a manager adds a photo.

- Scrambled Eggs on Sourdough by [Lala Azizli](https://unsplash.com/photos/healthy-breakfast-with-toast-eggs-and-vegetables-2h9_d8JPIO0).
- Crispy Chicken Burger by [Allen Y](https://unsplash.com/photos/a-crispy-chicken-burger-with-french-fries-Omyk3J_LPfk).
- Margherita Pizza from [Unsplash](https://unsplash.com/photos/pizza-with-green-leaves-on-white-ceramic-plate-pPA-_q5v1a8).
- French Fries by [engin akyurt](https://unsplash.com/photos/a-plate-of-french-fries-and-ketchup-on-a-table-uhNL2JJm4f4).
- Butter Croissant from [Unsplash](https://unsplash.com/photos/golden-croissants-with-butter-and-jam-on-table-F4PQYppd4NQ).
- Basque Cheesecake by [almira_s](https://unsplash.com/photos/basque-burnt-cheesecake-slice-on-plate--h4fJ3MCy04).
- Latte by [Tabitha Turner](https://unsplash.com/photos/white-ceramic-cup-with-saucer-on-brown-wooden-table-3n3mPoGko8g).
- Americano by [M Grate Pest](https://unsplash.com/photos/a-cup-of-coffee-sitting-on-top-of-a-saucer-1vBFkgwjQtg).
- Cappuccino by [Jhunelle Francis Sardido](https://unsplash.com/photos/a-cup-of-cappuccino-sitting-on-top-of-a-table-LSZmhnn5qEM).
- Matcha Latte by [Cody Chan](https://unsplash.com/photos/a-glass-of-refreshing-matcha-iced-latte-Oog-4Ox0rv8).
- Fresh Orange Juice by [Mateusz Feliksik](https://unsplash.com/photos/orange-juice-in-clear-drinking-glass-TmOGarNOGFs).
