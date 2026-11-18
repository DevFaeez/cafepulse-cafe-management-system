"""Prepare and reconcile the Fasa 8 rehearsal and verify a restored copy.

Use only against an isolated MySQL database. The browser rehearsal runs after
'setup' and before 'snapshot'. Keep the snapshot file private.
"""
import json
import os
import sys
from decimal import Decimal
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

api = os.environ["CAFEPULSE_CHECK_API"]
password = os.environ["CAFE_DEV_SEED_PASSWORD"]
day = os.environ["CAFEPULSE_PILOT_DATE"]
phone = "0100000801"


def call(path, expected=200, *, token=None, body=None, method=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = Request(api + path, headers=headers, data=None if body is None else json.dumps(body).encode(), method=method)
    try:
        response = urlopen(request, timeout=12)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        data = json.loads(raw) if raw else None
        assert response.code == expected, (path, response.code, expected)
        return data, response.headers


def login(role):
    data = {"email": f"{role}@cafepulse.example", "password": password}
    return call("/auth/signin", body=data)[0]


def setup():
    manager, staff = (login(role)["accessToken"] for role in ("manager", "staff"))
    assert call("/api/order/allorder", token=staff)[0] == []
    assert call("/api/stock/all", token=manager)[0] == []
    assert call("/api/admin/promotions", token=manager)[0] == []
    products = call("/api/product", token=manager)[0]
    latte = next(product for product in products if product["name"] == "Latte")
    large = call(f"/api/admin/productvariant/{latte['id']}", token=manager)[0][0]
    stock = call("/api/stock/create", 201, token=manager, body={
        "stockName": "Susu Rintis", "unit": "L", "openingQuantity": "5.2", "minimum": "5",
    })[0]
    for kind, id_, qty in [("PRODUCT", latte["id"], ".2"), ("VARIANT", large["id"], ".05")]:
        call(f"/api/admin/inventory-recipes/{kind}/{id_}", token=manager, method="PUT",
             body={"ingredients": [{"stockId": stock["id"], "quantity": qty}]})
    call("/api/admin/promotions", 201, token=manager, body={
        "code": "RINTIS10", "name": "Promosi rintis", "type": "PERCENT", "value": 10,
        "startDate": "2000-01-01T00:00", "endDate": "2099-01-01T00:00", "active": True,
    })
    print("Rehearsal fixtures ready")


def private_snapshot():
    manager, staff, kiosk = (login(role) for role in ("manager", "staff", "kiosk"))
    manager_token, staff_token, kiosk_token = (session["accessToken"] for session in (manager, staff, kiosk))
    query = urlencode({"from": day, "to": day})
    report_url = "/api/admin/reports/sales?" + query
    call(report_url, 403, token=staff_token)
    call("/api/stock/all", 403, token=kiosk_token)
    call("/api/admin/forecast/mappings", 403, token=staff_token)
    call("/api/customer/find-by-phone", 403, token=staff_token, body={"phoneNumber": phone})
    contact, headers = call("/api/customer/find-by-phone", token=manager_token, body={"phoneNumber": phone})
    assert contact["phoneNumber"] == phone and "no-store" in headers.get("Cache-Control", "")
    history = call("/api/order/history", token=staff_token)[0]["orders"]
    legacy = call("/api/order/allorder", token=staff_token)[0]
    assert len(legacy) == len(history) == 4
    assert phone not in json.dumps([legacy, history])
    assert all("phoneNumber" not in order["customer"] for order in legacy)
    assert call("/api/order/workbench", token=staff_token)[0] == []
    by_queue = {order["orderSequence"]: order for order in history}
    assert [by_queue[str(number)]["status"] for number in range(1, 5)] == [
        "ORDER_COMPLETED", "ORDER_COMPLETED", "ORDER_CANCELLED", "ORDER_REFUNDED",
    ]
    report = call(report_url, token=manager_token)[0]
    totals = report["totals"]
    for field, expected in {"gross": "42", "discounts": "3.40", "collected": "38.60",
                            "refunded": "10", "net": "28.60"}.items():
        assert Decimal(str(totals[field])) == Decimal(expected), (field, totals)
    assert [totals[key] for key in ("paidOrders", "refundedOrders", "cancelledOrders", "cups")] == [3, 1, 1, 3]
    stocks = call("/api/stock/all", token=staff_token)[0]
    assert len(stocks) == 1 and Decimal(str(stocks[0]["quantity"])) == Decimal("5.5")
    assert call("/api/stock/alerts", token=manager_token)[0] == []
    movements = call(f"/api/stock/{stocks[0]['id']}/movements", token=staff_token)[0]["movements"]
    assert [entry["kind"] for entry in movements] == ["ORDER_RETURN", "ORDER_USAGE", "WASTE", "RECEIPT", "ORDER_USAGE", "OPENING"]
    assert len([order for order in legacy if order["orderSequence"] == "1"]) == 1
    data = {"history": history, "report": report, "stocks": stocks, "movements": movements}
    return data, staff["accessToken"]


def snapshot(path):
    data, token = private_snapshot()
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w", encoding="utf-8") as file:
        json.dump({"data": data, "sourceToken": token}, file, sort_keys=True)
    print("Shift reconciled: 4 orders, RM38.60 received, RM10.00 refunded, RM28.60 net, 5.5 L remaining")


def restored(path):
    earlier = json.loads(path.read_text())
    call("/auth/session", 401, token=earlier["sourceToken"])
    data, _ = private_snapshot()
    assert data == earlier["data"], "Restored transactional snapshot differs"
    print("Restore verified via new login: identical orders, payments, report and stock ledger; old session rejected")


if len(sys.argv) == 2 and sys.argv[1] == "setup":
    setup()
elif len(sys.argv) == 3 and sys.argv[1] in {"snapshot", "restored"}:
    (snapshot if sys.argv[1] == "snapshot" else restored)(Path(sys.argv[2]))
else:
    raise SystemExit("Usage: check-pilot.py setup | snapshot FILE | restored FILE")
