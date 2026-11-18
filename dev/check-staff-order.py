"""Run against a disposable CafePulse database initialized with dev-seed."""
import json
import os
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4

base = os.environ["CAFEPULSE_CHECK_API"]
second = os.environ.get("CAFEPULSE_CHECK_SECOND_API", base)
password = os.environ["CAFE_DEV_SEED_PASSWORD"]


def call(path, expected=200, *, token=None, payload=None, key=None, method=None, server=base):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["Idempotency-Key"] = str(key)
    body = json.dumps(payload).encode() if payload is not None else None
    request = Request(server + path, data=body, headers=headers, method=method)
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        data = json.loads(raw) if raw else None
        if expected is not None:
            assert response.code == expected, (path, response.code, expected, data)
            return data
        return response.code


def login(account):
    return call("/auth/signin", payload={"email": f"{account}@cafepulse.example", "password": password})["accessToken"]


kiosk, staff, manager = (login(role) for role in ("kiosk", "staff", "manager"))
assert call("/api/order/workbench", token=staff) == [], "Use an empty test database"
assert call("/api/order/history", token=staff) == {"orders": [], "hasNext": False}
call("/api/order/history?page=-1", 400, token=staff)
call("/api/order/workbench", 403, token=kiosk)
call("/api/order/history", 403, token=kiosk)
menu = call("/api/product/available", token=kiosk)
latte = next(item for item in menu if item["name"] == "Latte")
variant = call(f"/api/admin/productvariant/{latte['id']}", token=kiosk)[0]
receipt = call("/api/order/create", 201, token=kiosk, key=uuid4(), payload={
    "customerPhone": "0100000301",
    "item": [{"product": latte["id"], "quantity": 2, "variantIds": [variant["id"]], "note": "Kurang ais"}],
})
order_id = receipt["id"]
order_path = f"/api/order/{order_id}"
payment = {"revision": 0, "method": "QR", "amount": "24.00"}


def active():
    orders = call("/api/order/workbench", token=staff)
    return next(order for order in orders if order["id"] == order_id)


order = active()
assert order["orderSequence"] == receipt["orderSequence"]
assert Decimal(str(order["totalAmount"])) == Decimal("24.00")
assert order["items"] == [{
    "orderNumber": receipt["orderSequence"], "productName": "Latte",
    "variants": [{"category": "Sample Size", "value": "Large"}], "quantity": 2, "note": "Kurang ais",
}]
assert order["payments"] == [] and "customer" not in order
call(order_path + "/payment", 403, token=kiosk, payload=payment)
call(order_path + "/status", 403, token=kiosk, method="PUT", payload={"revision": 0, "status": "ORDER_PREPARING"})
call(order_path + "/status", 409, token=staff, method="PUT", payload={"revision": 0, "status": "ORDER_PREPARING"})
call(order_path + "/status", 409, token=staff, method="PUT", payload={"revision": 0, "status": "ORDER_COMPLETED"})
call(order_path + "/payment", 400, token=staff, payload={**payment, "amount": "23.99"})

with ThreadPoolExecutor(max_workers=2) as threads:
    codes = list(threads.map(lambda server: call(order_path + "/payment", None, token=staff,
                                               payload=payment, server=server), [base, second]))
assert sorted(codes) == [204, 409]
paid = active()
assert len(paid["payments"]) == 1 and paid["payments"][0]["recordedBy"] == "staff@cafepulse.example"
assert paid["payments"][0]["recordedAt"] and paid["status"] == "ORDER_PENDING"
call(order_path + "/payment", 409, token=staff, payload={**payment, "revision": 1})
correction = {"revision": 1, "paymentId": paid["payments"][0]["id"], "reason": "Kaedah tersalah catat"}
call(order_path + "/payment/void", 403, token=staff, payload=correction)
call(order_path + "/payment/void", 204, token=manager, payload=correction)
corrected = active()["payments"][0]
assert corrected["voidedAt"] and corrected["voidedBy"] == "manager@cafepulse.example"
assert corrected["voidReason"] == correction["reason"]
call(order_path + "/payment", 204, token=staff, payload={**payment, "revision": 2, "method": "CASH"})
call(order_path + "/status", 409, token=staff, method="PUT", payload={"revision": 2, "status": "ORDER_PREPARING"})
call(order_path + "/status", 204, token=staff, method="PUT", payload={"revision": 3, "status": "ORDER_PREPARING"})
in_progress = active()
assert in_progress["preparingAt"] and in_progress["preparingBy"] == "staff@cafepulse.example"
assert len(in_progress["payments"]) == 2
call(order_path + "/payment/void", 409, token=manager, payload={
    **correction, "revision": 4, "paymentId": in_progress["payments"][0]["id"],
})
call(order_path + "/status", 204, token=staff, method="PUT", payload={"revision": 4, "status": "ORDER_COMPLETED"})
call(order_path + "/status", 409, token=staff, method="PUT", payload={"revision": 5, "status": "ORDER_PREPARING"})
assert call("/api/order/workbench", token=staff) == []
history = call("/api/order/history", token=staff)["orders"][0]
assert history["id"] == order_id and history["status"] == "ORDER_COMPLETED"
assert history["completedBy"] == "staff@cafepulse.example" and history["completedAt"]
assert history["payments"][0]["method"] == "CASH" and history["payments"][0]["voidedAt"] is None
print("Staff workflow verified: correct RM24, two simultaneous payments yield one record, correction audited, stale transitions rejected, history persisted")
