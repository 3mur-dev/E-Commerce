'use client';

import { useEffect, useMemo, useState } from 'react';
import { API_BASE } from '../../api';
import { PageBackground, PageFooter } from '../extra/PageLayout';

interface Order {
  id: number;
  date?: string;
  creationTimestamp?: string;
  status: string;
  total: number;
  orderNumber?: string;
  items?: { id: number; name?: string; productName?: string }[];
}
interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem('authToken');
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
};

const formatMoney = (value: number) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value ?? 0);

const formatDate = (value?: string) => {
  if (!value) return 'Unknown date';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(d);
};

const statusStyles: Record<string, string> = {
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  PAID: 'bg-blue-50 text-blue-700 border-blue-200',
  SHIPPED: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  DELIVERED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED: 'bg-rose-50 text-rose-700 border-rose-200',
};

function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

useEffect(() => {
  const fetchOrders = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch(`${API_BASE}/orders/me`, {
        method: 'GET',
        headers: {
          ...getAuthHeaders(),
          'Content-Type': 'application/json',
        },
      });

      let payload: ApiResponse<Order[]>;

      try {
        payload = (await response.json()) as ApiResponse<Order[]>;
      } catch {
        throw new Error('Invalid server response');
      }

      if (!response.ok || !payload.success) {
        throw new Error(
          payload.message || `Failed to load orders (${response.status})`,
        );
      }

      setOrders(Array.isArray(payload.data) ? payload.data : []);
    } catch (e) {
      console.error('Error fetching orders:', e);

      setError(
        e instanceof Error ? e.message : 'Failed to load orders',
      );
    } finally {
      setLoading(false);
    }
  };

  void fetchOrders();
}, []);

  const stats = useMemo(() => {
    const totalSpent = orders.reduce((sum, order) => sum + (order.total || 0), 0);
    const delivered = orders.filter((order) => order.status === 'DELIVERED').length;
    return {
      totalSpent,
      delivered,
      count: orders.length,
    };
  }, [orders]);

  return (
    <div className="min-h-screen bg-slate-50">
      <PageBackground />

      <main className="relative mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <section className="mb-8 rounded-3xl border border-slate-200 bg-white/80 p-6 shadow-sm backdrop-blur-sm sm:p-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-2xl">
              <p className="mb-2 text-sm font-semibold uppercase tracking-wide text-primary-600">
                Account
              </p>
              <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                Your Orders
              </h1>
              <p className="mt-3 text-slate-600">
                Review order history, track fulfillment, and open any order for details.
              </p>
            </div>

            <div className="grid grid-cols-3 gap-3 sm:gap-4">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  Orders
                </div>
                <div className="mt-1 text-2xl font-bold text-slate-900">{stats.count}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  Delivered
                </div>
                <div className="mt-1 text-2xl font-bold text-slate-900">{stats.delivered}</div>
              </div>
              <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                  Spent
                </div>
                <div className="mt-1 text-2xl font-bold text-slate-900">
                  {formatMoney(stats.totalSpent)}
                </div>
              </div>
            </div>
          </div>
        </section>

        {loading && (
          <div className="grid gap-4">
            {[1, 2, 3].map((n) => (
              <div
                key={n}
                className="h-36 animate-pulse rounded-3xl border border-slate-200 bg-white shadow-sm"
              />
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="rounded-3xl border border-rose-200 bg-rose-50 p-6 text-rose-700 shadow-sm">
            <div className="font-semibold">Could not load orders</div>
            <div className="mt-1 text-sm">{error}</div>
          </div>
        )}

        {!loading && !error && orders.length === 0 && (
          <div className="rounded-3xl border border-slate-200 bg-white p-10 text-center shadow-sm">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-2xl">
              📦
            </div>
            <h2 className="text-xl font-semibold text-slate-900">No orders yet</h2>
            <p className="mt-2 text-slate-600">
              Your order history will appear here after your first purchase.
            </p>
          </div>
        )}

        {!loading && !error && orders.length > 0 && (
          <div className="grid gap-4">
            {orders.map((order) => {
              const createdAt = order.creationTimestamp ?? order.date;
              const badgeClass =
                statusStyles[order.status] ?? 'bg-slate-50 text-slate-700 border-slate-200';

              return (
                <a
                  key={order.id}
                  href={`/orders/${order.id}`}
                  className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md sm:p-6"
                >
                  <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-3">
                        <h2 className="text-lg font-semibold text-slate-900">
                          Order #{order.orderNumber ?? order.id}
                        </h2>
                        <span
                          className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-wide ${badgeClass}`}
                        >
                          {order.status}
                        </span>
                      </div>

                      <div className="mt-2 flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600">
                        <span>{formatDate(createdAt)}</span>
                        <span>{order.items?.length ?? 0} item(s)</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between gap-6 lg:justify-end">
                      <div className="text-right">
                        <div className="text-xs font-medium uppercase tracking-wide text-slate-500">
                          Total
                        </div>
                        <div className="text-2xl font-bold text-slate-900">
                          {formatMoney(order.total)}
                        </div>
                      </div>
                    </div>
                  </div>

                  {order.items && order.items.length > 0 && (
                    <div className="mt-5 border-t border-slate-100 pt-4">
                      <div className="flex flex-wrap gap-2">
                        {order.items.slice(0, 4).map((item) => (
                          <span
                            key={item.id}
                            className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs text-slate-700"
                          >
                            {item.name ?? item.productName ?? `Item ${item.id}`}
                          </span>
                        ))}
                        {order.items.length > 4 && (
                          <span className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs text-slate-700">
                            +{order.items.length - 4} more
                          </span>
                        )}
                      </div>
                    </div>
                  )}
                </a>
              );
            })}
          </div>
        )}
      </main>

      <PageFooter />
    </div>
  );
}

export default OrdersPage;
