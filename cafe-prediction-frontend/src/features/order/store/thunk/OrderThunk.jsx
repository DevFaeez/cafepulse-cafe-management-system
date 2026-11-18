import { createAsyncThunk } from "@reduxjs/toolkit";
import { api } from "../../../../api/session";

export const createNewOrder = createAsyncThunk(
    "order/createorder",
    async ({ key, order }, { rejectWithValue }) => {
        try {
            const response = await api.post("/api/order/create", order, { headers: { "Idempotency-Key": key } });
            return response.data;
        } catch (error) {
            return rejectWithValue({
                status: error.response?.status,
                message: error.response?.data?.description,
            });
        }
    },
);
