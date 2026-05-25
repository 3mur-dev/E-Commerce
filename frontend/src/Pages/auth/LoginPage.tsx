import { useState } from "react";
import { LogIn, Eye, EyeOff } from "lucide-react";
import { API_BASE } from "../../api";
import { PageBackground, PageFooter } from "../extra/PageLayout";
import { useAuth } from "../../auth/AuthContext";
import toast from "react-hot-toast";

type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};
function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { login } = useAuth();

  const handleLogin = async (e: React.FormEvent) => {
  e.preventDefault();
  setLoading(true);
  setError("");

  try {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    const data: ApiResponse<{ token: string }> = await response.json();

    if (!data?.success) {
      setError(data?.message || "Login failed. Please try again.");
      try {
        toast.error(data?.message || "Login failed. Please try again.");
      } catch {}
      return;
    }

    const token = data?.data?.token;

    if (!token) {
      setError("Invalid server response (missing token)");
      return;
    }

    await login(token);
    try {
      toast.success("Signed in successfully");
    } catch {}
    window.location.href = "/products";

  } catch (error) {
    console.error("Login error:", error);
    setError("Network error. Please try again.");
    try {
      toast.error("Network error. Please try again.");
    } catch {}
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <main className="mx-auto flex min-h-screen max-w-md items-center justify-center px-4 py-12 sm:px-6 lg:px-8">
        <div className="w-full">
          <div className="rounded-3xl border border-white/50 bg-white/80 p-8 shadow-2xl backdrop-blur-sm">
            <div className="mb-8 text-center">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-gradient-to-r from-blue-600 to-blue-700">
                <LogIn className="text-white" size={28} />
              </div>
              <h2 className="text-3xl font-bold text-gray-800">Welcome Back</h2>
              <p className="mt-2 text-gray-600">Sign in to your account</p>
            </div>

            <form onSubmit={handleLogin} className="space-y-6">
              {error && (
                <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-700">
                  {error}
                </div>
              )}

              <div>
                <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                  Username
                </label>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  className="mt-1 block w-full rounded-xl border border-gray-300 px-4 py-3 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                  placeholder="Enter your username"
                />
              </div>

              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <div className="relative mt-1">
                  <input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="block w-full rounded-xl border border-gray-300 px-4 py-3 pr-12 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                    placeholder="Enter your password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 flex items-center pr-3"
                  >
                    {showPassword ? (
                      <EyeOff className="h-5 w-5 text-gray-400" />
                    ) : (
                      <Eye className="h-5 w-5 text-gray-400" />
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="flex w-full items-center justify-center rounded-xl bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-3 text-white shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-50"
              >
                {loading ? "Signing in..." : "Sign In"}
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-sm text-gray-600">
                Don't have an account?{" "}
                <a href="/register" className="font-medium text-blue-600 hover:text-blue-500">
                  Sign up
                </a>
              </p>
            </div>
          </div>
        </div>
      </main>

      <PageFooter />
    </div>
  );
}

export default LoginPage;