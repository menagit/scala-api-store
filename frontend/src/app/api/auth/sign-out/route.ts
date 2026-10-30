import { cookies } from "next/headers";

export async function POST() {
    const cookieStore = await cookies();
    const refreshToken = cookieStore.get("refresh_token")?.value;

    if (refreshToken) {
        await fetch(`${process.env.API_BASE_URL}/auth/sign-out`, {
            method: "POST",
            headers: { Cookie: `refresh_token=${refreshToken}` },
        }).catch(() => {});
    }

    cookieStore.delete({ name: "access_token", path: "/" });
    cookieStore.delete({ name: "refresh_token", path: "/api/auth" });

    return new Response(null, { status: 204 });
}
