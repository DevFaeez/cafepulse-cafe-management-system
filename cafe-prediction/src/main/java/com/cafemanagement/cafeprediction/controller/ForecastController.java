package com.cafemanagement.cafeprediction.controller;

import com.cafemanagement.cafeprediction.forecast.Drink;
import com.cafemanagement.cafeprediction.forecast.ForecastClient;
import com.cafemanagement.cafeprediction.forecast.ForecastRecords;
import com.cafemanagement.cafeprediction.forecast.ForecastScenario;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/forecast")
@PreAuthorize("hasRole('MANAGER')")
public class ForecastController {
    private final ForecastClient client;
    private final ForecastRecords records;

    public ForecastController(ForecastClient client, ForecastRecords records) {
        this.client = client;
        this.records = records;
    }

    @PostMapping("/predict")
    public Prediction predict(@Valid @RequestBody ForecastScenario scenario) {
        var values = client.predict(scenario);
        return new Prediction(scenario.date(), "SIMULATION",
                records.mappings().stream().map(mapping -> new Estimate(mapping, values.get(mapping.drink()))).toList());
    }

    @GetMapping("/demo/history")
    public JsonNode demoHistory() {
        return client.demoHistory();
    }

    @PostMapping("/demo/predict")
    public JsonNode demoPredict(@Valid @RequestBody ForecastScenario scenario) {
        return client.demoPredict(scenario);
    }

    @GetMapping("/mappings")
    public List<ForecastRecords.Mapping> mappings() {
        return records.mappings();
    }

    @PutMapping("/mappings/{drink}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void map(@PathVariable Drink drink, @Valid @RequestBody MappingInput input) {
        records.map(drink, input.productId());
    }

    @GetMapping("/observations/{date}")
    public ForecastRecords.Observation get(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return records.get(date);
    }

    @PutMapping("/observations")
    public ForecastRecords.Observation record(@Valid @RequestBody ForecastRecords.ObservationInput input, Principal actor) {
        return records.record(input, actor.getName());
    }

    @GetMapping("/training-data")
    public ForecastRecords.TrainingData training(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return records.training(from, to);
    }

    public record MappingInput(@Positive Long productId) {}
    public record Estimate(ForecastRecords.Mapping product, int cups) {}
    public record Prediction(LocalDate date, String source, List<Estimate> estimates) {}
}
