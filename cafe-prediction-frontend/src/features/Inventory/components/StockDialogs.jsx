import { useEffect, useRef, useState } from "react";
import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, MenuItem, Stack,
    TextField, Typography } from "@mui/material";
import { EmptyState, LoadingState } from "../../../components/WorkspaceUI";
import { api, currentSession, setStockMovementDraft } from "../../../api/session";
import { amount, kinds, stockError, units } from "../api/stock";

const numberProps = { min: "0", max: "999999999.999", step: "0.001" };
const dates = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Kuala_Lumpur",
});

export function StockSettingsDialog({ stock, close, changed }) {
    const [name, setName] = useState(stock?.stockName ?? "");
    const [unit, setUnit] = useState(stock?.unit ?? "L");
    const [opening, setOpening] = useState("");
    const [minimum, setMinimum] = useState(stock?.minimum ?? "");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const saving = useRef(false);
    const title = !stock ? "Add ingredient" : stock.configured ? "Ingredient settings" : "Opening count";

    async function save(event) {
        event.preventDefault();
        if (saving.current) return;
        saving.current = true;
        setBusy(true);
        setError("");
        try {
            const data = { stockName: name, unit, minimum, openingQuantity: stock?.configured || opening === "" ? null : opening };
            if (stock) await api.put(`/api/stock/${stock.id}/settings`, data);
            else await api.post("/api/stock/create", data);
            changed();
            close();
        } catch (failure) {
            setError(stockError(failure));
        } finally {
            saving.current = false;
            setBusy(false);
        }
    }

    return (
        <Dialog open onClose={busy ? undefined : close} fullWidth maxWidth="xs" aria-labelledby="stock-settings-title">
            <Box component="form" onSubmit={save}>
                <DialogTitle id="stock-settings-title">{title}</DialogTitle>
                <DialogContent dividers>
                    <Stack spacing={2} sx={{ pt: 0.5 }}>
                        {error && <Alert severity="error">{error}</Alert>}
                        <TextField label="Ingredient name" required value={name} onChange={event => setName(event.target.value)}
                            slotProps={{ htmlInput: { maxLength: 150 } }} />
                        <TextField select label="Unit" value={unit} disabled={Boolean(stock?.unit)} onChange={event => setUnit(event.target.value)}>
                            {Object.entries(units).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}
                        </TextField>
                        {!stock?.configured && <TextField label="Opening balance" type="number" required={Boolean(stock)} value={opening}
                            helperText={stock ? "Enter the quantity from a physical count." : "Leave blank if the opening count is pending."}
                            onChange={event => setOpening(event.target.value)} slotProps={{ htmlInput: numberProps }} />}
                        <TextField label="Minimum level" type="number" required value={minimum}
                            onChange={event => setMinimum(event.target.value)} slotProps={{ htmlInput: numberProps }} />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={close} disabled={busy}>Cancel</Button>
                    <Button type="submit" variant="contained" disabled={busy}>Save ingredient</Button>
                </DialogActions>
            </Box>
        </Dialog>
    );
}

const manual = ["RECEIPT", "USAGE", "ADJUSTMENT", "WASTE"];

