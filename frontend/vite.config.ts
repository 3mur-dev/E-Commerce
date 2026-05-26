import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "https://shoppioapi.onrender.com/api",
        changeOrigin: true,
      },
      "/images": {
        target: "https://shoppioapi.onrender.com/images",
        changeOrigin: true,
      },
    },
  },
});