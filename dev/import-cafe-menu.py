#!/usr/bin/env python3
"""Load the CafePulse sample menu through the existing manager API."""

import argparse
from collections import Counter
from decimal import Decimal
import fcntl
import json
import os
from pathlib import Path
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from cafe_inventory import import_inventory, load_inventory, template_counts


ROOT = Path(__file__).resolve().parent.parent
MENU = ROOT / "dev" / "menu-cafepulse.json"
PUBLIC = ROOT / "cafe-prediction-frontend" / "public"
CATEGORY_URL = "/api/admin/productcategory"
VARIANT_URL = "/api/admin/productvariant"
PLACEHOLDER = "/media/cafepulse.png"
LEGACY_PLACEHOLDERS = {"/vite.svg", PLACEHOLDER}


def normalized(value):
    return value.strip().casefold()


def load_menu():
    catalogue = json.loads(MENU.read_text(encoding="utf-8"))
    categories = set()
    names = set()
    for section in catalogue["sections"]:
        category = normalized(section["name"])
        if not category or category in categories:
            raise ValueError(f"Duplicate or empty category: {section['name']}")
        categories.add(category)
        for item in section["items"]:
            name = normalized(item["name"])
            price = Decimal(item["price"])
            image = item.get("image", PLACEHOLDER)
            if not name or name in names or price <= 0 or price.as_tuple().exponent < -2:
                raise ValueError(f"Invalid product or duplicate name: {item['name']}")
            names.add(name)
            if not image.startswith("/media/") or not (PUBLIC / image.lstrip("/")).is_file():
                raise ValueError(f"Missing local image for {item['name']}: {image}")
            for key in item.get("choices", ()):
                if key not in catalogue["choices"]:
                    raise ValueError(f"Unknown choice for {item['name']}: {key}")
    return catalogue


