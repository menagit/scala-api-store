import Link from "next/link";
import { buildHref } from "@/lib/buildHref";

export default function Pagination({
                                       current,
                                       page,
                                       totalPages,
                                   }: {
    current: Record<string, string | undefined>;
    page: number;
    totalPages: number;
}) {
    if (totalPages <= 1) return null;

    return (
        <div className="flex items-center gap-4 py-4">
            {page > 1 ? (
                <Link href={buildHref(current, { page: String(page - 1) })}>Previous</Link>
            ) : (
                <span className="text-gray-400">Previous</span>
            )}
            <span>
        Page {page} of {totalPages}
      </span>
            {page < totalPages ? (
                <Link href={buildHref(current, { page: String(page + 1) })}>Next</Link>
            ) : (
                <span className="text-gray-400">Next</span>
            )}
        </div>
    );
}