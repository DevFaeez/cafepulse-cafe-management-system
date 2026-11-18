
export function subtotalPrice(cartList) {
    return cartList?.reduce(
        (sum, item) => sum + item.price * item.quantity,
        0
    );
}

export function discountPriceCalculation(cartList, voucherType, voucherApply, point, pointApply) {

    let price  = [];
    let voucherDiscount = 0;
    let pointDiscount = 0;

    cartList.forEach(item => {
        for (let i = 0; i < item.quantity; i++) {
            price.push(item.price);
        }
    });

    price = price.sort((a, b) => a - b);
    // console.log("sorting price: ", price);


    if (voucherApply) {
        switch (voucherType) {
        case "b1f1": {

            const numItem = Math.floor(price.length / 2);
            for (let i = 0; i < numItem; i++) {

                voucherDiscount += price[i];

            }

            // console.log("Discount price voucher: ", voucherDiscount);
            break;
        }

        case "10off": {

            let sumPrice = price.reduce((sum, num) => sum + num, 0);
            voucherDiscount = sumPrice * 0.1;

            // console.log("Discount price voucher: ", voucherDiscount);
            break;
        }

        default:
            break;
    }
    }

    if (pointApply) {
        point = point / 3;
        let updatePoint = point * 2;

        pointDiscount = updatePoint * 0.1;
        // console.log("Discount point: ", Math.round(pointDiscount));

    }

    return voucherDiscount + pointDiscount;


}
