"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { readError } from "@/lib/readError";

const stepClass = "flex h-9 w-9 items-center justify-center rounded-full border border-line text-ink hover:border-brand hover:text-brand disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand";

export default function CartLineControls({ productId, quantity, available }: { productId: number; quantity: number; available: boolean }) {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function send(init: RequestInit) {
        setPending(true);
        setError(null);
        const response = await fetch(`/api/cart/items/${productId}`, init);
        setPending(false);
        if (response.ok) {
            router.refresh();
            return;
        }
        setError((await readError(response)).message);
        if (response.status === 401) router.refresh();
    }

    function update(next: number) {
        send({ method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ quantity: next }) });
    }

    return (
        <div>
            <div className="flex items-center gap-4">
                {available ? (
                    <div className="flex items-center gap-2">
                        <button type="button" aria-label="Decrease quantity" disabled={pending || quantity === 1} onClick={() => update(quantity - 1)} className={stepClass}>
                            −
                        </button>
                        <span className="w-6 text-center">{quantity}</span>
                        <button type="button" aria-label="Increase quantity" disabled={pending} onClick={() => update(quantity + 1)} className={stepClass}>
                            +
                        </button>
                    </div>
                ) : (
                    <span className="opacity-60">Qty {quantity}</span>
                )}
                <button
                    type="button"
                    disabled={pending}
                    onClick={() => send({ method: "DELETE" })}
                    className="rounded text-sm font-medium text-danger hover:underline disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                >
                    Remove
                </button>
            </div>
            {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
        </div>
    );
}
