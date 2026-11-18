import { useEffect, useMemo, useState } from "react";
import {
    Alert, Box, Button, Chip, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer,
    TableHead, TablePagination, TableRow, TextField, Typography,
} from "@mui/material";
import DownloadRounded from "@mui/icons-material/DownloadRounded";
import { api } from "../../api/session";
import { LoadingState } from "../../components/WorkspaceUI";

const integer = new Intl.NumberFormat("en-MY");
const currency = new Intl.NumberFormat("en-MY", { style: "currency", currency: "MYR" });
const dates = new Intl.DateTimeFormat("en-MY", { dateStyle: "medium", timeZone: "UTC" });
const dateLabel = date => dates.format(new Date(`${date}T00:00:00Z`));

function sales(row, product) {
    const [paid, refunded, discount, returned] = row.sales[product.sku];
    const gross = paid * product.priceCents;
    return { paid, refunded, discount, returned, gross, net: gross - discount - returned };
}

function totals(rows, products) {
    return rows.map(row => {
        let paid = 0;
        let refunded = 0;
        let net = 0;
        for (const product of products) {
            const item = sales(row, product);
            paid += item.paid;
            refunded += item.refunded;
            net += item.net;
        }
        return { ...row, paid, refunded, net };
    });
}

function download(content, filename, type) {
    const uri = URL.createObjectURL(new Blob([content], { type }));
    const link = document.createElement("a");
    link.href = uri;
    link.download = filename;
    link.click();
    setTimeout(() => URL.revokeObjectURL(uri), 1000);
}

function downloadTable(history) {
    const quote = value => `"${String(value).replaceAll('"', '""')}"`;
    const headers = ["source", "date", "sku", "product", "category", "unit", "currency", "unitPriceCents",
        "paidQuantity", "refundedQuantity", "grossCents", "discountCents", "refundedCents", "netCents",
        "closed", "unavailable", ...Object.keys(history.rows[0].features)];
    const lines = history.rows.flatMap(row => history.products.map(product => {
        const { paid, refunded, gross, discount, returned, net } = sales(row, product);
        return [history.source, row.date, product.sku, product.name, product.category, product.unit, history.currency,
            product.priceCents, paid, refunded, gross, discount, returned, net, row.closed,
            row.unavailable.includes(product.sku), ...headers.slice(16).map(key => row.features[key])].map(quote).join(",");
    }));
    download([headers.join(","), ...lines].join("\r\n"), `cafepulse-simulated-sales-${history.to}.csv`, "text/csv;charset=utf-8");
}

function Trend({ rows }) {
    const highest = Math.max(1, ...rows.map(row => row.paid - row.refunded));
    return (
        <Box sx={{ pt: 0.5 }}>
            <Typography component="h4" fontWeight={700}>Net units by day</Typography>
            <Box aria-hidden="true" sx={{ mt: 1.25, display: "flex", alignItems: "end", height: 90, gap: "2px",
                borderBottom: "1px solid", borderColor: "divider" }}>
                {rows.map(row => {
                    const amount = row.paid - row.refunded;
                    return <Box key={row.date} title={`${dateLabel(row.date)} · ${integer.format(amount)} net units`}
                        sx={{ flex: 1, minWidth: 0, height: `${amount ? amount * 100 / highest : 1}%`,
                            bgcolor: row.closed ? "divider" : "primary.main", borderRadius: "2px 2px 0 0" }} />;
                })}
            </Box>
            <Stack direction="row" justifyContent="space-between" sx={{ mt: 0.5 }}>
                <Typography variant="caption" color="text.secondary">{dateLabel(rows[0].date)}</Typography>
                <Typography variant="caption" color="text.secondary">{dateLabel(rows.at(-1).date)}</Typography>
            </Stack>
        </Box>
    );
}

