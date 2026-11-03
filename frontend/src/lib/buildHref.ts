export function buildHref(
    current: { [key: string]: string | undefined },
    changes: { [key: string]: string | undefined },
    path = "/",
): string {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries({ ...current, ...changes })) {
        if (value) query.set(key, value);
    }
    const text = query.toString();
    return text ? `${path}?${text}` : path;
}