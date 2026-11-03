"use client";

import StatusBadge from "@/components/StatusBadge";
import type { ManagerProduct } from "@/lib/types";

export default function ManagerProductCards({ products, onSelect }: {
    products: ManagerProduct[];
    onSelect: (id: number, opener: HTMLElement | null) => void;
}) {
    return (
        <ul className="mt-6 flex flex-col gap-3 md:hidden">
            {products.map((product) => (
                <li
                    key={product.id}
                    onClick={(event) => onSelect(product.id, event.currentTarget.querySelector("button"))}
                    className="flex cursor-pointer gap-3 rounded-2xl border border-line bg-white p-3 hover:border-brand"
                >
                    <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded bg-surface">
                        {product.images.length > 0 ? (
                            // eslint-disable-next-line @next/next/no-img-element
                            <img src={product.images[0].thumbUrl} alt="" className="h-full w-full object-contain" />
                        ) : (
                            <span className="text-center text-xs leading-tight text-muted">No image</span>
                        )}
                    </div>
                    <div className="flex min-w-0 flex-1 flex-col gap-1 text-sm">
                        <div className="flex items-start justify-between gap-2">
                            <h2 className="font-medium break-words text-ink">
                                <button
                                    type="button"
                                    className="text-left hover:text-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                                >
                                    {product.name}
                                </button>
                            </h2>
                            <StatusBadge status={product.status} />
                        </div>
                        <p className="text-muted">{product.category.name}</p>
                        <p>
                            {product.discountPercent > 0 ? (
                                <>
                                    <span className="font-semibold text-brand">${product.discountedPrice.toFixed(2)}</span>{" "}
                                    <span className="text-muted line-through">${product.price.toFixed(2)}</span>
                                </>
                            ) : (
                                <span className="font-semibold text-brand">${product.price.toFixed(2)}</span>
                            )}
                        </p>
                        <p className="text-muted">
                            Stock {product.stock} · Updated {product.updatedAt.slice(0, 10)}
                        </p>
                    </div>
                </li>
            ))}
        </ul>
    );
}
