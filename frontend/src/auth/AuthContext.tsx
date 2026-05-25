import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { API_BASE } from "../api";

export type AuthUser = {
  id: number;
  username: string;
  email: string;
  role: string;
};

type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data: T;
  error?: string;
};

type AuthContextValue = {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  token: string | null;
  login: (token: string) => Promise<AuthUser>;
  logout: () => void;
};

const TOKEN_KEY = "authToken";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function getStoredToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  return localStorage.getItem(TOKEN_KEY);
}

function setStoredToken(token: string): void {
  if (typeof window !== "undefined") {
    localStorage.setItem(TOKEN_KEY, token);
  }
}

function clearStoredToken(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function fetchCurrentUser(token: string): Promise<AuthUser> {
  const response = await fetch(`${API_BASE}/auth/me`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  let payload: ApiResponse<AuthUser>;

  try {
    payload = (await response.json()) as ApiResponse<AuthUser>;
  } catch {
    const err = new Error("Invalid server response");
    (err as any).status = response.status;
    throw err;
  }

  if (!response.ok || !payload.success || !payload.data) {
    const err = new Error(payload.error || payload.message || "Failed to load current user");
    (err as any).status = response.status;
    throw err;
  }

  return payload.data;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getStoredToken());
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(() => Boolean(getStoredToken()));

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
    setIsLoading(false);
  }, []);

  const hydrateUser = useCallback(
    async (nextToken: string): Promise<AuthUser> => {
      setIsLoading(true);

      try {
        const currentUser = await fetchCurrentUser(nextToken);

        setToken(nextToken);
        setUser(currentUser);

        return currentUser;
      } catch (error) {
        const status = (error as any)?.status;

        // Only treat 401/403 as authentication failures that should log the user out.
        if (status === 401 || status === 403) {
          logout();
          throw error instanceof Error ? error : new Error("Authentication failed");
        }

        // For other errors (network issues, server 5xx, invalid JSON, etc.) don't force logout.
        throw error instanceof Error ? error : new Error("Failed to hydrate user");
      } finally {
        setIsLoading(false);
      }
    },
    [logout],
  );

  const login = useCallback(
    async (nextToken: string): Promise<AuthUser> => {
      setStoredToken(nextToken);

      return hydrateUser(nextToken);
    },
    [hydrateUser],
  );

  useEffect(() => {
    if (!token || user) {
      return;
    }

    void hydrateUser(token);
  }, [token, user, hydrateUser]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      token,
      login,
      logout,
    }),
    [user, isLoading, token, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
}

export default AuthContext;