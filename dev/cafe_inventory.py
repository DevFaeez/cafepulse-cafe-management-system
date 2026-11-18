"""Load the CafePulse ingredient and recipe template using manager API access."""

from collections import defaultdict
from decimal import Decimal, InvalidOperation
import json
from pathlib import Path


INVENTORY = Path(__file__).with_name("inventory-cafepulse.json")
STOCK_UNITS = {"L", "ML", "KG", "G", "PCS", "PACK"}
MAX_QUANTITY = Decimal("999999999.999")


def quantity(value, *, allow_zero=False):
    try:
        number = Decimal(str(value))
    except InvalidOperation as error:
        raise ValueError(f"Invalid inventory quantity: {value}") from error
    if (not number.is_finite() or not 0 <= number <= MAX_QUANTITY
            or number.as_tuple().exponent < -3 or (not allow_zero and number == 0)):
        raise ValueError(f"Invalid inventory quantity: {value}")
    return number


def option_lines(option):
    return option.get("add", {}), option.get("replace", {})


def load_inventory(menu):
    template = json.loads(INVENTORY.read_text(encoding="utf-8"))
    ingredients = template["ingredients"]
    recipes = template["recipes"]
    products = {item["name"]: set(item.get("choices", ()))
                for section in menu["sections"] for item in section["items"]}
    if recipes.keys() != products.keys():
        raise ValueError("Inventory template must cover every CafePulse menu item.")
    ingredient_names = set()
    for ingredient in ingredients.values():
        name = ingredient["name"].strip().casefold()
        if not name or name in ingredient_names or ingredient["unit"] not in STOCK_UNITS:
            raise ValueError(f"Invalid or duplicate ingredient: {ingredient['name']}")
        ingredient_names.add(name)
        quantity(ingredient["minimum"], allow_zero=True)

    used = set()
    for name, recipe in recipes.items():
        base = recipe["base"]
        options = recipe.get("options", {})
        if not base or options.keys() != products[name]:
            raise ValueError(f"Incomplete base recipe or options for {name}.")
        used.update(base)
        for amount in base.values():
            quantity(amount)
        for option in options.values():
            add, replace = option_lines(option)
            if not (add or replace) or add.keys() & replace.keys():
                raise ValueError(f"Invalid option recipe for {name}.")
            for amount in add.values():
                quantity(amount)
            for source, target in replace.items():
                if (source not in base or source == target
                        or ingredients[source]["unit"] != ingredients[target]["unit"]):
                    raise ValueError(f"Invalid ingredient replacement for {name}: {source} → {target}.")
            used.update(add)
            used.update(replace)
            used.update(replace.values())
    if used != ingredients.keys():
        raise ValueError(f"Unknown or unused inventory ingredients: {', '.join(sorted(used ^ ingredients.keys()))}")
    return template


def template_counts(template):
    return {
        "ingredients": len(template["ingredients"]),
        "base_recipes": len(template["recipes"]),
        "option_recipes": sum(len(recipe.get("options", ())) for recipe in template["recipes"].values()),
    }


def unique(entries, label):
    if len(entries) != 1:
        raise ValueError(f"Expected one {label}. Found {len(entries)}.")
    return entries[0]


def find_recipe_targets(api, menu, template):
    products = defaultdict(list)
    for product in api.call("/api/product"):
        products[product["name"]].append(product)
    for name, recipe in template["recipes"].items():
        product = unique(products[name], f"product named {name}")
        yield f"PRODUCT/{product['id']}", recipe["base"], {}
        if "options" not in recipe:
            continue
        variants = defaultdict(list)
        for option in api.call(f"/api/admin/productvariant/{product['id']}"):
            variants[option["category"]["name"], option["value"]].append(option)
        for key, item in recipe["options"].items():
            choice = menu["choices"][key]
            label = (choice["group"], choice["value"])
            variant = unique(variants[label], f"{name} option {label[0]} / {label[1]}")
            yield f"VARIANT/{variant['id']}", *option_lines(item)


def install_ingredients(api, ingredients):
    records = defaultdict(list)
    for stock in api.call("/api/stock/all"):
        records[stock["stockName"].strip().casefold()].append(stock)

    matches = {}
    for key, ingredient in ingredients.items():
        existing = records[ingredient["name"].strip().casefold()]
        if existing:
            stock = unique(existing, f"ingredient named {ingredient['name']}")
            if stock["unit"] not in {None, ingredient["unit"]}:
                raise ValueError(f"{ingredient['name']} uses {stock['unit']}. Template expects {ingredient['unit']}.")
            matches[key] = stock

    created = 0
    for key, ingredient in ingredients.items():
        stock = matches.get(key)
        if stock and stock["unit"] is not None:
            continue
        body = {"stockName": stock["stockName"] if stock else ingredient["name"],
                "unit": ingredient["unit"], "minimum": ingredient["minimum"]}
        if stock:
            matches[key] = api.call(f"/api/stock/{stock['id']}/settings", "PUT", body)
        else:
            matches[key] = api.call("/api/stock/create", "POST", body)
            created += 1
    return matches, created


def recipe_signature(lines):
    return {line["stockId"]: (Decimal(str(line["quantity"])) if line.get("quantity") is not None else None,
                               line.get("replacementStockId")) for line in lines}


def import_inventory(api, menu, template):
    targets = list(find_recipe_targets(api, menu, template))
    stocks, new_ingredients = install_ingredients(api, template["ingredients"])
    counts = {"new_ingredients": new_ingredients, "new_recipes": 0, "existing_recipes": 0, "preserved_recipes": 0,
              "awaiting_opening_count": sum(not stock["configured"] for stock in stocks.values())}
    for target, add, replace in targets:
        additions = [{"stockId": stocks[key]["id"], "quantity": amount} for key, amount in add.items()]
        replacements = [{"stockId": stocks[source]["id"], "replacementStockId": stocks[replacement]["id"]}
                        for source, replacement in replace.items()]
        ingredients = additions + replacements
        path = f"/api/admin/inventory-recipes/{target}"
        existing = api.call(path)
        if existing:
            outcome = "existing_recipes" if recipe_signature(existing) == recipe_signature(ingredients) else "preserved_recipes"
            counts[outcome] += 1
        else:
            api.call(path, "PUT", {"ingredients": ingredients})
            counts["new_recipes"] += 1
    return counts
