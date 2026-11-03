import { cookies } from "next/headers";

const statusByCode: Record<string, number> = { FORBIDDEN: 403, NOT_FOUND: 404, CONFLICT: 409, VALIDATION_ERROR: 400 };

export async function callApi(path: string, init: RequestInit = {}): Promise<Response> {
    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) {
        return Response.json({ error: { code: "UNAUTHORIZED", message: "Authentication required" } }, { status: 401 });
    }

    const response = await fetch(`${process.env.API_BASE_URL}${path}`, {
        ...init,
        headers: { ...init.headers, Authorization: `Bearer ${accessToken}` },
    }).catch(() => null);

    if (!response) {
        return Response.json(
            { error: { code: "SERVICE_UNAVAILABLE", message: "Cannot reach the server. Try again later." } },
            { status: 503 },
        );
    }

    if (response.status === 204) return new Response(null, { status: 204 });
    return Response.json(await response.json(), { status: response.status });
}

export async function callGraphql<T>(query: string, variables: Record<string, unknown>): Promise<{ data: T } | { error: Response }> {
    const response = await callApi("/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ query, variables }),
    });
    if (!response.ok) return { error: response };

    const { data, errors } = await response.json();
    if (errors) {
        const [{ message, extensions }] = errors;
        const code = extensions?.code ?? "INTERNAL_ERROR";
        return { error: Response.json({ error: { code, message } }, { status: statusByCode[code] ?? 500 }) };
    }
    return { data };
}
