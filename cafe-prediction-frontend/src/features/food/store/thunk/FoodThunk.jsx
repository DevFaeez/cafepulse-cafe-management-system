import { createAsyncThunk } from "@reduxjs/toolkit";
import { api } from "../../../../api/session";

export const fetchAllAvailableFood = createAsyncThunk(
    "food/fetchfood",
    async (_, { rejectWithValue }) => {
        try {
            const response = await api.get("/api/product/available");
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || { message: "The menu couldn't be loaded." });
        }
    },
);

export const fetchProductVariant = createAsyncThunk(
    "food/productvariant",
    async (productId, { rejectWithValue }) => {
        try {
            const response = await api.get(`/api/admin/productvariant/${productId}`);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || { message: "Product options couldn't be loaded." });
        }
    },
);
