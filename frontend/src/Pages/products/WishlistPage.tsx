import { useEffect, useState } from "react";
import { Heart, House, ShoppingCart, Trash2 } from "lucide-react";
import { API_BASE, formatPrice, getAuthHeaders, resolveAssetUrl } from "../../api";
import toast from "react-hot-toast";
import { PageBackground, PageFooter } from "../extra/PageLayout";

interface WishlistItem {
  id: number;
  productId: number;
  productName: string;
  imageUrl?: string | null;
  price?: number | null;
  desiredQuantity: number;
  addedAt?: string;
}

interface WishlistResponse {
  id: number;
  name: string;
  visibility: "PRIVATE" | "PUBLIC" | "SHARED" | string;
  shareToken?: string | null;
  defaultList: boolean;
  userId: number | null;
  username: string | null;
  items: WishlistItem[];
}

interface apiResponse<T> {
  success: boolean;
  message: string;
  data: T;
};
const handleAddToCart = async (productId: number) => {
  try {
    const response = await fetch(`${API_BASE}/cart/add`, {
      method: "POST",
      headers: {
        ...getAuthHeaders(),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ productId, quantity: 1 }),
    });

    if (response.ok) {
      try {
        toast.success("Product added to cart successfully!");
      } catch {}
      return;
    }

    if (response.status === 401) {
      try {
        toast.error("Please log in to add items to your cart.");
      } catch {}
      return;
    }

    const errorText = await response.text().catch(() => null);
    try {
      toast.error(errorText || "Failed to add product to cart. Please try again.");
    } catch {}
  } catch (error) {
    console.error("Error adding to cart:", error);
    try {
      toast.error("Error adding to cart. Please check your connection.");
    } catch {}
  }
};

