import { useEffect, useState } from "react";
import {
    Alert, Box, Button, Checkbox, Chip, Divider, FormControlLabel, MenuItem, Paper, Stack, Tab, Table,
    TableBody, TableCell, TableContainer, TableHead, TableRow, Tabs, TextField, Typography,
} from "@mui/material";
import DownloadRounded from "@mui/icons-material/DownloadRounded";
import InsightsRounded from "@mui/icons-material/InsightsRounded";
import { EmptyState, LoadingState } from "../../components/WorkspaceUI";
import { api } from "../../api/session";
import SimulatedHistory from "./SimulatedHistory";

const weather = {
    SUNNY: "Sunny", PARTLY_CLOUDY: "Partly cloudy", CLOUDY: "Cloudy", LIGHT_RAIN: "Light rain",
    HEAVY_RAIN: "Heavy rain", THUNDERSTORM: "Thunderstorms",
};
const traffic = { LOW: "Low", MEDIUM: "Medium", HIGH: "High" };
const events = {
    SchoolHoliday: "School holiday", PublicHoliday: "Public holiday", Festival: "Festival", BackToSchool: "Back-to-school season",
    ExamSeason: "Exam season", PayDay: "Payday", BigEventNearby: "Major event nearby",
};
const promos = {
    Promo_5PercentDiscount: "5% off", Promo_10PercentDiscount: "10% off", Promo_15PercentDiscount: "15% off",
    Promo_20PercentDiscount: "20% off", Promo_Buy1Free1: "Buy one, get one free",
    Promo_Buy2Free1: "Buy two, get one free", Promo_NewProductLaunch: "New product launch",
    Promo_AnniversarySale: "Anniversary sale", Promo_SocialMediaCampaign: "Social media campaign",
};
const today = () => new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Kuala_Lumpur" });
const draft = () => ({ date: today(), morning: "SUNNY", afternoon: "SUNNY", evening: "CLOUDY", traffic: "MEDIUM", flags: [] });
const failureMessage = failure => failure.response?.data?.description ?? "We couldn't complete the forecast request. Try again.";
const url = "/api/admin/forecast";

function ScenarioFields({ value, change, disabled, actual = false, upcoming = false }) {
    const weekday = new Date(`${value.date}T00:00:00Z`).getUTCDay();
    const dayType = !Number.isNaN(weekday) ? (weekday === 0 || weekday === 6 ? "Weekend" : "Weekday") : "";
    const select = (key, label, choices) => <TextField select size="small" label={label} value={value[key]}
        disabled={disabled} onChange={event => change(key, event.target.value)}>
        {Object.entries(choices).map(([id, name]) => <MenuItem key={id} value={id}>{name}</MenuItem>)}
    </TextField>;

    function flags(title, choices) {
        return (
            <Box component="fieldset" className="cp-forecast-fieldset" disabled={disabled}>
                <Typography component="legend" fontWeight={700}>{title}</Typography>
                <Box sx={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(min(100%, 200px),1fr))" }}>
                    {Object.entries(choices).map(([id, label]) => <FormControlLabel key={id} label={label} control={
                        <Checkbox checked={value.flags.includes(id)} onChange={event => change("flags",
                            event.target.checked ? [...value.flags, id] : value.flags.filter(flag => flag !== id))} />
                    } />)}
                </Box>
            </Box>
        );
    }

    return (
        <Stack spacing={2}>
            <Box sx={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(min(100%, 165px),1fr))", gap: 1.5 }}>
                <TextField required type="date" size="small" label={actual ? "Trading date" : "Forecast date"}
                    value={value.date} disabled={disabled} onChange={event => change("date", event.target.value)}
                    helperText={dayType} slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: upcoming ? today() : undefined, max: actual ? today() : undefined } }} />
                {select("morning", "Weather, 8 am–1 pm", weather)}
                {select("afternoon", "Weather, 2–7 pm", weather)}
                {select("evening", "Weather, 8–11 pm", weather)}
                {select("traffic", "Foot traffic", traffic)}
            </Box>
            {flags("Holidays and events", events)}
            {flags("Scenario promotions", promos)}
        </Stack>
    );
}

const unitLabels = { CUP: "Cups", FOOD: "Food items", OTHER: "Other units" };

