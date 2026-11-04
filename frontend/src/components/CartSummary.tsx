export default function CartSummary({ total }: { total: number }) {
    return (
        <div className="mt-4 flex items-baseline justify-between">
            <span className="text-lg font-semibold text-ink">Total</span>
            <span className="text-lg font-semibold text-brand">${total.toFixed(2)}</span>
        </div>
    );
}
