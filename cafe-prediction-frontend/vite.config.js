import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const workspaceIcons = [
    "Add", "AddRounded", "AddShoppingCartOutlined", "ArrowForwardRounded",
    "ArrowOutwardRounded", "AssessmentOutlined", "BadgeOutlined", "BakeryDiningOutlined", "BarChartRounded", "CategoryOutlined",
    "CheckRounded", "CoffeeOutlined", "DeleteOutline", "DownloadRounded", "HistoryRounded",
    "HourglassEmptyRounded", "IcecreamOutlined", "InsightsRounded", "Inventory2Outlined", "LocalCafeOutlined",
    "LocalDrinkOutlined", "LocalPizzaOutlined", "LogoutRounded",
    "LoyaltyOutlined", "LunchDiningOutlined", "MenuBookOutlined", "PaymentsOutlined", "ReceiptLongOutlined", "RefreshRounded", "Remove",
    "RestaurantOutlined", "SearchRounded", "SellOutlined", "ShoppingBagOutlined", "TuneRounded",
];

export default defineConfig({
    plugins: [react(), tailwindcss()],
    optimizeDeps: {
        // The workspace modules load lazily after sign-in. Warm their deep imports on startup.
        include: workspaceIcons.map(icon => `@mui/icons-material/${icon}`),
    },
});
