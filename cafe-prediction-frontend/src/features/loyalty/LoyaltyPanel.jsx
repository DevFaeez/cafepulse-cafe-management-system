import { useEffect, useState } from "react";
import {
    Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, InputAdornment, Paper, Stack,
    Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow, TextField, Typography,
} from "@mui/material";
import GroupOutlined from "@mui/icons-material/GroupOutlined";
import SearchRounded from "@mui/icons-material/SearchRounded";
import { EmptyState, LoadingState } from "../../components/WorkspaceUI";
import { api } from "../../api/session";

const path = "/api/admin/loyalty";
const integer = new Intl.NumberFormat("en-MY");
const monthNames = new Intl.DateTimeFormat("en-MY", { month: "short", year: "numeric", timeZone: "UTC" });
const dayNames = new Intl.DateTimeFormat("en-MY", { dateStyle: "medium", timeZone: "UTC" });
const historyTime = new Intl.DateTimeFormat("en-MY", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Kuala_Lumpur" });
const count = value => integer.format(value);
const reasonNames = {
    EARNED: "Points earned", REDEEMED: "Redemption", EARNING_REVERSED: "Payment points reversed",
    REDEMPTION_RESTORED: "Redeemed points restored", LEGACY_EARNING_REVERSED: "Legacy order points reversed",
};
const errorText = error => error.response?.data?.description ?? "Customer data couldn't be loaded. Try again.";
const cardSize = { p: { xs: 1.5, sm: 2 }, minWidth: 0 };

function Retry({ error, retry }) {
    return <Alert severity="error" action={<Button onClick={retry}>Try again</Button>}>{error}</Alert>;
}

function CustomerMetrics({ overview }) {
    const metrics = [
        ["Total customers", overview.totalCustomers],
        ["Returning customers", overview.returningCustomers],
        ["Active in last 30 days", overview.activeCustomers],
        ["Available loyalty points", overview.availablePoints],
    ];
    return (
        <Box sx={{ display: "grid", gap: 1.25, gridTemplateColumns: { xs: "repeat(2,minmax(0,1fr))", md: "repeat(4,minmax(0,1fr))" } }}>
            {metrics.map(([label, value], index) => (
                <Paper component="dl" variant="outlined" key={label} className={index === 0 ? "cp-report-hero" : "cp-report-metric"}>
                    <Typography component="dt" variant="body2">{label}</Typography>
                    <Typography component="dd" className="cp-report-value">{count(value)}</Typography>
                </Paper>
            ))}
        </Box>
    );
}

function RegistrationChart({ rows }) {
    const highest = Math.max(1, ...rows.map(row => row.count));
    return (
        <Paper component="section" aria-labelledby="loyalty-registrations" variant="outlined" sx={cardSize}>
            <Typography id="loyalty-registrations" component="h3" variant="h6">New customers</Typography>
            <Typography variant="body2" color="text.secondary">Last six months</Typography>
            <Box component="ol" sx={{ p: 0, mb: 0, mt: 1.75, listStyle: "none", display: "grid", gap: 0.5, gridTemplateColumns: "repeat(6,minmax(0,1fr))" }}>
                {rows.map(row => {
                    const label = monthNames.format(new Date(`${row.month}-01T00:00:00Z`));
                    return (
                        <Box component="li" key={row.month} sx={{ textAlign: "center", minWidth: 0 }}>
                            <Stack justifyContent="end" alignItems="center" sx={{ height: 125, borderBottom: "1px solid", borderColor: "divider" }}>
                                <Typography variant="caption" fontWeight={700}>{count(row.count)}</Typography>
                                <Box aria-hidden="true" sx={{ height: `${Math.max(1, row.count * 96 / highest)}px`, flexShrink: 0,
                                    width: "min(100%, 42px)", bgcolor: row.count ? "primary.main" : "divider", borderRadius: "4px 4px 0 0" }} />
                            </Stack>
                            <Typography variant="caption" title={label}>{label}</Typography>
                        </Box>
                    );
                })}
            </Box>
        </Paper>
    );
}

function Distribution({ id, heading, subtitle, buckets, total }) {
    return (
        <Paper component="section" aria-labelledby={id} variant="outlined" sx={cardSize}>
            <Typography id={id} component="h3" variant="h6">{heading}</Typography>
            <Typography variant="body2" color="text.secondary">{subtitle}</Typography>
            <Stack component="ul" spacing={1.5} sx={{ p: 0, mb: 0, mt: 2, listStyle: "none" }}>
                {buckets.map(([name, value]) => (
                    <Box component="li" key={name}>
                        <Stack direction="row" gap={1} justifyContent="space-between">
                            <Typography variant="body2">{name}</Typography>
                            <Typography variant="body2" fontWeight={700}>{count(value)}</Typography>
                        </Stack>
                        <Box aria-hidden="true" sx={{ height: 6, mt: 0.5, borderRadius: 1, bgcolor: "var(--workspace-line)", overflow: "hidden" }}>
                            <Box sx={{ height: "100%", width: `${total ? value * 100 / total : 0}%`, borderRadius: 1, bgcolor: "primary.main" }} />
                        </Box>
                    </Box>
                ))}
            </Stack>
        </Paper>
    );
}

function CustomerOverview({ attempt }) {
    const [state, setState] = useState(null);
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        api.get(`${path}/overview`, { signal: controller.signal })
            .then(({ data }) => setState({ attempt, retry, data }))
            .catch(error => {
                if (!controller.signal.aborted) setState({ attempt, retry, error: errorText(error) });
            });
        return () => controller.abort();
    }, [attempt, retry]);

    if (state?.attempt !== attempt || state?.retry !== retry) return <LoadingState>Loading customer overview…</LoadingState>;
    if (state.error) return <Retry error={state.error} retry={() => setRetry(value => value + 1)} />;
    const overview = state.data;
    return <>
        <CustomerMetrics overview={overview} />
        <Box sx={{ display: "grid", gap: 1.25, gridTemplateColumns: { xs: "minmax(0,1fr)", md: "minmax(0,1.3fr) repeat(2,minmax(0,1fr))" } }}>
            <RegistrationChart rows={overview.registrations} />
            <Distribution id="loyalty-purchases" heading="Purchase frequency" subtitle="Paid orders after refunds and corrections"
                total={overview.totalCustomers} buckets={[
                    ["No paid orders", overview.purchases.none], ["One order", overview.purchases.once], ["Two or more", overview.purchases.repeat],
                ]} />
            <Distribution id="loyalty-points" heading="Loyalty balances" subtitle="Customers by available points"
                total={overview.totalCustomers} buckets={[
                    ["0 points", overview.points.none], ["1–99 points", overview.points.under100], ["100+ points", overview.points.atLeast100],
                ]} />
        </Box>
    </>;
}

