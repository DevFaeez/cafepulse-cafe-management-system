package com.cafemanagement.cafeprediction.forecast;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record ForecastScenario(@NotNull LocalDate date, @NotNull Weather morning, @NotNull Weather afternoon,
                               @NotNull Weather evening, @NotNull Traffic traffic,
                               @NotNull @Size(max = 16) Set<@NotNull Flag> flags) {
    public enum Weather {
        SUNNY("Sunny"), PARTLY_CLOUDY("Partly Cloudy"), CLOUDY("Cloudy"), LIGHT_RAIN("Light Rain"),
        HEAVY_RAIN("Heavy Rain"), THUNDERSTORM("Thunderstorm");
        private final String modelValue;
        Weather(String modelValue) { this.modelValue = modelValue; }
    }

    public enum Traffic {
        LOW("Low"), MEDIUM("Medium"), HIGH("High");
        private final String modelValue;
        Traffic(String modelValue) { this.modelValue = modelValue; }
    }

    public enum Flag {
        SchoolHoliday, PublicHoliday, Festival, BackToSchool, ExamSeason, PayDay, BigEventNearby,
        Promo_5PercentDiscount, Promo_10PercentDiscount, Promo_15PercentDiscount, Promo_20PercentDiscount,
        Promo_Buy1Free1, Promo_Buy2Free1, Promo_NewProductLaunch, Promo_AnniversarySale, Promo_SocialMediaCampaign
    }

    public Map<String, Object> features() {
        var data = new LinkedHashMap<String, Object>();
        data.put("DayType", date.getDayOfWeek().getValue() >= 6 ? "Weekend" : "Weekday");
        data.put("Weather_8am_1pm", morning.modelValue);
        data.put("Weather_2pm_7pm", afternoon.modelValue);
        data.put("Weather_8pm_11pm", evening.modelValue);
        data.put("Traffic_Level", traffic.modelValue);
        for (Flag flag : Flag.values()) data.put(flag.name(), flags.contains(flag) ? 1 : 0);
        return data;
    }
}
