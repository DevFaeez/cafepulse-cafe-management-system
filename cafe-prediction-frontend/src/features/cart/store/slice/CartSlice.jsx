import { createSlice } from "@reduxjs/toolkit";

function sameSelection(left, right) {
    return left.productId === right.productId
        && left.description === right.description
        && JSON.stringify(left.options.map(option => option.id)) === JSON.stringify(right.options.map(option => option.id));
}

const cartSlice = createSlice({
    name: "cart",
    initialState: { cartList: [] },
    reducers: {
        addToCart: (state, action) => {
            const item = action.payload;
            const existing = state.cartList.find(line => sameSelection(line, item) && line.quantity < 99);
            if (existing) existing.quantity += 1;
            else state.cartList.push(item);
        },
        replaceCartItem: (state, action) => {
            const index = state.cartList.findIndex(item => item.cartId === action.payload.cartId);
            if (index >= 0) state.cartList[index] = action.payload;
        },
        restoreCart: (state, action) => {
            state.cartList = action.payload;
        },
        clearCart: state => {
            state.cartList = [];
        },
        deleteCartItem: (state, action) => {
            state.cartList = state.cartList.filter(item => item.cartId !== action.payload);
        },
        updateQuantity: (state, action) => {
            const { cartId, quantity } = action.payload;
            const item = state.cartList.find(line => line.cartId === cartId);
            if (item && Number.isInteger(quantity) && quantity >= 1 && quantity <= 99) item.quantity = quantity;
        },
    },
});

export const { addToCart, replaceCartItem, restoreCart, clearCart, deleteCartItem, updateQuantity } = cartSlice.actions;
export const cartReducer = cartSlice.reducer;
