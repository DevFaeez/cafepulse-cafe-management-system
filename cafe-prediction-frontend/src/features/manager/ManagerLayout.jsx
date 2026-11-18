import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Divider, InputAdornment, MenuItem,
    Paper, Stack, TextField, Typography, useMediaQuery } from "@mui/material";
import AddRounded from "@mui/icons-material/AddRounded";
import SearchRounded from "@mui/icons-material/SearchRounded";
import RestaurantOutlined from "@mui/icons-material/RestaurantOutlined";
import CoffeeOutlined from "@mui/icons-material/CoffeeOutlined";
import CategoryOutlined from "@mui/icons-material/CategoryOutlined";
import TuneRounded from "@mui/icons-material/TuneRounded";
import BadgeOutlined from "@mui/icons-material/BadgeOutlined";
import SellOutlined from "@mui/icons-material/SellOutlined";
import LoyaltyOutlined from "@mui/icons-material/LoyaltyOutlined";
import Inventory2Outlined from "@mui/icons-material/Inventory2Outlined";
import MenuBookOutlined from "@mui/icons-material/MenuBookOutlined";
import BarChartRounded from "@mui/icons-material/BarChartRounded";
import InsightsRounded from "@mui/icons-material/InsightsRounded";
import { EmptyState, LoadingState, WorkspaceHeading, WorkspaceLink, WorkspaceNavigation, workspaceRailBreakpoint } from "../../components/WorkspaceUI";
import { Link } from "react-router-dom";
import { api } from "../../api/session";
import StaffInventoryPage from "../Inventory/pages/StaffInventoryPages";
import RecipePanel from "../Inventory/components/RecipePanel";
import SalesPanel from "../reports/SalesPanel";
import ForecastPanel from "../forecast/ForecastPanel";
import LoyaltyPanel from "../loyalty/LoyaltyPanel";

const managerTabs = [
    { id: "products", label: "Products", icon: <CoffeeOutlined fontSize="small" /> },
    { id: "categories", label: "Categories", icon: <CategoryOutlined fontSize="small" /> },
    { id: "variants", label: "Options", icon: <TuneRounded fontSize="small" /> },
    { id: "accounts", label: "Accounts", icon: <BadgeOutlined fontSize="small" />, divider: true },
    { id: "promos", label: "Promotions", icon: <SellOutlined fontSize="small" /> },
    { id: "loyalty", label: "Loyalty points", icon: <LoyaltyOutlined fontSize="small" /> },
    { id: "inventory", label: "Inventory", icon: <Inventory2Outlined fontSize="small" />, divider: true },
    { id: "recipes", label: "Recipes", icon: <MenuBookOutlined fontSize="small" /> },
    { id: "sales", label: "Sales report", icon: <BarChartRounded fontSize="small" />, divider: true },
    { id: "forecast", label: "Forecast", icon: <InsightsRounded fontSize="small" /> },
];

const salesUnits = { CUP: "Cups", FOOD: "Food items", OTHER: "Other units", UNCLASSIFIED: "Unclassified" };
const types = { PERCENT: "Percentage (%)", FIXED: "Fixed amount (RM)", BOGO: "Buy one, get one free" };
const currency = value => `RM ${Number(value).toFixed(2)}`;
const errorText = error => error.response?.data?.description ?? error.response?.data?.message ?? "We couldn't confirm the change. Try again.";

function useData(path) {
    const [state, setState] = useState(null);
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const refresh = useCallback(() => setReload(value => value + 1), []);
    useEffect(() => {
        if (!path) return;
        const controller = new AbortController();
        api.get(path, { signal: controller.signal }).then(({ data }) => {
            setState({ path, data });
            setError("");
        }).catch(failure => { if (!controller.signal.aborted) setError(errorText(failure)); });
        return () => controller.abort();
    }, [path, reload]);
    return { data: state?.path === path ? state.data : [], loading: Boolean(path && state?.path !== path && !error), error, refresh };
}

