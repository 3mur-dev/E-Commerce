import { useEffect, useMemo, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import {
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
  Heart,
  Check,
  House,
  RefreshCw,
  Search,
  ShoppingCart,
  SlidersHorizontal,
} from "lucide-react";
import { API_BASE, resolveAssetUrl } from "../../api";
import toast from "react-hot-toast";

interface Category {
  id: number;
  name: string;
}

interface Product {
  id: number;
  imageUrl: string;
  name: string;
  price: number;
  stock: number;
  shortDescription?: string;
  longDescription?: string;
  category?: Category;
  categoryId?: number;
  categoryName?: string;
  deleted?: boolean;
  favorited?: boolean;
  isFavorited?: boolean;
}

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

type CategoryOption = {
  key: string;
  label: string;
};

type WishlistItem = {
  productId: number;
};

type WishlistResponse = {
  id: number;
  name: string;
  visibility: string;
  shareToken?: string;
  defaultList: boolean;
  userId: number;
  username: string;
  items: WishlistItem[];
};
type WishlistResponseApiResponse = ApiResponse<WishlistResponse>;

type CartItemDto = {
  id: number;
  productId: number;
  productName: string;
  imageUrl: string;
  price: number;
  quantity: number;
  stock: number;
  subtotal: number;
};

type CartResponse = {
  items: CartItemDto[];
  total: number;
};

const PAGE_SIZE = 20;

const sortOptions = [
  { value: "newest", label: "Newest" },
  { value: "name-asc", label: "Name A-Z" },
  { value: "name-desc", label: "Name Z-A" },
  { value: "price-asc", label: "Price Low-High" },
  { value: "price-desc", label: "Price High-Low" },
  { value: "stock-desc", label: "Stock High-Low" },
  { value: "stock-asc", label: "Stock Low-High" },
] as const;

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem("authToken");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
};

const parseApiError = async (response: Response): Promise<string> => {
  const text = await response.text().catch(() => "");
  if (!text) return `Request failed with status ${response.status}`;

  try {
    const data = JSON.parse(text) as { message?: string; error?: string };
    return data.message || data.error || text;
  } catch {
    return text;
  }
};

const formatPrice = (price: number) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price);

const getCategoryKey = (product: Product) => {
  if (product.categoryId != null) return `id:${product.categoryId}`;
  if (product.category?.id != null) return `id:${product.category.id}`;
  if (product.category?.name) return `name:${product.category.name.toLowerCase()}`;
  if (product.categoryName) return `name:${product.categoryName.toLowerCase()}`;
  return "";
};

const getCategoryLabel = (product: Product) =>
  product.category?.name?.trim() ||
  product.categoryName?.trim() ||
  (product.categoryId != null ? `Category ${product.categoryId}` : "General");

function getStockMeta(stock: number) {
  if (stock >= 10) {
    return {
      label: `${stock} In Stock`,
      className: "bg-emerald-500 text-white",
      icon: <Check size={12} />,
    };
  }

  if (stock >= 5) {
    return {
      label: `${stock} In Stock`,
      className: "bg-amber-500 text-white",
      icon: <span className="text-[11px]">!</span>,
    };
  }

  if (stock > 0) {
    return {
      label: `${stock} Left`,
      className: "bg-rose-500 text-white",
      icon: <span className="text-[11px]">●</span>,
    };
  }

  return {
    label: "Out of Stock",
    className: "bg-slate-500 text-white",
    icon: null,
  };
}

