import { useEffect, useState } from "react";
import { CheckCircle2, Package, ShoppingBag } from "lucide-react";
import { Link, useLocation, useParams } from "react-router-dom";
import { fetchOrder, formatPrice, resolveAssetUrl } from "../../api";
import type { OrderResponse } from "../../api";
import { PageBackground, PageFooter } from "../extra/PageLayout";

function OrderConfirmationPage() {
  const { id } = useParams();
  const location = useLocation();
  const [order, setOrder] = useState<OrderResponse | null>(
    (location.state as { order?: OrderResponse } | null)?.order || null,
  );
  const [loading, setLoading] = useState(order == null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (order || !id) {
      return;
    }

    const loadOrder = async () => {
      try {
        const response = await fetchOrder(id);
        setOrder(response);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load order.");
      } finally {
        setLoading(false);
      }
    };

    loadOrder();
  }, [id, order]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <main className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="rounded-[2rem] border border-white/50 bg-white/85 p-8 shadow-2xl backdrop-blur-sm sm:p-10">
          {loading && <p className="text-center text-slate-600">Loading order details...</p>}

          {!loading && error && (
            <div className="rounded-3xl border border-rose-200 bg-rose-50 p-6 text-center text-rose-700">
              {error}
            </div>
          )}

          {!loading && order && (
            <>
              <div className="text-center">
                <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                  <CheckCircle2 size={42} />
                </div>
                <h1 className="mt-6 text-4xl font-bold text-slate-900">Order confirmed</h1>
                <p className="mt-3 text-slate-600">
                  Thanks for your purchase. Your order has been created successfully.
                </p>
              </div>

              <div className="mt-10 grid gap-6 md:grid-cols-2">
                <div className="rounded-3xl bg-slate-50 p-6">
                  <h2 className="flex items-center gap-2 text-xl font-semibold text-slate-900">
                    <Package size={20} />
                    Order Details
                  </h2>
                  <div className="mt-4 space-y-3 text-sm text-slate-700">
                    <p>
                      <span className="font-semibold">Order Number:</span> {order.orderNumber}
                    </p>
                    <p>
                      <span className="font-semibold">Status:</span> {order.status}
                    </p>
                    <p>
                      <span className="font-semibold">Payment:</span> {order.paymentMethod}
                    </p>
                    <p>
                      <span className="font-semibold">Payment Status:</span> {order.paymentStatus}
                    </p>
                    <p>
                      <span className="font-semibold">Total:</span> {formatPrice(order.total)}
                    </p>
                  </div>
                </div>

                <div className="rounded-3xl bg-slate-50 p-6">
                  <h2 className="text-xl font-semibold text-slate-900">Shipping Details</h2>
                  <div className="mt-4 space-y-3 text-sm text-slate-700">
                    <p className="font-semibold text-slate-900">{order.customerName}</p>
                    <p>{order.customerEmail}</p>
                    <p>{order.phone}</p>
                    <p>{order.addressLine1}</p>
                    {order.addressLine2 && <p>{order.addressLine2}</p>}
                    <p>
                      {order.city}
                      {order.state ? `, ${order.state}` : ""} {order.postalCode}
                    </p>
                    <p>{order.country}</p>
                    {order.note && (
                      <p>
                        <span className="font-semibold">Note:</span> {order.note}
                      </p>
                    )}
                  </div>
                </div>
              </div>

              <div className="mt-8 rounded-3xl bg-slate-50 p-6">
                <h2 className="flex items-center gap-2 text-xl font-semibold text-slate-900">
                  <ShoppingBag size={20} />
                  Items
                </h2>
                <div className="mt-4 space-y-4">
                  {order.items.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center gap-4 rounded-2xl bg-white p-4 shadow-sm"
                    >
                      <img
                        src={resolveAssetUrl(item.imageUrl)}
                        alt={item.productName}
                        className="h-16 w-16 rounded-2xl object-cover"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-semibold text-slate-900">{item.productName}</p>
                        <p className="text-sm text-slate-500">Quantity: {item.quantity}</p>
                      </div>
                      <p className="font-semibold text-slate-900">{formatPrice(item.subtotal)}</p>
                    </div>
                  ))}
                </div>
              </div>

              <div className="mt-8 flex flex-wrap justify-center gap-4">
                <Link
                  to="/products"
                  className="inline-flex items-center rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-3 font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
                >
                  Continue Shopping
                </Link>
                <Link
                  to="/cart"
                  className="inline-flex items-center rounded-2xl border border-slate-200 bg-white px-6 py-3 font-semibold text-slate-700 transition hover:border-blue-300 hover:text-blue-700"
                >
                  Back to Cart
                </Link>
              </div>
            </>
          )}
        </div>
      </main>

      <PageFooter />
    </div>
  );
}

export default OrderConfirmationPage;
