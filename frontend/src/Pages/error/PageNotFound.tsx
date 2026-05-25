import { ShoppingBag, ArrowLeft } from "lucide-react";
import { motion } from "framer-motion";

export default function PageNotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 px-6">
      <div className="text-center max-w-md">
        {/* Animated 404 */}
        <motion.h1
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="text-8xl font-extrabold text-indigo-600"
        >
          404
        </motion.h1>

        <h2 className="mt-4 text-2xl font-semibold text-slate-800">
          Oops! Page not found
        </h2>

        <p className="mt-2 text-slate-500">
          The page you’re looking for doesn’t exist or has been moved.
        </p>

        {/* Decorative icon */}
        <div className="flex justify-center my-8">
          <div className="p-5 rounded-full bg-white shadow-lg">
            <ShoppingBag className="w-10 h-10 text-indigo-500" />
          </div>
        </div>

        <footer className="mt-8">
          <a
            href="/"
            className="inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-blue-600 to-blue-700 px-5 py-3 text-sm font-semibold text-white shadow-lg transition-all duration-300 hover:from-blue-700 hover:to-blue-800"
          >
            <ArrowLeft size={16} />
            Back to Home
          </a>
        </footer>
      </div>
    </div>
  );
}