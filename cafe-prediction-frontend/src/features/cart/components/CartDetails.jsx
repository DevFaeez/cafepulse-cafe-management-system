import { useEffect, useRef, useState } from "react";
import { Alert, Button, Chip, Stack, TextField, Typography } from "@mui/material";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import { useDispatch } from "react-redux";
import { api, currentSession, setCheckoutState } from "../../../api/session";
import { createNewOrder } from "../../order/store/thunk/OrderThunk";
import { clearCart, restoreCart } from "../store/slice/CartSlice";
import { subtotalPrice } from "../util/cartDetails";

const ringgit = value => `RM ${Number(value).toFixed(2)}`;

export default function CartDetails({ cart, draft, children }) {
    const dispatch = useDispatch();
    const [phone, setPhone] = useState(draft?.order.customerPhone ?? "");
    const [promoCode, setPromo] = useState(draft?.order.promoCode ?? "");
    const [points, setPoints] = useState(draft?.order.points || "");
    const [offers, setOffers] = useState([]);
    const [estimate, setEstimate] = useState(null);
    const [checking, setChecking] = useState(false);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState("");
    const submitting = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        api.get("/api/order/offers", { signal: controller.signal })
            .then(({ data }) => setOffers(data)).catch(() => {});
        return () => controller.abort();
    }, []);

    const order = {
        customerPhone: phone.trim(),
        promoCode: promoCode.trim().toUpperCase(),
        points: Number(points || 0),
        item: cart.map(line => ({
            product: line.productId,
            quantity: line.quantity,
            variantIds: line.options.map(option => option.id),
            note: line.description,
        })),
    };
    const signature = JSON.stringify(order);
    const currentQuote = draft?.quote ?? (estimate?.signature === signature ? estimate.quote : null);
    const usingRewards = Boolean(order.promoCode || order.points);
    const locked = Boolean(draft) || checking;
    const applicableOffers = offers.filter(offer => !offer.productId || cart.some(item => item.productId === offer.productId));

    function valid() {
        if (!/^\+?[0-9]{8,15}$/.test(order.customerPhone.replace(/[ ()-]/g, ""))) {
            setMessage("Enter a valid phone number.");
            return false;
        }
        if (!Number.isInteger(order.points) || order.points < 0 || order.points % 10 !== 0) {
            setMessage("Redeem points in multiples of 10.");
            return false;
        }
        if (cart.length > 100) {
            setMessage("Your cart can contain up to 100 line items.");
            return false;
        }
        return true;
    }

    async function check() {
        if (!valid() || checking || draft) return;
        setChecking(true);
        setMessage("");
        setEstimate(null);
        try {
            const { data } = await api.post("/api/order/quote", order);
            setEstimate({ signature, quote: data });
        } catch (error) {
            setMessage(error.response?.data?.description ?? "We couldn't check your total. Try again.");
        } finally {
            setChecking(false);
        }
    }

    async function submit(event) {
        event.preventDefault();
        if (submitting.current || checking || !cart.length) return;
        const scope = currentSession()?.refreshToken;
        if (!scope || (!draft && !valid())) return;
        if (!draft && usingRewards && !currentQuote) {
            setMessage("Select Check total and points before placing your order.");
            return;
        }

        const pending = draft ?? {
            key: crypto.randomUUID(),
            cart,
            quote: currentQuote,
            order: { ...order, expectedTotal: currentQuote?.totalAmount ?? subtotalPrice(cart).toFixed(2) },
        };

        submitting.current = true;
        setBusy(true);
        setMessage("");
        setCheckoutState(scope, { draft: pending });
        try {
            const receipt = await dispatch(createNewOrder(pending)).unwrap();
            if (setCheckoutState(scope, { receipt })) {
                dispatch(clearCart());
                setPhone("");
                setPromo("");
                setPoints("");
                setEstimate(null);
            }
        } catch (error) {
            if (currentSession()?.refreshToken !== scope) return;
            if ([400, 409].includes(error.status)) {
                dispatch(restoreCart(pending.cart));
                setCheckoutState(scope, null);
                setEstimate(null);
                setMessage(error.message ?? "Review your order details and try again.");
            } else {
                setMessage("We couldn't confirm your order. Try resubmitting the same order.");
            }
        } finally {
            submitting.current = false;
            setBusy(false);
        }
    }

    const subtotal = currentQuote?.subtotal ?? subtotalPrice(cart);

    return (
        <form className="cp-pos-checkout" onSubmit={submit}>
            <div className="cp-pos-cart-scroll">
                {children}
                {cart.length > 0 && <Stack spacing={1.35} className="cp-pos-cart-fields">
                    <TextField label="Phone number" type="tel" value={draft?.order.customerPhone ?? phone}
                        size="small" autoComplete="off" required disabled={locked} color="secondary"
                        onChange={event => setPhone(event.target.value)} slotProps={{ htmlInput: { maxLength: 32 } }} />
                    <div className="cp-pos-cart-rewards">
                        <TextField label="Promo code (optional)" value={draft?.order.promoCode ?? promoCode} size="small" disabled={locked}
                            color="secondary" onChange={event => setPromo(event.target.value)} slotProps={{ htmlInput: { maxLength: 24 } }} />
                        <TextField label="Redeem points (optional)" type="number" value={draft ? draft.order.points || "" : points}
                            helperText="10 points equal RM1." size="small" disabled={locked} color="secondary" onChange={event => setPoints(event.target.value)}
                            slotProps={{ htmlInput: { min: 0, step: 10 } }} />
                    </div>
                    {applicableOffers.length > 0 && !draft && <Stack direction="row" gap={1} flexWrap="wrap">
                        {applicableOffers.map(offer => (
                            <Chip key={offer.code} label={`${offer.code} · ${offer.name}`} clickable disabled={checking}
                                onClick={() => setPromo(offer.code)} size="small" />
                        ))}
                    </Stack>}
                    {currentQuote && <Typography variant="body2" color="text.secondary">
                        {currentQuote.availablePoints} points available. {currentQuote.pointsEarned} points earned when payment is recorded.
                    </Typography>}
                </Stack>}
            </div>
            <div className="cp-pos-checkout-footer">
                {(message || (draft && !busy)) && <div className="cp-pos-checkout-feedback">
                    {message && <Alert severity="error">{message}</Alert>}
                    {draft && !busy && !message && <Alert severity="warning">This order hasn't been confirmed. Resubmit it to check the result.</Alert>}
                </div>}
                <dl className="cp-pos-totals">
                    <div><dt>Subtotal</dt><dd>{ringgit(subtotal)}</dd></div>
                    {currentQuote?.promoDiscount > 0 && <div><dt>Discount {currentQuote.promoCode}</dt><dd>−{ringgit(currentQuote.promoDiscount)}</dd></div>}
                    {currentQuote?.pointsDiscount > 0 && <div><dt>Points redeemed: {currentQuote.pointsRedeemed} points</dt><dd>−{ringgit(currentQuote.pointsDiscount)}</dd></div>}
                    <div className="cp-pos-final-total">
                        <dt>{currentQuote ? "Final total" : "Estimated total"}</dt>
                        <dd>{ringgit(currentQuote?.totalAmount ?? subtotal)}</dd>
                    </div>
                </dl>
                {cart.length > 0 ? (
                    <div className="cp-pos-checkout-actions">
                        {!draft && <Button type="button" variant="outlined" color="secondary" onClick={check} disabled={checking}>
                            {checking ? "Checking…" : "Check total and points"}
                        </Button>}
                        <Button type="submit" variant="contained" className="cp-pos-primary" disabled={busy || checking}
                            endIcon={<ArrowForwardRounded />} aria-busy={busy}>
                            {busy ? "Submitting…" : draft ? "Resubmit" : "Place order"}
                        </Button>
                    </div>
                ) : <Button variant="contained" disabled fullWidth>Place order</Button>}
            </div>
        </form>
    );
}
