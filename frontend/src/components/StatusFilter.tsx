import Link from "next/link";
import { pillProps } from "@/components/CategoryFilter";
import { buildHref } from "@/lib/buildHref";

const options = [
    { label: "All", status: undefined },
    { label: "Active", status: "ACTIVE" },
    { label: "Disabled", status: "DISABLED" },
];

export default function StatusFilter({ current }: { current: { [key: string]: string | undefined } }) {
    return (
        <nav aria-label="Status" className="mt-6 flex flex-wrap gap-2">
            {options.map((option) => (
                <Link
                    key={option.label}
                    href={buildHref(current, { status: option.status, page: undefined }, "/manager/products")}
                    {...pillProps(current.status === option.status)}
                >
                    {option.label}
                </Link>
            ))}
        </nav>
    );
}
