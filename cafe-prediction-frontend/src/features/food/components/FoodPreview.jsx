import { createElement, useId, useState } from "react";
import { ButtonBase, Typography } from "@mui/material";
import AddRounded from "@mui/icons-material/AddRounded";
import LocalCafeOutlined from "@mui/icons-material/LocalCafeOutlined";
import RestaurantOutlined from "@mui/icons-material/RestaurantOutlined";
import BakeryDiningOutlined from "@mui/icons-material/BakeryDiningOutlined";
import IcecreamOutlined from "@mui/icons-material/IcecreamOutlined";
import LocalDrinkOutlined from "@mui/icons-material/LocalDrinkOutlined";
import LocalPizzaOutlined from "@mui/icons-material/LocalPizzaOutlined";
import LunchDiningOutlined from "@mui/icons-material/LunchDiningOutlined";

const placeholders = new Set(["/vite.svg", "/media/cafepulse.png"]);
const sampleDrinkPhotos = new Map([
    ["Americano", "/media/menu-americano.webp"],
    ["Latte", "/media/cafe-counter.webp"],
]);
const categoryIcons = new Map([
    ["Breakfast & Brunch", BakeryDiningOutlined],
    ["Pastries & Bakery", BakeryDiningOutlined],
    ["Desserts", IcecreamOutlined],
    ["Cold Drinks", LocalDrinkOutlined],
    ["Pasta & Pizza", LocalPizzaOutlined],
    ["Snacks & Sides", LunchDiningOutlined],
]);

export default function FoodPreview({ food, onSelect, disabled }) {
    const [failedImage, setFailedImage] = useState(null);
    const descriptionId = useId();
    const configuredImage = food.productImage?.trim();
    const samplePhoto = food.productCategory?.name === "Sample Drinks" && sampleDrinkPhotos.get(food.name);
    const source = configuredImage && !placeholders.has(configuredImage) ? configuredImage : samplePhoto;
    const hasImage = source && failedImage !== source;
    const drink = food.salesUnit === "CUP" || /minum|kopi|coffee/i.test(food.productCategory?.name ?? "");
    const fallbackIcon = categoryIcons.get(food.productCategory?.name) ?? (drink ? LocalCafeOutlined : RestaurantOutlined);

    return (
        <ButtonBase className="cp-product" onClick={() => onSelect(food)} disabled={disabled} focusRipple
            aria-label={`${food.name} · RM ${Number(food.price).toFixed(2)}`} aria-describedby={food.description ? descriptionId : undefined}>
            <span className={`cp-product-visual${hasImage ? " cp-product-visual--photo" : ""}`} aria-hidden="true">
                {hasImage ? <img src={source} alt="" loading="lazy" onError={() => setFailedImage(source)} />
                    : createElement(fallbackIcon)}
            </span>
            <span className="cp-product-copy">
                <Typography component="span" className="cp-product-name">{food.name}</Typography>
                {food.description && <Typography component="span" className="cp-product-description" color="text.secondary" variant="body2" id={descriptionId}>{food.description}</Typography>}
                <span className="cp-product-bottom">
                    <Typography component="span" className="cp-product-price">RM {Number(food.price).toFixed(2)}</Typography>
                    <span className="cp-product-add" aria-hidden="true"><AddRounded fontSize="small" /></span>
                </span>
            </span>
        </ButtonBase>
    );
}
