import Link from "next/link";
import { buildHref } from "@/lib/buildHref";

const options = [
    { label: "Newest", sort: "createdAt", order: "desc" },
    { label: "Price: low to high", sort: "price", order: "asc" },
    { label: "Price: high to low", sort: "price", order: "desc" },
    { label: "Name: A to Z", sort: "name", order: "asc" },
    { label: "Name: Z to A", sort: "name", order: "desc" },
];

export default function SortSelector({ current }: { current: Record<string, string | undefined> }) {
    const sort = current.sort ?? "createdAt";
    const order = current.order ?? "desc";

    return (
        <div className="flex gap-4 py-2">
            {options.map((option) => (
                <Link
                    key={option.label}
                    href={buildHref(current, { sort: option.sort, order: option.order, page: undefined })}
                    className={sort === option.sort && order === option.order ? "font-bold underline" : ""}
                >
                    {option.label}
                </Link>
            ))}
        </div>
    );
}