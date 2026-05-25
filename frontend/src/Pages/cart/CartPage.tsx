import { useEffect, useState } from "react";
import { ArrowLeft, CreditCard, Minus, Plus, Receipt, ShoppingCart } from "lucide-react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { PageBackground, PageFooter } from "../extra/PageLayout";
import { API_BASE, formatPrice, getAuthHeaders, resolveAssetUrl } from "../../api";
import type { CartItem } from "../../api";

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
}

interface CartResponse {
  items: CartItem[];
  total: number;
}

function CartPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
  const token = localStorage.getItem("authToken");

  if (!token) {
    setError("Please log in to view your cart.");
    setLoading(false);
    return;
  }

  const fetchCart = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(`${API_BASE}/cart`, {
        method: "GET",
        headers: getAuthHeaders(),
      });

      let payload: ApiResponse<CartResponse>;

      try {
        payload = (await response.json()) as ApiResponse<CartResponse>;
      } catch {
        throw new Error("Invalid server response");
      }

      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(
          payload.error ||
            payload.message ||
            `Failed to load cart (${response.status})`,
        );
      }

      setItems(payload.data.items ?? []);
    } catch (fetchError) {
      console.error("Error fetching cart:", fetchError);

      setError(
        fetchError instanceof Error
          ? fetchError.message
          : "Something went wrong while fetching your cart.",
      );
    } finally {
      setLoading(false);
    }
  };

  fetchCart();
}, []);


  const cartTotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const updateQuantity = async (cartItemId: number, newQuantity: number) => {
    if (newQuantity < 1) return; // Don't allow quantities below 1

    try {
      const response = await fetch(`${API_BASE}/cart/items/${cartItemId}`, {
        method: "PUT",
        headers: getAuthHeaders(),
        body: JSON.stringify({ quantity: newQuantity })
      });

      if (response.ok) {
        // Update local state
        setItems(prev => prev.map(item =>
          item.id === cartItemId ? { ...item, quantity: newQuantity } : item
        ));
      } else if (response.status === 401) {
        try {
          toast.error("Please log in to update your cart.");
        } catch {}
      } else {
        try {
          toast.error("Failed to update quantity. Please try again.");
        } catch {}
      }
    } catch (error) {
      console.error("Error updating quantity:", error);
      try {
        toast.error("Error updating quantity. Please check your connection.");
      } catch {}
    }
  };

  const removeItem = async (cartItemId: number) => {
  try {
    const response = await fetch(`${API_BASE}/cart/items/${cartItemId}`, {
      method: "DELETE",
      headers: getAuthHeaders()
    });

    if (response.ok) {
      setItems(prev => prev.filter(item => item.id !== cartItemId));
    } else if (response.status === 401) {
      try {
        toast.error("Please log in to remove items from your cart.");
      } catch {}
    } else {
      try {
        toast.error("Failed to remove item. Please try again.");
      } catch {}
    }
  } catch (error) {
    console.error("Error removing item:", error);
    try {
      toast.error("Error removing item. Please check your connection.");
    } catch {}
  }
};

  const proceedToCheckout = () => {
    navigate("/checkout");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <main className="mx-auto max-w-6xl space-y-8 px-4 py-12 sm:px-6 lg:px-8">
        <div className="mb-12 text-center">
          <div className="inline-flex items-center rounded-full border border-white/50 bg-white/80 px-6 py-3 shadow-xl backdrop-blur-sm">
            <ShoppingCart className="mr-3 text-blue-600" size={24} />
            <div>
              <h2 className="bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-3xl font-bold text-transparent md:text-4xl">
                Your Shopping Cart
              </h2>
              <p className="mt-1 text-gray-600">
                {loading ? "Loading..." : `${items.length} items`}
              </p>
            </div>
          </div>
        </div>

        {loading && (
  <div className="space-y-4">
    {[1, 2, 3].map((i) => (
      <div
        key={i}
        className="animate-pulse rounded-3xl border border-white/50 bg-white/80 p-6 shadow-xl"
      >
        <div className="flex gap-4">
          {/* Image */}
          <div className="h-20 w-20 rounded-xl bg-gray-300" />

          {/* Text */}
          <div className="flex-1 space-y-3">
            <div className="h-4 w-3/4 rounded bg-gray-300" />
            <div className="h-4 w-1/2 rounded bg-gray-200" />
          </div>
        </div>

        {/* Bottom row */}
        <div className="mt-6 flex justify-between">
          <div className="h-10 w-32 rounded bg-gray-200" />
          <div className="h-6 w-20 rounded bg-gray-300" />
        </div>
      </div>
    ))}
  </div>
)}

        {!loading && error && (
          <div className="rounded-3xl border border-rose-200 bg-rose-50 p-10 text-center text-rose-700 shadow-xl backdrop-blur-sm">
            {error}
            {error === "Please log in to view your cart." && (
              <div className="mt-4">
                <button
                  onClick={() => window.location.href = '/login'}
                  className="rounded-lg bg-blue-600 px-6 py-2 text-white hover:bg-blue-700"
                >
                  Go to Login
                </button>
              </div>
            )}
          </div>
        )}

        {!loading && !error && items.length === 0 && (
          <div className="rounded-3xl border border-white/50 bg-white/80 p-10 text-center shadow-xl backdrop-blur-sm">
            <div className="mx-auto mb-5 flex h-20 w-20 items-center justify-center rounded-3xl bg-slate-100 text-slate-400">
              <ShoppingCart size={36} />
            </div>
            <h3 className="text-2xl font-semibold text-slate-900">Your cart is empty</h3>
            <p className="mx-auto mt-2 max-w-xl text-sm text-slate-600">
              Add some products to get started!
            </p>
            <div className="mt-6">
              <a
                href="/products"
                className="inline-flex items-center gap-2 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
              >
                <ShoppingCart size={16} />
                Browse Products
              </a>
            </div>
          </div>
        )}

        {!loading && !error && items.length > 0 && (
          <div className="grid gap-6 lg:grid-cols-3">
            <div className="space-y-4 lg:col-span-2">
              {items.map((item) => (
                <div
                  key={item.id}
                  className="group overflow-hidden rounded-3xl border border-white/50 bg-white/70 shadow-xl transition-all duration-300 hover:-translate-y-2 hover:shadow-2xl"
                >
                  <div className="px-4 py-2 sm:px-6 sm:py-3">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:space-x-6">
                        <div className="flex h-20 w-20 sm:h-24 sm:w-24 flex-shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-100 to-indigo-100">
                        <img src={resolveAssetUrl(item.imageUrl)} alt={item.productName} className="h-full w-full object-cover" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <h4 className="truncate text-xl font-bold text-gray-800">{item.productName}</h4>
                        <p className="mt-1 text-sm text-gray-500">{formatPrice(item.price)}</p>
                      </div>
                      <div className="flex-shrink-0 text-right">
                        <span className="inline-flex items-center rounded-full bg-green-100 px-3 py-1 text-xs font-medium text-green-800">
                          Stock: {item.stock}
                        </span>
                      </div>
                    </div>

                      <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <div className="flex items-center space-x-3">
                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity - 1)}
                          disabled={item.quantity <= 1}
                          className="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-xl font-bold shadow-md transition-all duration-200 hover:scale-110 hover:bg-gray-200 disabled:cursor-not-allowed disabled:opacity-50"
                          title="Decrease quantity"
                        >
                          <Minus size={18} />
                        </button>

                        <span className="w-16 text-center text-2xl font-bold text-gray-800">
                          {item.quantity}
                        </span>

                        <button
                          type="button"
                          onClick={() => updateQuantity(item.id, item.quantity + 1)}
                          disabled={item.quantity >= item.stock}
                          className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-600 text-xl font-bold text-white shadow-lg transition-all duration-200 hover:scale-110 hover:bg-blue-700 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-50"
                          title="Increase quantity"
                        >
                          <Plus size={18} />
                        </button>
                      </div>

                      <div className="flex items-center space-x-4">
                        <button
                          type="button"
                          onClick={() => removeItem(item.id)}
                          className="px-3 py-1 rounded-lg bg-red-50 text-red-600 font-medium hover:text-red-800 transition-colors"
                          title="Remove item"
                        >
                          Remove
                        </button>
                        <div className="text-right">
                          <p className="text-2xl font-bold text-gray-800">
                            {formatPrice(item.price * item.quantity)}
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div className="space-y-6">
              <div className="lg:sticky lg:top-8 rounded-3xl border border-white/50 bg-white/80 p-8 shadow-2xl backdrop-blur-sm">
                <h3 className="mb-6 flex items-center text-2xl font-bold text-gray-800">
                  <Receipt className="mr-3 text-blue-600" size={22} />
                  Order Summary
                </h3>

                <div className="mb-8 space-y-4">
                  <div className="flex justify-between text-lg">
                    <span className="text-gray-600">Subtotal ({items.length} items):</span>
                    <span className="font-bold text-gray-800">{formatPrice(cartTotal)}</span>
                  </div>
                  <div className="flex justify-between text-lg">
                    <span className="text-gray-600">Shipping:</span>
                    <span className="font-bold text-green-600">FREE</span>
                  </div>
                  <hr className="border-gray-200" />
                  <div className="flex justify-between text-2xl font-bold text-gray-800">
                    <span>Total:</span>
                    <span>{formatPrice(cartTotal)}</span>
                  </div>
                </div>

                <button onClick={proceedToCheckout} className="flex w-full items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4 text-lg font-bold text-white shadow-xl transition-all duration-300 hover:-translate-y-1 hover:from-blue-700 hover:to-blue-800 hover:shadow-2xl">
                  <CreditCard className="mr-3" size={20} />
                  Proceed to Checkout
                </button>

                <a
                  href="/products"
                  className="mt-4 block rounded-xl border-2 border-blue-200 bg-white/50 px-4 py-3 text-center font-semibold text-blue-600 backdrop-blur-sm transition-all duration-300 hover:border-blue-300 hover:text-blue-700"
                >
                  <ArrowLeft className="mr-2 inline-flex" size={16} />
                  Continue Shopping
                </a>
              </div>
            </div>
          </div>
        )}
      </main>

      <PageFooter />
    </div>
  );
}

export default CartPage;
