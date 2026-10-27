import Link from "next/link";
import { buildHref } from "@/lib/buildHref";
import type { Category } from "@/lib/types";

const pill = "rounded-full px-4 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand";

function pillProps(selected: boolean) {
    return selected
        ? { className: `${pill} bg-brand text-white`, "aria-current": "true" as const }
        : { className: `${pill} bg-surface text-ink-soft hover:bg-line` };
}

export default function CategoryFilter({
                                           current,
                                           categories,
                                       }: {
    current: { [key: string]: string | undefined };
    categories: Category[];
}) {
    return (
        <nav className="mt-6 flex flex-wrap gap-2">
            <Link
                href={buildHref(current, { categoryId: undefined, page: undefined })}
                {...pillProps(!current.categoryId)}
            >
                All
            </Link>
            {categories.map((category) => (
                <Link
                    key={category.id}
                    href={buildHref(current, { categoryId: String(category.id), page: undefined })}
                    {...pillProps(current.categoryId === String(category.id))}
                >
                    {category.name}
                </Link>
            ))}
        </nav>
    );
}
