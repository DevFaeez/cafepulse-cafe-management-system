"""Phase 4 integration checks. Run only on a disposable CafePulse database."""
import json
import os
from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from uuid import uuid4
from secrets import token_urlsafe

api = os.environ["CAFEPULSE_CHECK_API"]
second = os.environ.get("CAFEPULSE_CHECK_SECOND_API", api)
password = os.environ["CAFE_DEV_SEED_PASSWORD"]
phone = "0100000401"
new_account = "test-staff@cafepulse.example"
initial_password = token_urlsafe(24)
new_password = token_urlsafe(24)


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


def login(email, secret=password, status=200):
    return call("/auth/signin", status, body={"email": email, "password": secret})[1]


kiosk, staff, manager = (login(f"{role}@cafepulse.example")["accessToken"] for role in ("kiosk", "staff", "manager"))
menu = call("/api/product/available", token=kiosk)[1]
latte = next(product for product in menu if product["name"] == "Latte")
latte_id = latte["id"]
large_id = call(f"/api/admin/productvariant/{latte_id}", token=kiosk)[1][0]["id"]
assert call("/api/order/allorder", token=staff)[1] == [], "Run against a fresh disposable database"

promo_path = "/api/admin/promotions"
call(promo_path, 403, token=kiosk)
call("/api/admin/accounts", 403, token=staff)
base_promo = {"startDate": "2020-01-01T00:00", "endDate": "2099-01-01T00:00", "active": True, "productId": latte_id}
for code, kind, value in [("TEN", "PERCENT", "10.00"), ("FIVE", "FIXED", "5.00"),
                          ("PAIR", "BOGO", None), ("FREE", "PERCENT", "100"), ("ENDED", "FIXED", "2")]:
    promo = {**base_promo, "code": code, "name": code, "type": kind, "value": value}
    if code == "ENDED":
        promo["endDate"] = "2020-12-31T00:00"
    call(promo_path, 201, token=manager, body=promo)
offers = {offer["code"] for offer in call("/api/order/offers", token=kiosk)[1]}
assert offers == {"TEN", "FIVE", "PAIR", "FREE"}


def basket(promo="", points=0, quantity=2):
    return {"customerPhone": phone, "promoCode": promo, "points": points, "item": [
        {"product": latte_id, "quantity": quantity, "variantIds": [large_id]}]}


def quote(order, expected=200):
    return call("/api/order/quote", expected, token=kiosk, body=order)[1]


def checkout(order, expected=201, *, key=None, server=api):
    return call("/api/order/create", expected, token=kiosk, body=order, key=key or uuid4(), server=server)


def balance(expected):
    entry = call("/api/admin/loyalty/history", token=manager, body={"phone": phone})[1]
    assert entry["available"] == expected, entry
    return entry["movements"]


def close(order, revision, refund=False, expected=204):
    return call(f"/api/order/{order['id']}/close", expected, token=manager,
                body={"revision": revision, "refund": refund, "reason": "Ujian pelarasan"})


def payment(order, revision, *, method="CASH", expected=204):
    call(f"/api/order/{order['id']}/payment", expected, token=staff,
         body={"revision": revision, "method": method, "amount": order["totalAmount"]})


def amount(data, key, expected):
    assert Decimal(str(data[key])) == Decimal(expected), data


amount(quote(basket("TEN")), "totalAmount", "21.60")
amount(quote(basket("FIVE")), "totalAmount", "19.00")
amount(quote(basket("PAIR")), "totalAmount", "14.00")
quote(basket("PAIR", quantity=1), 400)
quote(basket("ENDED"), 400)
quote(basket("TEN", points=10), 400)
original = checkout(basket())[1]
balance(0)
payment(original, 0)
balance(15)
combo = basket("TEN", points=10)
estimate = quote(combo)
amount(estimate, "totalAmount", "20.60")
amount(estimate, "promoDiscount", "2.40")
amount(estimate, "pointsDiscount", "1.00")
checkout({**combo, "expectedTotal": "21.60"}, 409)
with ThreadPoolExecutor(max_workers=2) as pool:
    results = list(pool.map(lambda host: checkout({**combo, "expectedTotal": "20.60"}, None, server=host), (api, second)))
