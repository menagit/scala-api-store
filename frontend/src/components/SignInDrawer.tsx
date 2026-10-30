"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import type { ApiError, FieldError } from "@/lib/types";

const inputClass = "mt-1 w-full rounded border px-3 py-2 text-ink focus-visible:outline-2 focus-visible:outline-brand";

export default function SignInDrawer({ open, onClose }: { open: boolean; onClose: () => void }) {
    const router = useRouter();
    const dialogRef = useRef<HTMLDialogElement>(null);
    const emailRef = useRef<HTMLInputElement>(null);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [pending, setPending] = useState(false);
    const [error, setError] = useState<{ message: string; details?: FieldError[] } | null>(null);

    useEffect(() => {
        const dialog = dialogRef.current;
        if (!dialog) return;
        if (!open) {
            if (dialog.open) dialog.close();
            return;
        }
        if (!dialog.open) dialog.showModal();
        emailRef.current?.focus();
        document.body.style.overflow = "hidden";
        return () => {
            document.body.style.overflow = "";
        };
    }, [open]);

    const close = () => dialogRef.current?.close();
    const fieldError = (field: string) => error?.details?.find((detail) => detail.field === field)?.message;

    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setPending(true);
        setError(null);
        const response = await fetch("/api/auth/sign-in", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email, password }),
        });
        setPending(false);
        setPassword("");
        if (response.ok) {
            setEmail("");
            close();
            router.refresh();
            return;
        }
        const body: ApiError | null = await response.json().catch(() => null);
        setError(body?.error ?? { message: "Something went wrong. Try again." });
    }

    return (
        <dialog
            ref={dialogRef}
            aria-labelledby="sign-in-title"
            onClose={onClose}
            onClick={(event) => {
                if (event.target === event.currentTarget) close();
            }}
            className="ml-auto h-full max-h-none w-full max-w-none bg-white text-ink-soft shadow-xl sm:w-96 translate-x-full open:translate-x-0 starting:open:translate-x-full transition-[translate,display,overlay] transition-discrete duration-200 ease-out motion-reduce:transition-none backdrop:bg-transparent open:backdrop:bg-ink/50 starting:open:backdrop:bg-transparent backdrop:transition-[background-color,display,overlay] backdrop:transition-discrete backdrop:duration-200 backdrop:ease-out motion-reduce:backdrop:transition-none"
        >
            <div className="flex h-full flex-col overflow-y-auto p-6">
                <div className="flex items-center justify-between">
                    <h2 id="sign-in-title" className="text-xl font-semibold text-ink">Sign in</h2>
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

                <form onSubmit={handleSubmit} noValidate className="mt-6 flex flex-col gap-4">
                    <div>
                        <label htmlFor="sign-in-email" className="block text-sm font-medium text-ink">Email address</label>
                        <input
                            ref={emailRef}
                            id="sign-in-email"
                            type="email"
                            autoComplete="username"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            className={`${inputClass} ${fieldError("email") ? "border-danger" : "border-line"}`}
                        />
                        {fieldError("email") && <p className="mt-1 text-sm text-danger">{fieldError("email")}</p>}
                    </div>
                    <div>
                        <label htmlFor="sign-in-password" className="block text-sm font-medium text-ink">Password</label>
                        <input
                            id="sign-in-password"
                            type="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            className={`${inputClass} ${fieldError("password") ? "border-danger" : "border-line"}`}
                        />
                        {fieldError("password") && <p className="mt-1 text-sm text-danger">{fieldError("password")}</p>}
                    </div>
                    {error && <p role="alert" className="text-sm font-medium text-danger">{error.message}</p>}
                    <button
                        type="submit"
                        disabled={pending}
                        className="rounded bg-brand px-4 py-2 font-semibold text-white hover:opacity-90 disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                    >
                        {pending ? "Signing in..." : "Sign in"}
                    </button>
                </form>

                <p className="mt-6 border-t border-line pt-6 text-sm text-muted">
                    No account yet? <span className="cursor-not-allowed font-medium">Create an account</span>
                </p>
            </div>
        </dialog>
    );
}
