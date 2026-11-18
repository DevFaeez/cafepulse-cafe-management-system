export const units = { L: "L", ML: "mL", KG: "kg", G: "g", PCS: "pc", PACK: "pack" };
export const kinds = {
    OPENING: "Opening count", RECEIPT: "Stock received", USAGE: "Usage",
    ADJUSTMENT: "Count adjustment", WASTE: "Waste", ORDER_USAGE: "Recipe deduction", ORDER_RETURN: "Recipe return",
};
export const amount = value => Number(value).toLocaleString("en-MY", { maximumFractionDigits: 3 });
export const stockError = failure => failure.response?.data?.description
    ?? failure.response?.data?.message ?? "Couldn't connect to inventory. Try again.";
