import {
  ChevronDown,
  Home,
  LogIn,
  LogOut,
  Menu,
  Search,
  ShoppingCart,
  Store,
  UserCircle,
  X,
} from "lucide-react";
import { type FormEvent, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { useCart } from "./CartContext";

type NavPage = "home" | "products" | "wishlist" | "cart" | "account" | "orders";

interface NavbarProps {
  activePage?: NavPage;
}

const navLinks: Array<{
  key: Exclude<NavPage, "account">;
  label: string;
  href: string;
  icon: typeof Home;
}> = [
  { key: "products", label: "Products", href: "/products", icon: Store },
  { key: "cart", label: "Cart", href: "/cart", icon: ShoppingCart },
];

const adminLinks = [
  { label: "Admin Dashboard", href: "/admin/dashboard", icon: Home },
  { label: "Admin Products", href: "/admin/products", icon: Store },
  { label: "Admin Orders", href: "/admin/orders", icon: ShoppingCart },
  { label: "Admin Users", href: "/admin/users", icon: UserCircle },
];


function Navbar({ activePage = "products"}: NavbarProps) {
  const { user, isAuthenticated, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const searchKeyword = useMemo(
    () => new URLSearchParams(location.search).get("keyword") ?? "",
    [location.search],
  );

  const isAdmin = user?.role?.includes("ADMIN");
  const { cart } = useCart();

const cartCount = useMemo(
  () => cart.reduce((sum, item) => sum + item.quantity, 0),
  [cart],
);

  const displayedCartCount = useMemo(
    () => (cartCount > 99 ? "99+" : String(cartCount)),
    [cartCount],
  );

  const handleLogout = () => {
    logout();
    setAccountOpen(false);
    setMobileOpen(false);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const trimmedKeyword = String(formData.get("keyword") ?? "").trim();

    navigate(
      trimmedKeyword === ""
        ? "/products"
        : `/products?keyword=${encodeURIComponent(trimmedKeyword)}`,
    );
    setAccountOpen(false);
    setMobileOpen(false);
  };

  const handleNavClick = () => {
    setAccountOpen(false);
    setMobileOpen(false);
  };

  return (
    <header className="relative z-50 border-b border-white/40 bg-[linear-gradient(135deg,#6d5dfc_0%,#7c6cf7_38%,#9381ff_100%)] text-white shadow-2xl">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-4 py-5">
          <Link
            to="/"
            className="whitespace-nowrap bg-gradient-to-r from-white via-violet-100 to-indigo-100 bg-clip-text text-3xl font-bold text-transparent md:text-4xl"
          >
            Shoppio
          </Link>

          <form
            onSubmit={handleSearchSubmit}
            className="hidden max-w-xl flex-1 items-center gap-3 rounded-2xl border border-white/25 bg-white/16 px-4 py-2 md:flex"
          >
            <Search size={16} className="text-white/80" />
            <input
              key={`desktop-${searchKeyword}`}
              type="text"
              name="keyword"
              defaultValue={searchKeyword}
              placeholder="Search products..."
              className="w-full bg-transparent text-white placeholder:text-white/70 outline-none"
            />
            <button
              type="submit"
              className="rounded-xl bg-white/22 px-3 py-1.5 text-sm font-semibold transition-colors hover:bg-white/30"
            >
              Search
            </button>
          </form>

          <nav className="hidden items-center gap-5 text-sm lg:flex xl:text-base">
            {navLinks.map((link) => {
              const active = link.key === activePage;
              const Icon = link.icon;

              return (
                <Link
                  key={link.key}
                  to={link.href}
                  onClick={handleNavClick}
                  className={`group relative flex items-center font-medium transition-all duration-300 ${
                    active ? "font-bold text-violet-100" : "text-white/90 hover:text-white"
                  }`}
                >
                  <Icon size={16} className="mr-2" />
                  {link.label}

                  {link.key === "cart" && (
                    <span className="ml-1 rounded-full bg-red-500 px-2 py-0.5 text-xs font-bold text-white">
                      {displayedCartCount}
                    </span>
                  )}

                  <span
                    className={`absolute -bottom-1 left-0 h-0.5 bg-white transition-all duration-300 ${
                      active ? "w-full" : "w-0 group-hover:w-full"
                    }`}
                  />
                </Link>
              );
            })}
          </nav>

          <div className="ml-auto flex items-center gap-2">
              {!isAuthenticated ? (
              <Link
                to="/login"
                onClick={handleNavClick}
                className="rounded-xl border border-white/25 bg-white/16 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-white/24"
              >
                Login
              </Link>
            ) : (
              <div className="relative">
                <button
                type="button"
                onClick={() => setAccountOpen((open) => !open)}
                className="inline-flex items-center gap-2 rounded-xl border border-white/25 bg-white/16 px-3 py-2 text-sm font-semibold text-white"
                aria-haspopup="true"
                aria-expanded={accountOpen}
              >
              <UserCircle size={18} />
              <span className="hidden md:inline">
                {user?.username ?? "Account"}
              </span>
              <ChevronDown
                size={14}
                className={`transition-transform ${accountOpen ? "rotate-180" : ""}`}
              />
              </button>

{accountOpen && (
  <div className="absolute right-0 z-50 mt-2 w-52 rounded-2xl bg-white p-2 text-gray-800 shadow-2xl">

    {isAdmin && (
      <>
        {adminLinks.map((link) => {
          const Icon = link.icon;
          return (
            <Link
              key={link.href}
              to={link.href}
              onClick={handleNavClick}
              className="flex items-center rounded-xl px-3 py-2 text-sm transition-colors hover:bg-gray-100"
            >
              <Icon size={16} className="mr-2" />
              {link.label}
            </Link>
          );
        })}

        <div className="my-1 border-t" />
      </>
    )}

    <Link
      to="/orders"
      onClick={handleNavClick}
      className="block rounded-xl px-3 py-2 text-sm transition-colors hover:bg-gray-100"
    >
      My Orders
    </Link>

    <Link
      to="/wishlists"
      onClick={handleNavClick}
      className="block rounded-xl px-3 py-2 text-sm transition-colors hover:bg-gray-100"
    >
      My Wishlist
    </Link>

    <button
      type="button"
      onClick={handleLogout}
      className="mt-1 w-full rounded-xl px-3 py-2 text-left text-sm text-red-600 transition-colors hover:bg-red-50"
    >
      Logout
    </button>
  </div>
)}
              </div>
            )}
          </div>

          <button
            type="button"
            className="rounded-xl border border-white/25 bg-white/16 p-2 backdrop-blur-sm lg:hidden"
            onClick={() => setMobileOpen((open) => !open)}
            aria-expanded={mobileOpen}
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>

        {mobileOpen && (
          <nav className="pb-4 lg:hidden">
            <div className="flex flex-col gap-3 rounded-2xl border border-white/20 bg-white/12 p-4 backdrop-blur-sm">
              <form
                onSubmit={handleSearchSubmit}
                className="flex items-center gap-2 rounded-xl border border-white/20 bg-white/16 px-3 py-2">
                <Search size={16} className="text-white/80" />
                <input
                  key={`mobile-${searchKeyword}`}
                  type="text"
                  name="keyword"
                  defaultValue={searchKeyword}
                  placeholder="Search products..."
                  className="w-full bg-transparent text-white placeholder:text-white/70 outline-none"
                />
              </form>

              {navLinks.map((link) => {
                const active = link.key === activePage;
                const Icon = link.icon;

                return (
                  <Link
                    key={link.key}
                    to={link.href}
                    onClick={handleNavClick}
                    className={`flex items-center rounded-xl px-1 py-1 text-sm ${
                      active ? "font-semibold text-violet-100" : "text-white/90"
                    }`}
                  >
                    <Icon size={16} className="mr-2" />
                    {link.label}
                    {link.key === "cart" && (
                      <span className="ml-1 rounded-full bg-red-500 px-2 py-0.5 text-xs font-bold text-white">
                        {displayedCartCount}
                      </span>
                    )}
                  </Link>
                );
              })}

              <div className="mt-2 space-y-2 border-t border-white/20 pt-3">
                {!isAuthenticated ? (
                  <Link to="/login" onClick={handleNavClick} className="flex items-center text-white/90">
                    <LogIn size={16} className="mr-2" />
                    Login
                  </Link>
                ) : (
                  <>
                    <button type="button" onClick={handleLogout} className="flex items-center text-white/90">
                      <LogOut size={16} className="mr-2" />
                      Logout
                    </button>
                  </>
                )}
              </div>
            </div>
          </nav>
        )}
      </div>
    </header>
  );
}
export default Navbar;
