import ProductCard from "@/components/ProductCard";
import {firstValue } from "@/lib/firstValue";
import type { ApiError,ProductPage } from "@/lib/types";
import CategoryFilter from "@/components/CategoryFilter";
import SortSelector from "@/components/SortSelector";
import Pagination from "@/components/Pagination";
import { getCategories } from "@/lib/categories";

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
    const page: ProductPage = await response.json();
    const categories = await getCategories();

    return (
        <main>
            <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2">
                <h1 className="text-2xl font-semibold text-ink">Products</h1>
                <SortSelector current={current} />
            </div>
            <CategoryFilter current={current} categories={categories} />
            <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
                {page.items.map((product) => (
                    <ProductCard
                        key={product.id}
                        name={product.name}
                        categoryName={product.category.name}
                        price={product.price}
                        discountPercent={product.discountPercent}
                        discountedPrice={product.discountedPrice}
                        images={product.images}
                    />
                ))}
            </div>
            <Pagination current={current} page={page.page} totalPages={page.totalPages} />
        </main>
    );
}