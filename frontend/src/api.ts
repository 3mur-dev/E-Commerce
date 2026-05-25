export const API_BASE = import.meta.env.VITE_API_BASE || "/api";

export const resolveAssetUrl = (assetUrl?: string | null): string => {
  if (!assetUrl) return "";
  if (/^https?:\/\//i.test(assetUrl)) return assetUrl;

  const normalized = assetUrl.startsWith("/") ? assetUrl : `/${assetUrl}`;
  const origin = typeof window !== "undefined" ? window.location.origin : "http://localhost";

  try {
    const apiUrl = new URL(API_BASE, origin);
    return new URL(normalized, apiUrl.origin).toString();
  } catch {
    return normalized;
  }
};

export type PaymentMethod = "CARD" | "WALLET" | "CASH_ON_DELIVERY";

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
  imageUrl: string;
  price: number;
  quantity: number;
  stock: number;
  subtotal?: number;
}

export interface CartResponse {
  items: CartItem[];
  total: number;
}

export interface CheckoutRequest {
  customerName: string;
  customerEmail: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  paymentMethod: PaymentMethod;
  note?: string;
  idempotencyKey: string;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  imageUrl: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  status: string;
  paymentMethod: PaymentMethod;
  paymentStatus: string;
  creationTimestamp: string;
  total: number;
  sessionUrl?: string;
  stripeCheckoutUrl?: string;
  customerName: string;
  customerEmail: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  note?: string;
  items: OrderItemResponse[];
}
export interface StripeCheckoutSessionResponse {
  sessionId?: string;
  url?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

export const formatPrice = (price: number) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(price);

export const getAuthHeaders = (): Record<string, string> => {
  const token = localStorage.getItem("authToken");
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
};

async function parseError(response: Response): Promise<string> {
  const text = await response.text().catch(() => "");

  if (!text) {
    return `Request failed with status ${response.status}`;
  }

  try {
    const parsed = JSON.parse(text) as { message?: string; error?: string };
    return parsed.message || parsed.error || text;
  } catch {
    return text;
  }
}

export async function fetchCart(): Promise<CartResponse> {
  const response = await fetch(`${API_BASE}/cart`, {
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
      payload.message || `Failed to load cart (${response.status})`,
    );
  }

  return payload.data;
}

export async function submitCheckout(request: CheckoutRequest): Promise<OrderResponse> {
  const response = await fetch(`${API_BASE}/cart/checkout`, {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
  });

  let payload: ApiResponse<OrderResponse>;

  try {
    payload = (await response.json()) as ApiResponse<OrderResponse>;
  } catch {
    throw new Error("Invalid server response");
  }

  if (!response.ok || !payload.success || !payload.data) {
    throw new Error(
      payload.message || `Failed to submit checkout (${response.status})`,
      );
  }

  return payload.data;
}

export async function fetchOrder(
  id: string | number,
): Promise<OrderResponse> {
  const token = localStorage.getItem("authToken");

  const response = await fetch(`${API_BASE}/orders/${id}`, {
    method: "GET",
    headers: {
      Authorization: token ? `Bearer ${token}` : "",
      "Content-Type": "application/json",
    },
  });

  let payload: ApiResponse<OrderResponse>;

  try {
    payload =
      (await response.json()) as ApiResponse<OrderResponse>;
  } catch {
    throw new Error("Invalid server response");
  }

  if (!response.ok || !payload.success || !payload.data) {
    throw new Error(
        payload.message ||
        `Failed to load order (${response.status})`,
    );
  }

  return payload.data;
}

export async function createStripeSession(
  payload: CheckoutRequest,
): Promise<OrderResponse> {
  const res = await fetch(`${API_BASE}/orders/checkout`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${localStorage.getItem("authToken")}`,
    },
    body: JSON.stringify(payload),
  });

  let apiResponse: ApiResponse<OrderResponse>;

  try {
    apiResponse = (await res.json()) as ApiResponse<OrderResponse>;
  } catch {
    throw new Error("Invalid server response");
  }

  if (!res.ok || !apiResponse.success || !apiResponse.data) {
    throw new Error(
      apiResponse.message || `Failed to create Stripe session (${res.status})`,
    );
  }

  return apiResponse.data;
}