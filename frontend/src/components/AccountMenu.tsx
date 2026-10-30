"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import SignInDrawer from "@/components/SignInDrawer";
import type { Role } from "@/lib/types";

export default function AccountMenu({ role }: { role: Role | null }) {
    const router = useRouter();
    const wrapperRef = useRef<HTMLDivElement>(null);
    const buttonRef = useRef<HTMLButtonElement>(null);
    const [open, setOpen] = useState(false);

    const close = useCallback(() => {
        setOpen(false);
        buttonRef.current?.focus();
    }, []);

    useEffect(() => {
        if (!open || !role) return;
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") close();
        };
        const onMouseDown = (event: MouseEvent) => {
            if (!wrapperRef.current?.contains(event.target as Node)) setOpen(false);
        };
        document.addEventListener("keydown", onKeyDown);
        document.addEventListener("mousedown", onMouseDown);
        return () => {
            document.removeEventListener("keydown", onKeyDown);
            document.removeEventListener("mousedown", onMouseDown);
        };
    }, [open, role, close]);

    async function signOut() {
        await fetch("/api/auth/sign-out", { method: "POST" });
        setOpen(false);
        router.refresh();
    }

    return (
        <div ref={wrapperRef} className="relative">
            <button
                ref={buttonRef}
                type="button"
                aria-label="Account"
                aria-expanded={open}
                onClick={() => setOpen((value) => !value)}
                className={`flex rounded p-1 hover:text-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand ${role ? "text-brand" : "text-muted"}`}
            >
                <svg aria-hidden="true" viewBox="0 0 24 24" className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M4 21c0-4 4-6 8-6s8 2 8 6" />
                </svg>
            </button>
            {role && open && (
                <div className="absolute right-0 mt-2 w-48 rounded border border-line bg-white p-3 shadow-lg">
                    <p className="text-sm text-muted">Signed in as {role === "MANAGER" ? "Manager" : "Client"}</p>
                    <button
                        type="button"
                        onClick={signOut}
                        className="mt-2 w-full rounded px-2 py-1 text-left text-ink hover:bg-surface focus-visible:outline-2 focus-visible:outline-brand"
                    >
                        Sign out
                    </button>
                </div>
            )}
            {!role && <SignInDrawer open={open} onClose={close} />}
        </div>
    );
}
