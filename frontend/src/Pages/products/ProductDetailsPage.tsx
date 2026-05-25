import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { API_BASE, resolveAssetUrl } from "../../api";
import toast from "react-hot-toast";

interface Product {
  id: number;
  imageUrl: string;
  name: string;
  price: number;
  stock: number;
  shortDescription?: string;
  longDescription?: string;
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

type WishlistResponse = {
  items?: { productId?: number }[];
};

type WishlistResponseApiResponse = ApiResponse<WishlistResponse>;

type CartResponse = {
  items: unknown[];
  total: number;
};

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem("authToken");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    Accept: "application/json",
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
};

async function readErrorMessage(response: Response) {
  const text = await response.text().catch(() => "");
  if (!text) return `Request failed with status ${response.status}`;

  try {
    const data = JSON.parse(text) as { message?: string; error?: string };
    return data.message || data.error || text;
  } catch {
    return text;
  }
}

export default function ProductDetails() {
  const { id } = useParams();

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [liked, setLiked] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const res = await fetch(`${API_BASE}/products/${id}`, {
          headers: { Accept: "application/json" },
        });

        if (!res.ok) {
          throw new Error(await readErrorMessage(res));
        }

        const data = (await res.json()) as ApiResponse<Product>;

        setProduct(data.data ?? null);

        setLiked(
          Boolean(
            data.data?.favorited ??
              data.data?.isFavorited,
          ),
        );
      } catch (error) {
        console.error("Failed to load product:", error);
        setProduct(null);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      void fetchProduct();
    }
  }, [id]);

  const handleAddToCart = async (productId: number) => {
    try {
      const response = await fetch(`${API_BASE}/cart/add`, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({
          productId,
          quantity: 1,
        }),
      });

      if (response.status === 401) {
        setStatusMessage("Please log in to add items to your cart.");
        try {
          toast.error("Please log in to add items to your cart.");
        } catch {}
        return;
      }

      const payload =
        (await response.json()) as ApiResponse<CartResponse>;

      if (
        !response.ok ||
        !payload.success ||
        !payload.data
      ) {
        throw new Error(
          payload.message ||
            "Failed to add item to cart",
        );
      }

      const cart = payload.data;
      setStatusMessage(`Added to cart. Cart total: $${Number(cart.total ?? 0).toFixed(2)}`);
      try {
        toast.success(`Added to cart. Cart total: $${Number(cart.total ?? 0).toFixed(2)}`);
      } catch {}
    } catch (error) {
      console.error("Error adding to cart:", error);
      const msg = error instanceof Error ? error.message : "Error adding to cart. Please try again.";
      setStatusMessage(msg);
      try {
        toast.error(msg);
      } catch {}
    }
  };

  useEffect(() => {
    const fetchLikedState = async () => {
      const token = localStorage.getItem("authToken");
      if (!token || !id) {
        setLiked(false);
        return;
      }

      try {
        const res = await fetch(`${API_BASE}/wishlist`, {
          headers: getAuthHeaders(),
        });

        if (!res.ok) {
          setLiked(false);
          return;
        }

        const wishlist = (await res.json()) as WishlistResponseApiResponse;
        const isInWishlist = wishlist.data?.items?.some(
          (item) => String(item.productId) === String(id),
        );

        setLiked(Boolean(isInWishlist));
      } catch {
        setLiked(false);
      }
    };

    fetchLikedState();
  }, [id]);

  const handleToggleWishlist = async () => {
    if (!product) return;

    try {
      const res = await fetch(`${API_BASE}/products/${product.id}/favorite`, {
        method: "POST",
        headers: getAuthHeaders(),
      });


      if (res.status === 401) {
        setStatusMessage("Please log in to manage favorites.");
        try {
          toast.error("Please log in to manage favorites.");
        } catch {}
        return;
      }

      if (!res.ok) {
        throw new Error(await readErrorMessage(res));
      }

      const data = (await res.json()) as ApiResponse<Product>;
      const updated = data.data;

      setProduct(updated);
      setLiked(Boolean(updated.favorited ?? updated.isFavorited));
      const msg = updated.favorited || updated.isFavorited ? "Added to favorites." : "Removed from favorites.";
      setStatusMessage(msg);
      try {
        toast.success(msg);
      } catch {}
    } catch (err) {
      console.error(err);
      setStatusMessage("Failed to update favorites.");
      try {
        toast.error("Failed to update favorites.");
      } catch {}
    }
  };
  
  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-6">
        <div className="mx-auto max-w-6xl animate-pulse rounded-2xl bg-white p-6 shadow-xl md:p-10">
          <div className="flex flex-col gap-10 md:flex-row">
            <div className="w-full md:w-1/2">
              <div className="aspect-square w-full rounded-2xl bg-slate-200" />
            </div>
            <div className="flex w-full flex-col justify-between md:w-1/2">
              <div className="space-y-4">
                <div className="h-8 w-3/4 rounded bg-slate-300" />
                <div className="h-6 w-1/2 rounded bg-slate-200" />
                <div className="h-8 w-1/3 rounded bg-slate-300" />
                <div className="h-4 w-1/4 rounded bg-slate-200" />
                <div className="h-4 w-2/3 rounded bg-slate-200" />
              </div>
              <div className="mt-8 flex items-center gap-4">
                <div className="h-12 flex-1 rounded-xl bg-slate-300" />
                <div className="h-12 w-12 rounded-full bg-slate-200" />
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-6">
        <div className="mx-auto max-w-xl rounded-3xl border border-white/80 bg-white/85 p-10 text-center shadow-[0_14px_32px_-16px_rgba(15,23,42,0.35)] backdrop-blur-sm">
          <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-3xl bg-slate-100 text-slate-400">
            ?
          </div>
          <h3 className="text-2xl font-semibold text-slate-900">Product not found</h3>
          <p className="mx-auto mt-2 max-w-md text-sm text-slate-600">
            This product may have been removed or is no longer available.
          </p>
          <div className="mt-6">
            <a
              href="/products"
              className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
            >
              Back to Products
            </a>
          </div>
        </div>
      </div>
    );
  }

  const favorited = Boolean(liked || product.favorited || product.isFavorited);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 p-6">
      <div className="mx-auto max-w-6xl rounded-2xl bg-white p-6 shadow-xl md:p-10">
        <div className="flex flex-col gap-10 md:flex-row">
          <div className="w-full md:w-1/2">
            <div className="overflow-hidden rounded-2xl bg-slate-100 shadow-lg">
              <img
                src={resolveAssetUrl(product.imageUrl)}
                alt={product.name}
                className="h-full w-full object-cover transition-transform duration-500 hover:scale-105"
              />
            </div>
          </div>

          <div className="flex w-full flex-col justify-between md:w-1/2">
            <div>
              <h1 className="mb-3 text-3xl font-bold text-slate-900 md:text-4xl">
                {product.name}
              </h1>

              <p className="mt-2 text-sm leading-6 text-slate-600">
                {product.shortDescription || "No short description provided."}
              </p>

              <p className="mb-4 text-2xl font-semibold text-indigo-600">
                ${Number(product.price).toFixed(2)}
              </p>

              <p className="mb-6 text-sm text-slate-500">
                {product.stock > 0 ? (
                  <span className="font-medium text-green-600">
                    In stock ({product.stock})
                  </span>
                ) : (
                  <span className="font-medium text-red-500">Out of stock</span>
                )}
              </p>

              <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-600">
                {product.longDescription || "No detailed description available."}
              </p>

              {statusMessage && (
                <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                  {statusMessage}
                </div>
              )}
            </div>

            <div className="flex items-center gap-4">
              <button
                onClick={() => handleAddToCart(product.id)}
                className="flex-1 rounded-xl bg-indigo-600 py-3 font-semibold text-white shadow-md transition-all duration-300 hover:bg-indigo-700 hover:shadow-lg"
              >
                Add to Cart
              </button>

              <button
                onClick={handleToggleWishlist}
                aria-pressed={favorited}
                aria-label={favorited ? "Remove from wishlist" : "Add to wishlist"}
                title={favorited ? "Remove from wishlist" : "Add to wishlist"}
                className={`inline-flex h-12 w-12 items-center justify-center rounded-full border transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-pink-400 focus:ring-offset-2 active:scale-95 ${
                  favorited
                    ? "border-pink-200 bg-pink-50 text-pink-600 shadow-sm hover:bg-pink-100"
                    : "border-slate-300 bg-white text-slate-500 hover:border-pink-300 hover:bg-pink-50 hover:text-pink-600"
                }`}
              >
                <svg
                  viewBox="0 0 24 24"
                  fill={favorited ? "currentColor" : "none"}
                  stroke="currentColor"
                  strokeWidth="1.8"
                  className={`h-5 w-5 transition-transform duration-200 ${
                    favorited ? "scale-110" : "scale-100"
                  }`}
                >
                  <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41 0.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.53L12 21.35z" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
