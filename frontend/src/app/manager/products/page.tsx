import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import ManagerProductsView from "@/components/ManagerProductsView";
import Pagination from "@/components/Pagination";
import StatusFilter from "@/components/StatusFilter";
import { getCategories } from "@/lib/categories";
import { firstValue } from "@/lib/firstValue";
import type { ApiError, ManagerProductPage } from "@/lib/types";

export default async function ManagerProducts({ searchParams }: {
    searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
    const params = await searchParams;
    const current = { page: firstValue(params.page), status: firstValue(params.status) };

    const accessToken = (await cookies()).get("access_token")?.value;
    if (!accessToken) redirect("/");

    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(current)) {
        if (value) query.set(key, value);
    }

    const response = await fetch(`${process.env.API_BASE_URL}/manager/products?${query}`, {
        headers: { Authorization: `Bearer ${accessToken}` },
        cache: "no-store",
    }).catch(() => null);

    if (!response) {
        return (
            <main>
                <h1 className="text-2xl font-semibold text-ink">Products</h1>
                <p className="mt-6 font-medium">Cannot reach the server. Try again later.</p>
            </main>
        );
    }

    if (response.status === 401 || response.status === 403) redirect("/");

    if (!response.ok) {
        const body: ApiError = await response.json();
        return (
            <main>
                <h1 className="text-2xl font-semibold text-ink">Products</h1>
                <p className="mt-6 font-medium">{body.error.message}</p>
                <ul className="mt-2 list-disc pl-5">
                    {body.error.details?.map((detail) => (
                        <li key={`${detail.field}-${detail.message}`}>
                            {detail.field}: {detail.message}
                        </li>
                    ))}
                </ul>
            </main>
        );
    }

    const page: ManagerProductPage = await response.json();
    const categories = await getCategories();

    return (
        <main>
            <h1 className="text-2xl font-semibold text-ink">
                Products <span className="font-normal text-muted">({page.totalItems})</span>
            </h1>
            <StatusFilter current={current} />
            <ManagerProductsView items={page.items} categories={categories} />
            <Pagination current={current} page={page.page} totalPages={page.totalPages} basePath="/manager/products" />
        </main>
    );
}
