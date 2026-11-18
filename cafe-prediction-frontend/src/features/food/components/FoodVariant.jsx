import { useEffect, useId, useState } from "react";
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, FormControlLabel,
    FormLabel, Radio, RadioGroup, Stack, TextField, Typography } from "@mui/material";
import AddShoppingCartOutlined from "@mui/icons-material/AddShoppingCartOutlined";
import { LoadingState } from "../../../components/WorkspaceUI";
import { useDispatch, useSelector } from "react-redux";
import { fetchProductVariant } from "../store/thunk/FoodThunk";
import { productVariantClassification } from "../util/productVariantClassification";

function OptionsForm({ product, variants, item, onSave, onClose, formId }) {
    const { variantGroups } = productVariantClassification(variants);
    const [chosen, setChosen] = useState(() => Object.fromEntries(variantGroups.map(group => [
        group.groupName,
        item ? item.options.find(option => option.group === group.groupName)?.id ?? ""
            : group.options.find(option => option.isDefault)?.id ?? "",
    ])));
    const [description, setDescription] = useState(item?.description ?? "");
    const options = variantGroups.flatMap(group => group.options
        .filter(option => option.id === chosen[group.groupName])
        .map(option => ({ id: option.id, group: group.groupName, label: option.label, extraPrice: Number(option.extraPrice) })))
        .sort((first, second) => first.id - second.id);
    const price = Number(product.price) + options.reduce((sum, option) => sum + option.extraPrice, 0);

    function submit(event) {
        event.preventDefault();
        onSave({
            cartId: item?.cartId ?? crypto.randomUUID(),
            productId: product.id,
            productName: product.name,
            price,
            options,
            description: description.trim(),
            quantity: item?.quantity ?? 1,
        });
        onClose();
    }

    return (
        <>
            <DialogContent dividers>
                <Stack component="form" id={formId} spacing={2.5} onSubmit={submit}>
                    <div className="cp-dialog-summary">
                        <Typography variant="body2" color="text.secondary">Unit price</Typography>
                        <Typography component="strong">RM {price.toFixed(2)}</Typography>
                    </div>
                    {variantGroups.map(group => (
                        <FormControl key={group.groupName}>
                            <FormLabel id={`${formId}-${group.groupName.replace(/\W/g, "-")}`}>{group.groupName}</FormLabel>
                            <RadioGroup className="cp-choice-list" aria-labelledby={`${formId}-${group.groupName.replace(/\W/g, "-")}`} value={chosen[group.groupName] ?? ""} onChange={event => {
                                const value = event.target.value;
                                setChosen(previous => ({ ...previous, [group.groupName]: value ? Number(value) : "" }));
                            }}>
                                <FormControlLabel value="" control={<Radio />} label="Standard option" />
                                {group.options.map(option => (
                                    <FormControlLabel key={option.id} value={option.id} control={<Radio />}
                                        label={`${option.label} (+RM ${Number(option.extraPrice).toFixed(2)})`} />
                                ))}
                            </RadioGroup>
                        </FormControl>
                    ))}
                    <TextField label="Order notes (optional)" value={description} multiline minRows={2}
                        onChange={event => setDescription(event.target.value)} slotProps={{ htmlInput: { maxLength: 200 } }} />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button type="submit" form={formId} variant="contained" className="cp-pos-primary" endIcon={!item ? <AddShoppingCartOutlined /> : undefined}>{item ? "Save options" : "Add to cart"}</Button>
            </DialogActions>
        </>
    );
}

export default function FoodVariant({ product, item, onSave, onClose }) {
    const dispatch = useDispatch();
    const formId = useId();
    const { productVariant, variantLoading, variantError, variantProductId } = useSelector(state => state.food);

    useEffect(() => {
        dispatch(fetchProductVariant(product.id));
    }, [dispatch, product.id]);

    const isLoading = variantLoading || variantProductId !== product.id || (!productVariant && !variantError);
    return (
        <Dialog open onClose={onClose} fullWidth maxWidth="xs" className="cp-pos-dialog" aria-labelledby={`${formId}-title`}>
            <DialogTitle id={`${formId}-title`}>{product.name}</DialogTitle>
            {isLoading ? (
                <DialogContent dividers><LoadingState>Loading options…</LoadingState></DialogContent>
            ) : variantError ? (
                <>
                    <DialogContent><Alert severity="error">Product options couldn't be loaded.</Alert></DialogContent>
                    <DialogActions>
                        <Button onClick={onClose}>Close</Button>
                        <Button onClick={() => dispatch(fetchProductVariant(product.id))}>Try again</Button>
                    </DialogActions>
                </>
            ) : <OptionsForm product={product} variants={productVariant} item={item} onSave={onSave} onClose={onClose} formId={formId} />}
        </Dialog>
    );
}
