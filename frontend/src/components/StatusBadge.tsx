import type { ManagerProduct } from "@/lib/types";

export default function StatusBadge({ status }: { status: ManagerProduct["status"] }) {
    return status === "ACTIVE" ? (
        <span className="rounded-full bg-brand px-2 py-0.5 text-xs font-medium text-white">Active</span>
    ) : (
        <span className="rounded-full bg-surface px-2 py-0.5 text-xs font-medium text-muted">Disabled</span>
    );
}
