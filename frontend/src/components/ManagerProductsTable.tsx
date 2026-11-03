"use client";

import StatusBadge from "@/components/StatusBadge";
import type { ManagerProduct } from "@/lib/types";

const cell = "py-3 pr-4";

export default function ManagerProductsTable({ products, onSelect }: {
    products: ManagerProduct[];
    onSelect: (id: number, opener: HTMLElement | null) => void;
}) {
    return (
        <table className="mt-6 hidden w-full text-left text-sm md:table">
            <thead className="border-b border-line text-muted">
                <tr>
                    <th className={`${cell} font-medium`}><span className="sr-only">Image</span></th>
                    <th className={`${cell} font-medium`}>Name</th>
                    <th className={`${cell} font-medium`}>Category</th>
                    <th className={`${cell} font-medium`}>Price</th>
                    <th className={`${cell} font-medium`}>Stock</th>
                    <th className={`${cell} font-medium`}>Status</th>
                    <th className={`${cell} font-medium`}>Updated</th>
                </tr>
            </thead>
            <tbody>
                {products.map((product) => (
                    <tr
                        key={product.id}
                        onClick={(event) => onSelect(product.id, event.currentTarget.querySelector("button"))}
                        className="cursor-pointer border-b border-line hover:bg-surface"
                    >
                        <td className={cell}>
                            <div className="flex h-12 w-12 items-center justify-center overflow-hidden rounded bg-surface">
                                {product.images.length > 0 ? (
                                    // eslint-disable-next-line @next/next/no-img-element
                                    <img src={product.images[0].thumbUrl} alt="" className="h-full w-full object-contain" />
                                ) : (
                                    <span className="text-center text-xs leading-tight text-muted">No image</span>
                                )}
                            </div>
                        </td>
                        <td className={cell}>
                            <button
                                type="button"
                                className="text-left font-medium text-ink hover:text-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                            >
                                {product.name}
                            </button>
                        </td>
                        <td className={cell}>{product.category.name}</td>
                        <td className={cell}>
                            {product.discountPercent > 0 ? (
                                <>
                                    <span className="font-semibold text-brand">${product.discountedPrice.toFixed(2)}</span>{" "}
                                    <span className="text-muted line-through">${product.price.toFixed(2)}</span>
                                </>
                            ) : (
                                <span className="font-semibold text-brand">${product.price.toFixed(2)}</span>
                            )}
                        </td>
                        <td className={cell}>{product.stock}</td>
                        <td className={cell}><StatusBadge status={product.status} /></td>
                        <td className={`${cell} text-muted`}>{product.updatedAt.slice(0, 10)}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
