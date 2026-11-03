"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { readError } from "@/lib/readError";

export default function DeleteProductButton({ productId, onDeleted }: { productId: number; onDeleted: () => void }) {
    const router = useRouter();
    const [confirm, setConfirm] = useState(false);
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleClick(event: React.MouseEvent<HTMLButtonElement>) {
        if (!confirm) {
            event.currentTarget.focus();
            setConfirm(true);
            setError(null);
            return;
        }
        setPending(true);
        const response = await fetch(`/api/manager/products/${productId}`, { method: "DELETE" });
        setPending(false);
        if (!response.ok) {
            setConfirm(false);
            setError((await readError(response)).message);
            return;
        }
        router.refresh();
        onDeleted();
    }

    return (
        <div className="mt-8 border-t border-line pt-6">
            <button
                type="button"
                disabled={pending}
                onClick={handleClick}
                onBlur={() => setConfirm(false)}
                className={`rounded border border-danger px-4 py-2 font-semibold disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-danger ${confirm ? "bg-danger text-white" : "bg-white text-danger"}`}
            >
                {pending ? "Deleting..." : confirm ? "Confirm delete" : "Delete product"}
            </button>
            {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
        </div>
    );
}
