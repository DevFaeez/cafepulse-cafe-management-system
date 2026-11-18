"""Deterministic simulated CafePulse sales and a forecast fitted to that history."""

from datetime import datetime, timedelta
from functools import lru_cache
import json
from math import sqrt
from pathlib import Path
from random import Random
from zoneinfo import ZoneInfo

import numpy as np
from sklearn.linear_model import Ridge

if __package__:
    from .forecast_contract import CATEGORIES, FLAGS, WEATHER, validated
else:
    from forecast_contract import CATEGORIES, FLAGS, WEATHER, validated

ZONE = "Asia/Kuala_Lumpur"
DAYS = 365
SOURCE = "SIMULATION"
MENU = json.loads(Path(__file__).with_name("demo_menu.json").read_text(encoding="utf-8"))
PRODUCTS = tuple({key: value for key, value in product.items() if key != "baseline"} for product in MENU)
PROMO_FLAGS = tuple(flag for flag in FLAGS if flag.startswith("Promo_"))
DISCOUNTS = {f"Promo_{rate}PercentDiscount": rate for rate in (5, 10, 15, 20)}


def local_today():
    return datetime.now(ZoneInfo(ZONE)).date()


def scenario_for(day, rng):
    rainy = day.month in {10, 11, 12}
    afternoon = (15, 23, 19, 24, 13, 6) if rainy else (23, 29, 17, 19, 8, 4)
    scores = [rng.choices(WEATHER, weights, k=1)[0]
              for weights in ((29, 31, 22, 12, 5, 1), afternoon, (7, 37, 27, 18, 8, 3))]
    weekend = day.weekday() >= 5
    traffic = rng.choices(CATEGORIES["Traffic_Level"], (9, 37, 54) if weekend else (25, 56, 19), k=1)[0]
    flags = {flag: 0 for flag in FLAGS}
    flags.update(SchoolHoliday=int(day.month in {6, 12} and day.day >= 12),
                 PublicHoliday=int(rng.random() < .035), Festival=int(rng.random() < .028),
                 BackToSchool=int(day.month in {1, 7} and day.day <= 10),
                 ExamSeason=int(day.month in {5, 10} and day.day >= 12),
                 PayDay=int(day.day in {25, 26, 27}), BigEventNearby=int(rng.random() < .045))
    if rng.random() < (.31 if weekend else .20):
        flags[rng.choice(PROMO_FLAGS)] = 1
    return dict(zip(CATEGORIES, ["Weekend" if weekend else "Weekday", *scores, traffic], strict=True)) | flags


def sales_for(product, features, rng):
    weekend = 1.21 if features["DayType"] == "Weekend" else 1.0
    traffic = {"Low": .72, "Medium": 1.0, "High": 1.25}[features["Traffic_Level"]]
    afternoon = features["Weather_2pm_7pm"]
    weather = {"Heavy Rain": .77, "Thunderstorm": .68, "Light Rain": .92, "Sunny": 1.04}.get(afternoon, 1.0)
    for slot in ("Weather_8am_1pm", "Weather_8pm_11pm"):
        weather *= {"Heavy Rain": .94, "Thunderstorm": .89}.get(features[slot], 1.0)
    if product["category"] == "Cold Drinks":
        weather *= {"Sunny": 1.18, "Heavy Rain": .85, "Thunderstorm": .82}.get(afternoon, 1.0)
    events = (1 + .13 * features["PublicHoliday"] + .08 * features["PayDay"] + .18 * features["BigEventNearby"]
              + .06 * features["SchoolHoliday"] + .10 * features["Festival"] + .04 * features["BackToSchool"]
              - .05 * features["ExamSeason"])
    promo = 1.0 + .15 * sum(features[flag] for flag in PROMO_FLAGS)
    expected = product["baseline"] * weekend * traffic * weather * events * promo
    paid = max(0, round(rng.gauss(expected, 1.15 * sqrt(expected))))
    refunds = [index for index in range(paid) if rng.random() < .014]
    discount = next((DISCOUNTS[flag] for flag in DISCOUNTS if features[flag]), 0)
    price = product["priceCents"]
    paid_price = (price * (100 - discount) + 50) // 100
    free = 0
    if product["unit"] == "CUP":
        if features["Promo_Buy1Free1"]:
            free = paid // 4
        elif features["Promo_Buy2Free1"]:
            free = paid // 6
    discounted = paid * (price - paid_price) + free * paid_price
    returned = sum(paid_price for index in refunds if index >= free)
    return [paid, len(refunds), discounted, returned]


