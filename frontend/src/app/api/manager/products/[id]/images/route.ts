import { callApi } from "@/lib/api";

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    return callApi(`/products/${id}/images`, { method: "POST", body: await request.formData() });
}
