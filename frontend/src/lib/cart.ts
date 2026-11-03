import { cache } from "react";
import { cookies } from "next/headers";

export const getCartCount = cache(async (): Promise<number> => {
    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) return 0;

    const response = await fetch(`${process.env.API_BASE_URL}/graphql`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ query: "{ myCart { lines { quantity } } }" }),
        cache: "no-store",
    }).catch(() => null);
    if (!response?.ok) return 0;

    const { data } = await response.json();
    const lines: { quantity: number }[] = data?.myCart?.lines ?? [];
    return lines.reduce((sum, line) => sum + line.quantity, 0);
});
