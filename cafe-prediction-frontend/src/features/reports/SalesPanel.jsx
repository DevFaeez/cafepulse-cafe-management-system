import { useEffect, useRef, useState } from "react";
import {
    Alert, Box, Button, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead,
    TableRow, TextField, Typography,
} from "@mui/material";
import DownloadRounded from "@mui/icons-material/DownloadRounded";
import AssessmentOutlined from "@mui/icons-material/AssessmentOutlined";
import { EmptyState, LoadingState } from "../../components/WorkspaceUI";
import { api } from "../../api/session";

const money = value => `RM ${Number(value).toLocaleString("en-MY", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const count = value => Number(value).toLocaleString("en-MY");
const days = new Intl.DateTimeFormat("en-MY", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
const labels = { DAY: "Daily", WEEK: "Weekly", MONTH: "Monthly" };
const financials = [
    ["gross", "Gross sales"], ["discounts", "Discount"], ["collected", "Payments received"],
    ["refunded", "Refunds"],
];
const orders = [["paidOrders", "Paid orders"], ["refundedOrders", "Refunded orders"], ["cancelledOrders", "Cancelled orders"]];
const units = [["cups", "Cups"], ["food", "Food items"], ["other", "Other units"], ["unclassified", "Unclassified"]];
const dateLabel = date => days.format(new Date(`${date}T00:00:00Z`));
const periodLabel = ({ from, to }) => from === to ? dateLabel(from) : `${dateLabel(from)} – ${dateLabel(to)}`;
const errorText = error => error.response?.data?.description ?? "The report couldn't be loaded. Try again.";

function initialFilter() {
    const today = new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Kuala_Lumpur" });
    const first = new Date(`${today}T00:00:00Z`);
    first.setUTCDate(first.getUTCDate() - 6);
    return { from: first.toISOString().slice(0, 10), to: today, group: "DAY" };
}

function Summary({ amounts }) {
    return <Stack spacing={1.5} useFlexGap>
        <div className="cp-report-summary">
            <Paper component="dl" variant="outlined" className="cp-report-hero">
                <Typography component="dt" variant="body2">Net sales</Typography>
                <Typography component="dd" className="cp-report-value">{money(amounts.net)}</Typography>
            </Paper>
            <div className="cp-report-grid">
                {financials.map(([key, label]) => (
                    <Paper key={key} component="dl" variant="outlined" className="cp-report-metric">
                        <Typography component="dt" variant="body2" color="text.secondary">{label}</Typography>
                        <Typography component="dd" className="cp-report-value">{money(amounts[key])}</Typography>
                    </Paper>
                ))}
            </div>
        </div>
        <Paper component="dl" variant="outlined" className="cp-report-counts">
            {[...orders, ...units].map(([key, label]) => (
                <Box key={key}>
                    <Typography component="dt" variant="body2" color="text.secondary">{label}</Typography>
                    <Typography component="dd" className="cp-report-value">{count(amounts[key])}</Typography>
                </Box>
            ))}
        </Paper>
    </Stack>;
}

function Trend({ report, selected, select }) {
    const max = Math.max(0, ...report.periods.map(period => Number(period.amounts.net)));
    const min = Math.min(0, ...report.periods.map(period => Number(period.amounts.net)));
    const range = max - min || 1;
    const threshold = (-min / range) * 100;

    return (
        <Paper component="section" aria-labelledby="sales-trend" variant="outlined" sx={{ p: { xs: 1.5, sm: 2 } }}>
            <Typography id="sales-trend" component="h3" variant="h6" fontWeight={750} sx={{ mb: 1.5 }}>
                Net sales trend · {labels[report.group]}
            </Typography>
            {max === 0 && min === 0 ? <EmptyState icon={AssessmentOutlined} compact>Net sales were RM 0.00 in this date range.</EmptyState> :
                <Stack spacing={0.75} sx={{ maxHeight: 320, overflowY: "auto", pr: 1 }}>
                    {report.periods.map((period, index) => {
                        const value = Number(period.amounts.net);
                        const width = Math.abs(value) / range * 100;
                        const left = value < 0 ? threshold - width : threshold;
                        return (
                            <Box component="button" type="button" key={period.from} aria-pressed={index === selected}
                                onClick={() => select(index)} sx={{
                                    py: 0.75, px: 0.75, border: "1px solid transparent", borderRadius: 1, cursor: "pointer",
                                    display: "grid", gridTemplateColumns: "minmax(90px, 1fr) minmax(36px, 2fr) minmax(60px, auto)",
                                    gap: 1, alignItems: "center", textAlign: "left", color: "text.primary",
                                    minHeight: 42, backgroundColor: index === selected ? "var(--workspace-accent-tint)" : "transparent",
                                    "&:hover, &:focus-visible": { borderColor: "primary.main" },
                                }}>
                                <Typography component="span" variant="caption">{periodLabel(period)}</Typography>
                                <Box component="span" aria-hidden="true" sx={{ height: 8, borderRadius: 1, bgcolor: "var(--workspace-line)", position: "relative"}}>
                                    <Box component="span" sx={{ position: "absolute", left: `${left}%`, width: `${width}%`, height: 8,
                                        bgcolor: value < 0 ? "error.main" : "primary.main", borderRadius: 1 }} />
                                </Box>
                                <Typography component="span" variant="caption" fontWeight={700} sx={{ textAlign: "right" }}>{money(value)}</Typography>
                            </Box>
                        );
                    })}
                </Stack>}
        </Paper>
    );
}

function ProductRanking({ products }) {
    return (
        <Paper component="section" aria-labelledby="sales-products" variant="outlined" sx={{ p: { xs: 1.5, sm: 2 } }}>
            <Typography id="sales-products" component="h3" variant="h6" fontWeight={750}>Top-selling products</Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Ranked by net units after refunds.</Typography>
            {!products.length ? <EmptyState icon={AssessmentOutlined} compact>No products were sold in this date range.</EmptyState> :
                <TableContainer sx={{ maxHeight: 360 }} tabIndex={0} role="region" aria-labelledby="sales-products">
                    <Table className="cp-report-table" size="small" stickyHeader aria-label="Units sold by product">
                        <TableHead>
                            <TableRow><TableCell>Product</TableCell><TableCell align="right">Paid</TableCell>
                                <TableCell align="right">Refunded</TableCell><TableCell align="right">Net units</TableCell></TableRow>
                        </TableHead>
                        <TableBody>
                            {products.map(product => <TableRow key={product.productId}>
                                <TableCell component="th" scope="row">{product.productName}</TableCell>
                                <TableCell align="right">{count(product.paidQuantity)}</TableCell>
                                <TableCell align="right">{count(product.refundedQuantity)}</TableCell>
                                <TableCell align="right">{count(product.netQuantity)}</TableCell>
                            </TableRow>)}
                        </TableBody>
                    </Table>
                </TableContainer>}
        </Paper>
    );
}

export default function SalesPanel() {
    const [filter, setFilter] = useState(initialFilter);
    const [applied, apply] = useState(filter);
    const [report, setReport] = useState(null);
    const [selected, setSelected] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [exportError, setExportError] = useState("");
    const [exporting, setExporting] = useState(false);
    const download = useRef(null);

    useEffect(() => {
        const controller = new AbortController();
        api.get("/api/admin/reports/sales", { params: applied, signal: controller.signal }).then(({ data }) => {
            if (controller.signal.aborted) return;
            setReport(data);
            const lastWithActivity = data.periods.findLastIndex(period => (
                period.amounts.paidOrders || period.amounts.refundedOrders || period.amounts.cancelledOrders
            ));
            setSelected(lastWithActivity < 0 ? data.periods.length - 1 : lastWithActivity);
        }).catch(failure => {
            if (!controller.signal.aborted) setError(errorText(failure));
        }).finally(() => {
            if (!controller.signal.aborted) setLoading(false);
        });
        return () => controller.abort();
    }, [applied]);

    useEffect(() => () => download.current?.abort(), []);

    function submit(event) {
        event.preventDefault();
        setLoading(true);
        setError("");
        setExportError("");
        apply({ ...filter });
    }

    async function exportHistory() {
        const controller = new AbortController();
        download.current = controller;
        setExporting(true);
        setExportError("");
        try {
            const { from, to } = report;
            const { data } = await api.get("/api/admin/reports/product-history", { params: { from, to }, signal: controller.signal });
            if (controller.signal.aborted) return;
            const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }));
            const link = document.createElement("a");
            link.href = url;
            link.download = `cafepulse-product-history-${from}-${to}.json`;
            link.click();
            setTimeout(() => URL.revokeObjectURL(url), 1000);
        } catch (failure) {
            if (!controller.signal.aborted) setExportError(errorText(failure));
        } finally {
            if (!controller.signal.aborted) setExporting(false);
        }
    }

    const period = report?.periods[selected];
    const change = key => event => setFilter(previous => ({ ...previous, [key]: event.target.value }));

    return (
        <Stack spacing={2.5}>
            <Typography component="h2" variant="h5" className="cp-section-heading">Sales report</Typography>
            <Stack component="form" className="cp-control-surface cp-report-filter" onSubmit={submit} direction="row" flexWrap="wrap" gap={1.25} alignItems="center">
                <TextField required type="date" label="Start date" size="small" value={filter.from} onChange={change("from")}
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: filter.to } }} />
                <TextField required type="date" label="End date" size="small" value={filter.to} onChange={change("to")}
                    slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: filter.from } }} />
                <TextField select label="Group by" size="small" value={filter.group} onChange={change("group")} sx={{ minWidth: 155 }}>
                    {Object.entries(labels).map(([key, label]) => <MenuItem key={key} value={key}>{label}</MenuItem>)}
                </TextField>
                <Button variant="contained" type="submit" disabled={loading || exporting}>View report</Button>
            </Stack>
            {error && <Alert severity="error">{error}</Alert>}
            {exportError && <Alert severity="error">{exportError}</Alert>}
            {loading && <LoadingState>Loading report…</LoadingState>}
            {report && !loading && !error && <>
                <Stack direction="row" gap={1.5} flexWrap="wrap" justifyContent="space-between" alignItems="center">
                    <Typography color="text.secondary" variant="body2">{periodLabel(report)} · {report.zone}</Typography>
                    <Button variant="outlined" startIcon={<DownloadRounded />} disabled={exporting} onClick={exportHistory}>Download product history</Button>
                </Stack>
                <Summary amounts={report.totals} />
                <Trend report={report} selected={selected} select={setSelected} />
                {period && <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2 } }}>
                    <TextField select label="Period details" size="small" value={selected} onChange={event => setSelected(Number(event.target.value))}
                        sx={{ width: { xs: "100%", sm: "min(100%, 280px)" }, mb: 1.5 }}>
                        {report.periods.map((entry, index) => <MenuItem key={entry.from} value={index}>{periodLabel(entry)}</MenuItem>)}
                    </TextField>
                    <Summary amounts={period.amounts} />
                </Paper>}
                <ProductRanking products={report.products} />
            </>}
        </Stack>
    );
}
