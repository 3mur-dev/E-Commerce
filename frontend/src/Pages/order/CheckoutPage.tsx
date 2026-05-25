import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, CreditCard, MapPin, PackageCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";
import {
  createStripeSession,
  fetchCart,
  formatPrice,
  resolveAssetUrl,
  submitCheckout,
} from "../../api";
import type { CartItem, CheckoutRequest, PaymentMethod } from "../../api";
import type { FormEvent } from "react";
import { PageBackground, PageFooter } from "../extra/PageLayout";
import toast from "react-hot-toast";

const initialForm: Omit<CheckoutRequest, "idempotencyKey"> = {
  customerName: "",
  customerEmail: "",
  phone: "",
  addressLine1: "",
  addressLine2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "",
  paymentMethod: "CASH_ON_DELIVERY",
  note: "",
};

function CheckoutPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(initialForm);

  useEffect(() => {
    const token = localStorage.getItem("authToken");

    if (!token) {
      navigate("/login");
      return;
    }

    const loadCart = async () => {
      try {
        const cart = await fetchCart();
        setItems(cart.items || []);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load your cart.");
      } finally {
        setLoading(false);
      }
    };

    loadCart();
  }, [navigate]);

  const total = useMemo(
    () => items.reduce((sum, item) => sum + item.price * item.quantity, 0),
    [items],
  );

  const handleChange = (field: keyof typeof form, value: string | PaymentMethod) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleSubmit = async (e: FormEvent) => {
  e.preventDefault();

  setSubmitting(true);
  setError(null);

  try {
    const payload = {
      ...form,
      idempotencyKey: crypto.randomUUID(),
    };

    // STRIPE FLOW
    if (form.paymentMethod === "CARD") {
  const order = await createStripeSession(payload);
  const sessionUrl = order.sessionUrl ?? order.stripeCheckoutUrl;

  if (!sessionUrl) {
    throw new Error("Stripe session URL is missing from the server response.");
  }

  window.location.assign(sessionUrl);
  return;
}

    // NORMAL FLOW
    const order = await submitCheckout(payload);

    navigate(`/order-confirmation/${order.id}`, {
      state: { order },
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : "Checkout failed";
    setError(msg);
    try {
      toast.error(msg);
    } catch {}
  } finally {
    setSubmitting(false);
  }
};

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
        <PageBackground />
        <main className="mx-auto max-w-6xl px-4 py-16 text-center sm:px-6 lg:px-8">
          <div className="rounded-3xl border border-white/50 bg-white/80 p-10 shadow-xl backdrop-blur-sm">
            Loading checkout...
          </div>
        </main>
        <PageFooter />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <main className="mx-auto max-w-6xl space-y-8 px-4 py-12 sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.14em] text-blue-700">
              Checkout
            </p>
            <h1 className="mt-2 text-4xl font-bold text-slate-900">Complete your order</h1>
            <p className="mt-2 text-slate-600">
              Add your shipping details and choose a payment method.
            </p>
          </div>

          <button
            type="button"
            onClick={() => navigate("/cart")}
            className="inline-flex items-center gap-2 rounded-2xl border border-slate-200 bg-white/80 px-5 py-3 text-sm font-semibold text-slate-700 shadow-md transition hover:border-blue-300 hover:text-blue-700"
          >
            <ArrowLeft size={16} />
            Back to Cart
          </button>
        </div>

        {error && (
          <div className="rounded-3xl border border-rose-200 bg-rose-50 p-5 text-rose-700 shadow-lg">
            {error}
          </div>
        )}

        {!error && items.length === 0 && (
          <div className="rounded-3xl border border-white/50 bg-white/80 p-10 text-center shadow-xl backdrop-blur-sm">
            <h2 className="text-2xl font-semibold text-slate-900">Your cart is empty</h2>
            <p className="mt-2 text-slate-600">Add products before starting checkout.</p>
          </div>
        )}

        {items.length > 0 && (
          <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_360px]">
            <form
              onSubmit={handleSubmit}
              className="rounded-3xl border border-white/50 bg-white/80 p-8 shadow-xl backdrop-blur-sm"
            >
              <div className="mb-8 flex items-center gap-3">
                <div className="rounded-2xl bg-blue-100 p-3 text-blue-700">
                  <MapPin size={20} />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-slate-900">Shipping Details</h2>
                  <p className="text-sm text-slate-600">These fields match your Spring Boot API.</p>
                </div>
              </div>

              <div className="grid gap-5 md:grid-cols-2">
                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Full Name</span>
                  <input
                    value={form.customerName}
                    onChange={(event) => handleChange("customerName", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Email</span>
                  <input
                    type="email"
                    value={form.customerEmail}
                    onChange={(event) => handleChange("customerEmail", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Phone</span>
                  <input
                    value={form.phone}
                    onChange={(event) => handleChange("phone", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Country</span>
                  <input
                    value={form.country}
                    onChange={(event) => handleChange("country", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block md:col-span-2">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Address Line 1</span>
                  <input
                    value={form.addressLine1}
                    onChange={(event) => handleChange("addressLine1", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block md:col-span-2">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Address Line 2</span>
                  <input
                    value={form.addressLine2}
                    onChange={(event) => handleChange("addressLine2", event.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">City</span>
                  <input
                    value={form.city}
                    onChange={(event) => handleChange("city", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">State</span>
                  <input
                    value={form.state}
                    onChange={(event) => handleChange("state", event.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Postal Code</span>
                  <input
                    value={form.postalCode}
                    onChange={(event) => handleChange("postalCode", event.target.value)}
                    required
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>

                <label className="block">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Payment Method</span>
                  <select
                    value={form.paymentMethod}
                    onChange={(event) =>
                      handleChange("paymentMethod", event.target.value as PaymentMethod)
                    }
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  >
                    <option value="CASH_ON_DELIVERY">Cash on Delivery</option>
                    <option value="CARD">Card</option>
                  </select>
                </label>

                <label className="block md:col-span-2">
                  <span className="mb-2 block text-sm font-medium text-slate-700">Order Note</span>
                  <textarea
                    value={form.note}
                    onChange={(event) => handleChange("note", event.target.value)}
                    rows={4}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none transition focus:border-blue-400"
                  />
                </label>
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="mt-8 inline-flex w-full items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4 text-lg font-semibold text-white shadow-xl transition hover:-translate-y-0.5 hover:shadow-2xl disabled:cursor-not-allowed disabled:opacity-60"
              >
                <CreditCard size={20} />
                {submitting ? "Submitting order..." : "Submit Order"}
              </button>
            </form>

            <aside className="rounded-3xl border border-white/50 bg-white/80 p-8 shadow-xl backdrop-blur-sm">
              <div className="mb-6 flex items-center gap-3">
                <div className="rounded-2xl bg-emerald-100 p-3 text-emerald-700">
                  <PackageCheck size={20} />
                </div>
                <div>
                  <h2 className="text-2xl font-bold text-slate-900">Order Summary</h2>
                  <p className="text-sm text-slate-600">{items.length} items ready to order</p>
                </div>
              </div>

              <div className="space-y-4">
                {items.map((item) => (
                  <div key={item.id} className="flex items-center gap-4 rounded-2xl bg-slate-50 p-3">
                    <img
                      src={resolveAssetUrl(item.imageUrl)}
                      alt={item.productName}
                      className="h-16 w-16 rounded-2xl object-cover"
                    />
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-semibold text-slate-900">{item.productName}</p>
                      <p className="text-sm text-slate-500">Qty {item.quantity}</p>
                    </div>
                    <p className="font-semibold text-slate-900">
                      {formatPrice(item.price * item.quantity)}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-6 border-t border-slate-200 pt-6">
                <div className="flex items-center justify-between text-lg">
                  <span className="text-slate-600">Total</span>
                  <span className="text-2xl font-bold text-slate-900">{formatPrice(total)}</span>
                </div>
              </div>
            </aside>
          </div>
        )}
      </main>

      <PageFooter />
    </div>
  );
}

export default CheckoutPage;
