"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useSignInDrawer } from "@/components/SignInDrawerProvider";
import type { ApiError, Role } from "@/lib/types";

const labels = { idle: "Add to cart", pending: "Adding...", added: "Added" };

export default function AddToCartButton({ productId, inStock, role }: { productId: number; inStock: boolean; role: Role | null }) {
    const router = useRouter();
    const { openSignIn } = useSignInDrawer();
    const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
    const [status, setStatus] = useState<keyof typeof labels>("idle");
    const [error, setError] = useState<string | null>(null);

    useEffect(() => () => clearTimeout(timerRef.current), []);

    if (role === "MANAGER") return null;

    async function handleClick(event: React.MouseEvent<HTMLButtonElement>) {
        if (!role) {
            openSignIn(event.currentTarget);
            return;
        }
        clearTimeout(timerRef.current);
        setStatus("pending");
        setError(null);
        const response = await fetch("/api/cart/items", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ productId }),
        });
        if (response.ok) {
            setStatus("added");
            timerRef.current = setTimeout(() => setStatus("idle"), 2000);
            router.refresh();
            return;
        }
        setStatus("idle");
        if (response.status === 401) {
            setError("Your session expired. Sign in again.");
            router.refresh();
            return;
        }
        const body: ApiError | null = await response.json().catch(() => null);
        setError(body?.error.message ?? "Something went wrong. Try again.");
    }

    return (
        <div className="mt-3">
            <button
                type="button"
                disabled={!inStock || status === "pending"}
                onClick={handleClick}
                className="w-full rounded bg-brand px-4 py-2 font-semibold text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
                {inStock ? labels[status] : "Out of stock"}
            </button>
            {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
        </div>
    );
}
