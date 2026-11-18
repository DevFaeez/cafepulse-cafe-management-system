"""Prepare and reconcile reports using a disposable MySQL database started with dev-seed.

Run with 'seed' to print SQL that sets controlled UTC event timestamps. Pipe that SQL
into the same disposable database, then run this script with 'verify'.
"""
import json
import os
import sys
from decimal import Decimal
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from uuid import uuid4

base = os.environ["CAFEPULSE_CHECK_API"]
password = os.environ["CAFE_DEV_SEED_PASSWORD"]


def call(path, expected=200, *, token=None, body=None, method=None, key=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["Idempotency-Key"] = str(key)
    request = Request(base + path, headers=headers, data=None if body is None else json.dumps(body).encode(), method=method)
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        parsed = json.loads(raw) if raw else None
        assert response.code == expected, (path, response.code, expected, parsed)
        return parsed


def login(role):
    return call("/auth/signin", body={"email": f"{role}@cafepulse.example", "password": password})["accessToken"]


manager, staff, kiosk = (login(role) for role in ("manager", "staff", "kiosk"))


def seed():
    assert call("/api/order/allorder", token=staff) == [], "Use an empty disposable DB."
    products = call("/api/product", token=manager)
    latte = next(item for item in products if item["name"] == "Latte")
    assert latte["salesUnit"] == "CUP"
    category = latte["productCategory"]["id"]
    large = call(f"/api/admin/productvariant/{latte['id']}", token=kiosk)[0]["id"]

    def add_product(name, price, unit):
        return call("/api/product/add", 201, token=manager, body={"name": name, "description": "Ujian laporan",
                    "productImage": "/vite.svg", "productCategory": category, "price": price, "salesUnit": unit})["id"]

    roti = add_product("Roti Ujian", "5", "FOOD")
    bag = add_product("Beg Ujian", "2", "OTHER")
    unknown = add_product("Unit Lama Ujian", "3", None)

    def promo(code, kind, price, product_id):
        call("/api/admin/promotions", 201, token=manager, body={"code": code, "name": code, "type": kind,
             "value": price, "productId": product_id, "startDate": "2000-01-01T00:00",
             "endDate": "2099-01-01T00:00", "active": True})

    promo("REPORT10", "PERCENT", "10", latte["id"])
    promo("REPORTFREE", "FIXED", "5", roti)

    def item(product_id, quantity, variants=()):
        return {"product": product_id, "quantity": quantity, "variantIds": list(variants)}

    def order(*items, code=None):
        return call("/api/order/create", 201, token=kiosk, key=uuid4(), body={
            "customerPhone": "0100000601", "promoCode": code, "item": list(items),
        })

    def pay(receipt, revision=0, method="CASH"):
        call(f"/api/order/{receipt['id']}/payment", 204, token=staff,
             body={"revision": revision, "method": method, "amount": receipt["totalAmount"]})

    def close(receipt, refund):
        call(f"/api/order/{receipt['id']}/close", 204, token=manager,
             body={"revision": 1 if refund else 0, "refund": refund, "reason": "Ujian laporan"})

    a = order(item(latte["id"], 2, (large,)), code="REPORT10")
    b = order(item(roti, 3))
    c = order(item(latte["id"], 1, (large,)))
    d = order(item(roti, 1))
    e = order(item(roti, 1))
    f = order(item(roti, 1), code="REPORTFREE")
    g = order(item(bag, 1), item(unknown, 1))
    order(item(roti, 1))
    for receipt in [a, b, c, e, g]:
        pay(receipt)
    close(c, True)
    close(d, False)
    previous = next(row for row in call("/api/order/workbench", token=staff) if row["id"] == e["id"])
    call(f"/api/order/{e['id']}/payment/void", 204, token=manager, body={
         "revision": 1, "paymentId": previous["payments"][0]["id"], "reason": "Salah rekod"})
    pay(e, 2, "QR")
    pay(f, method="NO_CHARGE")

    call(f"/api/product/{latte['id']}/update", token=manager, method="PUT", body={
        "name": "Latte Baharu", "description": latte["description"], "price": "13.00",
        "productCategory": category, "productImage": latte["productImage"], "salesUnit": "FOOD",
    })

    # Chart dates are intentionally fixed. UTC 16:00:00 is midnight in Kuala Lumpur.
    print("SET time_zone = '+00:00';")
    for receipt, when in [(a, "2026-09-30 15:59:57"), (b, "2026-09-30 16:00:01"),
                          (c, "2026-09-30 15:59:59.999999"), (e, "2026-09-30 16:05:00"),
                          (f, "2026-09-30 16:20:00"), (g, "2026-10-05 00:00:00")]:
        print(f"UPDATE order_payment SET recorded_at='{when}' WHERE order_id={int(receipt['id'])} AND voided_at IS NULL;")
    print(f"UPDATE order_payment SET refunded_at='2026-10-01 02:30:00' WHERE order_id={int(c['id'])};")
    print(f"UPDATE order_payment SET recorded_at='2026-09-30 15:59:00', voided_at='2026-09-30 16:01:00' WHERE order_id={int(e['id'])} AND voided_at IS NOT NULL;")
    print(f"UPDATE orders SET closed_at='2026-09-30 16:00:05' WHERE id={int(d['id'])};")


def fetch(path, from_, to, group=None, token=manager, expected=200):
    params = {"from": from_, "to": to}
    if group:
        params["group"] = group
    return call(path + "?" + urlencode(params), expected, token=token)


def check_amounts(value, gross, discounts, collected, refunds, net, paid, refunded, cancelled, cups, food, other=0, unknown=0):
    for name, amount in [("gross", gross), ("discounts", discounts), ("collected", collected), ("refunded", refunds), ("net", net)]:
        assert Decimal(str(value[name])) == Decimal(str(amount)), (name, value)
    for name, qty in [("paidOrders", paid), ("refundedOrders", refunded), ("cancelledOrders", cancelled),
                      ("cups", cups), ("food", food), ("other", other), ("unclassified", unknown)]:
        assert value[name] == qty, (name, value)


def verify():
    path = "/api/admin/reports/sales"
    start, end = "2026-09-30", "2026-10-05"
    call(path, 400, token=manager)
    fetch(path, end, start, expected=400)
    fetch(path, "2025-01-01", end, expected=400)
    fetch(path, start, end, "UNKNOWN", expected=400)
    fetch(path, start, end, token=staff, expected=403)
    fetch(path, start, end, token=kiosk, expected=403)
    daily = fetch(path, start, end, "DAY")
    assert daily["zone"] == "Asia/Kuala_Lumpur" and len(daily["periods"]) == 6
    check_amounts(daily["totals"], 66, "7.4", "58.6", 12, "46.6", 6, 1, 1, 2, 5, 1, 1)
    check_amounts(daily["periods"][0]["amounts"], 36, "2.4", "33.6", 0, "33.6", 2, 0, 0, 3, 0)
    check_amounts(daily["periods"][1]["amounts"], 25, 5, 20, 12, 8, 3, 1, 1, -1, 5)
    check_amounts(daily["periods"][2]["amounts"], 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    check_amounts(daily["periods"][-1]["amounts"], 5, 0, 5, 0, 5, 1, 0, 0, 0, 0, 1, 1)
    assert [(row["productName"], row["netQuantity"]) for row in daily["products"][:2]] == [("Roti Ujian", 5), ("Latte Baharu", 2)]
    weekly = fetch(path, start, end, "WEEK")
    monthly = fetch(path, start, end, "MONTH")
    assert [(p["from"], p["to"]) for p in weekly["periods"]] == [(start, "2026-10-04"), (end, end)]
    assert [(p["from"], p["to"]) for p in monthly["periods"]] == [(start, start), ("2026-10-01", end)]
    assert weekly["totals"] == monthly["totals"] == daily["totals"]
    assert Decimal(str(weekly["periods"][0]["amounts"]["net"])) == Decimal("41.60")
    assert Decimal(str(monthly["periods"][1]["amounts"]["net"])) == Decimal("13")
    empty = fetch(path, "2026-09-29", "2026-09-29")
    assert empty["totals"]["paidOrders"] == 0 and empty["products"] == []
    history_path = "/api/admin/reports/product-history"
    fetch(history_path, start, end, token=kiosk, expected=403)
    rows = fetch(history_path, start, end)["rows"]
    latte = [line for line in rows if line["productName"] == "Latte Baharu"]
    assert len({line["productId"] for line in latte}) == 1
    assert [(line["date"], line["paidQuantity"], line["refundedQuantity"], line["netQuantity"]) for line in latte] == [
        (start, 3, 0, 3), ("2026-10-01", 0, 1, -1),
    ]
    print("Sales reconcile by day/week/month, including UTC midnight boundary, discounts, voids, refunds, cancellations, zero-charge and stable product ID")


if len(sys.argv) != 2 or sys.argv[1] not in ["seed", "verify"]:
    raise SystemExit("Usage: check-sales.py seed|verify")
(seed if sys.argv[1] == "seed" else verify)()
