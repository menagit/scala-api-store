import ProductCard from "@/components/ProductCard";
import type { ProductPage } from "@/lib/types";

export default async function Home() {
    const response = await fetch(`${process.env.API_BASE_URL}/products`, { cache: "no-store" });
    const page: ProductPage = await response.json();

    return (
        <main className="p-8">
            <h1 className="text-2xl font-semibold">API Store</h1>
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