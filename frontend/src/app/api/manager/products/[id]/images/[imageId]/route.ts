import { callApi } from "@/lib/api";

export async function DELETE(_request: Request, { params }: { params: Promise<{ id: string; imageId: string }> }) {
    const { id, imageId } = await params;
    return callApi(`/products/${id}/images/${imageId}`, { method: "DELETE" });
}
