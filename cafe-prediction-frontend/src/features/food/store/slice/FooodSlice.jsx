import { createSlice } from "@reduxjs/toolkit";
import { fetchAllAvailableFood, fetchProductVariant } from "../thunk/FoodThunk";

const foodSlice = createSlice({
    name: "food",
    initialState: {
        availableFoodList: [],
        loading: false,
        error: null,
        productVariant: null,
        variantLoading: false,
        variantRequestId: null,
        variantProductId: null,
        variantError: false
    },
    reducers: {

    },
    extraReducers(builder) {
        builder
            .addCase(fetchAllAvailableFood.pending, (state) => {
                state.error = null;
                state.loading = true;
            })
            .addCase(fetchAllAvailableFood.rejected, (state, action) => {
                state.loading = false;
                state.error = action.payload;
            })
            .addCase(fetchAllAvailableFood.fulfilled, (state, action) => {
                state.availableFoodList = action.payload;
                state.loading = false;
            })

            .addCase(fetchProductVariant.pending, (state, action) => {
                state.variantRequestId = action.meta.requestId;
                state.variantProductId = action.meta.arg;
                state.variantLoading = true;
                state.variantError = false;
                state.productVariant = null;
            })
            .addCase(fetchProductVariant.rejected, (state, action) => {
                if (state.variantRequestId !== action.meta.requestId) return;
                state.variantLoading = false;
                state.variantError = true;
            })
            .addCase(fetchProductVariant.fulfilled, (state, action) => {
                if (state.variantRequestId !== action.meta.requestId) return;
                state.productVariant = action.payload;
                state.variantLoading = false;
            });
    }
});

export const foodReducer = foodSlice.reducer;
