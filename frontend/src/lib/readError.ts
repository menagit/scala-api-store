import type { ApiError } from "@/lib/types";

export async function readError(response: Response): Promise<ApiError["error"]> {
    if (response.status === 401) return { code: "UNAUTHORIZED", message: "Your session expired. Sign in again." };
    const body: ApiError | null = await response.json().catch(() => null);
    return body?.error ?? { code: "UNKNOWN", message: "Something went wrong. Try again." };
}
