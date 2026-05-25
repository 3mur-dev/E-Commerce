import { type ReactNode, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_BASE } from "../../api";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { confirmDangerousAction } from "./adminShared";

const REFRESH_MS = 30_000;
const DEFAULT_PAGE_SIZE = 8;
const DEFAULT_ROLES = ["USER", "ADMIN"] as const;
const DEFAULT_STATUSES = ["ACTIVE", "DISABLED"] as const;

const getAuthHeaders = (): Record<string, string> => {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (typeof localStorage === "undefined") return headers;

  const token = localStorage.getItem("authToken");
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
};

type SortField = "createdAt" | "name" | "email" | "role" | "status" | "id";
type SortDirection = "asc" | "desc";

type AdminUserListResponse = {
  id: number;
  username?: string | null;
  email?: string | null;
  role?: string | null;
  status?: string | null;
  createdAt?: string | null;
  orderCount?: number | null;
  totalSpent?: number | string | null;
};

type AdminUserDetailResponse = AdminUserListResponse & {
  orders?: Array<{
    id?: number;
    orderNumber?: string | null;
    status?: string | null;
    total?: number | string | null;
    creationTimestamp?: string | null;
    createdAt?: string | null;
  }> | null;
};

type SpringPage<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data?: T;
};
const statusStyles: Record<string, string> = {
  ACTIVE: "border-emerald-200 bg-emerald-50 text-emerald-700",
  DISABLED: "border-rose-200 bg-rose-50 text-rose-700",
  SUSPENDED: "border-amber-200 bg-amber-50 text-amber-700",
  PENDING: "border-blue-200 bg-blue-50 text-blue-700",
  LOCKED: "border-slate-200 bg-slate-100 text-slate-700",
};

