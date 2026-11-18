package com.cafemanagement.cafeprediction.forecast;

public enum Drink {
    Drink_IcedCaramelMacchiato("Iced Caramel Macchiato"),
    Drink_MochaFrappe("Mocha Frappe"),
    Drink_StrawberrySmoothie("Strawberry Smoothie"),
    Drink_MangoPassionfruitCooler("Mango Passionfruit Cooler"),
    Drink_MatchaLatte("Matcha Latte"),
    Drink_ChocolateHazelnutShake("Chocolate Hazelnut Shake"),
    Drink_BlueberryLemonadeSparkler("Blueberry Lemonade Sparkler"),
    Drink_AvocadoCoffee("Avocado Coffee");

    private final String label;
    Drink(String label) { this.label = label; }
    public String label() { return label; }
}
