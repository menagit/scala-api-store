import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";
import CartLineItem from "@/components/CartLineItem";
import CartSummary from "@/components/CartSummary";
import { getCart } from "@/lib/cart";
import { getSession } from "@/lib/session";

export default async function CartPage() {
    if (!(await cookies()).get("access_token")) redirect("/");

    const session = await getSession();
    if (session && session.role !== "CLIENT") redirect("/");

    const cart = await getCart();
    if (!session || !cart) {
        return (
            <main>
                <h1 className="text-2xl font-semibold text-ink">Your cart</h1>
                <p className="mt-6 font-medium">Cannot reach the server. Try again later.</p>
            </main>
        );
    }

    return (
        <main>
            <h1 className="text-2xl font-semibold text-ink">Your cart</h1>
            {cart.lines.length === 0 ? (
                <div className="mt-6">
                    <p className="font-medium">Your cart is empty.</p>
                    <Link href="/" className="mt-2 inline-block rounded text-brand hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand">
                        Continue shopping
                    </Link>
                </div>
            ) : (
                <>
                    <ul className="mt-6 divide-y divide-line border-y border-line">
                        {cart.lines.map((line) => (
                            <CartLineItem key={line.productId} line={line} />
                        ))}
                    </ul>
                    <CartSummary total={cart.total} />
                </>
            )}
        </main>
    );
}
