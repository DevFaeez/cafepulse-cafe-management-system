import { Box, Button, Chip, Paper, Stack, Typography } from "@mui/material";

import { activePayment, formatMoney, paymentMethods } from "./orderDisplay";

const clock = new Intl.DateTimeFormat("en-MY", { hour: "2-digit", minute: "2-digit", timeZone: "Asia/Kuala_Lumpur" });
const timestamp = new Intl.DateTimeFormat("en-MY", {
    dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Kuala_Lumpur",
});

function localOrderTime(order) {
    if (order.acceptedAt) return timestamp.format(new Date(order.acceptedAt));
    // Existing creation times have no offset. They are shown as recorded by the application.
    const time = order.createdAt?.slice(11, 16);
    const date = order.businessDate ?? order.createdAt?.slice(0, 10);
    return [date, time].filter(Boolean).join(" · ");
}

export default function StaffOrderCard({ order, heading, busy, canCorrect, onPayment, onCorrect, onStatus, onCloseOrder }) {
    const payment = activePayment(order);
    const pending = order.status === "ORDER_PENDING";
    const completed = order.status === "ORDER_COMPLETED";
    const preparing = order.status === "ORDER_PREPARING";
    const refunded = order.status === "ORDER_REFUNDED";
    const cancelled = order.status === "ORDER_CANCELLED";
    const canAct = pending || preparing || (completed && canCorrect && payment);

    return (
        <Paper component="article" aria-label={`Queue ${order.orderSequence}`} variant="outlined"
            className="cp-order-ticket">
            <Stack spacing={1.4} useFlexGap>
                <Stack direction="row" justifyContent="space-between" gap={1.25} flexWrap="wrap" alignItems="flex-start">
                    <Box>
                        <Typography component={heading} className="cp-ticket-number">#{order.orderSequence}</Typography>
                        <Typography variant="body2" color="text.secondary">{localOrderTime(order)}</Typography>
                    </Box>
                    <Stack alignItems="flex-end" spacing={0.8}>
                        <Typography className="cp-ticket-price">{formatMoney(order.totalAmount)}</Typography>
                        <Chip size="small" color={refunded ? "info" : cancelled ? "error" : payment ? "success" : "warning"}
                            label={refunded ? "Refunded" : cancelled ? "Cancelled"
                                : payment ? (payment.method === "NO_CHARGE" ? "No charge" : `Paid · ${paymentMethods[payment.method]}`)
                                : (pending || preparing ? "Unpaid" : "No payment recorded")} />
                    </Stack>
                </Stack>

                {order.promoDiscount > 0 && <Typography variant="body2">Promotion {order.promoCode} −{formatMoney(order.promoDiscount)}</Typography>}
                {order.pointsDiscount > 0 && <Typography variant="body2">{order.pointsRedeemed} points −{formatMoney(order.pointsDiscount)}</Typography>}
                <Stack component="ul" spacing={1.15} className="cp-ticket-items">
                    {order.items.map((item, index) => (
                        <Box component="li" key={index}>
                            <Typography fontWeight={700}>{item.quantity} × {item.productName}</Typography>
                            {item.variants.length > 0 && <Typography color="text.secondary" variant="body2">
                                {item.variants.map(variant => `${variant.category}: ${variant.value}`).join(", ")}
                            </Typography>}
                            {item.note && <Typography variant="body2" className="cp-order-note">{item.note}</Typography>}
                        </Box>
                    ))}
                </Stack>

                {(payment || order.preparingAt || order.completedAt || order.closedAt || order.payments.some(record => record.voidedAt)) && (
                    <div className="cp-ticket-audit">
                        {payment && <Typography color="text.secondary" variant="body2">
                            Payment recorded at {clock.format(new Date(payment.recordedAt))} by {payment.recordedBy}.
                        </Typography>}
                        {order.preparingAt && <Typography color="text.secondary" variant="body2">
                            Preparation started at {timestamp.format(new Date(order.preparingAt))} by {order.preparingBy}.
                        </Typography>}
                        {order.completedAt && <Typography color="text.secondary" variant="body2">
                            Completed at {timestamp.format(new Date(order.completedAt))} by {order.completedBy}.
                        </Typography>}
                        {order.payments.filter(record => record.voidedAt).map(record => (
                            <Typography key={record.id} color="text.secondary" variant="body2" sx={{ overflowWrap: "anywhere" }}>
                                Record {paymentMethods[record.method]} corrected by {record.voidedBy} pada {timestamp.format(new Date(record.voidedAt))}. Reason – {record.voidReason}
                            </Typography>
                        ))}

                        {order.closedAt && <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
                            {order.status === "ORDER_REFUNDED" ? "Refund" : "Cancellation"} recorded by {order.closedBy} pada {timestamp.format(new Date(order.closedAt))}. Reason – {order.closeReason}
                        </Typography>}
                    </div>
                )}

                {canAct && <Stack direction="row" gap={1} flexWrap="wrap" className="cp-ticket-actions">
                    {!payment && <Button variant="contained" disabled={busy} onClick={() => onPayment(order)}>Record payment</Button>}
                    {pending && payment && <Button variant="contained" disabled={busy} onClick={() => onStatus(order, "ORDER_PREPARING")}>Start preparing</Button>}
                    {preparing && payment && <Button variant="contained" color="success" disabled={busy} onClick={() => onStatus(order, "ORDER_COMPLETED")}>Mark as ready</Button>}
                    {pending && payment && canCorrect && <Button disabled={busy} onClick={() => onCorrect(order, payment)}>Correct payment</Button>}
                    {pending && !payment && canCorrect && <Button disabled={busy} onClick={() => onCloseOrder(order, false)}>Cancel order</Button>}
                    {payment && canCorrect && <Button disabled={busy} onClick={() => onCloseOrder(order, true)}>Record refund</Button>}
                </Stack>}
            </Stack>
        </Paper>
    );
}
