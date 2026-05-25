import { type ReactNode, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_BASE, getAuthHeaders } from "../../api";
import logServerError from "../../utils/devLogger";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { confirmDangerousAction } from "./adminShared";

const REFRESH_MS = 30_000;
const DEFAULT_PAGE_SIZE = 8;
const ORDER_STATUSES = ["PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"] as const;

type OrderStatus = (typeof ORDER_STATUSES)[number];
type SortField = "created" | "total" | "status" | "customer" | "items" | "id";
type SortDirection = "asc" | "desc";

type AdminOrderItem = {
  id?: number;
  productId?: number;
  productName?: string | null;
  name?: string | null;
  quantity?: number | null;
  price?: number | string | null;
  subtotal?: number | string | null;
};

type AdminOrderResponse = {
  id: number;
  orderNumber?: string | null;
  status?: string | null;
  paymentMethod?: string | null;
  paymentStatus?: string | null;
  creationTimestamp?: string | null;
  date?: string | null;
  total?: number | string | null;
  customerName?: string | null;
  customerEmail?: string | null;
  phone?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
  note?: string | null;
  items?: AdminOrderItem[] | null;
  user?: {
    email?: string | null;
    name?: string | null;
  } | null;
};

type ApiResponse<T> = {
  success: boolean;
  data?: T;
  message?: string;
};

const statusStyles: Record<string, string> = {
  PENDING: "border-amber-200 bg-amber-50 text-amber-700",
  PROCESSING: "border-blue-200 bg-blue-50 text-blue-700",
  SHIPPED: "border-indigo-200 bg-indigo-50 text-indigo-700",
  DELIVERED: "border-emerald-200 bg-emerald-50 text-emerald-700",
  CANCELLED: "border-rose-200 bg-rose-50 text-rose-700",
};

const toNumber = (value: string | number | null | undefined) => {
  if (value === null || value === undefined) return 0;
  const num = typeof value === "string" ? Number(value) : value;
  return Number.isFinite(num) ? num : 0;
};

const formatKD = (value: string | number | null | undefined) =>
  `${toNumber(value).toFixed(2)} KD`;

const formatCount = (value: number) => new Intl.NumberFormat("en-US").format(value);

