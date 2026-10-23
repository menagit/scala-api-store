import Link from "next/link";
import { buildHref } from "@/lib/buildHref";
import { categories } from "@/lib/categories";

export default function CategoryFilter({
                                           current,
                                       }: {
    current: { [key: string]: string | undefined };
}) {
    return (
        <nav className="mt-6 flex flex-wrap gap-2">
            <Link
                href={buildHref(current, { categoryId: undefined, page: undefined })}
                className={current.categoryId ? "rounded border px-3 py-1" : "rounded border px-3 py-1 font-bold underline"}
            >
                All
            </Link>
            {categories.map((category) => (
                <Link
                    key={category.id}
                    href={buildHref(current, { categoryId: String(category.id), page: undefined })}
                    className={
                        current.categoryId === String(category.id)
                            ? "rounded border px-3 py-1 font-bold underline"
                            : "rounded border px-3 py-1"
                    }
                >
                    {category.name}
                </Link>
            ))}
        </nav>
    );
}