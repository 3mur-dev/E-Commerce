import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { API_BASE } from "../../api";

type DashboardData = {
  todayRevenue: string | number;
  monthlyRevenue: string | number;
  totalRevenue: string | number;
  totalOrders: number;
  totalUsers: number;
  totalProducts: number;
};

type Snapshot = {
  ts: number;
  todayRevenue: number;
  monthlyRevenue: number;
  totalRevenue: number;
};
type ApiResponse = {
  success: boolean;
  message?: string;
  data?: DashboardData;
};

const REFRESH_MS = 30_000;
const HISTORY_KEY = "admin-dashboard-history";

const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem("authToken");
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
};

const toNumber = (value: string | number | null | undefined) => {
  if (value === null || value === undefined) return 0;
  const num = typeof value === "string" ? Number(value) : value;
  return Number.isFinite(num) ? num : 0;
};

const formatKD = (value: string | number | null | undefined) =>
  `${toNumber(value).toFixed(2)} KD`;

const formatCount = (value: number) =>
  new Intl.NumberFormat("en-US").format(value);

const formatRelativeTime = (date: Date) => {
  const diff = Date.now() - date.getTime();
  const seconds = Math.floor(diff / 1000);
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  return `${hours}h ago`;
};

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
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/60 transition hover:shadow-md">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">{value}</h2>
      <p className="mt-4 text-sm leading-6 text-slate-500">{hint}</p>
    </section>
  );
}

function StatRow({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center justify-between border-b border-slate-100 py-3 last:border-0">
      <span className="text-sm text-slate-500">{label}</span>
      <span className="text-sm font-semibold text-slate-950">{value}</span>
    </div>
  );
}

function Sparkline({
  points,
  stroke = "#0f172a",
}: {
  points: number[];
  stroke?: string;
}) {
  if (points.length < 2) {
    return (
      <div className="flex h-44 items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50 text-sm text-slate-400">
        Waiting for live data
      </div>
    );
  }

  const width = 640;
  const height = 180;
  const padding = 12;
  const min = Math.min(...points);
  const max = Math.max(...points);
  const range = Math.max(max - min, 1);

  const scaleX = (index: number) =>
    padding + (index * (width - padding * 2)) / Math.max(points.length - 1, 1);

  const scaleY = (value: number) =>
    height - padding - ((value - min) * (height - padding * 2)) / range;

  const line = points
    .map((point, index) => `${scaleX(index)},${scaleY(point)}`)
    .join(" ");

  return (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <svg viewBox={`0 0 ${width} ${height}`} className="h-44 w-full">
        <defs>
          <linearGradient id="sparkFill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor={stroke} stopOpacity="0.18" />
            <stop offset="100%" stopColor={stroke} stopOpacity="0" />
          </linearGradient>
        </defs>

        <polyline
          fill="none"
          stroke={stroke}
          strokeWidth="3"
          strokeLinejoin="round"
          strokeLinecap="round"
          points={line}
        />

        <polygon
          fill="url(#sparkFill)"
          points={`${padding},${height - padding} ${line} ${width - padding},${height - padding}`}
        />
      </svg>
    </div>
  );
}