function ProductCard({
  product,
  favorite,
  onToggleFavorite,
  onAddToCart,
}: {
  product: Product;
  favorite: boolean;
  onToggleFavorite: (id: number) => void;
  onAddToCart: (id: number) => void;
}) {
  const stockMeta = getStockMeta(product.stock);

  return (
    <article className="group overflow-hidden rounded-xl border border-white/80 bg-white/90 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm transition-transform transition-shadow duration-300 hover:-translate-y-1.5 hover:shadow-[0_24px_45px_-22px_rgba(15,23,42,0.35)]">
      <Link to={`/products/${product.id}`} className="block">
        <div className="relative">
          <div className="relative aspect-[4/3] overflow-hidden bg-gradient-to-br from-slate-100 via-white to-blue-100">
            <span
              className={`absolute left-3 top-3 z-10 inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold shadow ${stockMeta.className}`}
            >
              {stockMeta.icon}
              {stockMeta.label}
            </span>

            <img
              src={resolveAssetUrl(product.imageUrl)}
              alt={product.name}
              className="h-full w-full object-cover object-center transition duration-700 group-hover:scale-110"
              loading="lazy"
              decoding="async"
            />
          </div>
        </div>

        <div className="p-4 sm:p-5">
          <h2 className="line-clamp-2 min-h-[3.4rem] text-lg font-semibold leading-tight text-slate-900 transition group-hover:text-blue-700">
            {product.name}
          </h2>

          <p className="mt-1 line-clamp-2 text-xs leading-5 text-slate-500">
            {product.shortDescription || "No description available."}
          </p>

          <div className="mt-4 flex items-end justify-between gap-4">
            <div>
              <p className="text-2xl font-bold tracking-tight text-slate-900">
                {formatPrice(product.price)}
              </p>
              <p className="mt-1 text-xs text-slate-500">Stock: {product.stock}</p>
            </div>
          </div>
        </div>
      </Link>

      <div className="grid grid-cols-2 gap-3 border-t border-slate-100 p-4 pt-0 sm:p-5 sm:pt-0">
<button
  type="button"
  aria-label={favorite ? "Remove from favorites" : "Add to favorites"}
  aria-pressed={favorite}
  onClick={(event) => {
    event.preventDefault();
    event.stopPropagation();
    onToggleFavorite(product.id);
  }}
  className={`absolute right-3 top-3 z-10 inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/70 bg-white/90 shadow-md transition hover:scale-105 ${
    favorite ? "text-red-500" : "text-slate-600 hover:text-red-500"
  }`}
>
  <Heart size={18} fill={favorite ? "currentColor" : "none"} />
</button>

        <button
          type="button"
          disabled={product.stock <= 0}
className="col-span-2 inline-flex h-11 w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-4 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:translate-y-0"
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            onAddToCart(product.id);
          }}
        >
          <ShoppingCart size={16} className="shrink-0" />
          <span>{product.stock > 0 ? "Add to Cart" : "Out of Stock"}</span>
        </button>
      </div>
    </article>
  );
}

