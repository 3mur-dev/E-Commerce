import { type FormEvent, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_BASE, resolveAssetUrl } from "../../api";
import { confirmDangerousAction } from "./adminShared";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";

type ProductResponse = {
  id: number;
  name: string;
  shortDescription?: string | null;
  longDescription?: string | null;
  price?: number | string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  stock: number;
  imageUrl?: string | null;
};

type CategoryResponse = {
  id: number;
  name: string;
};

type ProductFormState = {
  name: string;
  price: string;
  stock: string;
  categoryId: string;
  shortDescription: string;
  longDescription: string;
  imageUrl: string;
};

type ApiResponse<T> = {
  success: boolean;
  data?: T;
  message?: string;
};

const emptyForm = (): ProductFormState => ({
  name: "",
  price: "",
  stock: "0",
  categoryId: "",
  shortDescription: "",
  longDescription: "",
  imageUrl: "",
});

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem("authToken");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
};

const toNumber = (value: string | number | null | undefined) => {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === "string" ? Number(value) : value;
  return Number.isFinite(parsed) ? parsed : 0;
};

const formatPrice = (value: string | number | null | undefined) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(toNumber(value));

async function readError(response: Response) {
  const text = await response.text().catch(() => "");
  if (!text) return `Request failed with status ${response.status}`;

  try {
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    return parsed.message || parsed.error || text;
  } catch {
    return text;
  }
}

