import Link from "next/link";

export default function CartButton({ count }: { count: number }) {
    const label = count === 0 ? "Cart" : `Cart, ${count} ${count === 1 ? "item" : "items"}`;

    return (
        <Link
            href="/cart"
            aria-label={label}
            className="relative flex h-8 w-8 items-center justify-center rounded-full bg-brand text-white hover:opacity-90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
        >
            <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 4h2l2.4 11.2a1 1 0 0 0 1 .8h8.9a1 1 0 0 0 1-.8L20 8H6.2" />
                <circle cx="9" cy="20" r="1.5" />
                <circle cx="17" cy="20" r="1.5" />
            </svg>
            {count > 0 && (
                <span aria-hidden="true" className="absolute -top-1.5 -right-1.5 flex h-5 min-w-5 items-center justify-center rounded-full border border-brand bg-white px-1 text-xs font-semibold text-brand">
                    {count > 99 ? "99+" : count}
                </span>
            )}
        </Link>
    );
}
