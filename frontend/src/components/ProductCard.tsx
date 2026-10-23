type ProductCardProps = {
    name: string;
    price: number;
    discountPercent: number;
    discountedPrice: number;
};

export default function ProductCard({
                                        name,
                                        price,
                                        discountPercent,
                                        discountedPrice,
                                    }: ProductCardProps) {
    return (
        <div className="rounded border p-4">
            <h2 className="font-semibold">{name}</h2>
            <p>${price.toFixed(2)}</p>
            {discountPercent > 0 && (
                <p className="text-green-700">
                    {discountPercent}% off, now ${discountedPrice.toFixed(2)}
                </p>
            )}
        </div>
    );
}