export default function AdminProductsPage() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ProductFormState>(emptyForm);
  const [editingProduct, setEditingProduct] = useState<ProductResponse | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<ProductResponse | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const debouncedSearch = useDebouncedValue(searchInput.trim(), 300);
  const navigate = useNavigate();
  const location = useLocation();

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [sortField, setSortField] = useState<"name" | "price" | "stock" | "id">("id");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc");
  const [categoryFilter, setCategoryFilter] = useState<string>("");
  const [stockFilter, setStockFilter] = useState<"ALL" | "LOW" | "OUT">("ALL");
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string>("");
  const [serverTotal, setServerTotal] = useState<number | null>(null);

  const {
    data: products = [],
    isLoading: productsLoading,
    isError: productsError,
    error: productsQueryError,
  } = useQuery<ProductResponse[], Error>({
    queryKey: ["admin-products", { page: currentPage, size: pageSize, search: debouncedSearch, category: categoryFilter, stock: stockFilter, sort: `${sortField},${sortDirection}` }],
    queryFn: async ({ signal, queryKey }: { signal?: AbortSignal; queryKey: unknown }) => {
      const [, params] = queryKey as unknown as [string, Record<string, unknown>];
      const qp = new URLSearchParams();
      if (params.search) qp.set("search", String(params.search));
      if (params.category) qp.set("category", String(params.category));
      if (params.stock && params.stock !== "ALL") qp.set("stock", String(params.stock));
      qp.set("page", String(params.page ?? 1));
      qp.set("size", String(params.size ?? 10));
      qp.set("sort", String(params.sort ?? "id,asc"));

      const url = `${API_BASE}/admin/products?${qp.toString()}`;

      const response = await fetch(url, {
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        signal,
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const json = (await response.json()) as ApiResponse<unknown>;

      if (!json.success) {
        throw new Error(json.message || "Invalid response");
      }

      // backend may return either data: ProductResponse[] or data: { items: ProductResponse[], total: number }
      if (Array.isArray(json.data)) {
        // backend returned a plain array (non-paginated). Do not treat this as server-mode.
        setServerTotal(null);
        return json.data as ProductResponse[];
      }

      const maybe = json.data as { items?: ProductResponse[]; total?: number } | null;
      if (maybe && Array.isArray(maybe.items)) {
        // backend returned paginated shape
        setServerTotal(typeof maybe.total === "number" ? maybe.total : maybe.items.length);
        return maybe.items;
      }

      setServerTotal(null);
      return [] as ProductResponse[];
    },
    // no-op onSuccess; server total is set inside the queryFn when possible
  });

  const {
    data: categories = [],
    isLoading: categoriesLoading,
    isError: categoriesError,
    error: categoriesQueryError,
  } = useQuery<CategoryResponse[]>({
    queryKey: ["categories"],
    queryFn: async () => {
      // Fetch categories. Do not silently retry without Authorization —
      // surface server 5xx responses during development so backend can be fixed.
      const response = await fetch(`${API_BASE}/categories`, {
        headers: { Accept: "application/json", ...getAuthHeaders() },
      });

      if (!response.ok) {
        if (response.status >= 500 && import.meta.env.MODE !== "production") {
          const text = await response.clone().text().catch(() => "");
          console.error("Server error fetching /categories:", response.status, text);
        }
        throw new Error(await readError(response));
      }

      const json = (await response.json()) as ApiResponse<CategoryResponse[]>;
      if (!json.success || !json.data) {
        throw new Error(json.message || "Invalid response");
      }

      return json.data;
    },
  });

  const isServerMode = serverTotal !== null;
  const productArray = Array.isArray(products) ? (products as ProductResponse[]) : [];

  const filteredProducts = useMemo(() => {
    if (isServerMode) {
      // server already applied filters/pagination — treat products as the current page
      return productArray;
    }

    const keyword = debouncedSearch.toLowerCase();

    return productArray
      .filter((product: ProductResponse) => {
        if (categoryFilter && String(product.categoryId) !== categoryFilter) return false;

        if (stockFilter === "LOW" && product.stock > 5) return false;
        if (stockFilter === "OUT" && product.stock > 0) return false;

        if (!keyword) return true;

        const haystack = [
          String(product.id),
          product.name,
          product.shortDescription,
          product.longDescription,
          product.categoryName,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();

        return haystack.includes(keyword);
      })
      .sort((a: ProductResponse, b: ProductResponse) => {
        const dir = sortDirection === "asc" ? 1 : -1;
        const valA = sortField === "price" ? toNumber(a.price) : sortField === "stock" ? a.stock : sortField === "name" ? (a.name || "") : a.id;
        const valB = sortField === "price" ? toNumber(b.price) : sortField === "stock" ? b.stock : sortField === "name" ? (b.name || "") : b.id;

        if (typeof valA === "number" && typeof valB === "number") return (valA - valB) * dir;
        return String(valA).localeCompare(String(valB)) * dir;
      });
  }, [productArray, debouncedSearch, categoryFilter, stockFilter, sortField, sortDirection, isServerMode]);

  useEffect(() => {
    setCurrentPage(1);
  }, [debouncedSearch, categoryFilter, stockFilter, pageSize, sortField, sortDirection]);

  // read query params on mount
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const page = Number(params.get("page") || "1");
    const size = Number(params.get("size") || String(pageSize));
    const cat = params.get("category") || "";
    const stock = (params.get("stock") as "ALL" | "LOW" | "OUT") || "ALL";
    const sort = params.get("sort") || "id,asc";
    const searchQ = params.get("search") || "";

    setCurrentPage(Number.isFinite(page) && page > 0 ? page : 1);
    setPageSize(Number.isFinite(size) && size > 0 ? size : pageSize);
    setCategoryFilter(cat);
    setStockFilter(stock);
    const [sf, sd] = sort.split(",");
    if (sf === "name" || sf === "price" || sf === "stock" || sf === "id") setSortField(sf);
    setSortDirection(sd === "desc" ? "desc" : "asc");
    setSearchInput(searchQ);
  }, []); // run only once

  // write query params when relevant state changes
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("search", debouncedSearch);
    if (categoryFilter) params.set("category", categoryFilter);
    if (stockFilter && stockFilter !== "ALL") params.set("stock", stockFilter);
    params.set("page", String(currentPage));
    params.set("size", String(pageSize));
    params.set("sort", `${sortField},${sortDirection}`);

    navigate({ search: params.toString() }, { replace: true });
  }, [debouncedSearch, categoryFilter, stockFilter, currentPage, pageSize, sortField, sortDirection, navigate]);

  const saveProductMutation = useMutation({
    mutationFn: async () => {
      const payload = {
        name: form.name.trim(),
        price: Number(form.price),
        stock: Number(form.stock),
        categoryId: Number(form.categoryId),
        shortDescription: form.shortDescription.trim() || null,
        longDescription: form.longDescription.trim() || null,
        imageUrl: form.imageUrl.trim() || null,
      };

      if (!payload.name) throw new Error("Product name is required");
      if (!Number.isFinite(payload.price) || payload.price < 0) throw new Error("Price must be zero or higher");
      if (!Number.isFinite(payload.stock) || payload.stock < 0) throw new Error("Stock must be zero or higher");
      if (!Number.isFinite(payload.categoryId) || payload.categoryId <= 0) throw new Error("Choose a category");

      const url = editingProduct
        ? `${API_BASE}/admin/products/${editingProduct.id}`
        : `${API_BASE}/admin/products/add`;
      const method = editingProduct ? "PUT" : "POST";

      const response = await fetch(url, {
        method,
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const json: ApiResponse<ProductResponse> = await response.json();
      if (!json.success) {
        throw new Error(json.message || "Failed to save product");
      }
      return json.data as ProductResponse;
    },
    onSuccess: async (savedProduct) => {
      await queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      setSelectedProduct(savedProduct);
      setEditingProduct(null);
      setForm(emptyForm());
      setError("");
      setNotice(`Saved ${savedProduct.name}`);
    },
    onError: (mutationError) => {
      setNotice("");
      setError(mutationError instanceof Error ? mutationError.message : "Failed to save product");
    },
  });

  const deleteProductMutation = useMutation({
    mutationFn: async (product: ProductResponse) => {
      const response = await fetch(`${API_BASE}/admin/products/${product.id}`, {
        method: "DELETE",
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }
    },
    onSuccess: async (_, product) => {
      await queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      if (selectedProduct?.id === product.id) {
        setSelectedProduct(null);
      }
      if (editingProduct?.id === product.id) {
        setEditingProduct(null);
        setForm(emptyForm());
      }
      setError("");
      setNotice(`Deleted ${product.name}`);
    },
    onError: (mutationError) => {
      setNotice("");
      setError(mutationError instanceof Error ? mutationError.message : "Failed to delete product");
    },
  });

  const openEditor = (product?: ProductResponse) => {
    if (!product) {
      setEditingProduct(null);
      setForm(emptyForm());
      return;
    }

    setEditingProduct(product);
    setForm({
      name: product.name ?? "",
      price: String(toNumber(product.price)),
      stock: String(product.stock ?? 0),
      categoryId: String(product.categoryId ?? ""),
      shortDescription: product.shortDescription ?? "",
      longDescription: product.longDescription ?? "",
      imageUrl: product.imageUrl ?? "",
    });
  };

  useEffect(() => {
    const url = form.imageUrl?.trim() || editingProduct?.imageUrl || "";
    if (!url) {
      setImagePreviewUrl("");
      return;
    }

    try {
      // ensure valid URL (or allow relative)
      const parsed = /^https?:\/\//i.test(url) ? url : url.startsWith("/") ? url : `/${url}`;
      setImagePreviewUrl(parsed);
    } catch {
      setImagePreviewUrl("");
    }
  }, [form.imageUrl, editingProduct]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setNotice("");
    setError("");
    const errors: Record<string, string> = {};

    if (!form.name.trim()) errors.name = "Name is required";
    const priceVal = Number(form.price);
    if (!Number.isFinite(priceVal) || priceVal < 0) errors.price = "Price must be zero or higher";
    const stockVal = Number(form.stock);
    if (!Number.isFinite(stockVal) || stockVal < 0) errors.stock = "Stock must be zero or higher";
    if (!form.categoryId) errors.categoryId = "Choose a category";

    setFormErrors(errors);

    if (Object.keys(errors).length > 0) {
      setError("Fix validation errors before saving");
      return;
    }

    await saveProductMutation.mutateAsync();
  };

  const handleDelete = async (product: ProductResponse) => {
    if (!confirmDangerousAction(`Delete ${product.name}? This removes it from the active catalog.`)) {
      return;
    }

    setNotice("");
    setError("");
    await deleteProductMutation.mutateAsync(product);
  };

  const queryErrorMessage =
    productsError && productsQueryError
      ? productsQueryError instanceof Error
        ? productsQueryError.message
        : String(productsQueryError)
      : categoriesError && categoriesQueryError
      ? categoriesQueryError instanceof Error
        ? categoriesQueryError.message
        : String(categoriesQueryError)
      : "";

  const lowStockCount = productArray.filter((product) => product.stock <= 5).length;
  const totalCount = isServerMode ? (serverTotal ?? 0) : filteredProducts.length;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const paginatedProducts = isServerMode ? products : filteredProducts.slice((safePage - 1) * pageSize, (safePage - 1) * pageSize + pageSize);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white/80 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-slate-500">Admin Catalog</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Product management</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
            This view now matches the actual backend contract: list, create, edit, inspect, and delete active products.
          </p>
        </div>
      </header>

      <main className="mx-auto grid max-w-7xl gap-6 px-4 py-6 sm:px-6 lg:grid-cols-[minmax(0,1.5fr)_minmax(320px,1fr)] lg:px-8">
        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm font-medium text-slate-500">Products</p>
              <h2 className="mt-1 text-xl font-semibold text-slate-950">{productArray.length} active products</h2>
              <p className="mt-1 text-sm text-slate-500">{lowStockCount} product(s) at or below 5 stock</p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="flex gap-3 flex-1">
                <input
                  value={searchInput}
                  onChange={(event) => setSearchInput(event.target.value)}
                  placeholder="Search products"
                  className="h-11 rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400 w-full"
                />

                <select
                  value={categoryFilter}
                  onChange={(e) => setCategoryFilter(e.target.value)}
                  className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                >
                  <option value="">All categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={String(c.id)}>
                      {c.name}
                    </option>
                  ))}
                </select>

                <select
                  value={stockFilter}
                  onChange={(e) => setStockFilter(e.target.value as any)}
                  className="h-11 rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-slate-400"
                >
                  <option value="ALL">All stock</option>
                  <option value="LOW">Low (≤5)</option>
                  <option value="OUT">Out of stock</option>
                </select>
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => openEditor()}
                  className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800"
                >
                  New product
                </button>
              </div>
            </div>
          </div>

          {notice ? (
            <div
              role="status"
              aria-live="polite"
              className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700"
            >
              {notice}
            </div>
          ) : null}

          {(error || queryErrorMessage) ? (
            <div
              role="alert"
              aria-live="assertive"
              className="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
            >
              {error || queryErrorMessage}
            </div>
          ) : null}

          <div className="mt-5 overflow-hidden rounded-xl border border-slate-200">
            <div className="hidden sm:block">
              {productsLoading || categoriesLoading ? (
                <div className="p-6">
                  <div className="animate-pulse">
                    <div className="h-6 w-1/3 bg-slate-200 rounded mb-4" />
                    {Array.from({ length: 6 }).map((_, i) => (
                      <div key={i} className="flex items-center gap-4 py-4">
                        <div className="h-8 w-1/3 bg-slate-200 rounded" />
                        <div className="h-6 w-1/6 bg-slate-200 rounded" />
                        <div className="h-6 w-1/12 bg-slate-200 rounded" />
                        <div className="h-6 w-1/12 bg-slate-200 rounded" />
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <table className="min-w-full divide-y divide-slate-200" role="table" aria-label="Products table">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Product</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Category</th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <button
                          type="button"
                          onClick={() => {
                            if (sortField === "stock") setSortDirection(sortDirection === "asc" ? "desc" : "asc");
                            else setSortField("stock");
                          }}
                          className="inline-flex items-center gap-2 text-xs font-semibold focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
                          aria-label="Sort by stock"
                        >
                          Stock
                          <span className="text-slate-400">{sortField === "stock" ? (sortDirection === "asc" ? "↑" : "↓") : ""}</span>
                        </button>
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <button
                          type="button"
                          onClick={() => {
                            if (sortField === "price") setSortDirection(sortDirection === "asc" ? "desc" : "asc");
                            else setSortField("price");
                          }}
                          className="inline-flex items-center gap-2 text-xs font-semibold focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
                          aria-label="Sort by price"
                        >
                          Price
                          <span className="text-slate-400">{sortField === "price" ? (sortDirection === "asc" ? "↑" : "↓") : ""}</span>
                        </button>
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {paginatedProducts.map((product) => (
                      <motion.tr
                        key={product.id}
                        className="align-top hover:bg-slate-50"
                        initial={{ opacity: 0, y: 4 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.18 }}
                      >
                        <td className="px-4 py-4">
                          <button
                            type="button"
                            onClick={() => setSelectedProduct(product)}
                            className="text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
                            aria-label={`View details for ${product.name}`}
                          >
                            <div className="font-semibold text-slate-950 hover:underline">{product.name}</div>
                            <div className="mt-1 line-clamp-2 text-sm text-slate-500">
                              {product.shortDescription || "No short description provided."}
                            </div>
                          </button>
                        </td>
                        <td className="px-4 py-4 text-sm text-slate-700">{product.categoryName || "Unassigned"}</td>
                        <td className="px-4 py-4 text-sm text-slate-700">{product.stock}</td>
                        <td className="px-4 py-4 text-sm font-semibold text-slate-950">{formatPrice(product.price)}</td>
                        <td className="px-4 py-4">
                          <div className="flex flex-wrap gap-2">
                            <button
                              type="button"
                              onClick={() => setSelectedProduct(product)}
                              className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
                              aria-label={`Details ${product.name}`}
                            >
                              Details
                            </button>
                            <button
                              type="button"
                              onClick={() => openEditor(product)}
                              className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400"
                              aria-label={`Edit ${product.name}`}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              onClick={() => void handleDelete(product)}
                              className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-700 hover:bg-rose-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-400"
                              aria-label={`Delete ${product.name}`}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </motion.tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="space-y-3 p-4 sm:hidden">
              {productsLoading || categoriesLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 3 }).map((_, i) => (
                    <div key={i} className="rounded-2xl border border-slate-200 p-4 animate-pulse">
                      <div className="h-5 w-1/3 bg-slate-200 rounded mb-2" />
                      <div className="h-4 w-2/3 bg-slate-200 rounded" />
                    </div>
                  ))}
                </div>
              ) : (
                <>
                  {paginatedProducts.map((product) => {
                    return (
                      <div key={product.id} className="rounded-2xl border border-slate-200 p-4">
                        <button type="button" onClick={() => setSelectedProduct(product)} className="text-left">
                          <div className="font-semibold text-slate-950">{product.name}</div>
                          <div className="mt-1 text-sm text-slate-500">{product.categoryName || "Unassigned"}</div>
                        </button>
                        <div className="mt-3 flex items-center justify-between text-sm">
                          <span>Stock {product.stock}</span>
                          <span className="font-semibold">{formatPrice(product.price)}</span>
                        </div>
                        <div className="mt-3 flex gap-2">
                          <button type="button" onClick={() => openEditor(product)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-700">
                            Edit
                          </button>
                          <button type="button" onClick={() => void handleDelete(product)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-700">
                            Delete
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </>
              )}
            </div>

            <div className="flex items-center justify-between gap-3 p-4">
              <div className="text-sm text-slate-500">
                Showing {(safePage - 1) * pageSize + 1} - {Math.min(safePage * pageSize, filteredProducts.length)} of {filteredProducts.length}
              </div>

              <div className="flex items-center gap-2">
                <select
                  value={pageSize}
                  onChange={(e) => setPageSize(Number(e.target.value))}
                  className="h-9 rounded-xl border border-slate-200 px-3 text-sm outline-none"
                >
                  <option value={5}>5 / page</option>
                  <option value={10}>10 / page</option>
                  <option value={25}>25 / page</option>
                </select>

                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                    disabled={safePage <= 1}
                    className="rounded-lg border px-2 py-1 text-sm"
                  >
                    Prev
                  </button>
                  <div className="px-2 text-sm">{safePage} / {totalPages}</div>
                  <button
                    type="button"
                    onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                    disabled={safePage >= totalPages}
                    className="rounded-lg border px-2 py-1 text-sm"
                  >
                    Next
                  </button>
                </div>
              </div>
            </div>
          </div>

          {(productsLoading || categoriesLoading) && (
            <p className="mt-4 text-sm text-slate-500">Loading catalog data...</p>
          )}
        </section>

        <aside className="space-y-6">
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">{editingProduct ? "Edit product" : "Create product"}</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-950">
              {editingProduct ? editingProduct.name : "New catalog item"}
            </h2>

            <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
              <div>
                <label htmlFor="name" className="text-sm font-medium text-slate-700">Name</label>
                <input
                  id="name"
                  value={form.name}
                  onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                  className="mt-1 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400"
                />
                {formErrors.name ? (
                  <p className="mt-1 text-xs text-rose-600">{formErrors.name}</p>
                ) : null}
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="price" className="text-sm font-medium text-slate-700">Price</label>
                  <input
                    id="price"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.price}
                    onChange={(event) => setForm((current) => ({ ...current, price: event.target.value }))}
                    className="mt-1 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400"
                  />
                  {formErrors.price ? <p className="mt-1 text-xs text-rose-600">{formErrors.price}</p> : null}
                </div>

                <div>
                  <label htmlFor="stock" className="text-sm font-medium text-slate-700">Stock</label>
                  <input
                    id="stock"
                    type="number"
                    min="0"
                    step="1"
                    value={form.stock}
                    onChange={(event) => setForm((current) => ({ ...current, stock: event.target.value }))}
                    className="mt-1 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400"
                  />
                  {formErrors.stock ? <p className="mt-1 text-xs text-rose-600">{formErrors.stock}</p> : null}
                </div>
              </div>

              <div>
                <label htmlFor="category" className="text-sm font-medium text-slate-700">Category</label>
                <select
                  id="category"
                  value={form.categoryId}
                  onChange={(event) => setForm((current) => ({ ...current, categoryId: event.target.value }))}
                  className="mt-1 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400"
                >
                  <option value="">Select a category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                {formErrors.categoryId ? <p className="mt-1 text-xs text-rose-600">{formErrors.categoryId}</p> : null}
              </div>

              <div>
                <label htmlFor="imageUrl" className="text-sm font-medium text-slate-700">Image URL</label>
                <input
                  id="imageUrl"
                  value={form.imageUrl}
                  onChange={(event) => setForm((current) => ({ ...current, imageUrl: event.target.value }))}
                  className="mt-1 h-11 w-full rounded-xl border border-slate-200 px-4 text-sm outline-none focus:border-slate-400"
                />
                {imagePreviewUrl ? (
                  <div className="mt-2">
                    <img src={resolveAssetUrl(imagePreviewUrl)} alt="Preview" className="h-28 w-full rounded-md object-cover border" />
                  </div>
                ) : null}
              </div>

              <div>
                <label htmlFor="shortDescription" className="text-sm font-medium text-slate-700">Short description</label>
                <textarea
                  id="shortDescription"
                  rows={3}
                  value={form.shortDescription}
                  onChange={(event) => setForm((current) => ({ ...current, shortDescription: event.target.value }))}
                  className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:border-slate-400"
                />
              </div>

              <div>
                <label htmlFor="longDescription" className="text-sm font-medium text-slate-700">Long description</label>
                <textarea
                  id="longDescription"
                  rows={5}
                  value={form.longDescription}
                  onChange={(event) => setForm((current) => ({ ...current, longDescription: event.target.value }))}
                  className="mt-1 w-full rounded-xl border border-slate-200 px-4 py-3 text-sm outline-none focus:border-slate-400"
                />
              </div>

              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={saveProductMutation.isPending}
                  className="flex-1 rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-60"
                >
                  {saveProductMutation.isPending ? "Saving..." : editingProduct ? "Save changes" : "Create product"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEditingProduct(null);
                    setForm(emptyForm());
                    setError("");
                    setNotice("");
                  }}
                  className="rounded-xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Reset
                </button>
              </div>
            </form>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">Details</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-950">
              {selectedProduct ? selectedProduct.name : "Select a product"}
            </h2>

            {selectedProduct ? (
              <div className="mt-5 space-y-4">
                {selectedProduct.imageUrl ? (
                  <img
                    src={resolveAssetUrl(selectedProduct.imageUrl)}
                    alt={selectedProduct.name}
                    className="h-56 w-full rounded-2xl object-cover"
                  />
                ) : (
                  <div className="flex h-56 items-center justify-center rounded-2xl bg-slate-100 text-sm text-slate-400">
                    No image
                  </div>
                )}

                <div className="grid gap-3 text-sm">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <span className="text-slate-500">Category</span>
                    <span className="font-medium text-slate-950">{selectedProduct.categoryName || "Unassigned"}</span>
                  </div>
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <span className="text-slate-500">Stock</span>
                    <span className="font-medium text-slate-950">{selectedProduct.stock}</span>
                  </div>
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <span className="text-slate-500">Price</span>
                    <span className="font-medium text-slate-950">{formatPrice(selectedProduct.price)}</span>
                  </div>
                </div>

                <div>
                  <p className="text-sm font-medium text-slate-700">Short description</p>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    {selectedProduct.shortDescription || "No short description provided."}
                  </p>
                </div>

                <div>
                  <p className="text-sm font-medium text-slate-700">Long description</p>
                  <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">
                    {selectedProduct.longDescription || "No long description provided."}
                  </p>
                </div>
              </div>
            ) : (
              <p className="mt-4 text-sm text-slate-500">
                Use the list to inspect a product or open it in the editor.
              </p>
            )}
          </section>
        </aside>
      </main>
    </div>
  );
}
