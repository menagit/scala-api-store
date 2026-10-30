import { cookies } from "next/headers";

const secure = process.env.NODE_ENV === "production";

export async function POST(request: Request) {
    const { email, password } = await request.json();

    const response = await fetch(`${process.env.API_BASE_URL}/auth/sign-in`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    }).catch(() => null);

    if (!response) {
        return Response.json(
            { error: { code: "SERVICE_UNAVAILABLE", message: "Cannot reach the server. Try again later." } },
            { status: 503 },
        );
    }

    if (!response.ok) {
        return Response.json(await response.json(), { status: response.status });
    }

    const { access_token, expires_in } = await response.json();
    const setCookie = response.headers.getSetCookie().join("; ");
    const [, refreshToken] = setCookie.match(/refresh_token=([^;]+)/)!;
    const [, maxAge] = setCookie.match(/Max-Age=(\d+)/)!;

    const cookieStore = await cookies();
    cookieStore.set("access_token", access_token, { httpOnly: true, sameSite: "lax", path: "/", maxAge: expires_in, secure });
    cookieStore.set("refresh_token", refreshToken, { httpOnly: true, sameSite: "lax", path: "/api/auth", maxAge: Number(maxAge), secure });

    return new Response(null, { status: 204 });
}
