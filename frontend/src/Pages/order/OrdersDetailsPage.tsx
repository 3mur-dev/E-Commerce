import { Link, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import { fetchOrder, formatPrice } from "../../api";
import type { OrderResponse } from "../../api";

const statusStyles: Record<string, string> = {
  PENDING: "bg-amber-50 text-amber-700 border-amber-200",
  PAID: "bg-blue-50 text-blue-700 border-blue-200",
  SHIPPED: "bg-indigo-50 text-indigo-700 border-indigo-200",
  DELIVERED: "bg-emerald-50 text-emerald-700 border-emerald-200",
  CANCELLED: "bg-rose-50 text-rose-700 border-rose-200",
};

function Skeleton() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 animate-pulse">
      <div className="h-24 rounded-2xl bg-slate-200" />

      <div className="grid gap-6 md:grid-cols-2">
        <div className="h-64 rounded-2xl bg-slate-200" />
        <div className="h-64 rounded-2xl bg-slate-200" />
      </div>

      <div className="h-64 rounded-2xl bg-slate-200" />
    </div>
  );
}

export default function OrderDetails() {
  const { id } = useParams();

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    async function load() {
      if (!id) {
        setError("Order not found");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const data = await fetchOrder(id);

        if (!ignore) setOrder(data);
      } catch (e) {
        if (!ignore) {
          setError(e instanceof Error ? e.message : "Failed to load order");
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    }

    load();
    return () => {
      ignore = true; // prevents state update after unmount
    };
  }, [id]);

  if (loading) return <Skeleton />;

  if (error) {
    return (
      <div className="p-6">
        <p className="text-rose-600 font-medium">Error: {error}</p>
        <Link to="/orders" className="mt-4 inline-block text-blue-600 underline">
          Back to orders
        </Link>
      </div>
    );
  }

  if (!order) {
    return <div className="p-6 text-slate-500">Order not found.</div>;
  }

  const badgeClass =
    statusStyles[order.status] ??
    "bg-slate-50 text-slate-700 border-slate-200";

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-100 px-4 py-6 sm:p-6">
      <div className="mx-auto max-w-5xl space-y-6">

        {/* HEADER */}
        <div className="flex flex-wrap items-start justify-between gap-6 rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
          <div>
            <h1 className="text-2xl font-bold">
              Order #{order.orderNumber ?? order.id}
            </h1>
            <p className="text-sm text-slate-500">
              Created:{" "}
              {order.creationTimestamp
                ? new Date(order.creationTimestamp).toLocaleString()
                : "-"}
            </p>
          </div>

          <span className={`px-4 py-1.5 text-xs font-semibold border border-slate-100 rounded-full ${badgeClass}`}>
            {order.status}
          </span>
        </div>

        {/* GRID */}
        <div className="grid gap-6 md:grid-cols-2">

          {/* CUSTOMER */}
          <section className="rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold">Customer Details</h2>

            <div className="space-y-2 text-sm text-slate-700">
              <p><span className="text-slate-500">Name:</span> {order.customerName}</p>
              <p><span className="text-slate-500">Email:</span> {order.customerEmail}</p>
              <p><span className="text-slate-500">Phone:</span> {order.phone}</p>

              <div className="border-t pt-2">
                <p>{order.addressLine1}</p>
                {order.addressLine2 && <p>{order.addressLine2}</p>}
                <p>
                  {order.city}
                  {order.state ? `, ${order.state}` : ""} {order.postalCode}
                </p>
                <p className="text-slate-500">{order.country}</p>
              </div>

              {order.note && (
                <p className="italic text-slate-600">"{order.note}"</p>
              )}
            </div>
          </section>

          {/* PAYMENT */}
          <section className="rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold">Payment</h2>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">Method</span>
                <span className="font-medium">{order.paymentMethod}</span>
              </div>

              <div className="flex justify-between">
                <span className="text-slate-500">Status</span>
                <span className="font-medium">{order.paymentStatus}</span>
              </div>

              <div className="flex justify-between border-t pt-3">
                <span className="text-slate-500">Total</span>
                <span className="text-lg font-bold">
                  {formatPrice(order.total)}
                </span>
              </div>
            </div>
          </section>
        </div>

        {/* ITEMS */}
        <section className="rounded-2xl border border-slate-100 bg-white p-6 shadow-sm">
          <div className="mb-4 flex justify-between">
            <h2 className="text-lg font-semibold">Items</h2>
            <span className="text-xs text-slate-500">
              {order.items.length} item(s)
            </span>
          </div>

          {order.items.length === 0 ? (
            <p className="text-slate-500">No items found</p>
          ) : (
            <div className="overflow-x-auto border border-slate-100 rounded-xl">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-left text-slate-500">
                  <tr>
                    <th className="p-3">Product</th>
                    <th className="p-3">Qty</th>
                    <th className="p-3">Price</th>
                    <th className="p-3">Subtotal</th>
                  </tr>
                </thead>

                <tbody>
                  {order.items.map((item) => (
                    <tr key={item.id} className="border-t hover:bg-slate-50">
                      <td className="p-3">
                        <div className="font-medium">{item.productName}</div>
                        <div className="text-xs text-slate-400">
                          ID: {item.productId}
                        </div>
                      </td>
                      <td className="p-3">{item.quantity}</td>
                      <td className="p-3">{formatPrice(item.price)}</td>
                      <td className="p-3 font-medium">
                        {formatPrice(item.subtotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* BACK */}
        <Link
          to="/orders"
          className="inline-flex items-center gap-2 rounded-xl border border-slate-100 bg-white px-4 py-2 text-sm font-medium shadow-sm hover:bg-slate-50"
        >
          ← Back to orders
        </Link>
      </div>
    </div>
  );
}