"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { readError } from "@/lib/readError";
import type { ApiError, Category, ManagerProductDetail } from "@/lib/types";

const fields = ["name", "description", "categoryId", "price", "discountPercent", "stock"] as const;

type Values = Record<(typeof fields)[number], string>;

const inputClass = "mt-1 w-full rounded border bg-white px-3 py-2 text-ink focus-visible:outline-2 focus-visible:outline-brand";
const labelClass = "block text-sm font-medium text-ink";

function toValues(product: Omit<ManagerProductDetail, "images">): Values {
    return {
        name: product.name,
        description: product.description,
        categoryId: String(product.category.id),
        price: String(product.price),
        discountPercent: String(product.discountPercent),
        stock: String(product.stock),
    };
}

function toJson(key: keyof Values, value: string) {
    if (key === "name" || key === "description") return value;
    if (value === "") return null;
    return key === "price" || key === "discountPercent" ? value : Number(value);
}

export default function ProductForm({ product, categories }: { product: ManagerProductDetail; categories: Category[] }) {
    const router = useRouter();
    const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
    const [initial, setInitial] = useState(() => toValues(product));
    const [values, setValues] = useState(initial);
    const [pending, setPending] = useState(false);
    const [saved, setSaved] = useState(false);
    const [error, setError] = useState<ApiError["error"] | null>(null);

    useEffect(() => () => clearTimeout(timerRef.current), []);

    const changed = fields.filter((key) => values[key] !== initial[key]);
    const fieldError = (key: keyof Values) => error?.details?.find((detail) => detail.field === key)?.message;
    const field = (key: keyof Values) => ({
        id: `product-${key}`,
        value: values[key],
        onChange: (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
            setValues({ ...values, [key]: event.target.value }),
        className: `${inputClass} ${fieldError(key) ? "border-danger" : "border-line"}`,
    });

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        clearTimeout(timerRef.current);
        setPending(true);
        setSaved(false);
        setError(null);
        const response = await fetch(`/api/manager/products/${product.id}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(Object.fromEntries(changed.map((key) => [key, toJson(key, values[key])]))),
        });
        setPending(false);
        if (!response.ok) {
            setError(await readError(response));
            return;
        }
        const next = toValues(await response.json());
        setInitial(next);
        setValues(next);
        setSaved(true);
        timerRef.current = setTimeout(() => setSaved(false), 2000);
        router.refresh();
    }

    return (
        <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
            <div>
                <label htmlFor="product-name" className={labelClass}>Name</label>
                <input type="text" {...field("name")} />
                {fieldError("name") && <p className="mt-1 text-sm text-danger">{fieldError("name")}</p>}
            </div>
            <div>
                <label htmlFor="product-description" className={labelClass}>Description</label>
                <textarea rows={4} {...field("description")} />
                {fieldError("description") && <p className="mt-1 text-sm text-danger">{fieldError("description")}</p>}
            </div>
            <div>
                <label htmlFor="product-categoryId" className={labelClass}>Category</label>
                <select {...field("categoryId")}>
                    {categories.map((category) => (
                        <option key={category.id} value={category.id}>{category.name}</option>
                    ))}
                </select>
                {fieldError("categoryId") && <p className="mt-1 text-sm text-danger">{fieldError("categoryId")}</p>}
            </div>
            <div className="grid grid-cols-3 gap-3">
                <div>
                    <label htmlFor="product-price" className={labelClass}>Price</label>
                    <input type="text" inputMode="decimal" {...field("price")} />
                    {fieldError("price") && <p className="mt-1 text-sm text-danger">{fieldError("price")}</p>}
                </div>
                <div>
                    <label htmlFor="product-discountPercent" className={labelClass}>Discount %</label>
                    <input type="text" inputMode="decimal" {...field("discountPercent")} />
                    {fieldError("discountPercent") && <p className="mt-1 text-sm text-danger">{fieldError("discountPercent")}</p>}
                </div>
                <div>
                    <label htmlFor="product-stock" className={labelClass}>Stock</label>
                    <input type="text" inputMode="numeric" {...field("stock")} />
                    {fieldError("stock") && <p className="mt-1 text-sm text-danger">{fieldError("stock")}</p>}
                </div>
            </div>
            {error && <p role="alert" className="text-sm font-medium text-danger">{error.message}</p>}
            <div className="flex items-center gap-3">
                <button
                    type="submit"
                    disabled={pending || changed.length === 0}
                    className="rounded bg-brand px-4 py-2 font-semibold text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                >
                    {pending ? "Saving..." : "Save"}
                </button>
                {saved && <p role="status" className="text-sm font-medium text-brand">Saved</p>}
            </div>
        </form>
    );
}