function MenuEstimates({ result }) {
    const [category, setCategory] = useState("all");
    const categories = [...new Set(result.estimates.map(product => product.category))];
    const visible = category !== "all" ? result.estimates.filter(product => product.category === category) : result.estimates;

    return (
        <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2.5 } }}>
            <Stack spacing={1.5}>
                <Typography id="forecast-results" component="h3" variant="h6">Estimated net units · {result.date}</Typography>
                <Typography variant="body2" color="text.secondary">Trained on simulated sales through {result.trainingThrough}.</Typography>
                <TextField select label="Filter forecast by category" size="small" value={category}
                    onChange={event => setCategory(event.target.value)} sx={{ width: "100%", maxWidth: 300 }}>
                    <MenuItem value="all">All categories</MenuItem>
                    {categories.map(name => <MenuItem key={name} value={name}>{name}</MenuItem>)}
                </TextField>
                <TableContainer tabIndex={0} role="region" aria-labelledby="forecast-results">
                    <Table className="cp-forecast-table" size="small" aria-label="Simulated forecast by menu product">
                        <TableHead><TableRow><TableCell>Product</TableCell><TableCell>Unit</TableCell>
                            <TableCell align="right">Estimate</TableCell></TableRow></TableHead>
                        <TableBody>{visible.map(product => <TableRow key={product.sku}>
                            <TableCell component="th" scope="row">{product.name}</TableCell>
                            <TableCell>{unitLabels[product.unit]}</TableCell>
                            <TableCell align="right">{product.quantity.toLocaleString("en-MY")}</TableCell>
                        </TableRow>)}</TableBody>
                    </Table>
                </TableContainer>
            </Stack>
        </Paper>
    );
}

function Prediction() {
    const [mode, setMode] = useState("menu");
    const [scenario, setScenario] = useState(draft);
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    async function submit(event) {
        event.preventDefault();
        setError("");
        setResult(null);
        setBusy(true);
        try {
            const endpoint = mode === "menu" ? "demo/predict" : "predict";
            setResult((await api.post(`${url}/${endpoint}`, scenario)).data);
        } catch (failure) {
            setError(failureMessage(failure));
        } finally {
            setBusy(false);
        }
    }

    function change(key, value) {
        setResult(null);
        setScenario(previous => ({ ...previous, [key]: value }));
    }

    return <>
        <Alert severity="info">Forecasts use simulated sales. The menu model covers all 52 sample products, including food and drinks.</Alert>
        <Paper component="form" variant="outlined" onSubmit={submit} sx={{ p: { xs: 1.5, sm: 2.5 } }}>
            <Stack spacing={2}>
                <TextField select size="small" label="Forecast model" value={mode} disabled={busy} onChange={event => {
                    setMode(event.target.value);
                    setResult(null);
                    setError("");
                    if (event.target.value === "menu" && scenario.date < today()) change("date", today());
                }} sx={{ width: "100%", maxWidth: 300 }}>
                    <MenuItem value="menu">CafePulse menu</MenuItem>
                    <MenuItem value="drinks">Original eight-drink model</MenuItem>
                </TextField>
                <ScenarioFields value={scenario} change={change} disabled={busy} upcoming={mode === "menu"} />
                <Button variant="contained" disabled={busy} type="submit" sx={{ alignSelf: "flex-start" }}>
                    {busy ? "Generating forecast…" : "Generate forecast"}
                </Button>
                {error && <Alert severity="error">{error}</Alert>}
            </Stack>
        </Paper>
        {result && mode === "menu" && <MenuEstimates result={result} />}
        {result && mode === "drinks" && <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2.5 } }}>
            <Typography id="forecast-results" component="h3" variant="h6">Estimated cups · {result.date}</Typography>
            <TableContainer tabIndex={0} role="region" aria-labelledby="forecast-results">
                <Table className="cp-forecast-table" size="small" aria-label="Estimated cups by drink">
                    <TableHead><TableRow><TableCell>Model drink</TableCell><TableCell>CafePulse product</TableCell><TableCell align="right">Cups</TableCell></TableRow></TableHead>
                    <TableBody>
                        {result.estimates.map(({ product, cups }) => <TableRow key={product.drink}>
                            <TableCell component="th" scope="row">{product.label}</TableCell>
                            <TableCell>{product.productName ? <Stack direction="row" gap={1} flexWrap="wrap">
                                {product.productName}{!product.available && <Chip size="small" label="Unavailable" />}
                            </Stack> : "Not mapped"}</TableCell>
                            <TableCell align="right">{cups.toLocaleString("en-MY")}</TableCell>
                        </TableRow>)}
                    </TableBody>
                </Table>
            </TableContainer>
        </Paper>}
    </>;
}

