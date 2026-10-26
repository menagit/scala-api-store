import type { Category } from "@/lib/types";

export async function getCategories(): Promise<Category[]> {
  const res = await fetch(`${process.env.API_BASE_URL}/categories`, {
    next: { revalidate: 60 },
  });
  if (!res.ok) throw new Error(`Could not load categories (${res.status})`);
  const data: { items: Category[] } = await res.json();
  return data.items;
}