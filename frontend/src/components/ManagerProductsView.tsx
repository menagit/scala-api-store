"use client";

import { useRef, useState } from "react";
import ManagerProductCards from "@/components/ManagerProductCards";
import ManagerProductsTable from "@/components/ManagerProductsTable";
import ProductDrawer from "@/components/ProductDrawer";
import type { Category, ManagerProduct } from "@/lib/types";

export default function ManagerProductsView({ items, categories }: { items: ManagerProduct[]; categories: Category[] }) {
    const openerRef = useRef<HTMLElement | null>(null);
    const [productId, setProductId] = useState<number | null>(null);
    const [open, setOpen] = useState(false);
    const [openKey, setOpenKey] = useState(0);

    function select(id: number, opener: HTMLElement | null) {
        openerRef.current = opener;
        setProductId(id);
        setOpenKey((key) => key + 1);
        setOpen(true);
    }

    function close() {
        setOpen(false);
        openerRef.current?.focus();
    }

    return (
        <>
            <ManagerProductsTable products={items} onSelect={select} />
            <ManagerProductCards products={items} onSelect={select} />
            {productId !== null && (
                <ProductDrawer key={openKey} open={open} productId={productId} categories={categories} onClose={close} />
            )}
        </>
    );
}
