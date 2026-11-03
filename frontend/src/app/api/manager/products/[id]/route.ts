import { callApi, callGraphql } from "@/lib/api";

type Context = { params: Promise<{ id: string }> };

const deleteMutation = `mutation DeleteProduct($id: Long!) { deleteProduct(id: $id) { id } }`;

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

export async function DELETE(_request: Request, { params }: Context) {
    const { id } = await params;
    const result = await callGraphql(deleteMutation, { id: Number(id) });
    if ("error" in result) return result.error;
    return new Response(null, { status: 204 });
}