function Editor({ title, fields, initial, submit, close }) {
    const [values, setValues] = useState(initial);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function save(event) {
        event.preventDefault();
        if (busy) return;
        setBusy(true);
        setError("");
        try {
            await submit(values);
            close();
        } catch (failure) {
            setError(errorText(failure));
        } finally {
            setBusy(false);
        }
    }

    return (
        <Dialog open fullWidth maxWidth="sm" onClose={busy ? undefined : close} aria-labelledby="manager-editor-title">
            <form onSubmit={save}>
                <DialogTitle id="manager-editor-title">{title}</DialogTitle>
                <DialogContent dividers>
                    <Stack spacing={2} sx={{ pt: 0.75 }}>
                        {error && <Alert severity="error">{error}</Alert>}
                        {fields.filter(field => !field.show || field.show(values)).map(field => (
                            <TextField key={field.key} label={field.label} select={Boolean(field.options)}
                                type={field.type ?? "text"} required={field.required ?? true} disabled={busy}
                                multiline={field.multiline} minRows={field.multiline ? 2 : undefined}
                                value={values[field.key] ?? ""} onChange={event => setValues(prev => ({ ...prev, [field.key]: event.target.value }))}
                                slotProps={{ inputLabel: field.type === "datetime-local" ? { shrink: true } : undefined,
                                    htmlInput: { min: field.min, max: field.max, minLength: field.minLength, maxLength: field.maxLength, step: field.step,
                                        autoComplete: field.type === "password" ? "new-password" : undefined } }}>
                                {field.options?.map(option => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
                            </TextField>
                        ))}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={close} disabled={busy}>Cancel</Button>
                    <Button variant="contained" type="submit" disabled={busy}>Save</Button>
                </DialogActions>
            </form>
        </Dialog>
    );
}

function Rows({ children, empty, data, loading, error, refresh }) {
    if (loading) return <LoadingState>Loading…</LoadingState>;
    if (error) return <Alert severity="error" action={<Button onClick={refresh}>Try again</Button>}>{error}</Alert>;
    if (!data.length) return <EmptyState>{empty}</EmptyState>;
    return <Paper variant="outlined" className="cp-directory">
        <Stack divider={<Divider />}>{children}</Stack>
    </Paper>;
}

function Row({ title, detail, children }) {
    return (
        <Stack direction={{ xs: "column", sm: "row" }} gap={1.5} justifyContent="space-between" alignItems={{ sm: "center" }} className="cp-directory-row">
            <Box sx={{ minWidth: 0 }}>
                <Typography fontWeight={750}>{title}</Typography>
                {detail && <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>{detail}</Typography>}
            </Box>
            <Stack direction="row" flexWrap="wrap" useFlexGap spacing={1}>{children}</Stack>
        </Stack>
    );
}

const field = (key, label, extra = {}) => ({ key, label, ...extra });
const options = (data, key = "name") => data.map(item => ({ value: item.id, label: item[key] }));
const amount = (key, label, min = "0") => field(key, label, { type: "number", step: "0.01", min, max: "99999999.99" });

const productsPerPage = 12;

function ProductThumbnail({ product }) {
    const [failed, setFailed] = useState(null);
    const image = product.productImage?.trim();
    const showImage = image && image !== "/vite.svg" && image !== "/media/cafepulse.png" && image !== failed;
    return (
        <span className="cp-manager-thumbnail" aria-hidden="true">
            {showImage ? <img src={image} alt="" loading="lazy" onError={() => setFailed(image)} />
                : product.salesUnit === "CUP" ? <CoffeeOutlined fontSize="small" /> : <RestaurantOutlined fontSize="small" />}
        </span>
    );
}

function ProductCatalogue({ products, categories, onEdit, onToggle }) {
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState("");
    const [page, setPage] = useState(0);
    const term = query.trim().toLocaleLowerCase();
    const visible = products.data.filter(item => (!category || item.productCategory.id === category)
        && item.name.toLocaleLowerCase().includes(term));
    const pages = Math.max(1, Math.ceil(visible.length / productsPerPage));
    const currentPage = Math.min(page, pages - 1);
    const firstIndex = currentPage * productsPerPage;

    return <>
        <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="space-between" alignItems="center" className="cp-section-heading">
            <Typography component="h2" variant="h6">Products</Typography>
            <Button variant="contained" startIcon={<AddRounded />} disabled={!categories.data.length} onClick={() => onEdit()}>
                Add product
            </Button>
        </Stack>
        {!categories.data.length && !categories.loading && <Typography variant="body2">
            Add a category before adding products.
        </Typography>}
        <div className="cp-manager-catalogue">
            <div className="cp-manager-filters">
                <TextField label="Search products" size="small" value={query} onChange={event => {
                    setQuery(event.target.value); setPage(0);
                }} slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchRounded fontSize="small" /></InputAdornment> } }} />
                <TextField select label="Product category" size="small" value={category} slotProps={{ select: { displayEmpty: true }, inputLabel: { shrink: true } }} onChange={event => {
                    setCategory(event.target.value); setPage(0);
                }}>
                    <MenuItem value="">All categories</MenuItem>
                    {options(categories.data).map(option => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
                </TextField>
                {!products.loading && !products.error && <Typography className="cp-manager-result-count" variant="body2" color="text.secondary" role="status">
                    {visible.length} {visible.length === 1 ? "product" : "products"}
                </Typography>}
            </div>
            {products.loading ? <div className="cp-manager-filter-state"><LoadingState>Loading products…</LoadingState></div>
                : products.error ? <div className="cp-manager-filter-state">
                    <Alert severity="error" action={<Button onClick={products.refresh}>Try again</Button>}>{products.error}</Alert>
                </div> : !visible.length ? <div className="cp-manager-filter-state">
                    <EmptyState icon={SearchRounded}
                        action={(query || category) ? <Button onClick={() => { setQuery(""); setCategory(""); setPage(0); }}>Clear filters</Button> : null}>
                        {query || category ? "No products match your filters." : "No products."}
                    </EmptyState>
                </div> : <>
                    <div className="cp-manager-product-columns" aria-hidden="true">
                        <span>Products</span><span>Categories</span><span>Price</span><span>Status</span><span>Actions</span>
                    </div>
                    <ul className="cp-manager-product-list" aria-label="Product list">
                        {visible.slice(firstIndex, firstIndex + productsPerPage).map(item => {
                            const available = item.status === "PRODUCT_AVAILABLE";
                            return (
                                <li className="cp-manager-product-row" key={item.id}>
                                    <div className="cp-manager-product">
                                        <ProductThumbnail product={item} />
                                        <Typography fontWeight={700} className="cp-manager-product-name">{item.name}</Typography>
                                        <Typography variant="body2" color="text.secondary">{salesUnits[item.salesUnit ?? "UNCLASSIFIED"]}</Typography>
                                    </div>
                                    <Typography className="cp-manager-product-category" variant="body2" color="text.secondary">{item.productCategory.name}</Typography>
                                    <Typography className="cp-manager-product-price" fontWeight={700}>{currency(item.price)}</Typography>
                                    <div className="cp-manager-product-status">
                                        <Chip size="small" color={available ? "success" : "warning"} label={available ? "Available" : "Out of stock"} />
                                    </div>
                                    <div className="cp-manager-product-actions">
                                        <Button size="small" variant="outlined" color="secondary" onClick={() => onEdit(item)}>Edit product</Button>
                                        <Button size="small" color={available ? "secondary" : "primary"} onClick={() => onToggle(item, available)}>{available ? "Mark out of stock" : "Make available"}</Button>
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                    {pages > 1 && <div className="cp-manager-paging">
                        <Typography variant="body2" color="text.secondary">{firstIndex + 1}–{Math.min(firstIndex + productsPerPage, visible.length)} of {visible.length}</Typography>
                        <Stack direction="row" gap={1}>
                            <Button size="small" variant="outlined" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Previous</Button>
                            <Button size="small" variant="outlined" disabled={currentPage >= pages - 1} onClick={() => setPage(currentPage + 1)}>Next</Button>
                        </Stack>
                    </div>}
                </>}
        </div>
    </>;
}

function MenuPanel({ tab, edit, run, products, categories, variantGroups }) {
    const [productId, setProductId] = useState("");
    const variations = useData(productId ? `/api/admin/productvariant/${productId}` : null);

    function editName(item, variant) {
        const root = variant ? "/api/admin/productvariant" : "/api/admin/productcategory";
        const resource = variant ? variantGroups : categories;
        edit({
            title: `${item ? "Edit" : "Add"} ${variant ? "option group" : "category"}`,
            initial: { name: item?.name ?? "" },
            fields: [field("name", "Name", { maxLength: 100 })],
            submit: async ({ name }) => {
                if (item) await api.put(variant ? `${root}/${item.id}/updatecategory` : `${root}/${item.id}/update`, { name });
                else await api.post(variant ? `${root}/newcategory` : `${root}/new`, variant ? { name } : { productCategoryName: name });
                resource.refresh();
            },
        });
    }

    function editProduct(item) {
        edit({
            title: item ? "Edit product" : "Add product",
            initial: {
                name: item?.name ?? "", description: item?.description ?? "", price: item?.price ?? "",
                productCategory: item?.productCategory?.id ?? "", productImage: item?.productImage ?? "/media/cafepulse.png",
                salesUnit: item?.salesUnit ?? "UNCLASSIFIED",
            },
            fields: [field("name", "Product name", { maxLength: 100 }), field("description", "Description", { multiline: true, maxLength: 500 }),
                amount("price", "Price (RM)", "0.01"), field("productCategory", "Category", { options: options(categories.data) }),
                field("salesUnit", "Sales unit", { options: Object.entries(salesUnits).map(([value, label]) => ({ value, label })) }),
                field("productImage", "Image URL", { maxLength: 300 })],
            submit: async values => {
                if (item) await api.put(`/api/product/${item.id}/update`, values);
                else await api.post("/api/product/add", values);
                products.refresh();
            },
        });
    }

    function editVariant(item) {
        edit({
            title: item ? "Edit product option" : "Add product option",
            initial: { category: item?.category.id ?? "", value: item?.value ?? "", extraPrice: item?.extraPrice ?? "0.00" },
            fields: [field("category", "Option groups", { options: options(variantGroups.data) }),
                field("value", "Option name", { maxLength: 120 }), amount("extraPrice", "Extra charge (RM)")],
            submit: async values => {
                const body = { ...values, product: productId };
                if (item) await api.put(`/api/admin/productvariant/${item.id}`, body);
                else await api.post("/api/admin/productvariant/create", body);
                variations.refresh();
            },
        });
    }

    if (tab === "products") return <ProductCatalogue products={products} categories={categories} onEdit={editProduct}
        onToggle={(item, available) => run(async () => {
            await api.put(`/api/product/${item.id}/sale-status`, {
                status: available ? "PRODUCT_OUT_OF_STOCK" : "PRODUCT_AVAILABLE",
            });
            products.refresh();
        })} />;

    if (tab === "categories") return <>
        <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="space-between" alignItems="center" className="cp-section-heading">
            <Typography component="h2" variant="h6">Product category</Typography>
            <Button variant="contained" onClick={() => editName(null, false)}>Add category</Button>
        </Stack>
        <Rows {...categories} empty="No categories.">
            {categories.data.map(item => <Row key={item.id} title={item.name}><Button onClick={() => editName(item, false)}>Edit category</Button></Row>)}
        </Rows>
    </>;

    return <>
        <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="space-between" alignItems="center" className="cp-section-heading">
            <Typography component="h2" variant="h6">Option groups</Typography>
            <Button variant="outlined" onClick={() => editName(null, true)}>Add group</Button>
        </Stack>
        <Rows {...variantGroups} empty="No option groups.">
            {variantGroups.data.map(item => <Row key={item.id} title={item.name}><Button onClick={() => editName(item, true)}>Edit group</Button></Row>)}
        </Rows>
        <Stack direction={{ xs: "column", sm: "row" }} gap={2} className="cp-control-surface">
            <TextField select label="Product for options" value={productId} onChange={event => setProductId(event.target.value)} sx={{ minWidth: { sm: 230 }, flex: 1 }}>
                {options(products.data).map(option => <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>)}
            </TextField>
            <Button variant="contained" disabled={!productId || !variantGroups.data.length} onClick={() => editVariant()}>Add option</Button>
        </Stack>
        {productId && <Rows {...variations} empty="This product has no options.">
            {variations.data.map(item => <Row key={item.id} title={item.value} detail={`${item.category.name} · +${currency(item.extraPrice)}`}>
                <Button onClick={() => editVariant(item)}>Edit option</Button>
            </Row>)}
        </Rows>}
    </>;
}

function AccountsPanel({ edit, run }) {
    const accounts = useData("/api/admin/accounts");
    const password = field("password", "New password", { type: "password", minLength: 12, maxLength: 72 });

    function add() {
        edit({ title: "Add account", initial: { role: "staff", username: "", fullname: "", email: "", password: "" },
            fields: [field("role", "Role", { options: [{ value: "staff", label: "Staff" }, { value: "kiosk", label: "Kiosk" }] }),
                field("username", "Username", { minLength: 3, maxLength: 50 }), field("fullname", "Full name", { maxLength: 200 }),
                field("email", "Email", { type: "email", maxLength: 100 }), password],
            submit: async ({ role, ...body }) => {
                await api.post(role === "staff" ? "/auth/signup" : "/auth/kiosk", body);
                accounts.refresh();
            } });
    }

    function reset(account) {
        edit({ title: `Reset password · ${account.username}`, initial: { password: "" }, fields: [password],
            submit: body => api.put(`/api/admin/accounts/${account.id}/password`, body) });
    }

    return <>
        <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="space-between" alignItems="center" className="cp-section-heading">
            <Typography component="h2" variant="h6">Staff and kiosk accounts</Typography>
            <Button variant="contained" onClick={add}>Add account</Button>
        </Stack>
        <Rows {...accounts} empty="No staff or kiosk accounts.">
            {accounts.data.map(account => <Row key={account.id} title={account.fullname} detail={`${account.email} · ${account.role === "ROLE_STAFF" ? "Staff" : "Kiosk"}`}>
                <Chip size="small" color={account.status === "USER_ACTIVE" ? "success" : "default"} label={account.status === "USER_ACTIVE" ? "Active" : "Inactive"} />
                <Button onClick={() => reset(account)}>Reset password</Button>
                <Button onClick={() => run(async () => {
                    await api.put(`/api/admin/accounts/${account.id}/active`, { active: account.status !== "USER_ACTIVE" });
                    accounts.refresh();
                })}>{account.status === "USER_ACTIVE" ? "Deactivate" : "Activate"}</Button>
            </Row>)}
        </Rows>
    </>;
}

function PromotionsPanel({ edit, products }) {
    const promos = useData("/api/admin/promotions");
    function open(promo) {
        edit({
            title: promo ? "Edit promotion" : "Add promotion",
            initial: { code: promo?.code ?? "", name: promo?.name ?? "", type: promo?.discountType ?? "PERCENT",
                value: promo?.discountValue ?? "", productId: promo?.eligibleProduct?.id ?? "all",
                startDate: promo?.startDate?.slice(0, 16) ?? "", endDate: promo?.endDate?.slice(0, 16) ?? "", active: promo?.isActive === false ? "no" : "yes" },
            fields: [field("name", "Promotion name", { maxLength: 100 }), field("code", "Promo code", { minLength: 2, maxLength: 24 }),
                field("type", "Type", { options: Object.entries(types).map(([value, label]) => ({ value, label })) }),
                { ...amount("value", "Discount amount", "0.01"), show: values => values.type !== "BOGO" },
                field("productId", "Eligible products", { options: [{ value: "all", label: "All products" }, ...options(products.data)] }),
                field("startDate", "Starts (Malaysia time)", { type: "datetime-local" }),
                field("endDate", "Ends (Malaysia time)", { type: "datetime-local" }),
                field("active", "Status", { options: [{ value: "yes", label: "Enabled" }, { value: "no", label: "Disabled" }] })],
            submit: async values => {
                const body = { ...values, active: values.active === "yes", productId: values.productId === "all" ? null : Number(values.productId),
                    value: values.type === "BOGO" ? null : values.value };
                if (promo) await api.put(`/api/admin/promotions/${promo.id}`, body);
                else await api.post("/api/admin/promotions", body);
                promos.refresh();
            },
        });
    }
    return <>
        <Stack direction="row" gap={1} flexWrap="wrap" justifyContent="space-between" alignItems="center" className="cp-section-heading">
            <Typography component="h2" variant="h6">Promotions</Typography>
            <Button variant="contained" onClick={() => open()}>Add promotion</Button>
        </Stack>
        <Rows {...promos} empty="No promotions.">
            {promos.data.map(promo => <Row key={promo.id} title={promo.name}
                detail={`${promo.code ?? "No code"} · ${types[promo.discountType] ?? promo.discountType} · ${promo.eligibleProduct?.name ?? "All products"} · ends ${promo.endDate.replace("T", " ").slice(0, 16)}`}>
                <Chip size="small" color={promo.isActive ? "success" : "default"} label={promo.isActive ? "Enabled" : "Disabled"} />
                <Button onClick={() => open(promo)}>Edit promotion</Button>
            </Row>)}
        </Rows>
    </>;
}

export default function ManagerLayout() {
    const desktopNavigation = useMediaQuery(workspaceRailBreakpoint);
    const [tab, setTab] = useState("products");
    const [editor, setEditor] = useState(null);
    const [error, setError] = useState("");
    const products = useData("/api/product");
    const categories = useData("/api/admin/productcategory");
    const variantGroups = useData("/api/admin/productvariant");
    const alerts = useData("/api/stock/alerts");
    const refreshAlerts = alerts.refresh;

    useEffect(() => {
        const timer = setInterval(refreshAlerts, 15000);
        return () => clearInterval(timer);
    }, [refreshAlerts]);

    function changeTab(next) {
        setTab(next);
        setError("");
    }

    async function run(task) {
        setError("");
        try { await task(); } catch (failure) { setError(errorText(failure)); }
    }

    return (
        <div className="cp-workspace-shell cp-workspace-shell--operations">
            <WorkspaceNavigation label="Manager console" prefix="manager" items={managerTabs} current={tab}
                change={changeTab} vertical={desktopNavigation} mobileLabel="Manager section" />
            <main id="main-content" tabIndex={-1} className="cp-workspace-main">
                <WorkspaceHeading title="Manager console">
                    <WorkspaceLink component={Link} to="/staff">Open staff workspace</WorkspaceLink>
                </WorkspaceHeading>
                <Stack className="cp-manager-panel" component="section" spacing={2.25} id="manager-panel"
                    role={desktopNavigation ? "tabpanel" : "region"}
                    aria-labelledby={desktopNavigation ? `manager-tab-${tab}` : undefined}
                    aria-label={desktopNavigation ? undefined : managerTabs.find(item => item.id === tab).label}>
                    {error && <Alert severity="error">{error}</Alert>}
                    {tab !== "inventory" && alerts.data.length > 0 && <Alert severity="warning"
                        action={<Button color="inherit" onClick={() => changeTab("inventory")}>Open inventory</Button>}>
                        {alerts.data.length} ingredients are at or below their minimum stock level.
                    </Alert>}
                    {["products", "categories", "variants"].includes(tab) && <MenuPanel tab={tab} edit={setEditor} run={run} products={products} categories={categories} variantGroups={variantGroups} />}
                    {tab === "accounts" && <AccountsPanel edit={setEditor} run={run} />}
                    {tab === "promos" && <PromotionsPanel edit={setEditor} products={products} />}
                    {tab === "loyalty" && <LoyaltyPanel />}
                    {tab === "inventory" && <StaffInventoryPage manager onChange={refreshAlerts} />}
                    {tab === "recipes" && <RecipePanel products={products.data} />}
                    {tab === "sales" && <SalesPanel />}
                    {tab === "forecast" && <ForecastPanel products={products.data} />}
                </Stack>
                {editor && <Editor {...editor} close={() => setEditor(null)} />}
            </main>
        </div>
    );
}
