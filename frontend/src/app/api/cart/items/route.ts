import { callGraphql } from "@/lib/api";

const mutation = `mutation AddToCart($productId: Long!, $quantity: Int!) {
    addToCart(productId: $productId, quantity: $quantity) { lines { productId } }
}`;

export async function POST(request: Request) {
    const { productId } = await request.json();

    const result = await callGraphql(mutation, { productId, quantity: 1 });
    if ("error" in result) return result.error;

    return new Response(null, { status: 204 });
}
