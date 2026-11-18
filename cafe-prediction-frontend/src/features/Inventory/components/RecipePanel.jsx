import { useEffect, useState } from "react";
import { Alert, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import MenuBookOutlined from "@mui/icons-material/MenuBookOutlined";
import { EmptyState, LoadingState } from "../../../components/WorkspaceUI";
import { api } from "../../../api/session";
import { stockError, units } from "../api/stock";

function RecipeForm({ path, stocks, optionRecipe }) {
    const [lines, setLines] = useState(null);
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [busy, setBusy] = useState(false);
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        api.get(path, { signal: controller.signal })
            .then(({ data }) => { setLines(data.map(line => ({ ...line, key: crypto.randomUUID(), replace: Boolean(line.replacementStockId) }))); setError(""); })
            .catch(failure => { if (!controller.signal.aborted) setError(stockError(failure)); });
        return () => controller.abort();
    }, [path, attempt]);

    function setLine(key, changes) {
        setMessage("");
        setLines(items => items.map(item => item.key === key ? { ...item, ...changes } : item));
    }

    async function save(event) {
        event.preventDefault();
        if (busy) return;
        setBusy(true);
        setError("");
        setMessage("");
        try {
            await api.put(path, { ingredients: lines.map(({ stockId, quantity, replace, replacementStockId }) =>
                replace ? { stockId, replacementStockId } : { stockId, quantity }) });
            setMessage("Recipe saved.");
        } catch (failure) {
            setError(stockError(failure));
        } finally {
            setBusy(false);
        }
    }

    if (lines == null) {
        return error ? <Alert severity="error" action={<Button onClick={() => setAttempt(value => value + 1)}>Try again</Button>}>{error}</Alert>
            : <LoadingState>Loading recipe…</LoadingState>;
    }

    return (
        <Stack component="form" onSubmit={save} spacing={2} className="cp-control-surface">
            {error && <Alert severity="error">{error}</Alert>}
            {message && <Alert severity="success">{message}</Alert>}
            {!lines.length && <EmptyState icon={MenuBookOutlined} compact>This recipe has no ingredients.</EmptyState>}
            {lines.map((line, index) => {
                const unit = stocks.find(item => item.id === line.stockId)?.unit;
                const replacements = stocks.filter(stock => stock.unit === unit && stock.id !== line.stockId);
                const selected = new Set(lines.filter(other => other.key !== line.key).map(other => other.stockId));
                return <Stack key={line.key} direction={{ xs: "column", sm: "row" }} gap={1.5} flexWrap="wrap">
                    {optionRecipe && <TextField select label={`Action ${index + 1}`} value={line.replace ? "replace" : "add"} disabled={busy}
                        onChange={event => setLine(line.key, { replace: event.target.value === "replace" })} sx={{ minWidth: { sm: 140 } }}>
                        <MenuItem value="add">Add ingredient</MenuItem>
                        <MenuItem value="replace">Replace ingredient</MenuItem>
                    </TextField>}
                    <TextField select required label={`Ingredient ${index + 1}`} value={line.stockId} disabled={busy}
                        onChange={event => setLine(line.key, { stockId: event.target.value, replacementStockId: "" })}
                        sx={{ minWidth: { sm: 175 }, flex: 1 }}>
                        {stocks.map(stock => <MenuItem key={stock.id} value={stock.id} disabled={selected.has(stock.id)}>
                            {stock.stockName} ({units[stock.unit]})
                        </MenuItem>)}
                    </TextField>
                    {line.replace ? <TextField select required label={`Replacement ${index + 1}`} value={line.replacementStockId ?? ""}
                        disabled={busy || !unit} onChange={event => setLine(line.key, { replacementStockId: event.target.value })}
                        sx={{ minWidth: { sm: 175 }, flex: 1 }}>
                        {replacements.map(stock => <MenuItem key={stock.id} value={stock.id}>{stock.stockName} ({units[stock.unit]})</MenuItem>)}
                    </TextField> : <TextField type="number" required label={`Quantity ${index + 1}${unit ? ` (${units[unit]})` : ""}`}
                        value={line.quantity ?? ""} disabled={busy} onChange={event => setLine(line.key, { quantity: event.target.value })}
                        slotProps={{ htmlInput: { min: "0.001", max: "999999999.999", step: "0.001" } }} />}
                    <Button disabled={busy} aria-label={`Remove ingredient ${index + 1}`} onClick={() => {
                        setLines(items => items.filter(item => item.key !== line.key)); setMessage("");
                    }}>Remove</Button>
                </Stack>;
            })}
            <Stack direction="row" gap={1} flexWrap="wrap">
                <Button disabled={busy || lines.length >= Math.min(stocks.length, 100)} onClick={() => {
                    setLines(items => [...items, { key: crypto.randomUUID(), stockId: "", quantity: "" }]); setMessage("");
                }}>Add recipe ingredient</Button>
                <Button variant="contained" disabled={busy} type="submit">Save recipe</Button>
            </Stack>
        </Stack>
    );
}

