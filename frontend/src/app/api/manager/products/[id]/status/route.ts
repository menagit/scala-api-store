import { callGraphql } from "@/lib/api";

const mutations = {
    ACTIVE: `mutation EnableProduct($id: Long!) { product: enableProduct(id: $id) { status } }`,
    DISABLED: `mutation DisableProduct($id: Long!) { product: disableProduct(id: $id) { status } }`,
};

type Status = keyof typeof mutations;

export async function PUT(request: Request, { params }: { params: Promise<{ id: string }> }) {
    const { id } = await params;
    const { status }: { status: Status } = await request.json();

    const result = await callGraphql<{ product: { status: Status } }>(mutations[status], { id: Number(id) });
    if ("error" in result) return result.error;

    return Response.json(result.data.product);
}
