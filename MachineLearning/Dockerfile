FROM python:3.11-slim@sha256:9534e5a8e315485d4061ed659af0fd78a284c015f9b73661b41d6bab25604534
WORKDIR /app
ENV PYTHONUNBUFFERED=1 PYTHONDONTWRITEBYTECODE=1
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY interface ./interface
COPY model/best_gradientboosting.pkl ./model/
RUN chmod 0755 /app/model && chmod 0444 /app/model/best_gradientboosting.pkl
USER 10001
EXPOSE 5000
CMD ["waitress-serve", "--listen=0.0.0.0:5000", "interface.prediction_interface:app"]
