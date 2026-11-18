export function productVariantClassification(data) {
    if (!Array.isArray(data)) return { variantGroups: [] };

    // Group by category name
    const groupedVariants = data.reduce((acc, item) => {
        const categoryName = item.category.name;
        if (!acc[categoryName]) {
            acc[categoryName] = [];
        }
        acc[categoryName].push({
            // id: `${categoryName.toLowerCase().replace(/\s+/g, "_")}_${item.value.toLowerCase()}`,
            id: item.id,
            label: item.value,
            extraPrice: item.extraPrice,
            isDefault: item.defaultVariant
        });
        return acc;
    }, {});[]

    const variantGroups = Object.entries(groupedVariants).map(([groupName, options]) => ({
        groupName,
        options,
    }));


    return { variantGroups };
}
