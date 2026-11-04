import CartLineControls from "@/components/CartLineControls";
import type { CartLine } from "@/lib/types";

export default function CartLineItem({ line }: { line: CartLine }) {
    const greyed = line.available ? undefined : "opacity-60";

    return (
        <li className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
                <div className={greyed}>
                    <h2 className="font-medium text-ink-soft">{line.name}</h2>
                    {line.discountPercent > 0 ? (
                        <div className="flex flex-wrap items-baseline gap-x-2">
                            <span className="font-semibold text-brand">${line.discountedPrice.toFixed(2)}</span>
                            <span className="text-sm text-muted line-through">${line.price.toFixed(2)}</span>
                            <span className="text-sm text-muted">{line.discountPercent}% off</span>
                        </div>
                    ) : (
                        <span className="font-semibold text-brand">${line.price.toFixed(2)}</span>
                    )}
                </div>
                {!line.available && <p className="mt-1 text-sm font-medium text-ink-soft">No longer available</p>}
            </div>
            <div className="flex items-center justify-between gap-6 sm:justify-end">
                <CartLineControls productId={line.productId} quantity={line.quantity} available={line.available} />
                {line.available && <span className="font-semibold text-ink">${line.lineTotal.toFixed(2)}</span>}
            </div>
        </li>
    );
}
