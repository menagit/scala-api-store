"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { readError } from "@/lib/readError";
import type { ManagerProduct } from "@/lib/types";

type Status = ManagerProduct["status"];

export default function ProductStatusButton({ productId, status, onChange }: {
    productId: number;
    status: Status;
    onChange: (status: Status) => void;
}) {
    const router = useRouter();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleClick() {
        setPending(true);
        setError(null);
        const response = await fetch(`/api/manager/products/${productId}/status`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status: status === "ACTIVE" ? "DISABLED" : "ACTIVE" }),
        });
        setPending(false);
        if (!response.ok) {
            setError((await readError(response)).message);
            return;
        }
        const body: { status: Status } = await response.json();
        onChange(body.status);
        router.refresh();
    }

    return (
        <div className="flex flex-col items-end gap-1">
            <button
                type="button"
                disabled={pending}
                onClick={handleClick}
                className="rounded border border-line px-3 py-1 text-sm font-medium text-ink hover:border-brand hover:text-brand disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
            >
                {status === "ACTIVE" ? "Disable" : "Enable"}
            </button>
            {error && <p role="alert" className="text-sm text-danger">{error}</p>}
        </div>
    );
}
