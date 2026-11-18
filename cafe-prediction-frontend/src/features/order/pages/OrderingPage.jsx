import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, InputAdornment, MenuItem, Stack, TextField, Typography, useMediaQuery } from "@mui/material";
import CheckRounded from "@mui/icons-material/CheckRounded";
import LocalCafeOutlined from "@mui/icons-material/LocalCafeOutlined";
import SearchRounded from "@mui/icons-material/SearchRounded";
import ReceiptLongOutlined from "@mui/icons-material/ReceiptLongOutlined";
import ShoppingBagOutlined from "@mui/icons-material/ShoppingBagOutlined";
import { EmptyState, LoadingState } from "../../../components/WorkspaceUI";
import { useDispatch, useSelector } from "react-redux";
import { currentSession, setCheckoutState, subscribeToSession } from "../../../api/session";
import FoodPreview from "../../food/components/FoodPreview";
import FoodVariant from "../../food/components/FoodVariant";
import CustomerCartPage from "../../cart/pages/CustomerCartPage";
import { addToCart, replaceCartItem } from "../../cart/store/slice/CartSlice";
import { fetchAllAvailableFood } from "../../food/store/thunk/FoodThunk";

export default function OrderingPage() {
    const dispatch = useDispatch();
    const { availableFoodList, loading, error } = useSelector(state => state.food);
    const cart = useSelector(state => state.cart.cartList);
    const session = useSyncExternalStore(subscribeToSession, currentSession);
    const { draft, receipt } = session?.checkout ?? {};
    const itemCount = (draft?.cart ?? cart).reduce((total, item) => total + item.quantity, 0);
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState(null);
    const [selection, setSelection] = useState(null);
    const [cartError, setCartError] = useState("");
    const compactMenu = useMediaQuery("(max-width:710px)");
    const results = useRef(null);

    useEffect(() => {
        dispatch(fetchAllAvailableFood());
    }, [dispatch]);

    const categories = [...new Map(availableFoodList.filter(food => food.productCategory)
        .map(food => [food.productCategory.id, food.productCategory.name])).entries()];
    const keyword = query.trim().toLocaleLowerCase();
    const menu = availableFoodList.filter(food => (!category || food.productCategory?.id === category)
        && [food.name, food.description].some(value => value?.toLocaleLowerCase().includes(keyword)));

    function changeCategory(next) {
        setCategory(next);
        results.current?.scrollTo(0, 0);
    }

    function edit(item) {
        const product = availableFoodList.find(food => food.id === item.productId);
        if (product) {
            setCartError("");
            setSelection({ product, item });
        } else {
            setCartError(`${item.productName} is no longer on the menu. Remove it from your cart.`);
        }
    }

    function newOrder() {
        setCheckoutState(session.refreshToken, null);
        setCategory(null);
        setQuery("");
        setCartError("");
    }

    return (
        <div className="cp-workspace-shell cp-workspace-shell--kiosk">
            <nav className="cp-workspace-rail" aria-label="Kiosk navigation">
                <a href="#menu-panel" className="cp-pos-nav-link cp-pos-nav-link--active" aria-current="page">
                    <ReceiptLongOutlined fontSize="small" />
                    <span>Point of Sale</span>
                </a>
                <a href="#troli" className="cp-pos-nav-link">
                    <ShoppingBagOutlined fontSize="small" />
                    <span>Cart</span>
                    {itemCount > 0 && <span className="cp-pos-nav-count" aria-label={`${itemCount} item`}>{itemCount}</span>}
                </a>
            </nav>
            <main id="main-content" tabIndex={-1} className="cp-workspace-main">
                <header className="cp-pos-heading">
                    <Typography component="h1" variant="h4">Point of Sale (POS)</Typography>
                    <Typography variant="body2" color="text.secondary">Choose from the menu, review your order and pay at the counter.</Typography>
                </header>
                <div className="cp-pos-panels">
                    <section id="menu-panel" tabIndex={-1} className="cp-pos-catalog" aria-labelledby="menu-title">
                        <div className="cp-pos-catalog-toolbar">
                            <Typography component="h2" id="menu-title" variant="h6">Menu</Typography>
                            <TextField label="Search menu" value={query} onChange={event => {
                                    setQuery(event.target.value);
                                    results.current?.scrollTo(0, 0);
                                }} fullWidth
                                size="small" color="secondary"
                                slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchRounded fontSize="small" /></InputAdornment> } }} />
                        </div>
                        <div className="cp-pos-category-bar">
                            {compactMenu && categories.length > 3 ? (
                                <TextField select label="Menu category" value={category ?? ""} size="small" color="secondary" fullWidth
                                    onChange={event => changeCategory(event.target.value === "" ? null : event.target.value)}>
                                    <MenuItem value="">All categories</MenuItem>
                                    {categories.map(([id, name]) => <MenuItem key={id} value={id}>{name}</MenuItem>)}
                                </TextField>
                            ) : (
                                <Stack direction="row" gap={1} flexWrap="wrap" className="cp-pos-filters" role="group" aria-label="Menu category">
                                    <Button variant={category === null ? "contained" : "outlined"} aria-pressed={category === null} onClick={() => changeCategory(null)}>All</Button>
                                    {categories.map(([id, name]) => (
                                        <Button key={id} variant={category === id ? "contained" : "outlined"} aria-pressed={category === id} onClick={() => changeCategory(id)}>{name}</Button>
                                    ))}
                                </Stack>
                            )}
                        </div>
                        <div className="cp-pos-catalog-body" ref={results}>
                            {loading ? <LoadingState>Loading menu…</LoadingState> : error ? (
                                <Alert severity="error" action={<Button color="inherit" onClick={() => dispatch(fetchAllAvailableFood())}>Try again</Button>}>
                                    The menu couldn't be loaded.
                                </Alert>
                            ) : menu.length ? (
                                <div className="cp-pos-products">
                                    {menu.map(food => <FoodPreview key={food.id} food={food} disabled={Boolean(draft)}
                                        onSelect={product => { setCartError(""); setSelection({ product }); }} />)}
                                </div>
                            ) : <EmptyState icon={LocalCafeOutlined}
                                action={(keyword || category) ? <Button onClick={() => { setQuery(""); setCategory(null); }}>Clear filters</Button> : null}>
                                {keyword || category ? "No products match your filters." : "No products are available."}
                            </EmptyState>}
                        </div>
                    </section>
                    <div className="cp-pos-order-pane">
                        {cartError && <Alert severity="warning" sx={{ mb: 1 }}>{cartError}</Alert>}
                        <CustomerCartPage cart={draft?.cart ?? cart} draft={draft} onEdit={edit} itemCount={itemCount} />
                    </div>
                </div>
                {selection && (
                    <FoodVariant product={selection.product} item={selection.item}
                        onSave={item => dispatch(selection.item ? replaceCartItem(item) : addToCart(item))}
                        onClose={() => setSelection(null)} />
                )}
                {receipt && (
                    <Dialog open fullWidth maxWidth="xs" className="cp-pos-dialog" aria-labelledby="receipt-heading">
                        <DialogTitle id="receipt-heading">Order received</DialogTitle>
                        <DialogContent dividers>
                            <div className="cp-receipt">
                                <span className="cp-receipt-check" aria-hidden="true"><CheckRounded /></span>
                                <Typography color="text.secondary">Queue number</Typography>
                                <Typography component="p" className="cp-receipt-number">{receipt.orderSequence}</Typography>
                            </div>
                            {receipt.promoDiscount > 0 && <Typography sx={{ mt: 1 }}>Promotion {receipt.promoCode} −RM {Number(receipt.promoDiscount).toFixed(2)}</Typography>}
                            {receipt.pointsDiscount > 0 && <Typography>Redeemed {receipt.pointsRedeemed} points −RM {Number(receipt.pointsDiscount).toFixed(2)}</Typography>}
                            <Stack className="cp-cart-total" direction="row" justifyContent="space-between" gap={1}>
                                <Typography>Confirmed total</Typography>
                                <Typography component="strong" fontWeight={700}>RM {Number(receipt.totalAmount).toFixed(2)}</Typography>
                            </Stack>
                        </DialogContent>
                        <DialogActions><Button variant="contained" className="cp-pos-primary" onClick={newOrder}>New order</Button></DialogActions>
                    </Dialog>
                )}
            </main>
        </div>
    );
}
