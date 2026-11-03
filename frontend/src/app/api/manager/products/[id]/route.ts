import { callApi } from "@/lib/api";

type Context = { params: Promise<{ id: string }> };

export async function GET(_request: Request, { params }: Context) {
    const { id } = await params;
    return callApi(`/manager/products/${id}`);
}

export async function PATCH(request: Request, { params }: Context) {
    const { id } = await params;
    return callApi(`/products/${id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: await request.text(),
    });
}
