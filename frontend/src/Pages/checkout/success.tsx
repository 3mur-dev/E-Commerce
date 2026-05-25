import { CheckCircle2 } from "lucide-react";
import { Link } from "react-router-dom";

export default function CheckoutSuccess() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-lg rounded-3xl border border-white/50 bg-white/80 p-10 text-center shadow-2xl backdrop-blur-sm">
        
        <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100 text-emerald-600">
          <CheckCircle2 size={42} />
        </div>

        <h1 className="mt-6 text-4xl font-bold text-slate-900">
          Payment Successful
        </h1>

        <p className="mt-4 text-slate-600">
          Your order has been placed successfully and your payment was confirmed.
        </p>

        <div className="mt-8">
          <Link
            to="/orders"
            className="inline-flex items-center justify-center rounded-2xl bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-3 font-semibold text-white shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
          >
            View My Orders
          </Link>
        </div>
      </div>
    </div>
  );
}