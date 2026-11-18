import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen

base_url = os.environ.get("CAFEPULSE_CHECK_API", "http://localhost:8081")
password = os.environ["CAFE_DEV_SEED_PASSWORD"]


def call(path, status=200, *, method=None, token=None, payload=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(payload).encode() if payload is not None else None
    request = Request(base_url + path, data=body, method=method, headers=headers)
    try:
        response = urlopen(request, timeout=10)
    except HTTPError as error:
        response = error
    with response:
        assert response.code in (status if isinstance(status, tuple) else (status,)), f"{request.get_method()} {path} returned {response.code}, expected {status}"
        raw = response.read()
        return json.loads(raw) if raw else None


def login(name, role):
    session = call("/auth/signin", payload={"email": f"{name}@cafepulse.example", "password": password})
    assert session["role"] == role
    return session


call("/auth/signin", 400, payload={"email": "invalid", "password": ""})
call("/auth/signin", 401, payload={"email": "manager@cafepulse.example", "password": "wrong-password"})
call("/api/product/available", 401)
call("/auth/signup", 401, payload={"role": "ROLE_MANAGER"})

kiosk = login("kiosk", "ROLE_CUSTOMER")
staff = login("staff", "ROLE_STAFF")
manager = login("manager", "ROLE_MANAGER")
try:
    token = kiosk["accessToken"]
    menu = call("/api/product/available", token=token)
    latte = next(product for product in menu if product["name"] == "Latte")
    americano = next(product for product in menu if product["name"] == "Americano")
    variants = call(f"/api/admin/productvariant/{latte['id']}", token=token)
    assert len(variants) == 1 and variants[0]["value"] == "Large"
    assert call(f"/api/admin/productvariant/{americano['id']}", token=token) == []
    call(f"/api/admin/productvariant/get/{variants[0]['id']}", token=token)
    for path in ("/api/order/allorder", "/api/order/pendingpreparing/order", "/api/order/1/item", "/api/stock/all"):
        call(path, 403, token=token)
    for path in ("/api/product/add", "/api/admin/productcategory/new", "/api/admin/productvariant/create",
                 "/api/customer/find-by-phone", "/auth/signup", "/auth/kiosk"):
        call(path, 403, token=token, payload={"role": "ROLE_MANAGER"})
    print("Kiosk can read menu and variants; privileged APIs reject it")

    call("/api/order/allorder", token=staff["accessToken"])
    call("/api/order/pendingpreparing/order", token=staff["accessToken"])
    call("/api/stock/all", (200, 404), token=staff["accessToken"])
    call("/api/product/add", 403, token=staff["accessToken"], payload={})
    call("/api/order/create", 403, token=staff["accessToken"], payload={})
    call("/api/product", token=manager["accessToken"])
    call("/auth/signup", 400, token=manager["accessToken"], payload={})
    print("Staff and manager permissions are enforced")

    call("/auth/session", 401, token=kiosk["refreshToken"])
    call("/auth/refresh", 401, payload={"refreshToken": token})
    refreshed = call("/auth/refresh", payload={"refreshToken": kiosk["refreshToken"]})["currentAccessToken"]
    assert call("/auth/session", token=refreshed)["role"] == "ROLE_CUSTOMER"
    call("/auth/logout", 204, payload={"refreshToken": kiosk["refreshToken"]})
    call("/auth/session", 401, token=refreshed)
    call("/auth/session", 401, token=token)
    call("/auth/refresh", 401, payload={"refreshToken": kiosk["refreshToken"]})
    print("Refresh, token type separation and server-side logout verified")
finally:
    for session in (kiosk, staff, manager):
        call("/auth/logout", 204, payload={"refreshToken": session["refreshToken"]})
