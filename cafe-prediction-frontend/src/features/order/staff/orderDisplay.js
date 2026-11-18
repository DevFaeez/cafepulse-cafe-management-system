const money = new Intl.NumberFormat("en-MY", { style: "currency", currency: "MYR" });

export const paymentMethods = { CASH: "Cash", QR: "QR", CARD: "Card", NO_CHARGE: "No charge" };
export const formatMoney = value => money.format(Number(value));

export const activePayment = order => order.payments.find(payment => !payment.voidedAt && !payment.refundedAt);
