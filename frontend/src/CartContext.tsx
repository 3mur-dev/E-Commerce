import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { API_BASE, getAuthHeaders } from "./api";

type CartItem = {
  id: number;
  quantity: number;
};

type CartContextType = {
  cart: CartItem[];
  cartCount: number;
  error: string | null;
  refreshCart: () => Promise<void>;
};

type CartResponse = {
  items?: CartItem[];
  total?: number;
};

type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data?: T;
};

const POLL_INTERVAL_MS = 30_000;

const CartContext = createContext<CartContextType | null>(null);

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const inFlightRef = useRef(false);

  const refreshCart = useCallback(async () => {
    if (!localStorage.getItem("authToken")) {
      setCart([]);
      setError(null);
      return;
    }

    if (inFlightRef.current) return;
    inFlightRef.current = true;

    try {
      const response = await fetch(`${API_BASE}/cart`, {
        headers: {
          Accept: "application/json",
          ...getAuthHeaders(),
        },
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch cart (${response.status})`);
      }

      const data = (await response.json()) as ApiResponse<CartResponse> | CartItem[];
      const items = Array.isArray(data) ? data : data.data?.items ?? [];
      setCart(items);
      setError(null);
    } catch (err) {
      if ((err as Error).name !== "AbortError") {
        setError((err as Error).message || "Failed to refresh cart");
      }
    } finally {
      inFlightRef.current = false;
    }
  }, []);

  useEffect(() => {
    void refreshCart();

    const intervalId = window.setInterval(() => {
      void refreshCart();
    }, POLL_INTERVAL_MS);

    const handleFocus = () => {
      void refreshCart();
    };

    const handleVisibility = () => {
      if (document.visibilityState === "visible") {
        void refreshCart();
      }
    };

    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibility);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibility);
    };
  }, [refreshCart]);

  const cartCount = useMemo(
    () => cart.reduce((sum, item) => sum + item.quantity, 0),
    [cart],
  );

  const value = useMemo(
    () => ({ cart, cartCount, error, refreshCart }),
    [cart, cartCount, error, refreshCart],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart must be used inside CartProvider");
  }
  return context;
}
