export type Category = {
    id: number;
    name: string;
};

export type CoverImage = {
    largeUrl: string;
    thumbUrl: string;
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
    coverImage: CoverImage | null;
};

export type ProductPage = {
    items: Product[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
};


export type FieldError = {
    field: string;
    message: string;
};

export type ApiError = {
    error: {
        code: string;
        message: string;
        details?: FieldError[];
    };
};