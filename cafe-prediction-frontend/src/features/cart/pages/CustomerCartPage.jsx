import { Button, Typography } from "@mui/material";
import ShoppingBagOutlined from "@mui/icons-material/ShoppingBagOutlined";
import { EmptyState } from "../../../components/WorkspaceUI";
import { useDispatch } from "react-redux";
import CartDetails from "../components/CartDetails";
import ItemCart from "../components/ItemCart";
import { clearCart } from "../store/slice/CartSlice";

export default function CustomerCartPage({ cart, draft, onEdit, itemCount }) {
    const dispatch = useDispatch();
    return (
        <aside id="troli" tabIndex={-1} aria-label="Order cart" className="cp-pos-cart">
            <div className="cp-pos-cart-heading">
                <ShoppingBagOutlined aria-hidden="true" fontSize="small" />
                <Typography component="h2" variant="h6">Order</Typography>
                <span className="cp-pos-item-count" role="status">{itemCount} item</span>
                {cart.length > 0 && <Button size="small" color="secondary" disabled={Boolean(draft)}
                    onClick={() => dispatch(clearCart())}>Clear cart</Button>}
            </div>
            <CartDetails cart={cart} draft={draft}>
                {cart.length ? (
                    <ul className="cp-pos-cart-items">
                        {cart.map(item => <ItemCart key={item.cartId} item={item} disabled={Boolean(draft)} onEdit={onEdit} />)}
                    </ul>
                ) : <EmptyState icon={ShoppingBagOutlined} compact>Your cart is empty.</EmptyState>}
            </CartDetails>
        </aside>
    );
}
