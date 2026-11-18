import pandas as pd
import datetime
import random

# Columns as agreed
promos = [
    "Promo_5PercentDiscount",
    "Promo_10PercentDiscount",
    "Promo_15PercentDiscount",
    "Promo_20PercentDiscount",
    "Promo_Buy1Free1",
    "Promo_Buy2Free1",
    "Promo_NewProductLaunch",
    "Promo_AnniversarySale",
    "Promo_SocialMediaCampaign"
]


drinks = [
        "Drink_IcedCaramelMacchiato",
        "Drink_MochaFrappe",
        "Drink_StrawberrySmoothie",
        "Drink_MangoPassionfruitCooler",
        "Drink_MatchaLatte",
        "Drink_ChocolateHazelnutShake",
        "Drink_BlueberryLemonadeSparkler",
        "Drink_AvocadoCoffee"
    ]

cols = [
    "Date", "Day", "DayType",
    "Weather_8am_1pm", "Weather_2pm_7pm", "Weather_8pm_11pm",
    "Traffic_Level",
    "SchoolHoliday", "PublicHoliday", "Festival", "BackToSchool", "ExamSeason", "PayDay", "BigEventNearby"
] + promos + drinks + [
    "Cup_Total"
]

# Helper lists
weathers = ["Sunny", "Partly Cloudy", "Cloudy", "Light Rain", "Heavy Rain"]
traffic_levels = ["Low", "Medium", "High"]

# Dates
start = datetime.date(2015, 1, 1)
rows = []

# Define some fixed events for logic
public_holidays = {7}
festivals = {5}
anniversary = {10}
new_launch = {15}
back_to_school_days = set(range(1,11))
exam_days = {18,19,20,21}
payday_days = set(list(range(25,31)) + [1,2,3])
big_event_days = {12, 20, 27}

# Popularity weights for A-H (sum to something)
weights = [12,18,20,14,16,15,8,11]  # relative popularity