export default function RecipePanel({ products }) {
    const [product, setProduct] = useState("");
    const [variant, setVariant] = useState("base");
    const [variants, setVariants] = useState(null);
    const [stocks, setStocks] = useState([]);
    const [error, setError] = useState("");
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        api.get("/api/stock/all", { signal: controller.signal })
            .then(({ data }) => setStocks(data.filter(stock => stock.unit)))
            .catch(failure => { if (!controller.signal.aborted) setError(stockError(failure)); });
        return () => controller.abort();
    }, [retry]);

    useEffect(() => {
        if (!product) return;
        const controller = new AbortController();
        api.get(`/api/admin/productvariant/${product}`, { signal: controller.signal })
            .then(({ data }) => setVariants({ product, data }))
            .catch(failure => { if (!controller.signal.aborted) setError(stockError(failure)); });
        return () => controller.abort();
    }, [product, retry]);

    const choices = variants?.product === product ? variants.data : [];
    const path = !product ? null : variant === "base"
        ? `/api/admin/inventory-recipes/PRODUCT/${product}` : `/api/admin/inventory-recipes/VARIANT/${variant}`;

    return (
        <Stack spacing={2}>
            <Typography component="h2" variant="h6" className="cp-section-heading">Ingredient recipes</Typography>
            <Typography color="text.secondary" variant="body2">
                Enter the quantity used per unit sold. Options can add ingredients or replace the full amount of an ingredient, including additions from other options.
            </Typography>
            {error && <Alert severity="error" action={<Button onClick={() => { setError(""); setRetry(value => value + 1); }}>Try again</Button>}>{error}</Alert>}
            <Stack direction={{ xs: "column", sm: "row" }} gap={2} flexWrap="wrap" className="cp-control-surface">
                <TextField select label="Recipe product" value={product} onChange={event => {
                    setProduct(event.target.value); setVariant("base"); setError("");
                }} sx={{ minWidth: { sm: 180 }, flex: 1 }}>
                    {products.map(item => <MenuItem key={item.id} value={item.id}>{item.name}</MenuItem>)}
                </TextField>
                {product && <TextField select label="Recipe type" value={variant} onChange={event => setVariant(event.target.value)}
                    sx={{ minWidth: { sm: 180 }, flex: 1 }}>
                    <MenuItem value="base">Base recipe</MenuItem>
                    {choices.map(item => <MenuItem key={item.id} value={item.id}>{item.category.name} · {item.value}</MenuItem>)}
                </TextField>}
            </Stack>
            {!stocks.length && <Typography color="text.secondary">Set up ingredients and stock units in Inventory.</Typography>}
            {path && <RecipeForm key={path} path={path} stocks={stocks} optionRecipe={variant !== "base"} />}
        </Stack>
    );
}
