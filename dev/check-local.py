import json
import os
from decimal import Decimal
from urllib.error import HTTPError
from urllib.request import Request, urlopen


api = os.environ.get("CAFEPULSE_CHECK_API", "http://localhost:8081")
frontend = os.environ.get("CAFEPULSE_CHECK_FRONTEND", "http://localhost:5174")
origin = os.environ.get("CAFE_CORS_ORIGIN", "http://localhost:5174")
password = os.environ["CAFE_DEV_SEED_PASSWORD"]


def read_json(url, *, headers=None, payload=None):
    body = json.dumps(payload).encode() if payload is not None else None
    request_headers = dict(headers or {})
    if body is not None:
        request_headers["Content-Type"] = "application/json"
    request = Request(url, data=body, headers=request_headers)
    with urlopen(request, timeout=10) as response:
        return json.load(response)


def sign_in(account, role):
    credentials = {"email": f"{account}@cafepulse.example", "password": password}
    session = read_json(api + "/auth/signin", payload=credentials)
    assert session["role"] == role, f"Wrong role for {account}"
    return {"Authorization": f"Bearer {session['accessToken']}"}


if os.environ.get("CAFEPULSE_CHECK_SKIP_FRONTEND") != "1":
    with urlopen(frontend, timeout=10) as response:
        assert response.status == 200 and 'id="root"' in response.read().decode()
    print("Frontend HTTP 200")

sign_in("staff", "ROLE_STAFF")
sign_in("kiosk", "ROLE_CUSTOMER")
manager = sign_in("manager", "ROLE_MANAGER")
print("Sign-in verified for staff, kiosk and manager")

try:
    read_json(api + "/api/product/available")
except HTTPError as response:
    assert response.code in (401, 403), f"Unexpected response without token: {response.code}"
else:
    raise AssertionError("Protected API accepted a request without a token")

preflight = Request(
    api + "/api/product/available",
    method="OPTIONS",
    headers={
        "Origin": origin,
        "Access-Control-Request-Method": "GET",
        "Access-Control-Request-Headers": "authorization",
    },
)
with urlopen(preflight, timeout=10) as response:
    assert response.headers["Access-Control-Allow-Origin"] == origin
print("Menu requires a token and permits configured frontend origin")

menu = read_json(api + "/api/product/available", headers=manager)
assert sorted(item["name"] for item in menu) == ["Americano", "Latte"], menu
latte = next(item for item in menu if item["name"] == "Latte")
assert Decimal(str(latte["price"])) == Decimal("10.00"), latte
variants = read_json(api + f"/api/admin/productvariant/{latte['id']}", headers=manager)
assert len(variants) == 1 and variants[0]["value"] == "Large", variants
assert Decimal(str(variants[0]["extraPrice"])) == Decimal("2.00"), variants
print("Menu contains Americano, Latte RM10 and one Large option (+RM2)")
