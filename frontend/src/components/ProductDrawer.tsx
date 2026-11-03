"use client";

import { useEffect, useRef, useState } from "react";
import DeleteProductButton from "@/components/DeleteProductButton";
import ProductForm from "@/components/ProductForm";
import ProductImages from "@/components/ProductImages";
import ProductStatusButton from "@/components/ProductStatusButton";
import StatusBadge from "@/components/StatusBadge";
import { readError } from "@/lib/readError";
import type { Category, ManagerProductDetail } from "@/lib/types";

export default function ProductDrawer({ open, productId, categories, onClose }: {
    open: boolean;
    productId: number | null;
    categories: Category[];
    onClose: () => void;
}) {
    const dialogRef = useRef<HTMLDialogElement>(null);
    const [currentId, setCurrentId] = useState(productId);
    const [product, setProduct] = useState<ManagerProductDetail | null>(null);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const dialog = dialogRef.current;
        if (!dialog) return;
        if (!open) {
            if (dialog.open) dialog.close();
            return;
        }
        if (!dialog.open) dialog.showModal();
        document.body.style.overflow = "hidden";
        return () => {
            document.body.style.overflow = "";
        };
    }, [open]);

    useEffect(() => {
        if (currentId === null) return;
        fetch(`/api/manager/products/${currentId}`).then(async (response) => {
            if (response.ok) setProduct(await response.json());
            else setError((await readError(response)).message);
        });
    }, [currentId]);

    const close = () => dialogRef.current?.close();

    return (
        <dialog
            ref={dialogRef}
            aria-labelledby="product-drawer-title"
            onClose={onClose}
            onClick={(event) => {
                if (event.target === event.currentTarget) close();
            }}
            className="ml-auto h-full max-h-none w-full max-w-none bg-white text-ink-soft shadow-xl sm:w-[30rem] translate-x-full open:translate-x-0 starting:open:translate-x-full transition-[translate,display,overlay] transition-discrete duration-200 ease-out motion-reduce:transition-none backdrop:bg-transparent open:backdrop:bg-ink/50 starting:open:backdrop:bg-transparent backdrop:transition-[background-color,display,overlay] backdrop:transition-discrete backdrop:duration-200 backdrop:ease-out motion-reduce:backdrop:transition-none"
        >
            <div className="flex h-full flex-col overflow-y-auto p-6">
                <div className="flex items-center justify-between">
                    <h2 id="product-drawer-title" className="text-xl font-semibold text-ink">
                        {currentId === null ? "New product" : "Edit product"}
                    </h2>
                    <button
                        type="button"
                        onClick={close}
                        className="flex items-center gap-1 rounded text-muted hover:text-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                    >
                        <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                            <path d="M6 6l12 12M18 6L6 18" />
                        </svg>
                        Close
                    </button>
                </div>

                {error && <p role="alert" className="mt-6 text-sm font-medium text-danger">{error}</p>}
                {currentId !== null && !product && !error && <p className="mt-6 text-muted">Loading...</p>}
                {currentId === null && <ProductForm product={null} categories={categories} onCreated={setCurrentId} />}
                {product && (
                    <>
                        <div className="mt-4 flex items-start justify-between gap-3">
                            <StatusBadge status={product.status} />
                            <ProductStatusButton
                                productId={product.id}
                                status={product.status}
                                onChange={(status) => setProduct({ ...product, status })}
                            />
                        </div>
                        <ProductForm product={product} categories={categories} onCreated={setCurrentId} />
                        <ProductImages productId={product.id} images={product.images} />
                        <DeleteProductButton productId={product.id} onDeleted={close} />
                    </>
                )}
            </div>
        </dialog>
    );
}
