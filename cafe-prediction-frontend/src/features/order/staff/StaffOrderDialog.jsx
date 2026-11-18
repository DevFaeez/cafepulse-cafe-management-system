import { useState } from "react";
import { Alert, Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { formatMoney, paymentMethods } from "./orderDisplay";

export default function StaffOrderDialog({ action, busy, error, onClose, onSubmit }) {
    const freeOrder = Number(action.order.totalAmount) === 0;
    const [method, setMethod] = useState(freeOrder ? "NO_CHARGE" : "CASH");
    const [amount, setAmount] = useState(Number(action.order.totalAmount).toFixed(2));
    const [reason, setReason] = useState("");
    const [restoreStock, setRestoreStock] = useState(false);
    const canRestore = action.refund && action.order.status === "ORDER_PREPARING" && action.order.recipeStockDeducted;
    const isCorrection = Boolean(action.payment);
    const isClosure = action.refund !== undefined;
    const needsReason = isCorrection || isClosure;
    const title = isClosure ? (action.refund ? "Record refund" : "Cancel order")
        : isCorrection ? "Correct payment" : "Record payment";

    function submit(event) {
        event.preventDefault();
        const revision = action.order.revision;
        if (isClosure) {
            onSubmit({ revision, refund: action.refund, reason: reason.trim(), restoreStock });
        } else if (isCorrection) {
            onSubmit({ revision, paymentId: action.payment.id, reason: reason.trim() });
        } else {
            onSubmit({ revision, method, amount });
        }
    }

    return (
        <Dialog open onClose={busy ? undefined : onClose} fullWidth maxWidth="xs" aria-labelledby="staff-payment-title">
            <form onSubmit={submit}>
                <DialogTitle id="staff-payment-title">{title}</DialogTitle>
                <DialogContent dividers>
                    <Stack spacing={2}>
                        <div className="cp-dialog-summary cp-workspace-dialog-summary">
                            <Typography variant="body2" color="text.secondary">Queue #{action.order.orderSequence}</Typography>
                            <Typography component="strong">{formatMoney(action.order.totalAmount)}</Typography>
                        </div>
                        {needsReason ? <>
                            <Typography variant="body2">{isClosure
                                ? (action.refund && !freeOrder ? "Confirm the customer was refunded using the original payment method." : "Enter a reason to save in the order history.")
                                : "Verify the amount received before recording the payment again."}</Typography>
                            <TextField required multiline minRows={2} label={isClosure ? "Reason for closing" : "Reason for correction"} value={reason}
                                onChange={event => setReason(event.target.value)} slotProps={{ htmlInput: { maxLength: 200 } }} />
                            {canRestore && <FormControlLabel label="Return all unused ingredients to stock"
                                control={<Checkbox checked={restoreStock} onChange={event => setRestoreStock(event.target.checked)} />} />}
                        </> : <>
                            <TextField select label="Payment method" value={method} onChange={event => setMethod(event.target.value)}>
                                {Object.entries(paymentMethods).filter(([value]) => (value === "NO_CHARGE") === freeOrder).map(([value, label]) => <MenuItem key={value} value={value}>{label}</MenuItem>)}
                            </TextField>
                            <TextField required label="Amount received (RM)" type="number" value={amount}
                                onChange={event => setAmount(event.target.value)}
                                slotProps={{ htmlInput: { min: freeOrder ? "0" : "0.01", step: "0.01" } }} />
                        </>}
                        {error && <Alert severity="error">{error}</Alert>}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose} disabled={busy}>Cancel</Button>
                    <Button type="submit" variant="contained" color={needsReason ? "error" : "primary"} disabled={busy || (needsReason && !reason.trim())}>
                        {isClosure ? "Confirm" : isCorrection ? "Confirm correction" : "Confirm payment"}
                    </Button>
                </DialogActions>
            </form>
        </Dialog>
    );
}