function MappingRow({ mapping, products, changed }) {
    const [selected, setSelected] = useState(mapping.productId ?? "");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    async function save() {
        setBusy(true);
        setError("");
        try {
            await api.put(`${url}/mappings/${mapping.drink}`, { productId: selected || null });
            await changed();
        } catch (failure) {
            setError(failureMessage(failure));
        } finally {
            setBusy(false);
        }
    }

    const listed = products.some(product => product.id === mapping.productId);
    return (
        <Stack component="article" aria-label={`Mapping for ${mapping.label}`} spacing={0.75}>
            <Stack direction={{ xs: "column", sm: "row" }} gap={1.25} alignItems={{ sm: "center" }}>
                <Typography fontWeight={700} sx={{ flex: { sm: "0 1 33%" }, minWidth: 0, overflowWrap: "anywhere" }}>{mapping.label}</Typography>
                <TextField select size="small" label={`Product for ${mapping.label}`} value={selected} disabled={busy}
                    onChange={event => setSelected(event.target.value)} sx={{ minWidth: 0, flex: 1 }}>
                    <MenuItem value="">Not mapped</MenuItem>
                    {!listed && mapping.productId && <MenuItem disabled value={mapping.productId}>{mapping.productName}</MenuItem>}
                    {products.map(product => <MenuItem key={product.id} value={product.id}>{product.name}</MenuItem>)}
                </TextField>
                <Button disabled={busy || selected === (mapping.productId ?? "")} onClick={save}>Save mapping</Button>
            </Stack>
            {error && <Alert severity="error">{error}</Alert>}
        </Stack>
    );
}

function Observations({ mappings, products }) {
    const [scenario, setScenario] = useState(draft);
    const [tracked, setTracked] = useState(null);
    const [closed, setClosed] = useState(false);
    const [unavailable, setUnavailable] = useState([]);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    const productIds = tracked ?? mappings.filter(mapping => mapping.productId).map(mapping => mapping.productId);
    function change(key, value) {
        setMessage("");
        setScenario(prev => ({ ...prev, [key]: value }));
        if (key === "date") {
            setTracked(null);
            setUnavailable([]);
            setClosed(false);
        }
    }

    function show(data) {
        setScenario(data.scenario);
        setClosed(data.closed);
        setTracked(data.products.map(product => product.productId));
        setUnavailable(data.products.filter(product => product.unavailable).map(product => product.productId));
    }

    async function load() {
        setError(""); setMessage(""); setBusy(true);
        try {
            show((await api.get(`${url}/observations/${scenario.date}`)).data);
            setMessage("Observation loaded.");
        } catch (failure) {
            setError(failureMessage(failure));
        } finally {
            setBusy(false);
        }
    }

    async function save(event) {
        event.preventDefault();
        setError(""); setMessage(""); setBusy(true);
        try {
            show((await api.put(`${url}/observations`, { scenario, closed, unavailableProductIds: unavailable })).data);
            setMessage("Observation saved.");
        } catch (failure) {
            setError(failureMessage(failure));
        } finally {
            setBusy(false);
        }
    }

    return (
        <Paper component="form" variant="outlined" onSubmit={save} sx={{ p: { xs: 1.5, sm: 2.5 } }}>
            <Stack spacing={2}>
                <Typography color="text.secondary">Complete this after closing and mark any products that were out of stock or unavailable that day.</Typography>
                <ScenarioFields value={scenario} change={change} disabled={busy} actual />
                <Divider />
                <FormControlLabel label="Café closed" control={<Checkbox checked={closed} disabled={busy} onChange={event => setClosed(event.target.checked)} />} />
                {!productIds.length ? <EmptyState icon={InsightsRounded} compact>Map drinks to products to track their sales.</EmptyState> : (
                    <Stack>
                        <Typography fontWeight={700}>Out of stock or unavailable</Typography>
                        {productIds.map(id => <FormControlLabel key={id}
                            label={products.find(product => product.id === id)?.name ?? `Product #${id}`}
                            control={<Checkbox checked={unavailable.includes(id)} disabled={busy || closed}
                                onChange={event => setUnavailable(event.target.checked ? [...unavailable, id] : unavailable.filter(value => value !== id))} />} />)}
                    </Stack>
                )}
                {message && <Alert severity="success">{message}</Alert>}
                {error && <Alert severity="error">{error}</Alert>}
                <Stack direction="row" gap={1} flexWrap="wrap">
                    <Button type="button" disabled={busy || !scenario.date} onClick={load}>Load observation</Button>
                    <Button variant="contained" type="submit" disabled={busy}>Save observation</Button>
                </Stack>
            </Stack>
        </Paper>
    );
}

