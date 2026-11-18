from datetime import date
from math import isfinite
from os import getenv
from pathlib import Path

import joblib
import pandas as pd
from flask import Flask, jsonify, request

if __package__:
    from .demo_forecast import forecast, local_today, snapshot
    from .forecast_contract import DRINKS, FEATURES, validated
else:
    from demo_forecast import forecast, local_today, snapshot
    from forecast_contract import DRINKS, FEATURES, validated

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 8192
model_path = Path(__file__).resolve().parents[1] / "model" / "best_gradientboosting.pkl"
model = joblib.load(model_path)

if tuple(model.feature_names_in_) != FEATURES or len(model.named_steps["regressor"].estimators_) != len(DRINKS):
    raise RuntimeError("Saved model does not match the forecast contract")


@app.get("/health")
def health():
    return {"status": "ok", "source": "simulation"}


@app.post("/predict/sales")
def predict():
    payload = request.get_json(silent=True)
    try:
        values = validated(payload.get("prediction_data") if isinstance(payload, dict) else None)
    except ValueError as error:
        return jsonify(description=str(error)), 400
    output = model.predict(pd.DataFrame([values], columns=FEATURES))[0]
    if len(output) != len(DRINKS) or not all(isfinite(value) for value in output):
        return jsonify(description="Model returned invalid estimates"), 503
    return dict(zip(DRINKS, (max(0, round(value)) for value in output), strict=True))



@app.get("/demo/history")
def demo_history():
    return snapshot(local_today())[0]


@app.post("/demo/predict")
def demo_predict():
    payload = request.get_json(silent=True)
    try:
        if not isinstance(payload, dict) or not isinstance(payload.get("date"), str):
            raise ValueError("Enter a date for the demo forecast.")
        return forecast(date.fromisoformat(payload["date"]), payload.get("prediction_data"), local_today())
    except ValueError as error:
        return jsonify(description=str(error)), 400


if __name__ == "__main__":
    from waitress import serve

    serve(app, host="127.0.0.1", port=int(getenv("CAFE_FORECAST_PORT", "5000")))