const roleStyles: Record<string, string> = {
  ADMIN: "border-violet-200 bg-violet-50 text-violet-700",
  USER: "border-sky-200 bg-sky-50 text-sky-700",
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

const normalizeEnum = (value?: string | null, fallback = "UNKNOWN") => {
  const trimmed = value?.trim();
  return trimmed ? trimmed.toUpperCase() : fallback;
};

const getStatusTone = (status?: string | null) =>
  statusStyles[normalizeEnum(status)] ?? "border-slate-200 bg-slate-50 text-slate-700";

const getRoleTone = (role?: string | null) =>
  roleStyles[normalizeEnum(role)] ?? "border-slate-200 bg-slate-50 text-slate-700";

const getUserName = (user: AdminUserListResponse) =>
  user.username?.trim()
  || user.email?.trim()
  || `User #${user.id}`;

const getCreatedAt = (user: AdminUserListResponse) => user.createdAt ?? "";

const inferEnabled = (user: AdminUserListResponse) => {
  return normalizeEnum(user.status) !== "DISABLED";
};

const formatSortLabel = (field: SortField, direction: SortDirection) => {
  const prefix = direction === "asc" ? "Asc" : "Desc";
  const label =
    field === "createdAt"
      ? "Date"
      : field === "name"
        ? "Name"
        : field === "email"
          ? "Email"
          : field === "role"
            ? "Role"
            : field === "status"
              ? "Status"
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
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">User details</p>
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

export default function AdminUsersPage() {
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [sortField, setSortField] = useState<SortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [currentPage, setCurrentPage] = useState(0);
  
  const [actionBusyId, setActionBusyId] = useState<number | null>(null);
 const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const queryClient = useQueryClient();
  const search = useDebouncedValue(searchInput.trim(), 400);

  

  // 👇 USER DETAILS QUERY (PUT IT HERE)
  const {
    data: detailsData,
    isFetching: detailsLoading,
    refetch: refetchDetails,
  } = useQuery<AdminUserDetailResponse>({
    queryKey: ["admin-user", selectedUserId],
    queryFn: async () => {
      if (!selectedUserId) throw new Error("User id is required");

      const response = await fetch(`${API_BASE}/admin/users/${selectedUserId}`, {
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      const json: ApiResponse<AdminUserDetailResponse> = await response.json();

      if (!json.success || !json.data) {
        throw new Error(json.message || "User not found");
      }

      return json.data;
    },
    enabled: !!selectedUserId,
  });

  const {
    data: usersPage,
    isLoading: loading,
    isFetching,
    refetch: refetchUsers,
  } = useQuery<SpringPage<AdminUserListResponse>>({
    queryKey: ["admin-users", search, roleFilter, statusFilter, currentPage, pageSize],
    queryFn: async () => {
  const params = new URLSearchParams();

  if (search) params.set("search", search);
  if (roleFilter !== "ALL") params.set("role", roleFilter);
  if (statusFilter !== "ALL") params.set("status", statusFilter);

  params.set("page", String(currentPage));
  params.set("size", String(pageSize));

  const response = await fetch(`${API_BASE}/admin/users?${params.toString()}`, {
    headers: {
      Accept: "application/json",
      ...getAuthHeaders(),
    },
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  const json: ApiResponse<SpringPage<AdminUserListResponse>> = await response.json();

  if (!json.success || !json.data) {
    throw new Error(json.message || "Failed to load users");
  }

  return json.data;
    },
    refetchInterval: REFRESH_MS,
  });


  const rawUsers = usersPage?.content ?? [];

  const roleOptions = useMemo(() => {
    const dynamic = rawUsers.map((user) => normalizeEnum(user.role)).filter((role) => role !== "UNKNOWN");
    return Array.from(new Set([...DEFAULT_ROLES, ...dynamic])).sort();
  }, [rawUsers]);

  const statusOptions = useMemo(() => {
    const dynamic = rawUsers.map((user) => normalizeEnum(user.status)).filter((status) => status !== "UNKNOWN");
    return Array.from(new Set([...DEFAULT_STATUSES, ...dynamic])).sort();
  }, [rawUsers]);

  const users = useMemo(() => {
    const items = [...rawUsers];
    items.sort((a, b) => {
      const direction = sortDirection === "asc" ? 1 : -1;
      const valueFor = (user: AdminUserListResponse) => {
        switch (sortField) {
          case "name":
            return getUserName(user);
          case "email":
            return user.email ?? "";
          case "role":
            return normalizeEnum(user.role);
          case "status":
            return normalizeEnum(user.status);
          case "createdAt":
            return new Date(getCreatedAt(user)).getTime() || 0;
          default:
            return user.id;
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
  }, [rawUsers, sortField, sortDirection]);

  const stats = useMemo(() => {
    const admins = users.filter((user) => normalizeEnum(user.role) === "ADMIN").length;
    const enabled = users.filter((user) => inferEnabled(user)).length;
    const disabled = users.filter((user) => !inferEnabled(user)).length;
    return {
      total: usersPage?.totalElements ?? 0,
      visible: users.length,
      admins,
      enabled,
      disabled,
    };
  }, [users, usersPage]);

  useEffect(() => {
    setCurrentPage(0);
  }, [search, roleFilter, statusFilter, pageSize]);

  useEffect(() => {
    // detailsData is now the single source; no local detailsUser state
  }, [detailsData]);

const lastUpdatedLabel = useMemo(() => {
  if (!usersPage) return "just now";
  return formatRelativeTime(new Date());
}, [usersPage]);  const refreshing = isFetching && !loading;

  const updateUserInState = (updatedUser: AdminUserDetailResponse) => {
    queryClient.setQueryData<SpringPage<AdminUserListResponse> | undefined>(
      ["admin-users", search, roleFilter, statusFilter, currentPage, pageSize],
      (prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          content: prev.content.map((user) => (user.id === updatedUser.id ? { ...user, ...updatedUser } : user)),
        };
      },
    );
    // update the cached single-user query so modal shows latest data
    queryClient.setQueryData<AdminUserDetailResponse | undefined>(["admin-user", updatedUser.id], updatedUser);
  };

  const openDetails = (user: AdminUserListResponse) => {
    setSelectedUserId(user.id);
  };

  const statusMutation = useMutation({
    mutationFn: async ({ userId, nextStatus }: { userId: number; nextStatus: string }) => {
      const response = await fetch(`${API_BASE}/admin/users/${userId}/status`, {
        method: "PATCH",
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify({ status: nextStatus }),
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      return (await response.json()) as AdminUserDetailResponse;
    },
    onSuccess: (data, variables) => {
      updateUserInState(data);
      setNotice(`Updated user #${variables.userId} status to ${normalizeEnum(data.status, variables.nextStatus)}`);
      void refetchDetails();
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Error updating user status");
    },
    onSettled: () => {
      setActionBusyId(null);
    },
  });

  const updateUserStatus = async (userId: number, nextStatus: string) => {
    if (nextStatus === "DISABLED" && !confirmDangerousAction("Disable this account? The user will lose access until re-enabled.")) {
      return;
    }

    setActionBusyId(userId);
    setError("");
    setNotice("");
    await statusMutation.mutateAsync({ userId, nextStatus });
  };

  const roleMutation = useMutation({
    mutationFn: async ({ userId, nextRole }: { userId: number; nextRole: string }) => {
      const response = await fetch(`${API_BASE}/admin/users/${userId}/role`, {
        method: "PATCH",
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        body: JSON.stringify({ role: nextRole }),
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      return (await response.json()) as AdminUserDetailResponse;
    },
    onSuccess: (data, variables) => {
      updateUserInState(data);
      setNotice(`Updated user #${variables.userId} role to ${normalizeEnum(data.role, variables.nextRole)}`);
      void refetchDetails();
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Error updating user role");
    },
    onSettled: () => {
      setActionBusyId(null);
    },
  });

  const updateUserRole = async (userId: number, nextRole: string) => {
    if (nextRole !== "ADMIN" && !confirmDangerousAction("Change this user to a non-admin role?")) {
      return;
    }

    setActionBusyId(userId);
    setError("");
    setNotice("");
    await roleMutation.mutateAsync({ userId, nextRole });
  };

  const enableMutation = useMutation({
    mutationFn: async (user: AdminUserListResponse) => {
      const endpoint = inferEnabled(user) ? "disable" : "enable";
      const response = await fetch(`${API_BASE}/admin/users/${user.id}/${endpoint}`, {
        method: "POST",
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
      });

      if (!response.ok) {
        throw new Error(await parseError(response));
      }

      return (await response.json()) as AdminUserDetailResponse;
    },
    onSuccess: (data, user) => {
      updateUserInState(data);
      setNotice(`${inferEnabled(user) ? "Disabled" : "Enabled"} ${getUserName(user)}`);
      void refetchDetails();
    },
    onError: (err) => {
      setError(err instanceof Error ? err.message : "Error updating user");
    },
    onSettled: () => {
      setActionBusyId(null);
    },
  });

  const toggleUserEnabled = async (user: AdminUserListResponse) => {
    const confirmed = inferEnabled(user)
      ? confirmDangerousAction("Disable this account? The user will not be able to sign in.")
      : true;

    if (!confirmed) return;

    setActionBusyId(user.id);
    setError("");
    setNotice("");
    void enableMutation.mutateAsync(user);
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

  if (error && !usersPage) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto flex max-w-7xl items-center justify-center px-4 py-16 sm:px-6 lg:px-8">
          <div className="w-full max-w-xl rounded-2xl border border-red-200 bg-white p-6 shadow-sm">
            <p className="text-sm font-medium text-red-600">Users unavailable</p>
            <h1 className="mt-2 text-2xl font-semibold text-slate-950">{error}</h1>
            <p className="mt-3 text-sm text-slate-500">
              Verify the admin token, backend URL, and the `/api/admin/users` endpoints.
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
                Admin Users
              </p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
                Users management
              </h1>
              <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
                Search, filter, review, and manage user accounts with direct status, enable, and role controls.
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
          <MetricCard label="Total users" value={formatCount(stats.total)} hint="Total result count from the pageable admin users endpoint." />
          <MetricCard label="Visible now" value={formatCount(stats.visible)} hint="Users currently loaded on this page." />
          <MetricCard label="Admins" value={formatCount(stats.admins)} hint="Admin accounts visible in the current page result." />
          <MetricCard label="Enabled" value={formatCount(stats.enabled)} hint="Accounts currently available for sign-in in this page result." />
          <MetricCard label="Disabled" value={formatCount(stats.disabled)} hint="Accounts currently disabled in this page result." />
        </section>

        <section className="mt-6 grid gap-4 lg:grid-cols-3">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm lg:col-span-2">
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">Users table</p>
                  <h2 className="mt-1 text-xl font-semibold text-slate-950">
                    User list from /api/admin/users
                  </h2>
                </div>

                <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
                  <input
                    value={searchInput}
                    onChange={(event) => setSearchInput(event.target.value)}
                    placeholder="Search users"
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-slate-400 sm:w-72"
                  />
                  <select
                    value={roleFilter}
                    onChange={(event) => setRoleFilter(event.target.value)}
                    className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition focus:border-slate-400"
                  >
                    <option value="ALL">All roles</option>
                    {roleOptions.map((role) => (
                      <option key={role} value={role}>
                        {role}
                      </option>
                    ))}
                  </select>
                  <select
                    value={statusFilter}
                    onChange={(event) => setStatusFilter(event.target.value)}
                    className="h-11 rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none transition focus:border-slate-400"
                  >
                    <option value="ALL">All statuses</option>
                    {statusOptions.map((status) => (
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
                    <option value="createdAt">Sort by date</option>
                    <option value="name">Sort by name</option>
                    <option value="email">Sort by email</option>
                    <option value="role">Sort by role</option>
                    <option value="status">Sort by status</option>
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
                    onClick={() => void refetchUsers()}
                    className="h-11 rounded-xl border border-slate-200 bg-slate-900 px-4 text-sm font-medium text-white transition hover:bg-slate-800"
                  >
                    Refresh
                  </button>
                </div>
              </div>

              <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between">
                <div>{usersPage?.totalElements ?? 0} users match the current filter.</div>
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
                {users.length === 0 ? (
                  <div className="py-8 text-center text-sm text-slate-500">
                    No users match this filter.
                  </div>
                ) : (
                  users.map((user) => {
                    const busy = actionBusyId === user.id;
                    const normalizedRole = normalizeEnum(user.role, "USER");
                    const normalizedStatus = normalizeEnum(user.status, inferEnabled(user) ? "ACTIVE" : "DISABLED");

                    return (
                      <div key={user.id} className="rounded-2xl border border-slate-200 bg-white p-4">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <button
                              type="button"
                              onClick={() => void openDetails(user)}
                              className="text-left text-sm font-semibold text-slate-950 hover:underline"
                            >
                              {getUserName(user)}
                            </button>
                            <p className="mt-1 text-xs text-slate-500">
                              {user.email || "No email"} · {formatDateTime(getCreatedAt(user))}
                            </p>
                          </div>
                          <div className="flex flex-col gap-2">
                            <span className={`rounded-full border px-2 py-1 text-[11px] font-semibold ${getStatusTone(normalizedStatus)}`}>
                              {normalizedStatus}
                            </span>
                            <span className={`rounded-full border px-2 py-1 text-[11px] font-semibold ${getRoleTone(normalizedRole)}`}>
                              {normalizedRole}
                            </span>
                          </div>
                        </div>

                        <div className="mt-3 grid gap-2">
                          <select
                            value={normalizedStatus}
                            onChange={(event) => void updateUserStatus(user.id, event.target.value)}
                            disabled={busy}
                            className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {statusOptions.map((status) => (
                              <option key={status} value={status}>
                                {status}
                              </option>
                            ))}
                          </select>
                          <select
                            value={normalizedRole}
                            onChange={(event) => void updateUserRole(user.id, event.target.value)}
                            disabled={busy}
                            className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {roleOptions.map((role) => (
                              <option key={role} value={role}>
                                {role}
                              </option>
                            ))}
                          </select>
                          <div className="grid grid-cols-2 gap-2">
                            <button
                              type="button"
                              onClick={() => void openDetails(user)}
                              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700"
                            >
                              Details
                            </button>
                            <button
                              type="button"
                              onClick={() => void toggleUserEnabled(user)}
                              disabled={busy}
                              className={`rounded-lg border px-3 py-2 text-xs font-medium disabled:opacity-60 ${inferEnabled(user) ? "border-rose-200 text-rose-700" : "border-emerald-200 text-emerald-700"}`}
                            >
                              {inferEnabled(user) ? "Disable" : "Enable"}
                            </button>
                          </div>
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
                          User
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Contact
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Role
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Status
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Created
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {users.length === 0 ? (
                        <tr>
                          <td colSpan={6} className="px-4 py-12 text-center text-sm text-slate-500">
                            No users match this filter.
                          </td>
                        </tr>
                      ) : (
                        users.map((user) => {
                          const busy = actionBusyId === user.id;
                          const normalizedRole = normalizeEnum(user.role, "USER");
                          const normalizedStatus = normalizeEnum(user.status, inferEnabled(user) ? "ACTIVE" : "DISABLED");

                          return (
                            <tr key={user.id} className="group align-top transition hover:bg-slate-50">
                              <td className="px-4 py-4">
                                <div className="min-w-0">
                                  <button
                                    type="button"
                                    onClick={() => void openDetails(user)}
                                    className="truncate text-left text-sm font-semibold text-slate-950 hover:underline"
                                  >
                                    {getUserName(user)}
                                  </button>
                                  <p className="mt-1 text-sm leading-6 text-slate-500">
                                    {formatCount(user.orderCount ?? 0)} order(s) · {formatKD(user.totalSpent)}
                                  </p>
                                  <p className="mt-1 text-xs text-slate-400">ID {user.id}</p>
                                </div>
                              </td>
                              <td className="px-4 py-4">
                                <div>
                                  <p className="text-sm font-medium text-slate-950">{user.email || "No email"}</p>
                                  <p className="mt-1 text-sm text-slate-500">{formatCount(user.orderCount ?? 0)} order(s)</p>
                                </div>
                              </td>
                              <td className="px-4 py-4">
                                <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getRoleTone(normalizedRole)}`}>
                                  {normalizedRole}
                                </span>
                              </td>
                              <td className="px-4 py-4">
                                <span className={`inline-flex rounded-full border px-3 py-1 text-xs font-semibold ${getStatusTone(normalizedStatus)}`}>
                                  {normalizedStatus}
                                </span>
                              </td>
                              <td className="px-4 py-4 text-sm text-slate-700">
                                {formatDateTime(getCreatedAt(user))}
                              </td>
                              <td className="px-4 py-4">
                                <div className="flex flex-wrap gap-2">

                                  <select
                                    value={normalizedRole}
                                    onChange={(event) => void updateUserRole(user.id, event.target.value)}
                                    disabled={busy}
                                    className="h-10 rounded-lg border border-slate-200 bg-white px-3 text-xs font-medium text-slate-950 outline-none transition disabled:cursor-not-allowed disabled:opacity-60"
                                  >
                                    {roleOptions.map((role) => (
                                      <option key={role} value={role}>
                                        {role}
                                      </option>
                                    ))}
                                  </select>
                                  <button
                                    type="button"
                                    onClick={() => void toggleUserEnabled(user)}
                                    disabled={busy}
                                    className={`rounded-lg border px-3 py-2 text-xs font-medium transition disabled:opacity-60 ${inferEnabled(user) ? "border-rose-200 text-rose-700 hover:bg-rose-50" : "border-emerald-200 text-emerald-700 hover:bg-emerald-50"}`}
                                  >
                                    {inferEnabled(user) ? "Disable" : "Enable"}
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => void openDetails(user)}
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
                Page {Math.min((usersPage?.number ?? 0) + 1, Math.max(usersPage?.totalPages ?? 1, 1))} of {Math.max(usersPage?.totalPages ?? 1, 1)}
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => Math.max(0, page - 1))}
                  disabled={usersPage?.first ?? true}
                  className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  type="button"
                  onClick={() => setCurrentPage((page) => page + 1)}
                  disabled={usersPage?.last ?? true}
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
              <h2 className="mt-1 text-xl font-semibold text-slate-950">Account controls</h2>
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <StatRow label="Matching users" value={formatCount(stats.total)} />
              <StatRow label="Current page" value={formatCount(stats.visible)} />
              <StatRow label="Admins in page" value={formatCount(stats.admins)} />
              <StatRow label="Disabled in page" value={formatCount(stats.disabled)} />
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-950">Quick notes</p>
              <ul className="mt-3 space-y-3 text-sm leading-6 text-slate-600">
                <li>Search uses the backend `search` query so it scales with server-side pagination.</li>
                <li>Status and role changes call the exact patch endpoints you provided.</li>
                <li>Enable and disable use their dedicated action endpoints, separate from status updates.</li>
              </ul>
            </div>
          </aside>
        </section>
      </main>

      <ModalShell
        open={!!selectedUserId}
        title={detailsData ? getUserName(detailsData) : ""}
        onClose={() => setSelectedUserId(null)}
      >
        {detailsLoading ? (
          <div className="space-y-4">
            <div className="h-8 w-48 animate-pulse rounded bg-slate-200" />
            <div className="h-24 animate-pulse rounded-2xl bg-slate-200" />
            <div className="grid gap-4 lg:grid-cols-2">
              <div className="h-48 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-48 animate-pulse rounded-2xl bg-slate-200" />
            </div>
          </div>
        ) : detailsData ? (
          <div className="space-y-6">
            <div className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 md:flex-row md:items-start md:justify-between">
              <div>
                <p className="text-sm text-slate-500">Member since</p>
                <p className="mt-1 text-lg font-semibold text-slate-950">
                  {formatDateTime(getCreatedAt(detailsData))}
                </p>
                <p className="mt-2 text-sm text-slate-500">
                  {formatCount(detailsData.orderCount ?? 0)} order(s) · {formatKD(detailsData.totalSpent)}
                </p>
              </div>

              <div className="flex flex-col gap-2">
                <span className={`inline-flex w-fit rounded-full border px-3 py-1 text-xs font-semibold ${getStatusTone(detailsData.status)}`}>
                  {normalizeEnum(detailsData.status, inferEnabled(detailsData) ? "ACTIVE" : "DISABLED")}
                </span>
                <span className={`inline-flex w-fit rounded-full border px-3 py-1 text-xs font-semibold ${getRoleTone(detailsData.role)}`}>
                  {normalizeEnum(detailsData.role, "USER")}
                </span>
              </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h4 className="text-lg font-semibold text-slate-950">Profile</h4>
                <div className="mt-4 space-y-3 text-sm text-slate-700">
                  <p><span className="text-slate-500">Name:</span> {getUserName(detailsData)}</p>
                  <p><span className="text-slate-500">Email:</span> {detailsData.email || "Unavailable"}</p>
                  <p><span className="text-slate-500">Username:</span> {detailsData.username || "Unavailable"}</p>
                  <p><span className="text-slate-500">Role:</span> {normalizeEnum(detailsData.role, "USER")}</p>
                  <p><span className="text-slate-500">Status:</span> {normalizeEnum(detailsData.status, "ACTIVE")}</p>
                </div>
              </section>

              <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <h4 className="text-lg font-semibold text-slate-950">Access controls</h4>
                <div className="mt-4 space-y-4">
                  <div>
                    <p className="text-sm text-slate-500">Update status</p>
                    <select
                      value={normalizeEnum(detailsData.status, inferEnabled(detailsData) ? "ACTIVE" : "DISABLED")}
                        onChange={(event) => void updateUserStatus(detailsData.id, event.target.value)}
                        disabled={actionBusyId === detailsData.id}
                      className="mt-2 h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {statusOptions.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <p className="text-sm text-slate-500">Update role</p>
                    <select
                      value={normalizeEnum(detailsData.role, "USER")}
                        onChange={(event) => void updateUserRole(detailsData.id, event.target.value)}
                        disabled={actionBusyId === detailsData.id}
                      className="mt-2 h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-950 outline-none disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {roleOptions.map((role) => (
                        <option key={role} value={role}>
                          {role}
                        </option>
                      ))}
                    </select>
                  </div>

                  <button
                    type="button"
                    onClick={() => void toggleUserEnabled(detailsData)}
                    disabled={actionBusyId === detailsData.id}
                    className={`w-full rounded-xl border px-4 py-3 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-60 ${inferEnabled(detailsData) ? "border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100" : "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"}`}
                  >
                    {inferEnabled(detailsData) ? "Disable account" : "Enable account"}
                  </button>

                  <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <StatRow label="User ID" value={String(detailsData.id)} />
                    <StatRow label="Orders" value={formatCount(detailsData.orderCount ?? 0)} />
                    <StatRow label="Total spent" value={formatKD(detailsData.totalSpent)} />
                    <StatRow label="Created" value={formatDateTime(detailsData.createdAt)} />
                  </div>
                </div>
              </section>
            </div>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between">
                <h4 className="text-lg font-semibold text-slate-950">Orders</h4>
                <span className="text-sm text-slate-500">
                  {formatCount(detailsData.orders?.length ?? 0)} order(s)
                </span>
              </div>

              {detailsData.orders && detailsData.orders.length > 0 ? (
                <div className="mt-4 overflow-hidden rounded-xl border border-slate-200">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Order</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Status</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Created</th>
                        <th className="px-4 py-3 text-left font-medium text-slate-500">Total</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 bg-white">
                      {detailsData.orders.map((order, index) => (
                        <tr key={order.id ?? `${detailsData.id}-order-${index}`}>
                          <td className="px-4 py-4 font-medium text-slate-950">
                            #{order.orderNumber ?? order.id ?? index + 1}
                          </td>
                          <td className="px-4 py-4 text-slate-700">
                            {normalizeEnum(order.status, "UNKNOWN")}
                          </td>
                          <td className="px-4 py-4 text-slate-700">
                            {formatDateTime(order.creationTimestamp ?? order.createdAt)}
                          </td>
                          <td className="px-4 py-4 text-slate-700">
                            {formatKD(order.total)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-4 text-sm text-slate-500">No orders found for this user.</p>
              )}
            </section>
          </div>
        ) : null}
      </ModalShell>
    </div>
  );
}
