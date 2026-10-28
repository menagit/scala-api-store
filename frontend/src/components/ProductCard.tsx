import ImageCarousel from "@/components/ImageCarousel";
import type { ImageUrls } from "@/lib/types";

type ProductCardProps = {
    name: string;
    categoryName: string;
    price: number;
    discountPercent: number;
    discountedPrice: number;
    images: ImageUrls[];
};

export default function ProductCard({
                                        name,
                                        categoryName,
                                        price,
                                        discountPercent,
                                        discountedPrice,
                                        images,
                                    }: ProductCardProps) {
    return (
        <div className="flex flex-col overflow-hidden rounded-2xl border border-line bg-white">
            <div className="aspect-square bg-surface">
                <ImageCarousel name={name} images={images} />
            </div>
            <div className="flex flex-1 flex-col gap-1 p-4">
                <p className="text-sm text-muted">{categoryName}</p>
                <h2 className="line-clamp-2 font-medium text-ink-soft">{name}</h2>
            </div>
            <div className="border-t border-line px-4 py-3">
                {discountPercent > 0 ? (
                    <div className="flex flex-wrap items-baseline gap-x-2">
                        <span className="font-semibold text-brand">${discountedPrice.toFixed(2)}</span>
                        <span className="text-sm text-muted line-through">${price.toFixed(2)}</span>
                        <span className="text-sm text-muted">{discountPercent}% off</span>
                    </div>
                ) : (
                    <span className="font-semibold text-brand">${price.toFixed(2)}</span>
                )}
            </div>
        </div>
    );
}
