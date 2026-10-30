import { cookies } from "next/headers";
import type { Role } from "@/lib/types";

export async function getSession(): Promise<{ role: Role } | null> {
    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) return null;

    try {
        const response = await fetch(`${process.env.API_BASE_URL}/me`, {
            headers: { Authorization: `Bearer ${accessToken}` },
            cache: "no-store",
        });
        if (!response.ok) return null;
        const { role }: { role: Role } = await response.json();
        return { role };
    } catch {
        return null;
    }
}
