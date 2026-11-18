"""Feature and target order used by the saved 2025 model."""

WEATHER = ("Sunny", "Partly Cloudy", "Cloudy", "Light Rain", "Heavy Rain", "Thunderstorm")
CATEGORIES = {
    "DayType": ("Weekday", "Weekend"),
    "Weather_8am_1pm": WEATHER,
    "Weather_2pm_7pm": WEATHER,
    "Weather_8pm_11pm": WEATHER,
    "Traffic_Level": ("Low", "Medium", "High"),
}
FLAGS = (
    "SchoolHoliday", "PublicHoliday", "Festival", "BackToSchool", "ExamSeason", "PayDay", "BigEventNearby",
    "Promo_5PercentDiscount", "Promo_10PercentDiscount", "Promo_15PercentDiscount",
    "Promo_20PercentDiscount", "Promo_Buy1Free1", "Promo_Buy2Free1", "Promo_NewProductLaunch",
    "Promo_AnniversarySale", "Promo_SocialMediaCampaign",
)
FEATURES = (*CATEGORIES, *FLAGS)
DRINKS = (
    "Drink_IcedCaramelMacchiato", "Drink_MochaFrappe", "Drink_StrawberrySmoothie", "Drink_MangoPassionfruitCooler",
    "Drink_MatchaLatte", "Drink_ChocolateHazelnutShake", "Drink_BlueberryLemonadeSparkler", "Drink_AvocadoCoffee",
)


def validated(data):
    if not isinstance(data, dict) or set(data) != set(FEATURES):
        raise ValueError("Expected the 21 named model inputs")
    for name, choices in CATEGORIES.items():
        if data[name] not in choices:
            raise ValueError(f"Invalid category for {name}")
    for name in FLAGS:
        if type(data[name]) is not int or data[name] not in (0, 1):
            raise ValueError(f"Expected 0 or 1 for {name}")
    return {key: data[key] for key in FEATURES}
