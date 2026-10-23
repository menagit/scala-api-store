import ProductCard from "@/components/ProductCard";
import {firstValue } from "@/lib/firstValue";
import type { ApiError,ProductPage } from "@/lib/types";
import CategoryFilter from "@/components/CategoryFilter";
import SortSelector from "@/components/SortSelector";

const apiParams = ["page", "size", "sort", "order", "categoryId"];

export default async function Home({searchParams}: {
    searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}) {
    const params = await searchParams;

    const current: { [key: string]: string | undefined } = {};
    for (const key of apiParams) {
        current[key] = firstValue(params[key]);
    }

    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(current)) {
        if (value) query.set(key, value);
    }

    const response = await fetch(`${process.env.API_BASE_URL}/products?${query}`, {
        cache: "no-store",
    });

    if (!response.ok) {
        const body: ApiError = await response.json();
        return (
            <main className="p-8">
                <h1 className="text-2xl font-semibold">API Store</h1>
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
    const page: ProductPage = await response.json();

    return (
        <main className="p-8">
            <h1 className="text-2xl font-semibold">API Store</h1>
            <CategoryFilter current={current} />
            <SortSelector current={current} />
            <div className="mt-6 grid gap-4 sm:grid-cols-3">
                {page.items.map((product) => (
                    <ProductCard
                        key={product.id}
                        name={product.name}
                        price={product.price}
                        discountPercent={product.discountPercent}
                        discountedPrice={product.discountedPrice}
                    />
                ))}
            </div>
        </main>
    );
}