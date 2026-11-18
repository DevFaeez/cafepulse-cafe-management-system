"""Forecast integration checks. Use a disposable MySQL with dev-seed."""
import json
import os
from datetime import datetime, timedelta
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from uuid import uuid4
from zoneinfo import ZoneInfo

base = os.environ["CAFEPULSE_CHECK_API"]
offline = os.environ["CAFEPULSE_CHECK_SECOND_API"]
password = os.environ["CAFE_DEV_SEED_PASSWORD"]
today = datetime.now(ZoneInfo("Asia/Kuala_Lumpur")).date()
root = "/api/admin/forecast"


def call(path, expected=200, *, token=None, body=None, method=None, key=None, server=base):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if key:
        headers["Idempotency-Key"] = str(key)
    request = Request(server + path, headers=headers, data=None if body is None else json.dumps(body).encode(), method=method)
    try:
        response = urlopen(request, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        data = json.loads(raw) if raw else None
        assert response.code == expected, (path, response.code, expected, data)
        return data


def login(role):
    data = {"email": f"{role}@cafepulse.example", "password": password}
    return call("/auth/signin", body=data)["accessToken"]


manager, staff, kiosk = (login(role) for role in ("manager", "staff", "kiosk"))
sample = {"date": "2026-11-13", "morning": "CLOUDY", "afternoon": "PARTLY_CLOUDY", "evening": "SUNNY",
          "traffic": "LOW", "flags": ["Promo_5PercentDiscount", "Promo_NewProductLaunch", "Promo_SocialMediaCampaign"]}
predict = root + "/predict"
call(predict, 403, token=staff, body=sample)
call(predict, 403, token=kiosk, body=sample)
call(predict, 400, token=manager, body={**sample, "evening": "INVALID"})
call(predict, 400, token=manager, body={**sample, "flags": None})
result = call(predict, token=manager, body=sample)
assert result["source"] == "SIMULATION"
assert [entry["cups"] for entry in result["estimates"]] == [265, 390, 425, 292, 369, 348, 173, 249]
assert all(entry["product"]["productId"] is None for entry in result["estimates"])
call(predict, 503, token=manager, body=sample, server=offline)
print("Forecast matches all eight notebook estimates; input validation, role checks and unavailable-provider response verified")

category = call("/api/admin/productcategory", token=manager)[0]["id"]
product = call("/api/product/add", 201, token=manager, body={
    "name": "Matcha Latte", "description": "Produk ujian untuk ramalan", "productImage": "/vite.svg",
    "price": 9, "productCategory": category, "salesUnit": "CUP",
})
product_id = product["id"]
target = root + "/mappings/Drink_MatchaLatte"
call(target, 204, token=manager, body={"productId": product_id}, method="PUT")
call(root + "/mappings/Drink_AvocadoCoffee", 409, token=manager, body={"productId": product_id}, method="PUT")
call(f"/api/product/{product_id}/sale-status", token=manager, body={"status": "PRODUCT_OUT_OF_STOCK"}, method="PUT")
matcha = call(predict, token=manager, body=sample)["estimates"][4]
assert matcha["cups"] == 369 and matcha["product"]["productId"] == product_id and not matcha["product"]["available"]
call(f"/api/product/{product_id}/sale-status", token=manager, body={"status": "PRODUCT_AVAILABLE"}, method="PUT")
receipt = call("/api/order/create", 201, token=kiosk, key=uuid4(), body={"customerPhone": "0100000701",
               "item": [{"product": product_id, "quantity": 2}]}, server=offline)
call(f"/api/order/{receipt['id']}/payment", 204, token=staff,
     body={"revision": 0, "method": "CASH", "amount": 18}, server=offline)
print("A mapped inactive product is identified without replacing its estimate; checkout and payment work without the provider")

observations = root + "/observations"
call(observations + "/2000-01-01", 404, token=manager)
call(observations, 400, token=manager, method="PUT", body={
    "scenario": {**sample, "date": (today + timedelta(days=1)).isoformat()}, "closed": False, "unavailableProductIds": [],
})
for offset, closed, unavailable in [(2, True, []), (1, False, [product_id]), (0, False, [])]:
    day = (today - timedelta(days=offset)).isoformat()
    new = call(observations, token=manager, method="PUT", body={
        "scenario": {**sample, "date": day}, "closed": closed, "unavailableProductIds": unavailable,
    })
    assert new["closed"] == closed
    assert call(observations + "/" + day, token=manager)["products"] == new["products"]

call(target, 204, token=manager, body={"productId": None}, method="PUT")
data = call(root + "/training-data?" + urlencode({"from": today - timedelta(days=2), "to": today}), token=manager)
assert data["zone"] == "Asia/Kuala_Lumpur" and len(data["rows"]) == 3
assert data["rows"][0]["closed"] and data["rows"][1]["products"][0]["unavailable"]
assert data["rows"][2]["products"] == [{"productId": product_id, "unavailable": False, "paidQuantity": 2, "refundedQuantity": 0}]
assert len(data["rows"][2]["features"]) == 21
call(target, 204, token=manager, body={"productId": product_id}, method="PUT")
print("Observation retrieval, closed/out-of-stock flags and joining actual payments by stable product ID verified")
