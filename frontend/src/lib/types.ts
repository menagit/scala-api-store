export type Category = {
    id: number;
    name: string;
};

export type Product = {
    id: number;
    name: string;
    description: string;
    category: Category;
    price: number;
    discountPercent: number;
    discountedPrice: number;
    stock: number;
    inStock: boolean;
};

export type ProductPage = {
    items: Product[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
};