"use client";

import { useRef, useState } from "react";
import ManagerProductCards from "@/components/ManagerProductCards";
import ManagerProductsTable from "@/components/ManagerProductsTable";
import ProductDrawer from "@/components/ProductDrawer";
import type { Category, ManagerProduct } from "@/lib/types";

export default function ManagerProductsView({ items, categories }: { items: ManagerProduct[]; categories: Category[] }) {
    const openerRef = useRef<HTMLElement | null>(null);
    const [target, setTarget] = useState<number | "new" | null>(null);
    const [open, setOpen] = useState(false);
    const [openKey, setOpenKey] = useState(0);

    function select(id: number | "new", opener: HTMLElement | null) {
        openerRef.current = opener;
        setTarget(id);
        setOpenKey((key) => key + 1);
        setOpen(true);
    }

    function close() {
        setOpen(false);
        openerRef.current?.focus();
    }

    return (
        <>
            <div className="mt-6 flex justify-end">
                <button
                    type="button"
                    onClick={(event) => select("new", event.currentTarget)}
                    className="rounded bg-brand px-4 py-2 font-semibold text-white hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                >
                    New product
                </button>
            </div>
            <ManagerProductsTable products={items} onSelect={select} />
            <ManagerProductCards products={items} onSelect={select} />
            {target !== null && (
                <ProductDrawer
                    key={openKey}
                    open={open}
                    productId={target === "new" ? null : target}
                    categories={categories}
                    onClose={close}
                />
            )}
        </>
    );
}
