import { configureStore } from "@reduxjs/toolkit";
import { foodReducer } from "./features/food/store/slice/FooodSlice";
import { cartReducer } from "./features/cart/store/slice/CartSlice";

export function createStore() {
    return configureStore({
        reducer: {
            food: foodReducer,
            cart: cartReducer,
        },
    });
}
