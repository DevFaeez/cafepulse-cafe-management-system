import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import { Alert, Box, Button, Chip, Divider, MenuItem, Paper, Stack, TextField, Typography } from "@mui/material";
import Inventory2Outlined from "@mui/icons-material/Inventory2Outlined";
import { EmptyState, LoadingState } from "../../../components/WorkspaceUI";
import { api, currentSession, subscribeToSession } from "../../../api/session";
import { amount, stockError, units } from "../api/stock";
import { StockHistoryDialog, StockMovementDialog, StockSettingsDialog } from "../components/StockDialogs";

const statuses = { STOCK_OUT_OF_STOCK: "Out of stock", STOCK_LOW_STOCK: "Low", STOCK_ENOUGH: "In stock" };

export default function StaffInventoryPage({ manager = false, onChange }) {
    const [stocks, setStocks] = useState(null);
    const [error, setError] = useState("");
    const [filter, setFilter] = useState("all");
    const [query, setQuery] = useState("");
    const [selectedDialog, setDialog] = useState(null);
    const session = useSyncExternalStore(subscribeToSession, currentSession);
    const draft = session?.stockMovementDraft;
    const dialog = selectedDialog ?? (draft && { type: "movement", stock: draft.stock });
    const request = useRef(null);

    const refresh = useCallback(async () => {
        request.current?.abort();
        const controller = new AbortController();
        request.current = controller;
        try {
            const { data } = await api.get("/api/stock/all", { signal: controller.signal });
            if (!controller.signal.aborted) { setStocks(data); setError(""); }
        } catch (failure) {
            if (!controller.signal.aborted) setError(stockError(failure));
        }
    }, []);

    useEffect(() => {
        refresh();
        const timer = setInterval(refresh, 15000);
        return () => { clearInterval(timer); request.current?.abort(); };
    }, [refresh]);

    function changed() {
        refresh();
        onChange?.();
    }

    const low = stocks?.filter(item => item.configured && item.status !== "STOCK_ENOUGH") ?? [];
    const pending = stocks?.filter(item => !item.configured) ?? [];
    const filtered = filter === "low" ? low : filter === "pending" ? pending : stocks ?? [];
    const shown = filtered.filter(stock => stock.stockName.toLowerCase().includes(query.trim().toLowerCase()));
    const close = () => setDialog(null);

    return (
        <Stack spacing={2}>
            <Stack direction="row" flexWrap="wrap" gap={1} alignItems="center" justifyContent="space-between" className="cp-section-heading">
                <Typography component="h2" variant="h6" fontWeight={800}>Ingredient inventory</Typography>
                <Stack direction="row" flexWrap="wrap">
                    <Button onClick={refresh}>Refresh inventory</Button>
                    {manager && <Button variant="contained" onClick={() => setDialog({ type: "settings" })}>Add ingredient</Button>}
                </Stack>
            </Stack>
            {error && <Alert severity="warning">{error}</Alert>}
            {low.length > 0 && <Alert severity="warning" action={<Button color="inherit" onClick={() => setFilter("low")}>
                View alerts
            </Button>}>{low.length} ingredients are at or below their minimum stock level.</Alert>}
            {pending.length > 0 && <Alert severity="info">{pending.length} ingredients need an opening count before they can be used.</Alert>}
            {Boolean(stocks?.length) && <Stack direction={{ xs: "column", sm: "row" }} gap={1.5}>
                <TextField size="small" label="Search ingredients" value={query} onChange={event => setQuery(event.target.value)} />
                <TextField select size="small" label="Stock status" value={filter} onChange={event => setFilter(event.target.value)}
                    sx={{ minWidth: 170 }}>
                    <MenuItem value="all">All ingredients</MenuItem>
                    <MenuItem value="pending">Awaiting count</MenuItem>
                    <MenuItem value="low">Low or out of stock</MenuItem>
                </TextField>
            </Stack>}
            {!stocks && !error ? <LoadingState>Loading inventory…</LoadingState>
                : shown.length ? <Paper variant="outlined" className="cp-directory">
                    <Stack divider={<Divider />}>
                        {shown.map(stock => (
                            <Stack component="article" aria-label={stock.stockName} key={stock.id} direction={{ xs: "column", sm: "row" }}
                                alignItems={{ sm: "center" }} justifyContent="space-between" gap={1.25} className="cp-inventory-row">
                                <Stack direction="row" gap={1.25} alignItems="center" sx={{ minWidth: 0 }}>
                                    <span className="cp-inventory-symbol" aria-hidden="true"><Inventory2Outlined fontSize="small" /></span>
                                    <Box sx={{ minWidth: 0 }}>
                                    <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap">
                                        <Typography fontWeight={700} sx={{ overflowWrap: "anywhere" }}>{stock.stockName}</Typography>
                                        <Chip size="small" color={!stock.configured ? "default" : stock.status === "STOCK_OUT_OF_STOCK" ? "error" : stock.status === "STOCK_LOW_STOCK" ? "warning" : "success"}
                                            label={stock.configured ? statuses[stock.status] : "Not counted"} />
                                    </Stack>
                                    {stock.configured ? <Typography color="text.secondary" variant="body2" sx={{ mt: 0.5 }}>
                                        Balance {amount(stock.quantity)} {units[stock.unit]} · minimum {amount(stock.minimum)} {units[stock.unit]}
                                    </Typography> : <Typography color="text.secondary" variant="body2">
                                        {stock.unit ? `Unit ${units[stock.unit]} · minimum ${amount(stock.minimum)} ${units[stock.unit]}`
                                            : "Waiting for the manager to enter the opening count."}
                                    </Typography>}
                                    </Box>
                                </Stack>
                                <Stack direction="row" flexWrap="wrap" alignItems="center" gap={0.5}>
                                    {manager && <Button onClick={() => setDialog({ type: "settings", stock })}>
                                        {stock.configured ? "Ingredient settings" : "Set opening balance"}
                                    </Button>}
                                    {stock.configured && <>
                                        <Button onClick={() => setDialog({ type: "history", stock })}>Stock history</Button>
                                        <Button variant="outlined" onClick={() => setDialog({ type: "movement", stock })}>Record stock movement</Button>
                                    </>}
                                </Stack>
                            </Stack>
                        ))}
                    </Stack>
                </Paper> : stocks && <EmptyState icon={Inventory2Outlined}>{stocks.length ? "No ingredients match these filters." : "No ingredients recorded."}</EmptyState>}
            {dialog?.type === "settings" && <StockSettingsDialog stock={dialog.stock} close={close} changed={changed} />}
            {dialog?.type === "movement" && <StockMovementDialog stock={dialog.stock} draft={draft} close={close} changed={changed} />}
            {dialog?.type === "history" && <StockHistoryDialog stock={dialog.stock} close={close} />}
        </Stack>
    );
}