export default function AdminDashboard() {
  const [error, setError] = useState("");
  const [history, setHistory] = useState<Snapshot[]>([]);
  const {
    data,
    isLoading: loading,
    isFetching: isRefreshing,
    dataUpdatedAt,
    error: queryError,
  } = useQuery<DashboardData>({
    queryKey: ["admin-dashboard"],
    queryFn: async () => {
      const res = await fetch(`${API_BASE}/admin/dashboard`, {
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
      });

      if (!res.ok) {
        throw new Error("Unable to load dashboard");
      }

      const json: ApiResponse = await res.json();

    if (!json.success || !json.data) {
      throw new Error(json.message || "Invalid dashboard response");
    }

    return json.data; 
  },
  refetchInterval: REFRESH_MS,
});

  useEffect(() => {
    const raw = localStorage.getItem(HISTORY_KEY);
    if (raw) {
      try {
        const parsed = JSON.parse(raw) as Snapshot[];
        if (Array.isArray(parsed)) setHistory(parsed.slice(-12));
      } catch {
      }
    }
  }, []);

  useEffect(() => {
    if (queryError) {
      setError(queryError instanceof Error ? queryError.message : "Error loading dashboard");
    }
  }, [queryError]);

  useEffect(() => {
    if (!data) return;

    setError("");
    const snapshot: Snapshot = {
      ts: Date.now(),
      todayRevenue: toNumber(data.todayRevenue),
      monthlyRevenue: toNumber(data.monthlyRevenue),
      totalRevenue: toNumber(data.totalRevenue),
    };

    setHistory((prev) => {
      const next = [...prev, snapshot].slice(-12);
      localStorage.setItem(HISTORY_KEY, JSON.stringify(next));
      return next;
    });
  }, [data]);

  const stats = useMemo(() => {
    const today = toNumber(data?.todayRevenue);
    const monthly = toNumber(data?.monthlyRevenue);
    const total = toNumber(data?.totalRevenue);

    const monthlyShare = total > 0 ? (monthly / total) * 100 : 0;
    const todayShareOfMonth = monthly > 0 ? (today / monthly) * 100 : 0;

    return {
      today,
      monthly,
      total,
      monthlyShare,
      todayShareOfMonth,
      orders: data?.totalOrders ?? 0,
      users: data?.totalUsers ?? 0,
      products: data?.totalProducts ?? 0,
    };
  }, [data]);

  const chartPoints = useMemo(() => {
    if (history.length === 0) return [];
    return history.map((item) => item.monthlyRevenue);
  }, [history]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div className="space-y-6">
            <div className="h-10 w-72 animate-pulse rounded-lg bg-slate-200" />
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-32 animate-pulse rounded-2xl bg-slate-200" />
            </div>
            <div className="grid gap-4 lg:grid-cols-3">
              <div className="h-72 animate-pulse rounded-2xl bg-slate-200 lg:col-span-2" />
              <div className="h-72 animate-pulse rounded-2xl bg-slate-200" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto flex max-w-7xl items-center justify-center px-4 py-16 sm:px-6 lg:px-8">
          <div className="w-full max-w-xl rounded-2xl border border-red-200 bg-white p-6 shadow-sm">
            <p className="text-sm font-medium text-red-600">Dashboard unavailable</p>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">{error}</h1>
            <p className="mt-3 text-sm text-slate-500">
              Verify the admin token, backend URL, and database connection.
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (!data) return null;

  const lastUpdatedLabel = dataUpdatedAt ? formatRelativeTime(new Date(dataUpdatedAt)) : "just now";

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white/80 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-sm font-medium uppercase tracking-[0.2em] text-slate-500">
                Admin Overview
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
                Commerce dashboard
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
                Live operational view with revenue, growth, and catalog scale. Designed to read
                cleanly in a portfolio and still feel like a real admin system.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <div className="rounded-full border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
                Live refresh
              </div>
              <div className="rounded-full border border-slate-200 bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700">
                Updated {lastUpdatedLabel}
              </div>
              {isRefreshing ? (
                <div className="rounded-full border border-indigo-200 bg-indigo-50 px-4 py-2 text-sm font-medium text-indigo-700">
                  Refreshing
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <MetricCard
            label="Today Revenue"
            value={formatKD(stats.today)}
            hint="Revenue captured in the current server day window."
          />
          <MetricCard
            label="Monthly Revenue"
            value={formatKD(stats.monthly)}
            hint="Revenue captured since the first day of the month."
          />
          <MetricCard
            label="Total Revenue"
            value={formatKD(stats.total)}
            hint="All revenue from orders marked as paid."
          />
        </section>

        <section className="mt-6 grid gap-4 lg:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-2">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-slate-500">Revenue trend</p>
                <h2 className="mt-1 text-xl font-semibold text-slate-950">
                  Session-based live sample chart
                </h2>
              </div>
              <div className="text-right text-sm text-slate-500">
                <p>{stats.monthly > 0 ? `${stats.todayShareOfMonth.toFixed(1)}% of monthly revenue today` : "No paid revenue today"}</p>
                <p>{stats.total > 0 ? `${stats.monthlyShare.toFixed(1)}% of all-time revenue this month` : "No revenue recorded yet"}</p>
              </div>
            </div>

            <div className="mt-6">
              <Sparkline points={chartPoints} />
            </div>

            <div className="mt-5 grid gap-3 sm:grid-cols-3">
              <StatRow label="Current sample count" value={formatCount(history.length)} />
              <StatRow label="Auto refresh interval" value="30s" />
              <StatRow label="Backend endpoint" value="/api/admin/dashboard" />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">Platform snapshot</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-950">Operational scale</h2>

            <div className="mt-6">
              <StatRow label="Orders" value={formatCount(stats.orders)} />
              <StatRow label="Users" value={formatCount(stats.users)} />
              <StatRow label="Products" value={formatCount(stats.products)} />
            </div>

            <div className="mt-6 rounded-xl bg-slate-50 p-4">
              <p className="text-sm font-medium text-slate-500">Portfolio note</p>
              <p className="mt-2 text-sm leading-6 text-slate-700">
                This version shows data freshness, a live trend surface, and compact KPIs without
                feeling noisy. That is the difference between a functioning admin page and a
                portfolio piece that looks deliberate.
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
