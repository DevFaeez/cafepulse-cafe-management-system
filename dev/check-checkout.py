"""Checkout integration checks. Run only against a disposable CafePulse database seeded with dev-seed."""
import json
import os
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4

api = os.environ["CAFEPULSE_CHECK_API"]
other_api = os.environ["CAFEPULSE_CHECK_SECOND_API"]
password = os.environ["CAFE_DEV_SEED_PASSWORD"]
phone = "0100000201"


def call(path, expected=200, *, token=None, payload=None, key=None, method=None, server=api):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["Idempotency-Key"] = str(key)
    body = json.dumps(payload).encode() if payload is not None else None
    request = Request(server + path, data=body, headers=headers, method=method)
    try:
        response = urlopen(request, timeout=20)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        data = json.loads(raw) if raw else None
        assert response.code == expected, (path, response.code, expected, data)
        return data


def login(account):
    body = {"email": f"{account}@cafepulse.example", "password": password}
    return call("/auth/signin", payload=body)["accessToken"]


kiosk = login("kiosk")
manager = login("manager")
staff = login("staff")
menu = {product["name"]: product for product in call("/api/product/available", token=kiosk)}
latte = menu["Latte"]["id"]
americano = menu["Americano"]["id"]
large = call(f"/api/admin/productvariant/{latte}", token=kiosk)[0]["id"]


def make_order(product=latte, quantity=2, variants=(large,), customer_phone=phone):
    return {"customerPhone": customer_phone, "totalAmount": 0.01, "item": [
        {"product": product, "quantity": quantity, "variantIds": list(variants), "price": 0.01, "note": "Ujian"}]}


def submit(payload, expected=201, *, key=None, server=api):
    return call("/api/order/create", expected, token=kiosk, payload=payload, key=key, server=server)


assert call("/api/order/allorder", token=staff) == [], "Use a disposable, empty checkout test database"
submit(make_order(), 400)
submit(make_order(), 400, key="invalid-id")
submit({"customerPhone": phone, "item": []}, 400, key=uuid4())
submit({"customerPhone": phone, "item": [None]}, 400, key=uuid4())
submit(make_order(variants=(large, large)), 400, key=uuid4())
submit(make_order(product=americano), 400, key=uuid4())
submit(make_order(quantity=0), 400, key=uuid4())
submit(make_order(quantity=1.5), 400, key=uuid4())
submit(make_order(customer_phone="abc"), 400, key=uuid4())

call(f"/api/product/{latte}/updatestatus", token=manager, method="PUT")
try:
    submit(make_order(), 409, key=uuid4())
finally:
    call(f"/api/product/{latte}/updatestatus", token=manager, method="PUT")
assert call("/api/order/allorder", token=staff) == []
print("Empty, invalid quantity, invalid phone, foreign/duplicate variants and out-of-stock rejected")

key = uuid4()
with ThreadPoolExecutor(max_workers=8) as threads:
    receipts = list(threads.map(lambda index: submit(make_order(), key=key, server=api if index % 2 else other_api), range(8)))
receipt = receipts[0]
assert all(replay == receipt for replay in receipts)
assert receipt["orderSequence"] == "1" and Decimal(str(receipt["totalAmount"])) == Decimal("24.00")
assert receipt["status"] == "ORDER_PENDING"
assert "customer" not in receipt
assert submit(make_order(customer_phone="+60 10-0000201"), key=key) == receipt
submit(make_order(quantity=3), 409, key=key)
call(f"/api/product/{latte}/updatestatus", token=manager, method="PUT")
try:
    assert submit(make_order(), key=key) == receipt
finally:
    call(f"/api/product/{latte}/updatestatus", token=manager, method="PUT")
print("Eight concurrent replays across two servers produced one RM24 order and one queue number")

with ThreadPoolExecutor(max_workers=4) as threads:
    distinct = list(threads.map(lambda index: submit(make_order(), key=uuid4(), server=api if index % 2 else other_api), range(4)))
assert sorted(int(result["orderSequence"]) for result in distinct) == [2, 3, 4, 5]
orders = call("/api/order/allorder", token=staff)
assert len(orders) == 5
assert all(Decimal(str(order["totalAmount"])) == Decimal("24.00") for order in orders)
assert len({order["customer"]["id"] for order in orders}) == 1
print("Concurrent distinct checkouts received sequential numbers and reused the same customer")
