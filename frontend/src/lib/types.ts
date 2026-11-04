export type Category = {
    id: number;
    name: string;
};

export type ImageUrls = {
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
    coverImage: ImageUrls | null;
    images: ImageUrls[];
};

export type ProductPage = {
    items: Product[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
};

export type ManagerProduct = Product & {
    status: "ACTIVE" | "DISABLED";
    createdAt: string;
    updatedAt: string;
};

export type ManagerProductPage = Omit<ProductPage, "items"> & {
    items: ManagerProduct[];
};

export type ProductImage = ImageUrls & {
    id: number;
    position: number;
};

export type ManagerProductDetail = Omit<ManagerProduct, "coverImage" | "images"> & {
    images: ProductImage[];
};

export type CartLine = {
    productId: number;
    name: string;
    quantity: number;
    price: number;
    discountPercent: number;
    discountedPrice: number;
    lineTotal: number;
    available: boolean;
};

export type Cart = {
    lines: CartLine[];
    total: number;
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

export type Role = "CLIENT" | "MANAGER";