def day_record(day):
    rng = Random(7163 + day.toordinal() * 7919)
    features = scenario_for(day, rng)
    closed = day.weekday() == 0 and day.day <= 7 and day.month in {2, 5, 8, 11}
    quantities = {}
    unavailable = []
    for product in MENU:
        sku = product["sku"]
        out = not closed and rng.random() < .012
        if out:
            unavailable.append(sku)
        quantities[sku] = [0, 0, 0, 0] if closed or out else sales_for(product, features, rng)
    return {"date": day.isoformat(), "features": features, "closed": closed, "unavailable": unavailable, "sales": quantities}


def vector(features):
    return [int(features[key] == choice) for key, choices in CATEGORIES.items() for choice in choices] + [
        features[flag] for flag in FLAGS
    ]


@lru_cache(maxsize=2)
def snapshot(exclusive_end):
    days = [exclusive_end - timedelta(days=offset) for offset in range(DAYS, 0, -1)]
    rows = [day_record(day) for day in days]
    split = int(DAYS * .8)
    design = np.array([vector(row["features"]) for row in rows])
    weekdays = np.array([day.weekday() for day in days])
    training_mask = np.arange(DAYS) < split
    errors, baseline_errors, samples = 0.0, 0.0, 0
    models = []
    for product in PRODUCTS:
        sku = product["sku"]
        available = np.array([not row["closed"] and sku not in row["unavailable"] for row in rows])
        targets = np.array([row["sales"][sku][0] - row["sales"][sku][1] for row in rows])
        train = available & training_mask
        test = available & ~training_mask
        model = Ridge(alpha=3.0).fit(design[train], targets[train])
        estimates = np.maximum(0, np.rint(model.predict(design[test])))
        baseline = [np.mean(targets[train & (weekdays == day)]) for day in weekdays[test]]
        errors += np.abs(estimates - targets[test]).sum()
        baseline_errors += np.abs(np.array(baseline) - targets[test]).sum()
        samples += int(test.sum())
        models.append(Ridge(alpha=3.0).fit(design[available], targets[available]))

    history = {"source": SOURCE, "zone": ZONE, "currency": "MYR", "from": days[0].isoformat(),
               "to": days[-1].isoformat(), "products": PRODUCTS, "rows": rows,
               "salesFields": ["paidQuantity", "refundedQuantity", "discountCents", "refundedCents"],
               "evaluation": {"model": "Ridge regression", "trainingDays": split, "testDays": DAYS - split,
                              "testFrom": days[split].isoformat(), "mae": round(float(errors / samples), 2),
                              "baselineMae": round(float(baseline_errors / samples), 2)}}
    return history, models


def forecast(day, data, exclusive_end):
    features = validated(data)
    if day < exclusive_end:
        raise ValueError("Choose today or a later date for the menu forecast.")
    expected_day = "Weekend" if day.weekday() >= 5 else "Weekday"
    if features["DayType"] != expected_day:
        raise ValueError("The forecast date and day type must match.")
    history, models = snapshot(exclusive_end)
    inputs = [vector(features)]
    return {"date": day.isoformat(), "source": SOURCE, "trainingThrough": history["to"],
            "estimates": [{**product, "quantity": max(0, round(float(model.predict(inputs)[0])))}
                          for product, model in zip(PRODUCTS, models, strict=True)]}
