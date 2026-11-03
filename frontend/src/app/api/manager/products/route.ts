import { callApi } from "@/lib/api";

export async function POST(request: Request) {
    return callApi("/products", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: await request.text(),
    });
}