const formatRelativeTime = (date: Date) => {
  const diff = Date.now() - date.getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ago`;
};

const formatDateTime = (value?: string | null) => {
  if (!value) return "Unknown date";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
};

const getStatusTone = (status?: string | null) =>
  statusStyles[status ?? ""] ?? "border-slate-200 bg-slate-50 text-slate-700";

const normalizeStatus = (status?: string | null) => {
  const upper = (status ?? "").toUpperCase();
  return ORDER_STATUSES.includes(upper as OrderStatus) ? (upper as OrderStatus) : "PENDING";
};

const getCreatedAt = (order: AdminOrderResponse) => order.creationTimestamp ?? order.date ?? "";

const getCustomerLabel = (order: AdminOrderResponse) =>
  order.customerName?.trim() || order.user?.name?.trim() || order.customerEmail?.trim() || order.user?.email?.trim() || "Customer";

const getCustomerSecondary = (order: AdminOrderResponse) =>
  order.customerEmail?.trim() || order.user?.email?.trim() || order.phone?.trim() || "No contact info";

const getItemLabel = (item: AdminOrderItem) =>
  item.productName?.trim() || item.name?.trim() || (item.productId ? `Product #${item.productId}` : "Unnamed item");

const formatSortLabel = (field: SortField, direction: SortDirection) => {
  const prefix = direction === "asc" ? "Asc" : "Desc";
  const label =
    field === "created"
      ? "Date"
      : field === "total"
        ? "Total"
        : field === "status"
          ? "Status"
          : field === "customer"
            ? "Customer"
            : field === "items"
              ? "Items"
              : "ID";
  return `${label} ${prefix}`;
};

async function parseError(response: Response) {
  const text = await response.text().catch(() => "");
  if (!text) return `Request failed with status ${response.status}`;

  try {
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    return parsed.message || parsed.error || text;
  } catch {
    return text;
  }
}

function MetricCard({
  label,
  value,
  hint,
}: {
  label: string;
  value: string;
  hint: string;
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:shadow-md">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{value}</h2>
      <p className="mt-4 text-sm leading-6 text-slate-500">{hint}</p>
    </section>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between border-b border-slate-100 py-3 last:border-0">
      <span className="text-sm text-slate-500">{label}</span>
      <span className="text-sm font-semibold text-slate-950">{value}</span>
    </div>
  );
}

function ModalShell({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  useEffect(() => {
    if (!open) return;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/50 px-4 py-6">
      <div className="max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Order details</p>
            <h3 className="mt-1 text-xl font-semibold text-slate-950">{title}</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            Close
          </button>
        </div>
        <div className="max-h-[calc(90vh-73px)] overflow-auto p-5">{children}</div>
      </div>
    </div>
  );
}

export default function AdminOrdersPage() {
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [serverTotal, setServerTotal] = useState<number | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [sortField, setSortField] = useState<SortField>("created");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [currentPage, setCurrentPage] = useState(1);
  const [detailsOrder, setDetailsOrder] = useState<AdminOrderResponse | null>(null);
  const [actionBusyId, setActionBusyId] = useState<number | null>(null);
  const queryClient = useQueryClient();
  const search = useDebouncedValue(searchInput.trim(), 350);
  const {
    data: orders = [],
    isLoading: loading,
    isFetching,
    refetch: refetchOrders,
    dataUpdatedAt,
  } = useQuery<AdminOrderResponse[], Error>({
    queryKey: ["admin-orders", { page: currentPage, size: pageSize, search, status: statusFilter, sort: `${sortField},${sortDirection}` }],
    queryFn: async ({ signal, queryKey }: { signal?: AbortSignal; queryKey: unknown }) => {
      const [, params] = queryKey as unknown as [string, Record<string, unknown>];
      const qp = new URLSearchParams();
      if (params.search) qp.set("search", String(params.search));
      if (params.status && String(params.status) !== "ALL") qp.set("status", String(params.status));
      qp.set("page", String(params.page ?? 1));
      qp.set("size", String(params.size ?? DEFAULT_PAGE_SIZE));
      qp.set("sort", String(params.sort ?? "created,desc"));

      const url = `${API_BASE}/admin/orders?${qp.toString()}`;

      const response = await fetch(url, {
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        signal,
      });

      if (!response.ok) {
        if (response.status >= 500) logServerError(response, "/admin/orders");
        throw new Error(await parseError(response));
      }

      const json = (await response.json()) as ApiResponse<unknown>;
      if (!json.success) throw new Error(json.message || "Invalid response");

      // support both array and paginated shapes { items: [], total: number }
      if (Array.isArray(json.data)) {
        setServerTotal(null);
        return json.data as AdminOrderResponse[];
      }

      const maybe = json.data as { items?: AdminOrderResponse[]; total?: number } | null;
      if (maybe && Array.isArray(maybe.items)) {
        setServerTotal(typeof maybe.total === "number" ? maybe.total : maybe.items.length);
        return maybe.items;
      }

      setServerTotal(null);
      return [] as AdminOrderResponse[];
    },
    refetchInterval: REFRESH_MS,
  });

  useEffect(() => {
    setCurrentPage(1);
  }, [search, statusFilter, pageSize, sortField, sortDirection]);

  const isServerMode = serverTotal !== null;

  const stats = useMemo(() => {
    const total = isServerMode ? (serverTotal ?? orders.length) : orders.length;
    const pending = orders.filter((order) => normalizeStatus(order.status) === "PENDING").length;
    const shipped = orders.filter((order) => normalizeStatus(order.status) === "SHIPPED").length;
    const delivered = orders.filter((order) => normalizeStatus(order.status) === "DELIVERED").length;
    const revenue = orders.reduce((sum, order) => sum + toNumber(order.total), 0);
    return { total, pending, shipped, delivered, revenue };
  }, [orders]);

  const filteredSortedOrders = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (isServerMode) {
      // when server-mode is enabled the server is responsible for filtering/sorting/pagination
      return orders;
    }

    const items = orders.filter((order) => {
      const status = normalizeStatus(order.status);
      if (statusFilter !== "ALL" && status !== statusFilter) return false;

      if (!query) return true;

      const haystack = [
        String(order.id),
        order.orderNumber,
        order.status,
        order.paymentMethod,
        order.paymentStatus,
        order.customerName,
        order.customerEmail,
        order.phone,
        order.addressLine1,
        order.city,
        order.country,
        order.user?.email,
        order.items?.map((item) => getItemLabel(item)).join(" "),
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return haystack.includes(query);
    });

    items.sort((a, b) => {
      const direction = sortDirection === "asc" ? 1 : -1;
      const valueFor = (order: AdminOrderResponse) => {
        switch (sortField) {
          case "created":
            return new Date(getCreatedAt(order)).getTime() || 0;
          case "total":
            return toNumber(order.total);
          case "status":
            return normalizeStatus(order.status);
          case "customer":
            return getCustomerLabel(order);
          case "items":
            return order.items?.length ?? 0;
          default:
            return order.id;
        }
      };

      const left = valueFor(a);
      const right = valueFor(b);

      if (typeof left === "number" && typeof right === "number") {
        return (left - right) * direction;
      }

      return String(left).localeCompare(String(right)) * direction;
    });

    return items;
  }, [orders, search, statusFilter, sortField, sortDirection]);

  const totalCount = isServerMode ? (serverTotal ?? 0) : filteredSortedOrders.length;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  const safePage = Math.min(currentPage, totalPages);

  useEffect(() => {
    if (currentPage !== safePage) setCurrentPage(safePage);
  }, [currentPage, safePage]);

  const paginatedOrders = useMemo(() => {
    if (isServerMode) return orders;
    const start = (safePage - 1) * pageSize;
    return filteredSortedOrders.slice(start, start + pageSize);
  }, [filteredSortedOrders, safePage, pageSize, orders, isServerMode]);

  const lastUpdatedLabel = dataUpdatedAt ? formatRelativeTime(new Date(dataUpdatedAt)) : "just now";
  const refreshing = isFetching && !loading;

  const updateOrderInState = (orderId: number, status: OrderStatus) => {
    queryClient.setQueryData<AdminOrderResponse[]>(["admin-orders"], (prev = []) =>
      prev.map((order) => (order.id === orderId ? { ...order, status } : order)),
    );
    setDetailsOrder((prev) => (prev && prev.id === orderId ? { ...prev, status } : prev));
  };

  const orderStatusMutation = useMutation({
    mutationFn: async ({ order, nextStatus }: { order: AdminOrderResponse; nextStatus: OrderStatus }) => {
      const response = await fetch(`${API_BASE}/admin/orders/${order.id}/status`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify({ status: nextStatus }),
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      return response.json() as Promise<AdminOrderResponse>;
    },
    onMutate: async ({ order, nextStatus }) => {
      setActionBusyId(order.id);
      setError("");
      setNotice("");

      const previousOrders = queryClient.getQueryData<AdminOrderResponse[]>(["admin-orders"]) || [];
      queryClient.setQueryData<AdminOrderResponse[]>(["admin-orders"], (prev = []) =>
        prev.map((item) => (item.id === order.id ? { ...item, status: nextStatus } : item)),
      );

      if (detailsOrder?.id === order.id) {
        setDetailsOrder({ ...detailsOrder, status: nextStatus });
      }

      return { previousOrders };
    },
    onSuccess: (updatedOrder, { order, nextStatus }) => {
      const statusToApply = normalizeStatus(updatedOrder.status) || nextStatus;
      updateOrderInState(order.id, statusToApply);
      setNotice(`Updated order #${order.orderNumber ?? order.id}`);
    },
    onError: (err, _variables, context) => {
      if (context?.previousOrders) {
        queryClient.setQueryData(["admin-orders"], context.previousOrders);
      }
      setError(err instanceof Error ? err.message : "Error updating order");
    },
    onSettled: () => {
      setActionBusyId(null);
    },
  });

  const updateOrderStatus = async (order: AdminOrderResponse, nextStatus: OrderStatus) => {
    const currentStatus = normalizeStatus(order.status);
    if (currentStatus === nextStatus) return;
    if (
      nextStatus === "CANCELLED"
      && !confirmDangerousAction(`Cancel order #${order.orderNumber ?? order.id}? This is usually a one-way operational change.`)
    ) {
      return;
    }

    setActionBusyId(order.id);
    setError("");
    setNotice("");
    await orderStatusMutation.mutateAsync({ order, nextStatus });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div className="space-y-6">
            <div className="h-10 w-72 animate-pulse rounded-lg bg-slate-200" />
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
            </div>
            <div className="grid gap-4 lg:grid-cols-3">
              <div className="h-[34rem] animate-pulse rounded-2xl bg-slate-200 lg:col-span-2" />
              <div className="h-[34rem] animate-pulse rounded-2xl bg-slate-200" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error && orders.length === 0) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto flex max-w-7xl items-center justify-center px-4 py-16 sm:px-6 lg:px-8">
          <div className="w-full max-w-xl rounded-2xl border border-red-200 bg-white p-6 shadow-sm">
            <p className="text-sm font-medium text-red-600">Orders unavailable</p>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">{error}</h1>
            <p className="mt-3 text-sm text-slate-500">
              Verify the admin token, backend URL, and order admin endpoints.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white/80 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm font-medium uppercase tracking-[0.2em] text-slate-500">
                Admin Orders
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
                Orders management
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
                Live order feed, search, status filters, quick status updates, and a detailed
                inspection modal for each purchase.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <div className="rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
                Live refresh
              </div>
              <div className="rounded-full border border-slate-200 bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700">
                Updated {lastUpdatedLabel}
              </div>
              {refreshing ? (
                <div className="rounded-full border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-medium text-indigo-700">
                  Refreshing
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
          <MetricCard label="Total orders" value={formatCount(stats.total)} hint="Every order returned by the admin orders endpoint." />
          <MetricCard label="Pending" value={formatCount(stats.pending)} hint="Orders waiting for payment or fulfillment progress." />
          <MetricCard label="Shipped" value={formatCount(stats.shipped)} hint="Orders already marked as shipped." />
          <MetricCard label="Delivered" value={formatCount(stats.delivered)} hint="Completed deliveries visible in the current dataset." />
          <MetricCard label="Revenue" value={formatKD(stats.revenue)} hint="Summed total value across the loaded orders." />
        </section>

        <section className="mt-6 grid gap-4 lg:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-2">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">Orders table</p>
                  <h2 className="mt-1 text-xl font-semibold text-slate-950">
                    Order list from /api/admin/orders
                  </h2>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
                  <input
                    value={searchInput}
                    onChange={(event) => setSearchInput(event.target.value)}
                    placeholder="Search orders"
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-slate-400 sm:w-72"
                  />
                  <select
                    value={statusFilter}
                    onChange={(event) => setStatusFilter(event.target.value)}
                    className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition focus:border-slate-400"
                  >
                    <option value="ALL">All statuses</option>
                    {ORDER_STATUSES.map((status) => (
                      <option key={status} value={status}>
                        {status}
                      </option>
                    ))}
                  </select>
                  <select
                    value={sortField}
                    onChange={(event) => setSortField(event.target.value as SortField)}
                    className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition focus:border-slate-400"
                  >
                    <option value="created">Sort by date</option>
                    <option value="total">Sort by total</option>
                    <option value="status">Sort by status</option>
                    <option value="customer">Sort by customer</option>
                    <option value="items">Sort by item count</option>
                    <option value="id">Sort by id</option>
                  </select>
                  <button
                    type="button"
                    onClick={() => setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"))}
                    className="h-11 rounded-xl border border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                  >
                    {formatSortLabel(sortField, sortDirection)}
                  </button>
                  <button
                    type="button"
                    onClick={() => void refetchOrders()}
                    className="h-11 rounded-xl border border-slate-200 bg-slate-900 px-4 text-sm font-medium text-white transition hover:bg-slate-800"
                  >
                    Refresh
                  </button>
                </div>
              </div>

              <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
                <div>{filteredSortedOrders.length} orders match the current filter.</div>
                <div className="flex items-center gap-3">
                  <span>Rows per page</span>
                  <select
                    value={pageSize}
                    onChange={(event) => setPageSize(Number(event.target.value))}
                    className="h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none focus:border-slate-400"
                  >
                    <option value={6}>6</option>
                    <option value={8}>8</option>
                    <option value={12}>12</option>
                    <option value={24}>24</option>
                  </select>
                </div>
              </div>

              {error ? (
                <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                  {error}
                </div>
              ) : null}

              {notice ? (
                <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
                  {notice}
                </div>
              ) : null}
            </div>

            <div className="mt-5 overflow-hidden rounded-xl border border-slate-200">
              <div className="space-y-3 p-4 sm:hidden">
                {paginatedOrders.length === 0 ? (
                  <div className="py-8 text-center text-sm text-slate-500">
                    No orders match this filter.
                  </div>
                ) : (
                  paginatedOrders.map((order) => {
                    const status = normalizeStatus(order.status);
                    const busy = actionBusyId === order.id;

                    return (
                      <div key={order.id} className="rounded-2xl border border-slate-200 bg-white p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <button
                              type="button"
                              onClick={() => setDetailsOrder(order)}
                              className="text-left text-sm font-semibold text-slate-950 hover:underline"
                            >
                              Order #{order.orderNumber ?? order.id}
                            </button>
                            <p className="mt-1 text-xs text-slate-500">
                              {getCustomerLabel(order)} · {formatDateTime(getCreatedAt(order))}
                            </p>
                          </div>
                          <span className={`rounded-full border px-2 py-1 text-[11px] font-semibold ${getStatusTone(status)}`}>
                            {status}
                          </span>
                        </div>

                        <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
                          <div>
                            <p className="text-slate-500">Total</p>
                            <p className="font-semibold text-slate-950">{formatKD(order.total)}</p>
                          </div>
                          <div>
                            <p className="text-slate-500">Items</p>
                            <p className="font-semibold text-slate-950">{formatCount(order.items?.length ?? 0)}</p>
                          </div>
                        </div>

                        <div className="mt-3 flex flex-col gap-2">
                          <select
                            value={status}
                            onChange={(event) => void updateOrderStatus(order, event.target.value as OrderStatus)}
                            disabled={busy}
                            className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {ORDER_STATUSES.map((option) => (
                              <option key={option} value={option}>
                                {option}
                              </option>
                            ))}
                          </select>
                          <button
                            type="button"
                            onClick={() => setDetailsOrder(order)}
                            className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700"
                          >
                            View details
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              <div className="hidden sm:block">
                <div className="max-h-[34rem] overflow-auto">
                  <table className="min-w-full divide-y divide-slate-200">
                    <thead className="sticky top-0 bg-slate-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Order
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Customer
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Total
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Status
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Date
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {paginatedOrders.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="px-4 py-12 text-center text-sm text-slate-500">
                            No orders match this filter.
                          </td>
                        </tr>
                      ) : (
                        paginatedOrders.map((order) => {
                          const status = normalizeStatus(order.status);
                          const busy = actionBusyId === order.id;

                          return (
                            <tr key={order.id} className="group align-top transition hover:bg-slate-50">
                              <td className="px-4 py-4">
                                <div className="min-w-0">
                                  <button
                                    type="button"
                                    onClick={() => setDetailsOrder(order)}
                                    className="truncate text-left text-sm font-semibold text-slate-950 hover:underline"
                                  >
                                    Order #{order.orderNumber ?? order.id}
                                  </button>
                                  <p className="mt-1 text-sm leading-6 text-slate-500">
                                    {formatCount(order.items?.length ?? 0)} item(s) · {order.paymentMethod || "Payment pending"}
                                  </p>
                                  <p className="mt-1 text-xs text-slate-400">
                                    ID {order.id}
                                  </p>
                                </div>
                              </td>
                              <td className="px-4 py-4">
                                <div>
                                  <p className="text-sm font-semibold text-slate-950">{getCustomerLabel(order)}</p>
                                  <p className="mt-1 text-sm text-slate-500">{getCustomerSecondary(order)}</p>
                                </div>
                              </td>
                              <td className="px-4 py-4 text-sm font-semibold text-slate-950">
                                {formatKD(order.total)}
                              </td>
                              <td className="px-4 py-4">
                                <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getStatusTone(status)}`}>
                                  {status}
                                </span>
                              </td>
                              <td className="px-4 py-4 text-sm text-slate-700">
                                {formatDateTime(getCreatedAt(order))}
                              </td>
                              <td className="px-4 py-4">
                                <div className="flex flex-wrap gap-2">
                                  <select
                                    value={status}
                                    onChange={(event) => void updateOrderStatus(order, event.target.value as OrderStatus)}
                                    disabled={busy}
                                    className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-xs font-medium text-slate-950 outline-none transition disabled:cursor-not-allowed disabled:opacity-60"
                                  >
                                    {ORDER_STATUSES.map((option) => (
                                      <option key={option} value={option}>
                                        {option}
                                      </option>
                                    ))}
                                  </select>
                                  <button
                                    type="button"
                                    onClick={() => setDetailsOrder(order)}
                                    className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                                  >
                                    View details
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="mt-4 flex flex-col gap-3 border-t border-slate-200 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="text-sm text-slate-500">
                Page {safePage} of {totalPages}
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
                  disabled={safePage === 1}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
                  disabled={safePage === totalPages}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          </div>

          <aside className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div>
              <p className="text-sm font-medium text-slate-500">Operations snapshot</p>
              <h2 className="mt-1 text-xl font-semibold text-slate-950">What needs attention</h2>
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <StatRow label="Pending orders" value={formatCount(stats.pending)} />
              <StatRow label="Shipped orders" value={formatCount(stats.shipped)} />
              <StatRow label="Delivered orders" value={formatCount(stats.delivered)} />
              <StatRow label="Visible revenue" value={formatKD(stats.revenue)} />
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-950">Quick notes</p>
              <ul className="mt-3 space-y-3 text-sm leading-6 text-slate-600">
                <li>Use the status dropdown to move orders through fulfillment without leaving the table.</li>
                <li>The details modal shows customer, address, payment, and line items for a faster support workflow.</li>
                <li>Search matches order id, customer, contact details, payment fields, and product names.</li>
              </ul>
            </div>
          </aside>
        </section>
      </main>

      <ModalShell
        open={Boolean(detailsOrder)}
        title={`Order #${detailsOrder?.orderNumber ?? detailsOrder?.id ?? ""}`}
        onClose={() => setDetailsOrder(null)}
      >
        {detailsOrder ? (
          <div className="space-y-6">
            <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:flex-row md:items-start md:justify-between">
              <div>
                <p className="text-sm text-slate-500">Created</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {formatDateTime(getCreatedAt(detailsOrder))}
                </p>
                <p className="mt-2 text-sm text-slate-500">
                  Payment {detailsOrder.paymentMethod || "Unknown"} · {detailsOrder.paymentStatus || "Unknown"}
                </p>
              </div>

              <div className="flex flex-col gap-3">
                <span className={`inline-flex w-fit rounded-full border px-3 py-1 text-xs font-semibold ${getStatusTone(detailsOrder.status)}`}>
                  {normalizeStatus(detailsOrder.status)}
                </span>
                <div className="text-right">
                  <p className="text-sm text-slate-500">Order total</p>
                  <p className="text-2xl font-semibold text-slate-950">{formatKD(detailsOrder.total)}</p>
                </div>
              </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h4 className="text-lg font-semibold text-slate-950">Customer</h4>
                <div className="mt-4 space-y-3 text-sm text-slate-700">
                  <p><span className="text-slate-500">Name:</span> {getCustomerLabel(detailsOrder)}</p>
                  <p><span className="text-slate-500">Email:</span> {detailsOrder.customerEmail || detailsOrder.user?.email || "Unavailable"}</p>
                  <p><span className="text-slate-500">Phone:</span> {detailsOrder.phone || "Unavailable"}</p>
                  <p><span className="text-slate-500">Address:</span> {detailsOrder.addressLine1 || "Unavailable"}</p>
                  {detailsOrder.addressLine2 ? <p>{detailsOrder.addressLine2}</p> : null}
                  <p>
                    {detailsOrder.city || "City unavailable"}
                    {detailsOrder.state ? `, ${detailsOrder.state}` : ""} {detailsOrder.postalCode || ""}
                  </p>
                  <p>{detailsOrder.country || "Country unavailable"}</p>
                  {detailsOrder.note ? (
                    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3 text-slate-600">
                      {detailsOrder.note}
                    </div>
                  ) : null}
                </div>
              </section>

              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h4 className="text-lg font-semibold text-slate-950">Fulfillment</h4>
                <div className="mt-4 space-y-4">
                  <div>
                    <p className="text-sm text-slate-500">Update status</p>
                    <select
                      value={normalizeStatus(detailsOrder.status)}
                      onChange={(event) => void updateOrderStatus(detailsOrder, event.target.value as OrderStatus)}
                      disabled={actionBusyId === detailsOrder.id}
                      className="mt-2 h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {ORDER_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <StatRow label="Order number" value={detailsOrder.orderNumber || String(detailsOrder.id)} />
                    <StatRow label="Payment method" value={detailsOrder.paymentMethod || "Unknown"} />
                    <StatRow label="Payment status" value={detailsOrder.paymentStatus || "Unknown"} />
                    <StatRow label="Line items" value={formatCount(detailsOrder.items?.length ?? 0)} />
                  </div>
                </div>
              </section>
            </div>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center justify-between">
                <h4 className="text-lg font-semibold text-slate-950">Items</h4>
                <span className="text-sm text-slate-500">
                  {formatCount(detailsOrder.items?.length ?? 0)} item(s)
                </span>
              </div>

              {detailsOrder.items && detailsOrder.items.length > 0 ? (
                <div className="mt-4 overflow-hidden rounded-xl border border-slate-200">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Product</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Qty</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Price</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {detailsOrder.items.map((item, index) => (
                        <tr key={item.id ?? `${detailsOrder.id}-${index}`}>
                          <td className="px-4 py-4">
                            <div className="font-medium text-slate-950">{getItemLabel(item)}</div>
                            <div className="text-xs text-slate-400">
                              {item.productId ? `ID ${item.productId}` : "No product id"}
                            </div>
                          </td>
                          <td className="px-4 py-4 text-slate-700">{formatCount(item.quantity ?? 0)}</td>
                          <td className="px-4 py-4 text-slate-700">{formatKD(item.price)}</td>
                          <td className="px-4 py-4 font-medium text-slate-950">{formatKD(item.subtotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-4 text-sm text-slate-500">No items found for this order.</p>
              )}
            </section>
          </div>
        ) : null}
      </ModalShell>
    </div>
  );
}