class ManagerAPI:
    def __init__(self, url):
        self.url = url.rstrip("/")
        self.token = None

    def call(self, path, method="GET", data=None):
        headers = {"Accept": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        if data is not None:
            headers["Content-Type"] = "application/json"
        request = Request(self.url + path, method=method, headers=headers,
                          data=None if data is None else json.dumps(data).encode())
        try:
            with urlopen(request, timeout=12) as response:
                raw = response.read()
        except HTTPError as failure:
            raise RuntimeError(f"{method} {path} returned HTTP {failure.code}") from None
        return json.loads(raw) if raw else None


def index_by_name(records):
    indexed = {}
    for record in records:
        indexed.setdefault(normalized(record["name"]), []).append(record)
    return indexed


def translate_names(api, path, suffix, names, counts):
    existing = index_by_name(api.call(path))
    for old_name, new_name in names.items():
        old = existing.get(normalized(old_name), [])
        if len(old) == 1 and normalized(new_name) not in existing:
            api.call(f"{path}/{old[0]['id']}/{suffix}", "PUT", {"name": new_name})
            counts["translated_groups"] += 1


def translate_existing_menu(api, catalogue, counts):
    category_names = {section["previousName"]: section["name"] for section in catalogue["sections"]}
    category_names["Minuman Ujian"] = "Sample Drinks"
    group_names = {option["previousGroup"]: option["group"] for option in catalogue["choices"].values()}
    group_names["Saiz Ujian"] = "Sample Size"
    translate_names(api, CATEGORY_URL, "update", category_names, counts)
    translate_names(api, VARIANT_URL, "updatecategory", group_names, counts)
    products = index_by_name(api.call("/api/product"))

    for section in catalogue["sections"]:
        for item in section["items"]:
            matches = [record for record in products.get(normalized(item["previousName"]), [])
                       if record["productCategory"]["name"] == section["name"]
                       and record["description"] == item["previousDescription"]]
            if len(matches) != 1:
                continue
            product = matches[0]
            if any(record["id"] != product["id"] for record in products.get(normalized(item["name"]), [])):
                raise ValueError(f"Cannot translate {product['name']}: {item['name']} is already in use.")
            body = {
                "name": item["name"],
                "description": item["description"],
                "price": str(product["price"]),
                "salesUnit": product["salesUnit"],
                "productImage": product["productImage"],
                "productCategory": product["productCategory"]["id"],
            }
            api.call(f"/api/product/{product['id']}/update", "PUT", body)
            counts["translated_products"] += 1


def load_variants(api, product, item, choices, groups, counts):
    variants = api.call(f"{VARIANT_URL}/{product['id']}")
    existing = {(normalized(v["category"]["name"]), normalized(v["value"])) for v in variants}
    for key in item.get("choices", ()):
        option = choices[key]
        group = normalized(option["group"])
        value = normalized(option["value"])
        if (group, value) in existing:
            continue
        body = {
            "product": product["id"],
            "category": groups[group][0]["id"],
            "value": option["value"],
            "extraPrice": option["extraPrice"],
        }
        previous_value = normalized(option["previousValue"])
        previous_groups = {group, normalized(option["previousGroup"])}
        if key == "besar" and item.get("adoptSample"):
            previous_groups.update({"saiz ujian", "sample size"})
        old_variant = next((variant for variant in variants
                            if normalized(variant["category"]["name"]) in previous_groups
                            and normalized(variant["value"]) in {value, previous_value}), None)
        if old_variant:
            body["extraPrice"] = str(old_variant["extraPrice"])
            api.call(f"{VARIANT_URL}/{old_variant['id']}", "PUT", body)
            counts["moved_choices"] += 1
        else:
            api.call(f"{VARIANT_URL}/create", "POST", body)
            counts["new_choices"] += 1
        existing.add((group, value))


def import_menu(api, catalogue):
    counts = Counter()
    translate_existing_menu(api, catalogue, counts)
    categories = index_by_name(api.call(CATEGORY_URL))
    groups = index_by_name(api.call(VARIANT_URL))
    existing = index_by_name(api.call("/api/product"))
    for option in catalogue["choices"].values():
        group = normalized(option["group"])
        if group not in groups:
            groups[group] = [api.call(f"{VARIANT_URL}/newcategory", "POST", {"name": option["group"]})]
            counts["new_groups"] += 1

    for section in catalogue["sections"]:
        category = normalized(section["name"])
        if category not in categories:
            categories[category] = [api.call(f"{CATEGORY_URL}/new", "POST", {"productCategoryName": section["name"]})]
            counts["new_categories"] += 1
        category_id = categories[category][0]["id"]
        for item in section["items"]:
            name = normalized(item["name"])
            records = existing.get(name, [])
            previous_records = existing.get(normalized(item["previousName"]), [])
            matches = [product for product in records if product["productCategory"]["id"] == category_id]
            sample = next((product for product in records
                           if product["productCategory"]["name"] in {"Minuman Ujian", "Sample Drinks"}
                           and product["description"] in {item["description"], item["previousDescription"]}
                           and product["productImage"] in LEGACY_PLACEHOLDERS), None)
            body = {
                "name": item["name"],
                "description": item["description"],
                "price": item["price"],
                "salesUnit": item.get("salesUnit", section["salesUnit"]),
                "productImage": item.get("image", PLACEHOLDER),
                "productCategory": category_id,
            }
            if len(matches) == 1 and matches[0]["description"] == item["description"]:
                product = matches[0]
                counts["existing_products"] += 1
            elif not matches and len(records) == 1 and item.get("adoptSample") and sample:
                body["price"] = str(sample["price"])
                product = api.call(f"/api/product/{sample['id']}/update", "PUT", body)
                counts["moved_products"] += 1
            elif records or previous_records:
                print(f"Preserved existing product: {item['name']}", file=sys.stderr)
                counts["preserved_products"] += 1
                continue
            else:
                product = api.call("/api/product/add", "POST", body)
                counts["new_products"] += 1

            if item.get("choices"):
                load_variants(api, product, item, catalogue["choices"], groups, counts)

    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api", default="http://127.0.0.1:8081", help="CafePulse API address")
    parser.add_argument("--check", action="store_true", help="Validate the bundled catalogue and photos locally")
    parser.add_argument("--with-inventory", action="store_true", help="Also install ingredient settings and recipes without opening counts")
    args = parser.parse_args()
    try:
        catalogue = load_menu()
        inventory = load_inventory(catalogue) if args.with_inventory else None
        if args.check:
            print(sum(len(section["items"]) for section in catalogue["sections"]), "products in the catalogue")
            if inventory is not None:
                print(json.dumps(template_counts(inventory)))
            return 0
        password = os.environ.get("CAFEPULSE_MANAGER_PASSWORD") or os.environ.get("CAFE_DEV_SEED_PASSWORD")
        if not password:
            raise ValueError("Set CAFEPULSE_MANAGER_PASSWORD or load the existing test configuration.")
        api = ManagerAPI(args.api)
        runtime = ROOT / ".runtime"
        runtime.mkdir(exist_ok=True)
        with (runtime / "menu-import.lock").open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            account = {"email": os.environ.get("CAFEPULSE_MANAGER_EMAIL", "manager@cafepulse.example"),
                       "password": password}
            session = api.call("/auth/signin", "POST", account)
            try:
                if session["role"] != "ROLE_MANAGER":
                    raise ValueError("A CafePulse manager account is required.")
                api.token = session["accessToken"]
                counts = dict(import_menu(api, catalogue))
                if inventory is not None:
                    counts["inventory"] = import_inventory(api, catalogue, inventory)
            finally:
                try:
                    api.call("/auth/logout", "POST", {"refreshToken": session["refreshToken"]})
                except (RuntimeError, URLError) as error:
                    print(f"Sign-out: {error}", file=sys.stderr)
        print(json.dumps(dict(sorted(counts.items())), ensure_ascii=False))
        return 0
    except (KeyError, ValueError, RuntimeError, OSError, URLError) as error:
        print(f"Import menu: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
