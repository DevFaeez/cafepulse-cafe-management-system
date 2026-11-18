package com.cafemanagement.cafeprediction.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Component
public class ForecastClient {
    private final ObjectMapper json;
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public ForecastClient(ObjectMapper json, @Value("${cafe.forecast.url}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl.replaceFirst("/+$", "");
    }

    public Map<Drink, Integer> predict(ForecastScenario scenario) {
        JsonNode values = post("/predict/sales", Map.of("prediction_data", scenario.features()));
        if (!values.isObject() || values.size() != Drink.values().length) throw unavailable();
        var cups = new EnumMap<Drink, Integer>(Drink.class);
        for (Drink drink : Drink.values()) {
            JsonNode value = values.path(drink.name());
            if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) throw unavailable();
            cups.put(drink, value.intValue());
        }
        return cups;
    }

    public JsonNode demoHistory() {
        JsonNode history = exchange(request("/demo/history").GET().build());
        if (!"SIMULATION".equals(history.path("source").asText())
                || !history.path("rows").isArray() || !history.path("products").isArray()) throw unavailable();
        return history;
    }

    public JsonNode demoPredict(ForecastScenario scenario) {
        JsonNode prediction = post("/demo/predict", Map.of("date", scenario.date().toString(), "prediction_data", scenario.features()));
        if (!"SIMULATION".equals(prediction.path("source").asText()) || !prediction.path("estimates").isArray()) throw unavailable();
        return prediction;
    }

    private JsonNode post(String path, Map<String, ?> payload) {
        try {
            String body = json.writeValueAsString(payload);
            return exchange(request(path).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        } catch (IOException ex) {
            throw unavailable();
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(5));
    }

    private JsonNode exchange(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 400 && request.uri().getPath().endsWith("/demo/predict")) {
                String detail = json.readTree(response.body()).path("description").asText("Enter a valid forecast scenario.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
            }
            if (response.statusCode() != 200) throw unavailable();
            JsonNode values = json.readTree(response.body());
            if (values == null) throw unavailable();
            return values;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException ex) {
            throw unavailable();
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The forecasting service couldn't be reached. Try again.");
    }
}
