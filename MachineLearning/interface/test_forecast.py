import unittest
from datetime import date, timedelta

from .evaluate_actual import evaluate, split_chronological, usable_series
from .forecast_contract import DRINKS, FLAGS, validated


SAMPLE = {**dict.fromkeys(FLAGS, 0), "DayType": "Weekday", "Weather_8am_1pm": "Cloudy",
          "Weather_2pm_7pm": "Partly Cloudy", "Weather_8pm_11pm": "Sunny", "Traffic_Level": "Low",
          "Promo_5PercentDiscount": 1, "Promo_NewProductLaunch": 1, "Promo_SocialMediaCampaign": 1}


class ContractTests(unittest.TestCase):
    def test_feature_validation(self):
        self.assertEqual(len(validated(SAMPLE)), 21)
        for bad in ({}, {**SAMPLE, "Traffic_Level": "invalid"}, {**SAMPLE, "Festival": True}):
            with self.assertRaises(ValueError):
                validated(bad)

    def test_sales_conditions_and_chronological_split(self):
        first = date(2026, 1, 1)
        rows = [{
            "date": (first + timedelta(days=i)).isoformat(), "features": SAMPLE,
            "closed": i == 0, "products": [{"productId": 7, "paidQuantity": i, "unavailable": i == 1}],
        } for i in range(37)]
        series = usable_series(reversed(rows))[7]
        self.assertEqual(len(series), 35)
        self.assertEqual(evaluate(series[:27])["status"], "insufficient_data")
        train, test = split_chronological(series)
        self.assertLess(train[-1][0], test[0][0])
        result = evaluate(series)
        self.assertEqual(result["status"], "evaluated")
        self.assertGreaterEqual(result["baselineMaeCups"], 0)
        self.assertGreaterEqual(result["modelMaeCups"], 0)

    def test_saved_model_matches_notebook(self):
        from .prediction_interface import app

        with app.test_client() as client:
            self.assertEqual(client.get("/health").status_code, 200)
            self.assertEqual(client.post("/predict/sales", json={"prediction_data": {}}).status_code, 400)
            prediction = client.post("/predict/sales", json={"prediction_data": SAMPLE})
            self.assertEqual(prediction.status_code, 200)
            self.assertEqual(prediction.json, dict(zip(DRINKS, [265, 390, 425, 292, 369, 348, 173, 249])))


class DemoForecastTests(unittest.TestCase):
    def test_history_covers_whole_menu_with_reconciled_simulated_sales(self):
        from .demo_forecast import snapshot

        end = date(2026, 11, 16)
        history, _ = snapshot(end)
        self.assertEqual(history["source"], "SIMULATION")
        self.assertEqual(len(history["rows"]), 365)
        self.assertEqual(history["to"], "2026-11-15")
        self.assertEqual(len(history["products"]), 52)
        self.assertEqual(len({product["sku"] for product in history["products"]}), 52)
        self.assertEqual(history["evaluation"]["testDays"], 73)
        self.assertLessEqual(history["evaluation"]["mae"], history["evaluation"]["baselineMae"])
        self.assertTrue(any(row["closed"] for row in history["rows"]))
        self.assertTrue(any(row["unavailable"] for row in history["rows"]))
        for i, row in enumerate(history["rows"]):
            self.assertEqual(row["date"], (end - timedelta(days=365 - i)).isoformat())
            self.assertEqual(len(validated(row["features"])), 21)
            self.assertEqual(len(row["sales"]), 52)
            for product in history["products"]:
                paid, refunded, discount, returned = row["sales"][product["sku"]]
                self.assertGreaterEqual(paid, refunded)
                self.assertGreaterEqual(paid * product["priceCents"], discount + returned)
                if row["closed"] or product["sku"] in row["unavailable"]:
                    self.assertEqual((paid, refunded, discount, returned), (0, 0, 0, 0))

    def test_forecasts_change_with_foot_traffic_and_reject_using_training_dates(self):
        from .demo_forecast import forecast

        day = date(2026, 11, 16)
        low = forecast(day, SAMPLE, day)
        high = forecast(day, {**SAMPLE, "Traffic_Level": "High"}, day)
        self.assertEqual(low["source"], "SIMULATION")
        self.assertEqual(len(low["estimates"]), 52)
        self.assertGreater(sum(row["quantity"] for row in high["estimates"]), sum(row["quantity"] for row in low["estimates"]))
        with self.assertRaises(ValueError):
            forecast(day - timedelta(days=1), SAMPLE, day)


if __name__ == "__main__":
    unittest.main()
