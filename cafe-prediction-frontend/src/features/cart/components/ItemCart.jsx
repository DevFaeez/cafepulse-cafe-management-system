import { Box, Button, IconButton, Stack, Typography } from "@mui/material";
import ReceiptLongOutlined from "@mui/icons-material/ReceiptLongOutlined";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import RemoveIcon from "@mui/icons-material/Remove";
import AddIcon from "@mui/icons-material/Add";
import { useDispatch } from "react-redux";
import { deleteCartItem, updateQuantity } from "../store/slice/CartSlice";

export default function ItemCart({ item, disabled, onEdit }) {
    const dispatch = useDispatch();
    const { productName, quantity, options, cartId, description } = item;
    function setQuantity(next) {
        dispatch(updateQuantity({ cartId, quantity: next }));
    }

    return (
        <li className="cp-pos-cart-item">
            <Stack direction="row" gap={1} justifyContent="space-between" alignItems="start">
                <Box sx={{ minWidth: 0 }}>
                    <Typography fontWeight={700} sx={{ overflowWrap: "anywhere" }}>{productName}</Typography>
                    {options.length > 0 && <Typography color="text.secondary" variant="body2">
                        {options.map(option => option.label).join(", ")}
                    </Typography>}
                    {description && <Typography color="text.secondary" variant="body2" sx={{ overflowWrap: "anywhere" }}>{description}</Typography>}
                </Box>
                <IconButton className="cp-pos-remove" size="small" aria-label={`Remove ${productName}`} disabled={disabled}
                    onClick={() => dispatch(deleteCartItem(cartId))}><DeleteOutlineIcon fontSize="small" /></IconButton>
            </Stack>
            <Typography className="cp-pos-line-price" color="text.secondary" variant="body2">
                RM {Number(item.price).toFixed(2)} × {quantity} <strong>= RM {(item.price * quantity).toFixed(2)}</strong>
            </Typography>
            <Stack direction="row" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={0.75} sx={{ mt: 0.5 }}>
                <Stack direction="row" alignItems="center" className="cp-pos-stepper">
                    <IconButton size="small" aria-label={`Decrease quantity for ${productName}`} disabled={disabled || quantity <= 1} onClick={() => setQuantity(quantity - 1)}><RemoveIcon fontSize="small" /></IconButton>
                    <Typography aria-label={`Quantity ${productName}`} sx={{ minWidth: 20, textAlign: "center" }}>{quantity}</Typography>
                    <IconButton size="small" aria-label={`Increase quantity for ${productName}`} disabled={disabled || quantity >= 99} onClick={() => setQuantity(quantity + 1)}><AddIcon fontSize="small" /></IconButton>
                </Stack>
                <Button className="cp-pos-order-edit" startIcon={<ReceiptLongOutlined fontSize="small" />}
                    size="small" disabled={disabled} onClick={() => onEdit(item)}>Edit options</Button>
            </Stack>
        </li>
    );
}
