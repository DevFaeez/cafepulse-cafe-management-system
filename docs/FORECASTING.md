# CafePulse forecasting

Sign in as a manager and open **Forecast**. Start the Python service using the [README](../README.md).

## Menu forecasts

**Forecast scenario** defaults to **CafePulse menu**. Select today or a future date, weather for three time slots, foot traffic and any event or promotion flags, then select **Generate forecast**. Results show estimated net units for all 52 sample products, with a category filter.

The menu model uses Ridge regression per product and 21 scenario inputs. A deterministic simulator supplies 365 completed days of sales history, ending yesterday in **Asia/Kuala_Lumpur**. Sample names, prices, weather, events, discounts, refunds, closures and stockouts belong to that simulation.

**Simulated history** supports product and period filters and downloads of the complete CSV or JSON history. There are 18,980 product-day records. Net sales are gross sales less discounts and refunds. These figures are stored separately from operating orders, stock, customer records and sales reports.

## Evaluation

The menu model trains on the earliest 80% of the simulated calendar days and is tested on the newest 20%. With 365 days, this is a 292-day training period and a 73-day test period. Closed days and unavailable products are excluded for each product.

The history screen reports mean absolute error in units per product per day and compares the model with an average for the same weekday. Future forecasts use models fitted on all eligible simulated history.

Results describe performance on synthetic data. Assess predictions against store sales before using them for stock planning.

## Original eight-drink model

Select **Original eight-drink model** to run the bundled Gradient Boosting model. It takes the same 21 scenario inputs and estimates cups for eight original drink types.

Use **Product mapping** to connect those drink types to matching CafePulse products with **Cups** as the sales unit. Each product can be mapped once. Unmapped or unavailable drinks remain identified in the prediction table.

## Store observations and exports

After trading, record the day's actual weather, traffic and event conditions in **Actual observations**. Mark closures and unavailable products. Save mappings before beginning observations, since each observation tracks the configured product IDs.

**Training data** combines operating observations with recorded paid and refunded product quantities. Export the date range as JSON to evaluate the series using the bundled command.

~~~sh
docker run --rm -i cafepulse-forecast:menu-simulation \
  python -m interface.evaluate_actual /dev/stdin < cafepulse-training-data.json
~~~

The evaluator uses paid quantities on open, available days, compares Gradient Boosting with a same-weekday baseline and reports MAE per product. It requires at least 21 eligible training days and seven test days. It reports `insufficient_data` until those requirements are met. Evaluation produces a report while keeping the deployed model in place.
