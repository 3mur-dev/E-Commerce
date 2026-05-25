import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function AdminRoute() {
  const { user, token, isLoading } = useAuth();
  const normalizedRole = user?.role?.toUpperCase?.() ?? "";
  const isAdmin = normalizedRole.includes("ADMIN");

  if (token && isLoading) {
    return null;
  }

  if (!token || !isAdmin) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
}