export function StockMovementDialog({ stock, draft, close, changed }) {
    const [type, setType] = useState(draft?.body.type ?? "RECEIPT");
    const [quantity, setQuantity] = useState(draft?.body.quantity ?? "");
    const [reason, setReason] = useState(draft?.body.reason ?? "");
    const [pending, setPending] = useState(draft ?? null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const saving = useRef(false);
    const uncertain = Boolean(pending);

    async function save(event) {
        event.preventDefault();
        if (saving.current) return;
        const scope = currentSession()?.refreshToken;
        if (!scope) return;
        const submission = pending ?? { key: crypto.randomUUID(), stock, body: { type, quantity, reason, revision: stock.revision } };
        saving.current = true;
        setBusy(true);
        setError("");
        setPending(submission);
        setStockMovementDraft(scope, submission);
        try {
            await api.post(`/api/stock/${stock.id}/movements`, submission.body, {
                headers: { "Idempotency-Key": submission.key },
            });
            setStockMovementDraft(scope, null);
            changed();
            close();
        } catch (failure) {
            const status = failure.response?.status;
            if ([400, 404, 409].includes(status)) {
                setStockMovementDraft(scope, null);
                setPending(null);
                setError(stockError(failure));
                changed();
            } else {
                setError("We couldn't confirm the stock movement. Resubmit the same details.");
            }
        } finally {
            saving.current = false;
            setBusy(false);
        }
    }

    return (
        <Dialog open onClose={busy || uncertain ? undefined : close} fullWidth maxWidth="xs" aria-labelledby="stock-movement-title">
            <Box component="form" onSubmit={save}>
                <DialogTitle id="stock-movement-title">Record stock movement · {stock.stockName}</DialogTitle>
                <DialogContent dividers>
                    <Stack spacing={2} sx={{ pt: 0.5 }}>
                        <Typography>Recorded balance {amount(stock.quantity)} {units[stock.unit]}</Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        {uncertain && !busy && !error && <Alert severity="warning">This stock movement hasn't been confirmed. Resubmit it to check the result.</Alert>}
                        <TextField select label="Movement type" value={type} disabled={uncertain}
                            onChange={event => setType(event.target.value)}>
                            {manual.map(value => <MenuItem key={value} value={value}>{kinds[value]}</MenuItem>)}
                        </TextField>
                        <TextField label={type === "ADJUSTMENT" ? "Balance after count" : "Quantity"} type="number" required
                            value={quantity} disabled={uncertain} onChange={event => setQuantity(event.target.value)}
                            slotProps={{ htmlInput: { ...numberProps, min: type === "ADJUSTMENT" ? "0" : "0.001" } }} />
                        <TextField label="Reason for movement" required multiline minRows={2} value={reason} disabled={uncertain}
                            onChange={event => setReason(event.target.value)} slotProps={{ htmlInput: { maxLength: 200 } }} />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={close} disabled={busy || uncertain}>Cancel</Button>
                    <Button type="submit" variant="contained" disabled={busy}>{uncertain ? "Resubmit" : "Save movement"}</Button>
                </DialogActions>
            </Box>
        </Dialog>
    );
}

export function StockHistoryDialog({ stock, close }) {
    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(null);
    const [retry, setRetry] = useState(0);
    const data = snapshot?.page === page ? snapshot.data : null;
    const error = snapshot?.page === page ? snapshot.error : "";

    useEffect(() => {
        const controller = new AbortController();
        api.get(`/api/stock/${stock.id}/movements?page=${page}`, { signal: controller.signal })
            .then(result => setSnapshot({ page, data: result.data }))
            .catch(failure => { if (!controller.signal.aborted) setSnapshot({ page, error: stockError(failure) }); });
        return () => controller.abort();
    }, [page, stock.id, retry]);

    return (
        <Dialog open onClose={close} fullWidth maxWidth="sm" aria-labelledby="stock-history-title">
            <DialogTitle id="stock-history-title">Stock history · {stock.stockName}</DialogTitle>
            <DialogContent dividers>
                {error ? <Alert severity="error" action={<Button onClick={() => setRetry(value => value + 1)}>Try again</Button>}>{error}</Alert>
                    : !data ? <LoadingState>Loading history…</LoadingState>
                    : !data.movements.length ? <EmptyState compact>No stock movements recorded.</EmptyState>
                    : <Stack divider={<Divider />} spacing={1.5}>
                        {data.movements.map(entry => <Box key={entry.id} className="cp-stock-event">
                            <Stack direction="row" justifyContent="space-between" gap={1}>
                                <Typography fontWeight={700}>{kinds[entry.kind]}{entry.orderSequence ? ` · #${entry.orderSequence}` : ""}</Typography>
                                <Typography className="cp-stock-delta">{entry.delta > 0 ? "+" : ""}{amount(entry.delta)} {units[stock.unit]}</Typography>
                            </Stack>
                            <Typography color="text.secondary" variant="body2">
                                Balance {amount(entry.balanceAfter)} {units[stock.unit]} · {dates.format(new Date(entry.recordedAt))}
                            </Typography>
                            <Typography variant="body2" sx={{ overflowWrap: "anywhere" }}>{entry.actor} · {entry.reason}</Typography>
                        </Box>)}
                    </Stack>}
            </DialogContent>
            <DialogActions>
                <Button disabled={page === 0} onClick={() => setPage(value => value - 1)}>Previous</Button>
                <Typography variant="body2">Page {page + 1}</Typography>
                <Button disabled={!data?.hasNext} onClick={() => setPage(value => value + 1)}>Next</Button>
                <Button onClick={close}>Close</Button>
            </DialogActions>
        </Dialog>
    );
}
