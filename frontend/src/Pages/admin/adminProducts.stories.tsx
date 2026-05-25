import AdminProductsPage from "./adminProducts";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";

const queryClient = new QueryClient();

export default {
  title: "Pages/Admin/AdminProducts",
  component: AdminProductsPage,
};

export const Default = () => (
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <div style={{ padding: 20 }}>
        <AdminProductsPage />
      </div>
    </BrowserRouter>
  </QueryClientProvider>
);