function WishlistPage() {
  const [wishlist, setWishlist] = useState<WishlistResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchWishlist = async () => {
      try {
        const response = await fetch(`${API_BASE}/wishlist`, {
          method: "GET",
          headers: getAuthHeaders(),
        });

        if (response.status === 401) {
          setError("Please log in to view your wishlist.");
          return;
        }

        if (!response.ok) {
          throw new Error(`Request failed with status ${response.status}`);
        }

        const data = (await response.json()) as apiResponse<WishlistResponse>;
        if (data.success) {
          setWishlist(data.data);
        } else {
          setError(data.message);
        }
      } catch (fetchError) {
        console.error("Error fetching wishlist:", fetchError);
        setError("Something went wrong while loading your wishlist. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchWishlist();
  }, []);

const handleRemoveFromWishlist = async (productId: number) => {
  if (!wishlist?.id) return;

  const previous = wishlist;

  // Optimistic UI update
  setWishlist((current) =>
    current
      ? {
          ...current,
          items: current.items.filter((item) => item.productId !== productId),
        }
      : current
  );

  try {
    const response = await fetch(
      `${API_BASE}/wishlist/${wishlist.id}/items/${productId}`,
      {
        method: "DELETE",
        headers: getAuthHeaders(),
      }
    );

    if (response.status === 401) {
      setWishlist(previous);
      try {
        toast.error("Please log in to modify your wishlist.");
      } catch {}
      return;
    }

    if (!response.ok) {
      setWishlist(previous);
      const errorText = await response.text().catch(() => null);
      try {
        toast.error(errorText || "Failed to remove item from wishlist.");
      } catch {}
      return;
    }

    const updatedWishlist = (await response.json()) as apiResponse<WishlistResponse>;
    if (updatedWishlist.success) {
      setWishlist(updatedWishlist.data);
    } else {
      setWishlist(previous);
      try {
        toast.error(updatedWishlist.message || "Failed to remove item from wishlist.");
      } catch {}
    }
  } catch (error) {
    console.error("Error removing wishlist item:", error);
    setWishlist(previous);
    try {
      toast.error("Error removing item from wishlist. Please check your connection.");
    } catch {}
  }
};

  const items = wishlist?.items ?? [];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <main className="mx-auto max-w-6xl space-y-8 px-4 py-12 sm:px-6 lg:px-8">
        <section className="rounded-3xl border border-white/50 bg-white/70 p-6 shadow-xl backdrop-blur-sm">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-800">
                {wishlist?.name ?? "Your Wishlist"}
              </h1>
              <p className="mt-1 text-sm text-gray-500">
                {loading ? "Loading..." : `${items.length} items`}
              </p>
            </div>


          </div>
        </section>

{loading && (
  <section className="space-y-4">
    {Array.from({ length: 4 }).map((_, i) => (
      <div
        key={i}
        className="animate-pulse rounded-3xl border border-white/50 bg-white/70 p-6 shadow-xl backdrop-blur-sm"
      >
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          
          {/* Image */}
          <div className="h-24 w-24 rounded-2xl bg-slate-200" />

          {/* Text */}
          <div className="flex-1 space-y-2">
            <div className="h-5 w-2/3 rounded bg-slate-300" />
            <div className="h-4 w-1/3 rounded bg-slate-200" />
            <div className="h-3 w-1/4 rounded bg-slate-200" />
          </div>

          {/* Buttons */}
          <div className="flex flex-col gap-3 sm:w-52">
            <div className="h-10 w-full rounded-xl bg-slate-300" />
            <div className="h-10 w-full rounded-xl bg-slate-200" />
            <div className="h-10 w-full rounded-xl bg-slate-200" />
          </div>
        </div>
      </div>
    ))}
  </section>
)}

        {!loading && error && (
  <section className="rounded-3xl border border-rose-200 bg-rose-50 p-10 text-center text-rose-700 shadow-xl backdrop-blur-sm">
    
    <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-3xl bg-rose-100 text-rose-400">
      ❌
    </div>

    <h3 className="text-2xl font-semibold text-rose-800">
      Something went wrong
    </h3>

    <p className="mx-auto mt-2 max-w-xl text-sm text-rose-700">
      {error}
    </p>

    <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
      <button
        onClick={() => window.location.reload()}
        className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-rose-500 to-rose-600 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
      >
        Retry
      </button>

      <a
        href="/products"
        className="inline-flex items-center gap-2 rounded-2xl border border-rose-200 bg-white px-5 py-3 text-sm font-semibold text-rose-700 transition hover:bg-rose-100"
      >
        Browse Products
      </a>
    </div>
  </section>
)}

        {!loading && !error && items.length === 0 && (
          <section className="rounded-3xl border border-white/50 bg-white/70 p-10 text-center shadow-xl backdrop-blur-sm">
            <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-3xl bg-slate-100 text-slate-400">
              <Heart size={36} />
            </div>
            <h3 className="text-2xl font-semibold text-slate-900">No items in your wishlist yet</h3>
            <p className="mx-auto mt-2 max-w-xl text-sm text-slate-600">
              Tap the heart on any product to save it here for later.
            </p>
            <div className="mt-6">
              <a
                href="/products"
                className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
              >
                <House size={16} />
                Browse Products
              </a>
            </div>
          </section>
        )}

        {!loading && !error && items.length > 0 && (
          <section className="space-y-4">
            {items.map((item) => (
              <div
                key={item.id}
                className="rounded-3xl border border-white/50 bg-white/70 p-6 shadow-xl backdrop-blur-sm"
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
                  <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-2xl bg-gray-100">
                    {item.imageUrl ? (
                      <img
                        src={resolveAssetUrl(item.imageUrl)}
                        alt={item.productName}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <Heart className="text-rose-400" size={28} />
                    )}
                  </div>

                  <div className="flex-1 space-y-1">
                    <h4 className="text-xl font-bold text-gray-800">{item.productName}</h4>
                    <p className="text-sm text-gray-500">
                      {item.price != null ? formatPrice(item.price) : "No price"}
                    </p>
                    <p className="text-xs text-gray-500">Quantity: {item.desiredQuantity}</p>
                  </div>

                  <div className="flex flex-col gap-3 sm:w-52">
                    <button
                      type="button"
                      onClick={() => void handleAddToCart(item.productId)}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 py-2 font-bold text-white shadow-lg transition-all hover:from-blue-700 hover:to-blue-800"
                    >
                      <ShoppingCart size={16} />
                      Add to Cart
                    </button>

                    <button
                      type="button"
                      onClick={() => void handleRemoveFromWishlist(item.productId)}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl border border-rose-200 bg-rose-50 py-2 font-semibold text-rose-700 transition-all hover:bg-rose-100"
                    >
                      <Trash2 size={16} />
                      Remove
                    </button>

                    <a
                      href={`/products/${item.productId}`}
                      className="flex w-full items-center justify-center gap-2 rounded-2xl border border-white/60 bg-white/90 py-2 font-semibold text-gray-700 transition-all hover:bg-white"
                    >
                      <House size={16} />
                      View Product
                    </a>
                  </div>
                </div>
              </div>
            ))}
          </section>
        )}
      </main>

      <PageFooter />
    </div>
  );
}

export default WishlistPage