const ProductList = () => {
  const [searchParams] = useSearchParams();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [keyword, setKeyword] = useState(() => searchParams.get("keyword") ?? "");
  const [categoryId, setCategoryId] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [sortBy, setSortBy] = useState<(typeof sortOptions)[number]["value"]>("newest");
  const [inStockOnly, setInStockOnly] = useState(false);
  const [favorites, setFavorites] = useState<Set<number>>(new Set());
  const [currentPage, setCurrentPage] = useState(0);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  useEffect(() => {
    const loadWishlist = async () => {
      try {
        const response = await fetch(`${API_BASE}/wishlist`, {
          headers: getAuthHeaders(),
        });

        if (response.status === 401) {
          setFavorites(new Set());
          return;
        }

        if (!response.ok) {
          throw new Error(await parseApiError(response));
        }

        const data = (await response.json()) as WishlistResponseApiResponse;
        const ids = new Set<number>((data.data?.items ?? []).map((item) => item.productId));
        setFavorites(ids);
      } catch (err) {
        console.error("Failed to load wishlist:", err);
      }
    };

    loadWishlist();
  }, []);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await fetch(`${API_BASE}/products`, {
          headers: { Accept: "application/json" },
        });

        if (!response.ok) {
          throw new Error(await parseApiError(response));
        }

        const data = (await response.json()) as ApiResponse<Product[]>;
        const activeProducts = (data.data ?? []).filter((product) => !product.deleted);
        setProducts(activeProducts);
      } catch (fetchError) {
        console.error("Error fetching products:", fetchError);
        setError("Something went wrong while fetching products. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  const categories = useMemo(() => {
    const categoryMap = new Map<string, CategoryOption>();
    products.forEach((product) => {
      const key = getCategoryKey(product);
      if (key) {
        categoryMap.set(key, {
          key,
          label: getCategoryLabel(product),
        });
      }
    });
    return [...categoryMap.values()].sort((a, b) => a.label.localeCompare(b.label));
  }, [products]);

  const filteredProducts = useMemo(() => {
    const loweredKeyword = keyword.trim().toLowerCase();
    const min = minPrice === "" ? null : Number(minPrice);
    const max = maxPrice === "" ? null : Number(maxPrice);

    const next = products.filter((product) => {
      const matchesKeyword =
        loweredKeyword === "" ||
        product.name.toLowerCase().includes(loweredKeyword) ||
        getCategoryLabel(product).toLowerCase().includes(loweredKeyword);

      const matchesCategory = categoryId === "" || getCategoryKey(product) === categoryId;
      const matchesMin = min == null || product.price >= min;
      const matchesMax = max == null || product.price <= max;
      const matchesStock = !inStockOnly || product.stock > 0;

      return matchesKeyword && matchesCategory && matchesMin && matchesMax && matchesStock;
    });

    switch (sortBy) {
      case "name-asc":
        next.sort((a, b) => a.name.localeCompare(b.name));
        break;
      case "name-desc":
        next.sort((a, b) => b.name.localeCompare(a.name));
        break;
      case "price-asc":
        next.sort((a, b) => a.price - b.price);
        break;
      case "price-desc":
        next.sort((a, b) => b.price - a.price);
        break;
      case "stock-desc":
        next.sort((a, b) => b.stock - a.stock);
        break;
      case "stock-asc":
        next.sort((a, b) => a.stock - b.stock);
        break;
      default:
        next.sort((a, b) => b.id - a.id);
        break;
    }

    return next;
  }, [products, keyword, categoryId, minPrice, maxPrice, inStockOnly, sortBy]);

  useEffect(() => {
    setCurrentPage(0);
  }, [keyword, categoryId, minPrice, maxPrice, inStockOnly, sortBy]);

  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / PAGE_SIZE));
  const safePage = Math.min(currentPage, totalPages - 1);
  const paginatedProducts = filteredProducts.slice(
    safePage * PAGE_SIZE,
    safePage * PAGE_SIZE + PAGE_SIZE,
  );

  const activeChips = [
    keyword ? `Keyword: ${keyword}` : null,
    categoryId
      ? `Category: ${categories.find((category) => category.key === categoryId)?.label ?? ""}`
      : null,
    minPrice ? `Min: ${formatPrice(Number(minPrice))}` : null,
    maxPrice ? `Max: ${formatPrice(Number(maxPrice))}` : null,
    inStockOnly ? "In stock only" : null,
    sortBy !== "newest"
      ? `Sort: ${sortOptions.find((option) => option.value === sortBy)?.label ?? sortBy}`
      : null,
  ].filter(Boolean) as string[];

  const resetFilters = () => {
    setKeyword("");
    setCategoryId("");
    setMinPrice("");
    setMaxPrice("");
    setSortBy("newest");
    setInStockOnly(false);
    setCurrentPage(0);
  };

  const handleAddToCart = async (productId: number) => {
    try {
      const response = await fetch(`${API_BASE}/cart/add`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ productId, quantity: 1 }),
      });

      if (response.status === 401) {
        setActionMessage("Please log in to add items to your cart.");
        return;
      }

      if (!response.ok) {
        throw new Error(await parseApiError(response));
      }

      const data = (await response.json()) as ApiResponse<CartResponse>;
      setActionMessage(`Added to cart. Cart total: ${formatPrice(data.data.total ?? 0)}`);
      try {
        toast.success(`Added to cart. Cart total: ${formatPrice(data.data.total ?? 0)}`);
      } catch {
        /* swallow if toast fails */
      }
    } catch (err) {
      console.error("Add to cart failed:", err);
      setActionMessage("Failed to add product to cart. Please try again.");
      try {
        toast.error("Failed to add product to cart. Please try again.");
      } catch {
        /* swallow if toast fails */
      }
    }
  };

  const toggleFavorite = async (id: number) => {
    try {
      const response = await fetch(`${API_BASE}/products/${id}/favorite`, {
        method: "POST",
        headers: getAuthHeaders(),
      });

      if (response.status === 401) {
        setActionMessage("Please log in to manage favorites.");
        return;
      }

      if (!response.ok) {
        throw new Error(await parseApiError(response));
      }

      const data = (await response.json()) as ApiResponse<Product>;
      const isFavorited = data.data?.favorited ?? data.data?.isFavorited ?? false;

      setFavorites((current) => {
        const next = new Set(current);
        if (isFavorited) next.add(id);
        else next.delete(id);
        return next;
      });

      setProducts((current) =>
        current.map((product) =>
          product.id === id
            ? { ...product, favorited: isFavorited, isFavorited }
            : product,
        ),
      );
    } catch (err) {
      console.error("Error toggling favorite:", err);
      setActionMessage("Failed to update favorite. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 text-slate-900">
      <main className="mx-auto max-w-7xl px-2 pb-16 pt-10 sm:px-6 sm:pt-12 lg:px-8">
        <section className="mb-10 sm:mb-12">
          <p className="inline-flex items-center rounded-full border border-blue-200 bg-white/85 px-4 py-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-600 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)]">
            Curated Marketplace
          </p>

          <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-5xl">
                Explore Products
              </h1>
              <p className="mt-3 max-w-2xl text-sm text-slate-600 sm:text-base">
                Premium picks across categories, tuned for fast browsing and clean checkout.
              </p>
            </div>

            <div className="inline-flex items-center rounded-2xl border border-white/70 bg-white/85 px-5 py-3 text-sm text-slate-700 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)]">
              {loading
                ? "Loading catalog..."
                : `Showing ${paginatedProducts.length} of ${filteredProducts.length} products`}
            </div>
          </div>

          {actionMessage && (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-white/90 px-4 py-3 text-sm text-slate-700 shadow-sm">
              {actionMessage}
            </div>
          )}
        </section>

        <div className="grid grid-cols-1 gap-8 lg:grid-cols-[300px_minmax(0,1fr)]">
          <aside className="lg:sticky lg:top-24 lg:self-start">
            <button
              type="button"
              className="mb-4 inline-flex w-full items-center justify-between rounded-2xl border border-slate-200 bg-white/90 px-5 py-4 text-sm font-semibold text-slate-700 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] transition hover:border-blue-300 hover:text-blue-700 lg:hidden"
              onClick={() => setMobileFiltersOpen((open) => !open)}
              aria-expanded={mobileFiltersOpen}
            >
              <span className="inline-flex items-center gap-2">
                <SlidersHorizontal size={16} />
                Filters & Sort
              </span>
              <ChevronDown
                size={16}
                className={`transition-transform ${mobileFiltersOpen ? "rotate-180" : ""}`}
              />
            </button>

            <div
              className={`${
                mobileFiltersOpen ? "block" : "hidden"
              } rounded-3xl border border-white/80 bg-white/90 p-5 shadow-[0_24px_45px_-22px_rgba(15,23,42,0.35)] backdrop-blur-sm lg:block`}
            >
              <div className="space-y-5">
                <div>
                  <label
                    htmlFor="keyword"
                    className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-slate-500"
                  >
                    Search
                  </label>
                  <div className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 focus-within:border-blue-300 focus-within:bg-white">
                    <Search size={16} className="text-slate-400" />
                    <input
                      id="keyword"
                      type="text"
                      value={keyword}
                      onChange={(event) => setKeyword(event.target.value)}
                      placeholder="Product name..."
                      className="w-full bg-transparent text-sm text-slate-800 outline-none placeholder:text-slate-400"
                    />
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="categoryId"
                    className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-slate-500"
                  >
                    Category
                  </label>
                  <select
                    id="categoryId"
                    value={categoryId}
                    onChange={(event) => setCategoryId(event.target.value)}
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-800 outline-none transition focus:border-blue-300 focus:bg-white"
                  >
                    <option value="">All Categories</option>
                    {categories.map((category) => (
                      <option key={category.key} value={category.key}>
                        {category.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label
                      htmlFor="minPrice"
                      className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-slate-500"
                    >
                      Min Price
                    </label>
                    <input
                      id="minPrice"
                      type="number"
                      min="0"
                      step="0.01"
                      value={minPrice}
                      onChange={(event) => setMinPrice(event.target.value)}
                      placeholder="0.00"
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-800 outline-none transition focus:border-blue-300 focus:bg-white"
                    />
                  </div>

                  <div>
                    <label
                      htmlFor="maxPrice"
                      className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-slate-500"
                    >
                      Max Price
                    </label>
                    <input
                      id="maxPrice"
                      type="number"
                      min="0"
                      step="0.01"
                      value={maxPrice}
                      onChange={(event) => setMaxPrice(event.target.value)}
                      placeholder="999.99"
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-800 outline-none transition focus:border-blue-300 focus:bg-white"
                    />
                  </div>
                </div>

                <div>
                  <label
                    htmlFor="sortOption"
                    className="mb-2 block text-xs font-semibold uppercase tracking-[0.1em] text-slate-500"
                  >
                    Sort
                  </label>
                  <select
                    id="sortOption"
                    value={sortBy}
                    onChange={(event) => setSortBy(event.target.value as typeof sortBy)}
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-800 outline-none transition focus:border-blue-300 focus:bg-white"
                  >
                    {sortOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <label className="inline-flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                  <input
                    type="checkbox"
                    checked={inStockOnly}
                    onChange={(event) => setInStockOnly(event.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                  />
                  In-stock only
                </label>

                <div className="pt-1">
                  <button
                    type="button"
                    onClick={resetFilters}
                    className="inline-flex w-full items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:border-blue-300 hover:text-blue-700"
                  >
                    <RefreshCw size={16} className="mr-2" />
                    Reset
                  </button>
                </div>
              </div>
            </div>
          </aside>

          <section>
            <div className="mb-6 rounded-3xl border border-white/80 bg-white/80 p-4 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm sm:p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <p className="text-sm text-slate-600">
                  {keyword ? (
                    <>
                      Search: <span className="font-semibold text-slate-800">{keyword}</span>
                    </>
                  ) : (
                    "Browsing all products"
                  )}
                </p>
                <button
                  type="button"
                  onClick={resetFilters}
                  className="text-sm font-semibold text-blue-700 transition hover:text-blue-900"
                >
                  Clear all filters
                </button>
              </div>

              {activeChips.length > 0 && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {activeChips.map((chip) => (
                    <span
                      key={chip}
                      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-medium ${
                        chip.startsWith("Keyword:")
                          ? "border border-blue-200 bg-blue-50 text-blue-700"
                          : chip.startsWith("Category:")
                            ? "border border-sky-200 bg-sky-50 text-sky-800"
                            : chip.startsWith("Min:")
                              ? "border border-emerald-200 bg-emerald-50 text-emerald-800"
                              : chip.startsWith("Max:")
                                ? "border border-indigo-200 bg-indigo-50 text-indigo-800"
                                : chip === "In stock only"
                                  ? "border border-teal-200 bg-teal-50 text-teal-800"
                                  : "border border-slate-200 bg-slate-100 text-slate-700"
                      }`}
                    >
                      {chip}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {loading && (
              <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-4 xl:grid-cols-4">
                {Array.from({ length: 8 }).map((_, i) => (
                  <div
                    key={i}
                    className="animate-pulse overflow-hidden rounded-xl border border-white/80 bg-white/90 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)]"
                  >
                    <div className="aspect-[4/3] bg-slate-200" />
                    <div className="space-y-3 p-4 sm:p-5">
                      <div className="h-4 w-3/4 rounded bg-slate-300" />
                      <div className="h-4 w-1/2 rounded bg-slate-200" />
                      <div className="mt-3 h-6 w-1/3 rounded bg-slate-300" />
                      <div className="mt-4 h-10 w-full rounded-xl bg-slate-200" />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {!loading && error && (
              <div className="rounded-3xl border border-rose-200 bg-rose-50 p-10 text-center text-rose-700 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm">
                {error}
              </div>
            )}

            {!loading && !error && paginatedProducts.length > 0 && (
              <div className="grid grid-cols-2 gap-1.5 sm:grid-cols-4 xl:grid-cols-4">
                {paginatedProducts.map((product, index) => (
                  <div
                    key={product.id}
                    className="opacity-0 animate-[fade-in-up_.65s_ease_forwards]"
                    style={{ animationDelay: `${index * 70}ms` }}
                  >
                    <ProductCard
                      product={product}
                      favorite={favorites.has(product.id)}
                      onToggleFavorite={toggleFavorite}
                      onAddToCart={handleAddToCart}
                    />
                  </div>
                ))}
              </div>
            )}

            {!loading && !error && filteredProducts.length === 0 && (
              <div className="rounded-3xl border border-white/80 bg-white/85 p-10 text-center shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm">
                <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-3xl bg-slate-100 text-slate-400">
                  <House size={36} />
                </div>
                <h3 className="text-2xl font-semibold text-slate-900">No products found</h3>
                <p className="mx-auto mt-2 max-w-xl text-sm text-slate-600">
                  No results matched your current filters. Adjust your range or clear filters to
                  view more items.
                </p>
                <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
                  <button
                    type="button"
                    onClick={resetFilters}
                    className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
                  >
                    <RefreshCw size={16} />
                    Clear Search
                  </button>
                  <a
                    href="/"
                    className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-blue-300 hover:text-blue-700"
                  >
                    <House size={16} />
                    Back to Home
                  </a>
                </div>
              </div>
            )}
          </section>
        </div>

        {!loading && !error && filteredProducts.length > PAGE_SIZE && (
          <section className="mt-10 sm:mt-12">
            <div className="rounded-3xl border border-white/80 bg-white/85 p-4 shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm sm:p-5">
              <div className="flex flex-wrap items-center justify-center gap-2 sm:gap-3">
                <button
                  type="button"
                  onClick={() => setCurrentPage(0)}
                  className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-transparent bg-gradient-to-r from-blue-600 to-blue-700 text-sm text-white shadow-md transition hover:-translate-y-0.5 hover:shadow-lg"
                  aria-label="First page"
                >
                  <ChevronsLeft size={16} />
                </button>

                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.max(0, page - 1))}
                  disabled={safePage === 0}
                  className={`inline-flex h-11 w-11 items-center justify-center rounded-xl text-sm shadow-sm transition ${
                    safePage === 0
                      ? "pointer-events-none border border-slate-200 bg-slate-100 text-slate-400"
                      : "border border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:text-blue-700"
                  }`}
                  aria-label="Previous page"
                >
                  <ChevronLeft size={16} />
                </button>

                {Array.from({ length: totalPages }, (_, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => setCurrentPage(index)}
                    className={`inline-flex h-11 min-w-11 items-center justify-center rounded-xl border px-3 text-sm font-semibold transition ${
                      index === safePage
                        ? "border-transparent bg-gradient-to-r from-blue-600 to-blue-700 text-white shadow-md"
                        : "border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:text-blue-700"
                    }`}
                  >
                    {index + 1}
                  </button>
                ))}

                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.min(totalPages - 1, page + 1))}
                  disabled={safePage >= totalPages - 1}
                  className={`inline-flex h-11 w-11 items-center justify-center rounded-xl text-sm shadow-sm transition ${
                    safePage >= totalPages - 1
                      ? "pointer-events-none border border-slate-200 bg-slate-100 text-slate-400"
                      : "border border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:text-blue-700"
                  }`}
                  aria-label="Next page"
                >
                  <ChevronRight size={16} />
                </button>

                <button
                  type="button"
                  onClick={() => setCurrentPage(totalPages - 1)}
                  className="inline-flex h-11 w-11 items-center justify-center rounded-xl border border-transparent bg-gradient-to-r from-blue-600 to-blue-700 text-sm text-white shadow-md transition hover:-translate-y-0.5 hover:shadow-lg"
                  aria-label="Last page"
                >
                  <ChevronsRight size={16} />
                </button>
              </div>

              <p className="mt-4 text-center text-xs text-slate-600 sm:text-sm">
                Page <span className="font-semibold text-slate-800">{safePage + 1}</span> of{" "}
                <span className="font-semibold text-slate-800">{totalPages}</span> (
                {filteredProducts.length} products)
              </p>
            </div>
          </section>
        )}
      </main>
    </div>
  );
};

export default ProductList;
