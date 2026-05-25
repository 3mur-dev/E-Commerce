import Navbar from "./Navbar.tsx";
import AboutPage from "./Pages/extra/AboutPage.tsx";
import CartPage from "./Pages/cart/CartPage.tsx";
import CheckoutPage from "./Pages/order/CheckoutPage.tsx";
import ContactPage from "./Pages/extra/ContactPage.tsx";
import HomePage from "./Pages/HomePage.tsx";
import LoginPage from "./Pages/auth/LoginPage.tsx";
import OrderConfirmationPage from "./Pages/order/OrderConfirmationPage.tsx";
import PrivacyPage from "./Pages/extra/PrivacyPage.tsx";
import ProductsPage from "./Pages/products/ProductsPage.tsx";
import RegisterPage from "./Pages/auth/RegisterPage.tsx";
import SupportPage from "./Pages/extra/SupportPage.tsx";
import TermsPage from "./Pages/extra/TermsPage.tsx";
import WishlistPage from "./Pages/products/WishlistPage.tsx";
import { Route, Routes, useLocation } from "react-router-dom";
import OrdersPage from "./Pages/order/OrdersPage.tsx";
import PageNotFound from "./Pages/error/PageNotFound.tsx";  
import OrderDetails from "./Pages/order/OrdersDetailsPage.tsx";
import ProductDetails from "./Pages/products/ProductDetailsPage.tsx";
import AdminDashboard from "./Pages/admin/dashboard.tsx";
import AdminProductsPage from "./Pages/admin/adminProducts.tsx";
import AdminOrdersPage from "./Pages/admin/adminOrders.tsx";
import AdminUsersPage from "./Pages/admin/adminUsers.tsx";
import AdminRoute from "./guards/AdminRoute.tsx";
import Unauthorized from "./Pages/error/Unauthorized.tsx";
import VerifyEmailPage from "./Pages/auth/VerifyEmailPage.tsx";
import CheckoutSuccess from "./Pages/checkout/success.tsx";

function App() {
const location = useLocation();
  const activePage =
    location.pathname === "/"
      ? "home"
      : location.pathname.startsWith("/products")
        ? "products"
        : location.pathname.startsWith("/cart")
          ? "cart"
          : location.pathname.startsWith("/wishlists")
            ? "wishlist"
            : location.pathname.startsWith("/orders")
              ? "orders"
              : "account";

  return (
    <>
      <Navbar activePage={activePage} />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/products"
          element={<ProductsPage key={`${location.pathname}${location.search}`} />}
        />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/order-confirmation/:id" element={<OrderConfirmationPage />} />
        <Route path="/wishlists" element={<WishlistPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/terms" element={<TermsPage />} />
        <Route path="/support" element={<SupportPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:id" element={<OrderDetails />} />
        <Route path="/products/:id" element={<ProductDetails />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/checkout/success" element={<CheckoutSuccess />} />
        <Route element={<AdminRoute />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/products" element={<AdminProductsPage />} />
          <Route path="/admin/orders" element={<AdminOrdersPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
        </Route>
        <Route path="/unauthorized" element={<Unauthorized />} />
        <Route path="*" element={<PageNotFound />} />
      </Routes>
    </>
  );
}

export default App;