assert sorted(status for status, _ in results) == [201, 400], results
reserved = next(data for status, data in results if status == 201)
balance(5)
close(reserved, 0)
close(reserved, 1, expected=409)
assert balance(15)[0]["reason"] == "REDEMPTION_RESTORED"

key = uuid4()
paid = checkout(combo, key=key)[1]
payment(paid, 0)
balance(20)
close(paid, 1, refund=True)
assert {movement["reason"] for movement in balance(15)} == {
    "EARNED", "REDEEMED", "REDEMPTION_RESTORED", "EARNING_REVERSED",
}
replay = checkout(combo, key=key)[1]
assert replay["id"] == paid["id"] and replay["status"] == "ORDER_REFUNDED"
balance(15)

free = checkout(basket("FREE"))[1]
amount(free, "totalAmount", "0")
payment(free, 0, expected=400)
payment(free, 0, method="NO_CHARGE")
balance(15)

pricing = {"name": latte["name"], "description": latte["description"], "productImage": latte["productImage"],
           "productCategory": latte["productCategory"]["id"], "price": "11.00"}
call(f"/api/product/{latte_id}/update", token=manager, method="PUT", body=pricing)
amount(quote(basket()), "totalAmount", "26.00")
older = next(item for item in call("/api/order/workbench", token=staff)[1] if item["id"] == original["id"])
amount(older, "totalAmount", "24.00")
call(f"/api/product/{latte_id}/update", token=manager, method="PUT", body={**pricing, "price": "10.00"})
other_phone = "0100000402"
debt_order = checkout({**basket(), "customerPhone": other_phone})[1]
payment(debt_order, 0)
another = checkout({**basket(points=10), "customerPhone": other_phone})[1]
paid_order_view = next(item for item in call("/api/order/workbench", token=staff)[1] if item["id"] == debt_order["id"])
call(f"/api/order/{debt_order['id']}/payment/void", 204, token=manager,
     body={"revision": 1, "paymentId": paid_order_view["payments"][0]["id"], "reason": "Ujian mata sudah digunakan"})
debt = call("/api/admin/loyalty/history", token=manager, body={"phone": other_phone})[1]
assert debt["available"] == 0 and debt["balance"] == -10
close(another, 0)
assert call("/api/admin/loyalty/history", token=manager, body={"phone": other_phone})[1]["balance"] == 0
payment(debt_order, 2)
assert call("/api/admin/loyalty/history", token=manager, body={"phone": other_phone})[1]["available"] == 15
print("Promotions, expired/invalid offers, immutable prices, post-payment rewards, concurrency and reversals verified")

call("/auth/signup", 201, token=manager, body={"username": "staff-four", "fullname": "Staf Fasa Empat",
     "email": new_account, "password": initial_password})
accounts = call("/api/admin/accounts", token=manager)[1]
account = next(item for item in accounts if item["email"] == new_account)
assert "password" not in account and account["role"] == "ROLE_STAFF"
staff_path = f"/api/admin/accounts/{account['id']}"
first = login(new_account, initial_password)
call(staff_path + "/password", 204, token=manager, method="PUT", body={"password": new_password})
call("/auth/session", 401, token=first["accessToken"])
call("/auth/refresh", 401, body={"refreshToken": first["refreshToken"]})
login(new_account, initial_password, status=401)
second_session = login(new_account, new_password)
call(staff_path + "/active", 204, token=manager, method="PUT", body={"active": False})
call("/auth/session", 401, token=second_session["accessToken"])
login(new_account, new_password, status=401)
call(staff_path + "/active", 204, token=manager, method="PUT", body={"active": True})
login(new_account, new_password)
call("/auth/session", 401, token=second_session["accessToken"])
print("Account creation, password reset, deactivation, reactivation and permanent revocation of old sessions verified")