for i in range(2500):
    date = start + datetime.timedelta(days=i)
    day_name = date.strftime("%A")
    daytype = "Weekend" if day_name in ["Saturday","Sunday"] else "Weekday"

    # Weather logic: create plausible sequences
    if 3 <= i <= 6 or 16 <= i <= 18:
        w1 = random.choice(["Cloudy","Light Rain","Heavy Rain","Thunderstorm"])
        w2 = random.choice(["Light Rain","Heavy Rain","Thunderstorm","Cloudy"])
        w3 = random.choice(["Light Rain","Cloudy","Partly Cloudy"])
    else:
        w1 = random.choice(["Sunny","Partly Cloudy","Cloudy"])
        if "Cloud" in w1 and random.random() < 0.25:
            w2 = random.choice(["Light Rain","Cloudy"])
        else:
            w2 = random.choice(["Sunny","Partly Cloudy","Cloudy"])
        w3 = random.choice(["Partly Cloudy","Cloudy","Light Rain"])

    # Traffic logic
    if daytype == "Weekend":
        traffic = random.choices(["Medium","High"], weights=[30,70])[0]
    else:
        if date.day in payday_days:
            traffic = "High"
        elif date.day in back_to_school_days:
            traffic = random.choices(["Low","Medium"], weights=[60,40])[0]
        else:
            traffic = random.choices(["Low","Medium","High"], weights=[40,45,15])[0]

    # External events binaries
    school_holiday = 1 if date.day in {1,2,3} else 0
    public_holiday = 1 if date.day in public_holidays else 0
    festival = 1 if date.day in festivals else 0
    backtoschool = 1 if date.day in back_to_school_days else 0
    examseason = 1 if date.day in exam_days else 0
    payday = 1 if date.day in payday_days else 0
    big_event = 1 if date.day in big_event_days else 0

    # Internal promos: place logically
    promo_vals = {p: 0 for p in promos}
    if date.day in anniversary:
        promo_vals["Promo_AnniversarySale"] = 1
        promo_vals["Promo_SocialMediaCampaign"] = 1
    if date.day in new_launch:
        promo_vals["Promo_NewProductLaunch"] = 1
        promo_vals["Promo_SocialMediaCampaign"] = 1
        promo_vals["Promo_5PercentDiscount"] = 1
    if daytype == "Weekend" and random.random() < 0.25:
        promo_vals["Promo_SocialMediaCampaign"] = 1
    if sum(promo_vals.values()) == 0:
        pick_count = random.choices([0,1,2], weights=[30,50,20])[0]
        available = ["Promo_5PercentDiscount","Promo_10PercentDiscount","Promo_Buy1Free1","Promo_SocialMediaCampaign"]
        chosen = random.sample(available, pick_count)
        for c in chosen:
            promo_vals[c] = 1
    if sum(promo_vals.values()) > 2:
        # turn off extras but keep anniversary/new launch/socialmedia
        for k in list(promo_vals.keys()):
            if promo_vals[k]==1 and k not in ["Promo_AnniversarySale","Promo_NewProductLaunch","Promo_SocialMediaCampaign"]:
                promo_vals[k]=0
            if sum(promo_vals.values()) <= 2:
                break

    # Sales total logic
    base = random.randint(1200, 3200)
    if traffic == "High":
        base = int(base * 1.2)
    elif traffic == "Low":
        base = int(base * 0.85)
    if public_holiday:
        base = int(base * 1.25)
    if festival:
        base = int(base * 1.3)
    if big_event:
        base = int(base * 1.25)
    if backtoschool:
        base = int(base * 0.9)
    if examseason:
        base = int(base * 0.85)
    if payday:
        base = int(base * 1.18)
    if promo_vals["Promo_5PercentDiscount"]:
        base = int(base * 1.05)
    if promo_vals["Promo_10PercentDiscount"]:
        base = int(base * 1.10)
    if promo_vals["Promo_15PercentDiscount"]:
        base = int(base * 1.15)
    if promo_vals["Promo_20PercentDiscount"]:
        base = int(base * 1.20)
    if promo_vals["Promo_Buy1Free1"]:
        base = int(base * 1.30)
    if promo_vals["Promo_Buy2Free1"]:
        base = int(base * 1.40)
    if promo_vals["Promo_NewProductLaunch"]:
        base = int(base * 1.22)
    if promo_vals["Promo_AnniversarySale"]:
        base = int(base * 1.25)
    if promo_vals["Promo_SocialMediaCampaign"]:
        base = int(base * 1.12)
    total_sales = min(max(base, 1000), 5000)

    # Distribute across water types using weights with noise
    w = weights.copy()
    w = [max(1, int(x * random.uniform(0.9,1.15))) for x in w]
    ssum = sum(w)
    sales = [int(total_sales * (x/ssum)) for x in w]
    diff = total_sales - sum(sales)
    idx = 0
    while diff > 0:
        sales[idx % 8] += 1
        idx += 1
        diff -= 1
    while diff < 0:
        sales[idx % 8] = max(0, sales[idx % 8] - 1)
        idx += 1
        diff += 1

    row = {
        "Date": date.isoformat(),
        "Day": day_name,
        "DayType": daytype,
        "Weather_8am_1pm": w1,
        "Weather_2pm_7pm": w2,
        "Weather_8pm_11pm": w3,
        "Traffic_Level": traffic,
        "SchoolHoliday": school_holiday,
        "PublicHoliday": public_holiday,
        "Festival": festival,
        "BackToSchool": backtoschool,
        "ExamSeason": examseason,
        "PayDay": payday,
        "BigEventNearby": big_event,
    }

    for p in promos:
        row[p] = promo_vals[p]
    for idx_w, label in enumerate(drinks):
        row[label] = sales[idx_w]
    row["Cup_Total"] = total_sales
    rows.append(row)

df = pd.DataFrame(rows, columns=cols)
# Save to Excel
file_path = "./dataset/sales2.xlsx"
df.to_excel(file_path, index=False)

file_path
