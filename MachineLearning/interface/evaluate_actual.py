"""Evaluate one or more real sales series from a CafePulse training-data JSON export."""

import argparse
import json
from collections import defaultdict
from datetime import date
from pathlib import Path
from statistics import mean

from .forecast_contract import CATEGORIES, FEATURES, FLAGS, validated

MIN_TRAIN = 21
MIN_TEST = 7


def usable_series(rows):
    series = defaultdict(list)
    seen_dates = set()
    for row in sorted(rows, key=lambda entry: entry["date"]):
        day = date.fromisoformat(row["date"])
        if day in seen_dates:
            raise ValueError(f"Duplicate observation for {day}")
        seen_dates.add(day)
        fields = validated(row["features"])
        if row["closed"]:
            continue
        seen_products = set()
        for product in row["products"]:
            key = product["productId"]
            if key in seen_products:
                raise ValueError(f"Duplicate product {key} on {day}")
            seen_products.add(key)
            if not product["unavailable"]:
                paid = product["paidQuantity"]
                if type(paid) is not int or paid < 0:
                    raise ValueError("Paid quantity must be a nonnegative integer")
                series[key].append((day, fields, paid))
    return dict(series)


def split_chronological(series):
    test_days = max(MIN_TEST, (len(series) + 4) // 5)
    if len(series) - test_days < MIN_TRAIN:
        return None
    return series[:-test_days], series[-test_days:]


def evaluate(series):
    split = split_chronological(series)
    if not split:
        return {"status": "insufficient_data", "usableDays": len(series)}

    import numpy as np
    import pandas as pd
    from sklearn.compose import ColumnTransformer
    from sklearn.ensemble import GradientBoostingRegressor
    from sklearn.metrics import mean_absolute_error
    from sklearn.pipeline import make_pipeline
    from sklearn.preprocessing import OneHotEncoder

    train, test = split
    overall = mean(quantity for _, _, quantity in train)
    weekdays = defaultdict(list)
    for day, _, quantity in train:
        weekdays[day.weekday()].append(quantity)
    baseline = [mean(weekdays[day.weekday()]) if weekdays[day.weekday()] else overall for day, _, _ in test]

    preprocess = ColumnTransformer(
        [("categories", OneHotEncoder(handle_unknown="ignore", sparse_output=False), list(CATEGORIES)),
         ("flags", "passthrough", list(FLAGS))]
    )
    model = make_pipeline(preprocess, GradientBoostingRegressor(random_state=42))
    model.fit(pd.DataFrame([features for _, features, _ in train], columns=FEATURES),
              [quantity for _, _, quantity in train])
    predicted = model.predict(pd.DataFrame([features for _, features, _ in test], columns=FEATURES))
    observed = [quantity for _, _, quantity in test]
    error = lambda values: round(mean_absolute_error(observed, np.rint(np.maximum(0, values))), 3)
    baseline_mae = error(baseline)
    model_mae = error(predicted)
    return {
        "status": "evaluated",
        "trainDays": len(train),
        "testDays": len(test),
        "trainThrough": train[-1][0].isoformat(),
        "testFrom": test[0][0].isoformat(),
        "testThrough": test[-1][0].isoformat(),
        "baselineMaeCups": baseline_mae,
        "modelMaeCups": model_mae,
        "beatsBaseline": model_mae < baseline_mae,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    args = parser.parse_args()
    payload = json.loads(args.file.read_text(encoding="utf-8"))
    rows = usable_series(payload["rows"])
    result = {
        "status": "ready" if any(split_chronological(series) for series in rows.values()) else "insufficient_data",
        "products": [{"productId": id_, **evaluate(series)} for id_, series in sorted(rows.items())],
    }
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
