"use client";

import { createContext, useContext, useRef, useState } from "react";
import SignInDrawer from "@/components/SignInDrawer";

type SignInDrawerContextValue = {
    openSignIn: (opener: HTMLElement) => void;
    closeSignIn: () => void;
};

const SignInDrawerContext = createContext<SignInDrawerContextValue | null>(null);

export function useSignInDrawer() {
    return useContext(SignInDrawerContext)!;
}

export default function SignInDrawerProvider({ children }: { children: React.ReactNode }) {
    const openerRef = useRef<HTMLElement | null>(null);
    const [open, setOpen] = useState(false);

    function openSignIn(opener: HTMLElement) {
        openerRef.current = opener;
        setOpen(true);
    }

    function closeSignIn() {
        setOpen(false);
        openerRef.current?.focus();
    }

    return (
        <SignInDrawerContext value={{ openSignIn, closeSignIn }}>
            {children}
            <SignInDrawer open={open} onClose={closeSignIn} />
        </SignInDrawerContext>
    );
}
