import Link from "next/link";

export default function TopBar() {
    return (
        <header className="sticky top-0 z-10 border-b border-line bg-white">
            <div className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-4 sm:px-6">
                <Link href="/" className="rounded text-xl font-bold text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
                    API Store
                </Link>
                <nav>
                    <Link href="/" className="rounded text-muted hover:text-brand focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
                        Products
                    </Link>
                </nav>
            </div>
        </header>
    );
}
