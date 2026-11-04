import { cache } from "react";
import { cookies } from "next/headers";
import type { Cart } from "@/lib/types";

const query = "{ myCart { lines { productId name quantity price discountPercent discountedPrice lineTotal available } total } }";

export const getCart = cache(async (): Promise<Cart | null> => {
    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) return null;

    try {
        const response = await fetch(`${process.env.API_BASE_URL}/graphql`, {
            method: "POST",
            headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
            body: JSON.stringify({ query }),
            cache: "no-store",
        });
        if (!response.ok) return null;
        const { data } = await response.json();
        return data?.myCart ?? null;
    } catch {
        return null;
    }
});

export const getCartCount = cache(async (): Promise<number> => {
    const cart = await getCart();
    return cart?.lines.reduce((sum, line) => sum + line.quantity, 0) ?? 0;
});
