"use client";

import { useState } from "react";
import type { ImageUrls } from "@/lib/types";

type ImageCarouselProps = {
    name: string;
    images: ImageUrls[];
};

const arrow =
    "absolute top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full border border-line bg-white text-ink-soft opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 pointer-coarse:opacity-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand";

export default function ImageCarousel({ name, images }: ImageCarouselProps) {
    const [index, setIndex] = useState(0);

    if (images.length === 0) {
        return (
            <div aria-hidden="true" className="flex h-full items-center justify-center text-sm text-muted">
                No image
            </div>
        );
    }

    const count = images.length;

    return (
        <div className="group relative h-full">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={images[index].thumbUrl} alt={name} loading="lazy" className="h-full w-full object-contain p-4" />
            {count > 1 && (
                <>
                    <button
                        type="button"
                        aria-label="Previous image"
                        onClick={() => setIndex((index - 1 + count) % count)}
                        className={`${arrow} left-2`}
                    >
                        ‹
                    </button>
                    <button
                        type="button"
                        aria-label="Next image"
                        onClick={() => setIndex((index + 1) % count)}
                        className={`${arrow} right-2`}
                    >
                        ›
                    </button>
                    <div aria-hidden="true" className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1.5">
                        {images.map((image, i) => (
                            <span
                                key={image.thumbUrl}
                                className={`h-1.5 w-1.5 rounded-full ${i === index ? "bg-brand" : "bg-muted"}`}
                            />
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}
