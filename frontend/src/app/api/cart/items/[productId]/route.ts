import { callGraphql } from "@/lib/api";

type Context = { params: Promise<{ productId: string }> };

const updateMutation = `mutation UpdateCartItemQuantity($productId: Long!, $quantity: Int!) {
    updateCartItemQuantity(productId: $productId, quantity: $quantity) { total }
}`;

const removeMutation = `mutation RemoveFromCart($productId: Long!) {
    removeFromCart(productId: $productId) { total }
}`;

export async function PATCH(request: Request, { params }: Context) {
    const { productId } = await params;
    const { quantity } = await request.json();

    const result = await callGraphql(updateMutation, { productId: Number(productId), quantity });
    if ("error" in result) return result.error;

    return new Response(null, { status: 204 });
}

export async function DELETE(_request: Request, { params }: Context) {
    const { productId } = await params;

    const result = await callGraphql(removeMutation, { productId: Number(productId) });
    if ("error" in result) return result.error;

    return new Response(null, { status: 204 });
}
