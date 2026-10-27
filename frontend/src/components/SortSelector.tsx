import Link from "next/link";
import { buildHref } from "@/lib/buildHref";

const options = [
    { label: "Newest", sort: "createdAt", order: "desc" },
    { label: "Price: low to high", sort: "price", order: "asc" },
    { label: "Price: high to low", sort: "price", order: "desc" },
    { label: "Name: A to Z", sort: "name", order: "asc" },
    { label: "Name: Z to A", sort: "name", order: "desc" },
];

const link = "rounded focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand";

export default function SortSelector({ current }: { current: Record<string, string | undefined> }) {
    const sort = current.sort ?? "createdAt";
    const order = current.order ?? "desc";

    return (
        <nav aria-label="Sort" className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
            <span className="text-muted">Sort by</span>
            {options.map((option) => {
                const selected = sort === option.sort && order === option.order;
                return (
                    <Link
                        key={option.label}
                        href={buildHref(current, { sort: option.sort, order: option.order, page: undefined })}
                        className={selected ? `${link} font-semibold text-brand` : `${link} text-muted hover:text-ink`}
                        aria-current={selected ? "true" : undefined}
                    >
                        {option.label}
                    </Link>
                );
            })}
        </nav>
    );
}
