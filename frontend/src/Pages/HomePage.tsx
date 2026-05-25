import { Play, ShoppingBag, ShoppingCart } from "lucide-react";
import { PageBackground, PageFooter } from "./extra/PageLayout";

function HomePage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      <PageBackground />

      <section className="relative overflow-hidden pb-32 pt-24">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-blue-700/20" />
        <div className="relative mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mx-auto max-w-4xl">
            <h1 className="mb-8 text-5xl font-bold leading-tight tracking-tight text-transparent md:text-7xl bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text">
              Premium Products
              <span className="block bg-gradient-to-r from-blue-500 to-blue-600 bg-clip-text text-transparent">
                For Modern Living
              </span>
            </h1>
            <p className="mx-auto mb-12 max-w-2xl text-xl leading-relaxed text-gray-600 md:text-2xl">
              Discover our carefully curated collection of high-quality products designed to
              enhance your lifestyle.
            </p>
            <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
              <a
                href="/products"
                className="group inline-flex items-center rounded-3xl bg-gradient-to-r from-blue-600 to-blue-700 px-10 py-5 text-lg font-bold text-white shadow-2xl transition-all duration-500 hover:-translate-y-2 hover:from-blue-700 hover:to-blue-800 hover:shadow-3xl"
              >
                <ShoppingBag className="mr-3 text-xl transition-transform group-hover:scale-110" size={22} />
                Shop Now
              </a>
              <a
                href="/products"
                className="group inline-flex items-center rounded-3xl border-2 border-white/50 bg-white/80 px-10 py-5 text-lg font-bold text-gray-800 shadow-2xl backdrop-blur-sm transition-all duration-500 hover:-translate-y-2 hover:bg-white hover:shadow-3xl"
              >
                <Play className="mr-3 text-blue-600 transition-transform group-hover:scale-110" size={22} />
                View Products
              </a>
            </div>
          </div>
        </div>
        <div className="absolute bottom-0 left-0 h-24 w-full bg-gradient-to-t from-slate-50 to-transparent" />
      </section>

      <section className="py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-8 text-center md:grid-cols-4">
            {[
              ["5k+", "Happy Customers", "from-blue-500 to-blue-600"],
              ["1000+", "Products Sold", "from-emerald-500 to-emerald-600"],
              ["30+", "Countries Served", "from-amber-500 to-amber-600"],
              ["99.9%", "Satisfaction", "from-purple-500 to-purple-600"],
            ].map(([value, label, gradient]) => (
              <div key={label}>
                <div className={`mb-4 bg-gradient-to-r ${gradient} bg-clip-text text-4xl font-bold text-transparent md:text-5xl`}>
                  {value}
                </div>
                <div className="text-xl font-semibold text-gray-600">{label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-white py-24 text-gray-900">
  <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
    <h2 className="mb-6 text-4xl font-bold md:text-5xl">
      Ready to Get Started?
    </h2>

    <p className="mx-auto mb-12 max-w-2xl text-xl text-gray-600">
      Join thousands of satisfied customers who trust Omar's Shop for quality products.
    </p>

    <a
      href="/products"
      className="group relative inline-flex items-center rounded-3xl bg-blue-600 px-12 py-6 text-xl font-bold text-white shadow-lg transition-all duration-300 hover:-translate-y-1.5 hover:bg-blue-700 hover:shadow-[0_20px_40px_-10px_rgba(37,99,235,0.6)]"
    
    >
      <span className="pointer-events-none absolute inset-0 overflow-hidden rounded-3xl">
  <span className="absolute -left-1/3 top-0 h-full w-1/3 rotate-12 bg-white/30 blur-xl transition-all duration-700 group-hover:left-full" />
</span>
      <ShoppingCart className="mr-4" size={24} />
      Start Shopping Now
    </a>
  </div>
</section>
      <PageFooter />
    </div>
  );
}

export default HomePage;