function CustomerDirectory({ select, attempt }) {
    const [phone, setPhone] = useState("");
    const [page, setPage] = useState(0);
    const [result, setResult] = useState(null);
    const [retry, setRetry] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        const wait = setTimeout(() => {
            api.post(`${path}/customers`, { phone, page }, { signal: controller.signal })
                .then(({ data }) => setResult({ phone, page, attempt, retry, data }))
                .catch(error => {
                    if (!controller.signal.aborted) setResult({ phone, page, attempt, retry, error: errorText(error) });
                });
        }, phone ? 250 : 0);
        return () => { clearTimeout(wait); controller.abort(); };
    }, [phone, page, attempt, retry]);

    const current = result?.phone === phone && result.page === page && result.attempt === attempt && result.retry === retry ? result : null;
    return (
        <Paper component="section" aria-labelledby="customer-directory" variant="outlined" sx={cardSize}>
            <Stack spacing={1.5}>
                <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ sm: "center" }} gap={1.5}>
                    <Typography id="customer-directory" component="h3" variant="h6">Customer directory</Typography>
                    <TextField type="tel" size="small" label="Search phone number" value={phone}
                        onChange={event => { setPhone(event.target.value); setPage(0); }} sx={{ width: "100%", maxWidth: { sm: 300 } }}
                        slotProps={{ htmlInput: { maxLength: 32 },
                            input: { startAdornment: <InputAdornment position="start"><SearchRounded /></InputAdornment> } }} />
                </Stack>
                {!current && <LoadingState>Loading customers…</LoadingState>}
                {current?.error && <Retry error={current.error} retry={() => setRetry(value => value + 1)} />}
                {current?.data && (current.data.total === 0
                    ? <EmptyState icon={GroupOutlined} compact>{phone.trim() ? "No phone numbers match this search." : "No customers have registered yet."}</EmptyState>
                    : <>
                        <TableContainer role="region" tabIndex={0} aria-labelledby="customer-directory">
                            <Table size="small" aria-label="Customer phone numbers" sx={{ minWidth: 420 }}>
                                <TableHead><TableRow>
                                    <TableCell>Phone number</TableCell><TableCell>Joined</TableCell>
                                    <TableCell align="right">Paid orders</TableCell><TableCell align="right">Available points</TableCell>
                                </TableRow></TableHead>
                                <TableBody>
                                    {current.data.customers.map(customer => (
                                        <TableRow key={customer.id} hover>
                                            <TableCell component="th" scope="row">
                                                <Button onClick={() => select(customer)} aria-label={`View loyalty history for ${customer.phone}`}
                                                    sx={{ minHeight: 44, pl: 0, justifyContent: "flex-start", fontVariantNumeric: "tabular-nums" }}>
                                                    {customer.phone}
                                                </Button>
                                            </TableCell>
                                            <TableCell>{dayNames.format(new Date(`${customer.joinedOn}T00:00:00Z`))}</TableCell>
                                            <TableCell align="right">{count(customer.paidOrders)}</TableCell>
                                            <TableCell align="right">{count(customer.availablePoints)}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <TablePagination component="div" count={current.data.total} page={current.data.page}
                            rowsPerPageOptions={[]} rowsPerPage={current.data.pageSize} onPageChange={(_, value) => setPage(value)} />
                    </>)}
            </Stack>
        </Paper>
    );
}

function CustomerHistory({ customer, close }) {
    const [state, setState] = useState(null);
    const [attempt, setAttempt] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        api.get(`${path}/customers/${customer.id}/history`, { signal: controller.signal })
            .then(({ data }) => setState({ attempt, data }))
            .catch(error => {
                if (!controller.signal.aborted) setState({ attempt, error: errorText(error) });
            });
        return () => controller.abort();
    }, [customer.id, attempt]);

    const current = state?.attempt === attempt ? state : null;
    const data = current?.data;
    return (
        <Dialog open onClose={close} fullWidth maxWidth="sm" aria-labelledby="customer-history-heading">
            <DialogTitle id="customer-history-heading">Loyalty history · {customer.phone}</DialogTitle>
            <DialogContent dividers>
                {!current && <LoadingState>Loading loyalty history…</LoadingState>}
                {current?.error && <Retry error={current.error} retry={() => setAttempt(value => value + 1)} />}
                {data && <Stack spacing={1.5}>
                    <Typography fontWeight={700} className="cp-cart-quote">
                        {count(data.available)} points available{data.balance < 0 ? ` · Pending adjustment ${count(-data.balance)} points` : ""}
                    </Typography>
                    {data.movements.length ? (
                        <Stack component="ul" spacing={1} divider={<Divider component="li" />} sx={{ m: 0, p: 0, listStyle: "none" }}>
                            {data.movements.map((entry, index) => (
                                <Box component="li" key={index}>
                                    <Typography fontWeight={700}>{entry.points > 0 ? "+" : ""}{count(entry.points)} points · {reasonNames[entry.reason]}</Typography>
                                    <Typography variant="body2" color="text.secondary">
                                        Queue #{entry.queue} · Balance {count(entry.balanceAfter)} · {historyTime.format(new Date(entry.time))}
                                    </Typography>
                                </Box>
                            ))}
                        </Stack>
                    ) : <EmptyState icon={GroupOutlined} compact>No points activity recorded yet.</EmptyState>}
                </Stack>}
            </DialogContent>
            <DialogActions><Button onClick={close}>Close</Button></DialogActions>
        </Dialog>
    );
}

export default function LoyaltyPanel() {
    const [customer, setCustomer] = useState(null);
    const [attempt, setAttempt] = useState(0);
    return (
        <Stack spacing={2}>
            <Stack direction="row" gap={1} alignItems="center" justifyContent="space-between" flexWrap="wrap" className="cp-section-heading">
                <Typography component="h2" variant="h6">Customers &amp; loyalty</Typography>
                <Button onClick={() => setAttempt(value => value + 1)}>Refresh customers</Button>
            </Stack>
            <CustomerOverview attempt={attempt} />
            <CustomerDirectory select={setCustomer} attempt={attempt} />
            {customer && <CustomerHistory key={customer.id} customer={customer} close={() => setCustomer(null)} />}
        </Stack>
    );
}
