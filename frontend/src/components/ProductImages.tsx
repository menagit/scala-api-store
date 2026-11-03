"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { readError } from "@/lib/readError";
import type { ProductImage } from "@/lib/types";

export default function ProductImages({ productId, images: loaded }: { productId: number; images: ProductImage[] }) {
    const router = useRouter();
    const [images, setImages] = useState(loaded);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function upload(event: React.ChangeEvent<HTMLInputElement>) {
        const input = event.currentTarget;
        const file = input.files?.[0];
        if (!file) return;
        const form = new FormData();
        form.append("file", file);
        setUploading(true);
        setError(null);
        const response = await fetch(`/api/manager/products/${productId}/images`, { method: "POST", body: form });
        setUploading(false);
        input.value = "";
        if (!response.ok) {
            const { message, details } = await readError(response);
            setError(details?.[0] ? `File ${details[0].message}` : message);
            return;
        }
        const image: ProductImage = await response.json();
        setImages((current) => [...current, image]);
        router.refresh();
    }

    async function remove(imageId: number) {
        setError(null);
        const response = await fetch(`/api/manager/products/${productId}/images/${imageId}`, { method: "DELETE" });
        if (!response.ok) {
            setError((await readError(response)).message);
            return;
        }
        setImages((current) => current.filter((image) => image.id !== imageId));
        router.refresh();
    }

    return (
        <section className="mt-8 border-t border-line pt-6">
            <h3 className="font-semibold text-ink">Images</h3>
            <p className="mt-1 text-sm text-muted">The first image is the cover.</p>
            {images.length === 0 ? (
                <p className="mt-3 text-sm text-muted">No images yet.</p>
            ) : (
                <ul className="mt-3 grid grid-cols-3 gap-3">
                    {images.map((image, index) => (
                        <li key={image.id} className="flex flex-col gap-1">
                            <div className="aspect-square overflow-hidden rounded bg-surface">
                                {/* eslint-disable-next-line @next/next/no-img-element */}
                                <img src={image.thumbUrl} alt="" className="h-full w-full object-contain" />
                            </div>
                            <button
                                type="button"
                                onClick={() => remove(image.id)}
                                aria-label={`Remove image ${index + 1}`}
                                className="rounded text-sm text-muted hover:text-danger focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand"
                            >
                                Remove
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            <label className="mt-4 block text-sm font-medium text-ink">
                Add image
                <input
                    type="file"
                    accept="image/jpeg,image/png"
                    disabled={uploading}
                    onChange={upload}
                    className="mt-1 block w-full text-sm text-muted file:mr-3 file:rounded file:border file:border-line file:bg-white file:px-3 file:py-1 file:font-medium file:text-ink hover:file:border-brand disabled:opacity-60"
                />
            </label>
            {uploading && <p role="status" className="mt-2 text-sm text-muted">Uploading...</p>}
            {error && <p role="alert" className="mt-2 text-sm text-danger">{error}</p>}
        </section>
    );
}
