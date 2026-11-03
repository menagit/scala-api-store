import { cookies } from "next/headers";

const mutation = `mutation AddToCart($productId: Long!, $quantity: Int!) {
    addToCart(productId: $productId, quantity: $quantity) { lines { productId } }
}`;

const statusByCode: Record<string, number> = { FORBIDDEN: 403, NOT_FOUND: 404, CONFLICT: 409, VALIDATION_ERROR: 400 };

export async function POST(request: Request) {
    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) {
        return Response.json({ error: { code: "UNAUTHORIZED", message: "Authentication required" } }, { status: 401 });
    }

    const { productId } = await request.json();

    const response = await fetch(`${process.env.API_BASE_URL}/graphql`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ query: mutation, variables: { productId, quantity: 1 } }),
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

    const { errors } = await response.json();
    if (errors) {
        const [{ message, extensions }] = errors;
        const code = extensions?.code ?? "INTERNAL_ERROR";
        return Response.json({ error: { code, message } }, { status: statusByCode[code] ?? 500 });
    }

    return new Response(null, { status: 204 });
}