function TrainingExport() {
    const [to, setTo] = useState(today);
    const [from, setFrom] = useState(() => `${today().slice(0, 7)}-01`);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [count, setCount] = useState(null);

    async function download(event) {
        event.preventDefault();
        setError(""); setCount(null); setBusy(true);
        try {
            const { data } = await api.get(`${url}/training-data`, { params: { from, to } });
            setCount(data.rows.length);
            if (!data.rows.length) return;
            const link = document.createElement("a");
            const file = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }));
            link.href = file;
            link.download = `cafepulse-training-data-${from}-${to}.json`;
            link.click();
            setTimeout(() => URL.revokeObjectURL(file), 1000);
        } catch (failure) {
            setError(failureMessage(failure));
        } finally {
            setBusy(false);
        }
    }

    return (
        <Paper component="form" variant="outlined" onSubmit={download} sx={{ p: { xs: 1.5, sm: 2.5 } }}>
            <Stack spacing={2}>
                <Typography>Combine operating observations with daily sales quantities for mapped products.</Typography>
                <Stack direction={{ xs: "column", sm: "row" }} gap={1.5}>
                    <TextField type="date" label="Start date" value={from} onChange={event => setFrom(event.target.value)} required
                        slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: to } }} />
                    <TextField type="date" label="End date" value={to} onChange={event => setTo(event.target.value)} required
                        slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: from, max: today() } }} />
                    <Button type="submit" variant="contained" startIcon={<DownloadRounded />} disabled={busy}>Download data</Button>
                </Stack>
                {error && <Alert severity="error">{error}</Alert>}
                {count !== null && <Alert severity={count ? "success" : "info"}>{count ? `${count} days of observations exported.` : "No operating observations in this date range."}</Alert>}
            </Stack>
        </Paper>
    );
}

export default function ForecastPanel({ products }) {
    const [tab, setTab] = useState("predict");
    const [mappings, setMappings] = useState(null);
    const [error, setError] = useState("");
    async function refresh() {
        setMappings((await api.get(`${url}/mappings`)).data);
        setError("");
    }
    useEffect(() => { refresh().catch(failure => setError(failureMessage(failure))); }, []);
    const cups = products.filter(product => product.salesUnit === "CUP");

    return (
        <Stack spacing={2}>
            <Typography component="h2" variant="h5" className="cp-section-heading">Sales forecast</Typography>
            <Tabs value={tab} onChange={(_, value) => setTab(value)} className="cp-forecast-tabs" aria-label="Forecast sections">
                <Tab value="predict" id="forecast-tab-predict" aria-controls="forecast-panel" label="Forecast scenario" />
                <Tab value="history" id="forecast-tab-history" aria-controls="forecast-panel" label="Simulated history" />
                <Tab value="mapping" id="forecast-tab-mapping" aria-controls="forecast-panel" label="Product mapping" />
                <Tab value="observations" id="forecast-tab-observations" aria-controls="forecast-panel" label="Actual observations" />
                <Tab value="training" id="forecast-tab-training" aria-controls="forecast-panel" label="Training data" />
            </Tabs>
            {error && <Alert severity="warning" action={<Button onClick={() => refresh().catch(failure => setError(failureMessage(failure)))}>Try again</Button>}>{error}</Alert>}
            <Stack spacing={2} role="tabpanel" id="forecast-panel" aria-labelledby={`forecast-tab-${tab}`}>
                {tab === "predict" && <Prediction />}
                {tab === "history" && <SimulatedHistory />}
                {tab === "mapping" && <Paper variant="outlined" sx={{ p: { xs: 1.5, sm: 2.5 } }}>
                    <Stack spacing={1.5} divider={<Divider />}>
                        <Typography color="text.secondary">Choose products that match the model drinks. Each product must use Cups as its sales unit.</Typography>
                        {!mappings && !error && <LoadingState>Loading mappings…</LoadingState>}
                        {mappings?.map(mapping => <MappingRow key={`${mapping.drink}-${mapping.productId}`} mapping={mapping} products={cups} changed={refresh} />)}
                    </Stack>
                </Paper>}
                {tab === "observations" && <Observations mappings={mappings ?? []} products={products} />}
                {tab === "training" && <TrainingExport />}
            </Stack>
        </Stack>
    );
}