function History({ history }) {
    const [sku, setSku] = useState("all");
    const [days, setDays] = useState(30);
    const [page, setPage] = useState(0);
    const product = history.products.find(item => item.sku === sku);
    const evaluation = history.evaluation;
    const records = useMemo(() => totals(history.rows.slice(-days), product ? [product] : history.products),
        [history, days, product]);
    const summary = records.reduce((total, row) => ({
        units: total.units + row.paid - row.refunded, net: total.net + row.net
    }), { units: 0, net: 0 });
    const pageSize = 10;
    const visible = records.slice().reverse().slice(page * pageSize, (page + 1) * pageSize);

    return (
        <Stack spacing={2}>
            <Alert severity="info">Simulated sales for {integer.format(history.rows.length)} days across {history.products.length} menu products.
                The figures are separate from order history and sales reports.</Alert>
            <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2.5 } }}>
                <Stack spacing={2}>
                    <Typography component="h3" variant="h6">Simulated sales · {dateLabel(history.from)} – {dateLabel(history.to)}</Typography>
                    <Stack direction={{ xs: "column", sm: "row" }} gap={1.5}>
                        <TextField select label="Product" value={sku} size="small" sx={{ flex: 1, minWidth: 0 }}
                            onChange={event => { setSku(event.target.value); setPage(0); }}>
                            <MenuItem value="all">All products</MenuItem>
                            {history.products.map(item => <MenuItem key={item.sku} value={item.sku}>{item.name}</MenuItem>)}
                        </TextField>
                        <TextField select label="Period" value={days} size="small" sx={{ minWidth: 150 }}
                            onChange={event => { setDays(Number(event.target.value)); setPage(0); }}>
                            {[30, 90, 365].map(value => <MenuItem key={value} value={value}>Last {value} days</MenuItem>)}
                        </TextField>
                    </Stack>
                    <Stack direction="row" gap={2} flexWrap="wrap">
                        <Chip label={`${integer.format(summary.units)} net units`} />
                        <Chip label={`${currency.format(summary.net / 100)} net sales`} />
                    </Stack>
                    <Trend rows={records.slice(-30)} />
                    <TableContainer role="region" tabIndex={0} aria-label="Daily simulated sales table">
                        <Table className="cp-forecast-table" aria-label="Daily simulated sales" size="small">
                            <TableHead><TableRow>
                                <TableCell>Date</TableCell><TableCell>Conditions</TableCell><TableCell align="right">Paid units</TableCell>
                                <TableCell align="right">Refunded units</TableCell><TableCell align="right">Net sales</TableCell>
                            </TableRow></TableHead>
                            <TableBody>{visible.map(row => <TableRow key={row.date}>
                                <TableCell component="th" scope="row">{dateLabel(row.date)}</TableCell>
                                <TableCell>{row.closed ? "Café closed" : `${row.features.Weather_2pm_7pm} · ${row.features.Traffic_Level.toLowerCase()} traffic`}
                                    {!row.closed && product && row.unavailable.includes(product.sku) && " · Out of stock"}
                                </TableCell>
                                <TableCell align="right">{integer.format(row.paid)}</TableCell>
                                <TableCell align="right">{integer.format(row.refunded)}</TableCell>
                                <TableCell align="right">{currency.format(row.net / 100)}</TableCell>
                            </TableRow>)}</TableBody>
                        </Table>
                    </TableContainer>
                    <TablePagination component="div" rowsPerPageOptions={[]} rowsPerPage={pageSize} count={records.length}
                        page={page} onPageChange={(_, value) => setPage(value)} />
                </Stack>
            </Paper>
            <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2.5 } }}>
                <Stack spacing={1.5}>
                    <Typography component="h3" variant="h6">Model evaluation</Typography>
                    <Typography variant="body2" color="text.secondary">
                        Tested on the latest {evaluation.testDays} simulated days, after training on the first {evaluation.trainingDays}.
                        Closed days and unavailable products are excluded.
                    </Typography>
                    <Stack direction="row" gap={1} flexWrap="wrap">
                        <Chip label={`Forecast error ${evaluation.mae} units`} />
                        <Chip label={`Same-weekday average error ${evaluation.baselineMae} units`} variant="outlined" />
                    </Stack>
                    <Typography variant="body2" color="text.secondary">Mean absolute error per product per day.</Typography>
                    <Stack direction="row" flexWrap="wrap" gap={1}>
                        <Button startIcon={<DownloadRounded />} onClick={() => downloadTable(history)}>Download full CSV</Button>
                        <Button startIcon={<DownloadRounded />} onClick={() => download(JSON.stringify(history),
                            `cafepulse-simulated-sales-${history.to}.json`, "application/json")}>Download full JSON</Button>
                    </Stack>
                </Stack>
            </Paper>
        </Stack>
    );
}

export default function SimulatedHistory() {
    const [history, setHistory] = useState(null);
    const [error, setError] = useState("");
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        api.get("/api/admin/forecast/demo/history", { signal: controller.signal })
            .then(({ data }) => setHistory(data))
            .catch(failure => {
                if (!controller.signal.aborted) setError(failure.response?.data?.description ?? "The history couldn't be loaded. Try again.");
            });
        return () => controller.abort();
    }, [attempt]);

    if (error) return <Alert severity="error" action={<Button onClick={() => {
        setError(""); setAttempt(value => value + 1);
    }}>Try again</Button>}>{error}</Alert>;
    return history ? <History history={history} /> : <LoadingState>Loading simulated history…</LoadingState>;
}
