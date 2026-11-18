"""Fasa 5 integration checks. Run on a fresh disposable MySQL with dev-seed."""
import json
import os
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4

api = os.environ["CAFEPULSE_CHECK_API"]
second = os.environ.get("CAFEPULSE_CHECK_SECOND_API", api)
password = os.environ["CAFE_DEV_SEED_PASSWORD"]


def call(path, expected=200, *, token=None, body=None, key=None, method=None, server=api):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["Idempotency-Key"] = str(key)
    request = Request(server + path, data=None if body is None else json.dumps(body).encode(),
                      method=method, headers=headers)
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        data = json.loads(raw) if raw else None
        if expected is not None:
            assert response.code == expected, (path, response.code, expected, data)
        return response.code, data


def login(role):
    return call("/auth/signin", body={"email": f"{role}@cafepulse.example", "password": password})[1]["accessToken"]


manager, staff, kiosk = (login(role) for role in ("manager", "staff", "kiosk"))
assert call("/api/stock/all", token=staff)[1] == [], "Run against a clean disposable database"
call("/api/stock/all", 403, token=kiosk)
call("/api/stock/create", 403, token=staff, body={"stockName": "Cubaan staf"})
call("/api/stock/alerts", 403, token=staff)
legacy = call("/api/stock/create", 201, token=manager, body={"stockName": "Susu Ujian"})[1]
milk = legacy["id"]
milk_path = f"/api/stock/{milk}"
assert legacy["configured"] is False and legacy["quantity"] is None
call(milk_path + "/movements", 409, token=staff, key=uuid4(),
     body={"type": "RECEIPT", "quantity": 3, "reason": "Bahan belum dikira"})
settings = {"stockName": "Susu Ujian", "unit": "L", "minimum": 5, "openingQuantity": 4}
opening = call(milk_path + "/settings", token=manager, method="PUT", body=settings)[1]
assert opening["configured"] and opening["quantity"] == 4 and opening["status"] == "STOCK_LOW_STOCK"
assert [stock["id"] for stock in call("/api/stock/alerts", token=manager)[1]] == [milk]
call(milk_path + "/settings", 400, token=manager, method="PUT", body={**settings, "unit": "ML", "openingQuantity": None})
call(milk_path + "/update", 409, token=staff, body={"status": "STOCK_ENOUGH"})

receipt = {"type": "RECEIPT", "quantity": 3, "reason": "Bekalan pagi"}
key = uuid4()
with ThreadPoolExecutor(max_workers=2) as pool:
    deliveries = list(pool.map(lambda host: call(milk_path + "/movements", 201, token=staff, body=receipt, key=key, server=host)[1], (api, second)))
assert deliveries[0] == deliveries[1] and deliveries[0]["balanceAfter"] == 7
assert deliveries[0]["actor"] == "staff@cafepulse.example"
call(milk_path + "/movements", 409, token=staff, key=key, body={**receipt, "quantity": 4})
assert call("/api/stock/alerts", token=manager)[1] == []
assert len(call(milk_path + "/movements", token=staff)[1]["movements"]) == 2
print("Unknown legacy balance, 4 L low alert, duplicate +3 L delivery, final 7 L and actor verified")

def balance(stock_id, expected):
    stock = next(entry for entry in call("/api/stock/all", token=staff)[1] if entry["id"] == stock_id)
    assert Decimal(str(stock["quantity"])) == Decimal(str(expected)), stock
    return stock


def move(path, kind, quantity, expected=201, revision=None):
    return call(path + "/movements", expected, token=staff, key=uuid4(),
                body={"type": kind, "quantity": quantity, "reason": "Ujian kiraan", "revision": revision})[1]


move(milk_path, "USAGE", 1)
move(milk_path, "WASTE", "0.5")
move(milk_path, "WASTE", 99, 409)
move(milk_path, "ADJUSTMENT", 5, 409, opening["revision"])
stock = balance(milk, "5.5")
move(milk_path, "ADJUSTMENT", 5, revision=stock["revision"])
assert balance(milk, 5)["status"] == "STOCK_LOW_STOCK"
beans = call("/api/stock/create", 201, token=manager, body={
    "stockName": "Kopi Ujian", "unit": "KG", "openingQuantity": 0, "minimum": ".100",
})[1]["id"]

menu = call("/api/product/available", token=kiosk)[1]
latte = next(item for item in menu if item["name"] == "Latte")["id"]
large = call(f"/api/admin/productvariant/{latte}", token=kiosk)[1][0]["id"]
base_path = f"/api/admin/inventory-recipes/PRODUCT/{latte}"
variant_path = f"/api/admin/inventory-recipes/VARIANT/{large}"
call(base_path, 403, token=staff)

def recipe(path, *pairs):
    return call(path, token=manager, method="PUT",
                body={"ingredients": [{"stockId": item, "quantity": qty} for item, qty in pairs]})[1]


recipe(base_path, (milk, ".200"), (beans, ".050"))
recipe(variant_path, (milk, ".050"))

def order():
    result = call("/api/order/create", 201, token=kiosk, key=uuid4(), body={"customerPhone": "0100000501",
                  "item": [{"product": latte, "quantity": 2, "variantIds": [large]}]})[1]
    call(f"/api/order/{result['id']}/payment", 204, token=staff,
         body={"method": "CASH", "amount": result["totalAmount"], "revision": 0})
    return result


def begin(created, expected=204, server=api):
    return call(f"/api/order/{created['id']}/status", expected, method="PUT", token=staff,
                body={"status": "ORDER_PREPARING", "revision": 1}, server=server)[0]


def refund(created, restore, expected=204, revision=2):
    return call(f"/api/order/{created['id']}/close", expected, token=manager,
                body={"refund": True, "revision": revision, "reason": "Pulangan ujian", "restoreStock": restore})


first = order()
begin(first, 409)
balance(milk, 5)
before = next(item for item in call("/api/order/workbench", token=staff)[1] if item["id"] == first["id"])
assert before["status"] == "ORDER_PENDING" and before["revision"] == 1
move(f"/api/stock/{beans}", "RECEIPT", 1)
with ThreadPoolExecutor(max_workers=2) as pool:
    outcomes = list(pool.map(lambda host: begin(first, None, host), (api, second)))
assert sorted(outcomes) == [204, 409]
balance(milk, "4.5")
balance(beans, ".9")
recipe(base_path, (milk, ".4"), (beans, ".05"))
refund(first, True)
refund(first, True, 409, revision=3)
balance(milk, 5)
balance(beans, 1)

used = order()
begin(used)
refund(used, False)
balance(milk, "4.1")
balance(beans, ".9")
recent = call(milk_path + "/movements", token=staff)[1]["movements"]
returned = next(entry for entry in recent if entry["kind"] == "ORDER_RETURN")
assert returned["delta"] == 0.5 and returned["orderSequence"] == first["orderSequence"]
assert returned["actor"] == "manager@cafepulse.example"
print("Manual movement guards, atomic insufficient-recipe rejection, once-only deductions, and exact optional returns verified")
