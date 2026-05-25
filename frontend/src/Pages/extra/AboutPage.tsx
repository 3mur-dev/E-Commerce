import { Cog, Headphones, Heart, ShoppingCart, UserRound } from "lucide-react";
import { PageBackground, PageFooter } from "./PageLayout";

function AboutPage() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 font-sans antialiased">
      <PageBackground />

      <section className="relative overflow-hidden pb-32 pt-24">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-blue-700/20" />
        <div className="relative mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mx-auto max-w-5xl">
            <h1 className="mb-8 text-5xl font-bold leading-tight md:text-7xl">
              <span className="bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text text-transparent">
                About Our Story
              </span>
              <span className="block bg-gradient-to-r from-blue-500 to-blue-600 bg-clip-text text-transparent">
                Shoppio
              </span>
            </h1>
            <p className="mx-auto mb-12 max-w-3xl text-xl leading-relaxed text-gray-600 md:text-2xl">
              Discover the passion and commitment behind the brand that brings you premium products
              for modern living.
            </p>
          </div>
        </div>
        <div className="absolute bottom-0 left-0 h-24 w-full bg-gradient-to-t from-slate-50 to-transparent" />
      </section>

      <section className="bg-white/50 py-24 backdrop-blur-sm">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <div className="mb-24 text-center">
            <h2 className="mb-6 bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-4xl font-bold text-transparent md:text-5xl">
              Our Journey
            </h2>
            <p className="mx-auto max-w-3xl text-xl leading-relaxed text-gray-600">
              Founded with passion and commitment to excellence.
            </p>
          </div>

          <div className="mb-24 grid items-center gap-16 lg:grid-cols-2">
            <div className="space-y-8">
              <div className="group rounded-3xl border border-white/50 bg-white/70 p-10 shadow-xl transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl">
                <div className="mb-6 flex items-start">
                  <div className="mr-6 flex h-16 w-16 flex-shrink-0 items-center justify-center rounded-3xl bg-gradient-to-r from-blue-500 to-blue-600 text-white shadow-2xl">
                    <Heart size={26} />
                  </div>
                  <div>
                    <h3 className="mb-3 text-2xl font-bold text-gray-800">High-Quality Products</h3>
                    <p className="text-lg leading-relaxed text-gray-600">
                      Dedicated to bringing you premium products at competitive prices with seamless
                      shopping experience.
                    </p>
                  </div>
                </div>
              </div>

              <div className="group rounded-3xl border border-white/50 bg-white/70 p-10 shadow-xl transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl">
                <div className="mb-6 flex items-start">
                  <div className="mr-6 flex h-16 w-16 flex-shrink-0 items-center justify-center rounded-3xl bg-gradient-to-r from-emerald-500 to-emerald-600 text-white shadow-2xl">
                    <ShoppingCart size={26} />
                  </div>
                  <div>
                    <h3 className="mb-3 text-2xl font-bold text-gray-800">Reliable Service</h3>
                    <p className="text-lg leading-relaxed text-gray-600">
                      Exceptional customer service and reliable shipping with curated selection tailored
                      to your needs.
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-8 text-lg leading-relaxed text-gray-700">
              <p>
                Welcome to{" "}
                <span className="bg-gradient-to-r from-blue-600 to-blue-700 bg-clip-text text-2xl font-bold text-transparent">
                  Omar&apos;s Shop
                </span>
                ! We are dedicated to bringing you high-quality products at competitive prices, making your
                shopping experience seamless and enjoyable.
              </p>
              <p>
                Our mission is to provide a trusted online store where customers can find exactly what they
                need with ease. We believe in building lasting relationships with our customers and growing
                together as a community.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="py-24">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-20 text-center">
            <h2 className="mb-6 bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-4xl font-bold text-transparent md:text-5xl">
              Meet Our Team
            </h2>
            <p className="mx-auto max-w-2xl text-xl text-gray-600">
              The passionate people behind Omar&apos;s Shop
            </p>
          </div>

          <div className="grid gap-8 md:grid-cols-3">
            {[
              ["Omar", "Founder & CEO", "Visionary leader driving innovation and growth.", "from-blue-500 to-blue-600", <UserRound size={50} />],
              ["Agent1", "Customer Support", "Your satisfaction is our top priority.", "from-emerald-500 to-emerald-600", <Headphones size={50} />],
              ["Agent2", "Operations Manager", "Keeping everything running smoothly.", "from-amber-500 to-amber-600", <Cog size={50} />],
            ].map(([name, role, text, gradient, icon]) => (
              <div
                key={String(name)}
                className="group overflow-hidden rounded-3xl border border-white/50 bg-white/70 shadow-xl transition-all duration-500 hover:-translate-y-4 hover:shadow-2xl"
              >
                <div className={`flex h-64 items-center justify-center bg-gradient-to-br ${gradient}`}>
                  <div className="flex h-32 w-32 items-center justify-center rounded-full border-4 border-white/30 bg-gradient-to-br from-white/20 to-transparent text-white shadow-2xl">
                    {icon}
                  </div>
                </div>
                <div className="p-8 text-center">
                  <h3 className="mb-2 text-2xl font-bold text-gray-800 transition-colors group-hover:text-blue-600">
                    {name}
                  </h3>
                  <p className="mb-4 text-xl font-semibold text-blue-600">{role}</p>
                  <p className="leading-relaxed text-gray-600">{text}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-gradient-to-r from-blue-600 to-blue-700 py-24 text-white">
        <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
          <h2 className="mb-6 bg-gradient-to-r from-white to-blue-100 bg-clip-text text-4xl font-bold text-transparent md:text-5xl">
            Ready to Shop?
          </h2>
          <p className="mx-auto mb-12 max-w-2xl text-xl opacity-90">
            Join thousands of satisfied customers who trust Omar&apos;s Shop.
          </p>
          <a
            href="/products"
            className="inline-flex items-center rounded-3xl bg-white px-12 py-6 text-xl font-bold text-blue-600 shadow-2xl transition-all duration-500 hover:-translate-y-2 hover:bg-gray-50 hover:shadow-3xl"
          >
            <ShoppingCart className="mr-4 text-2xl" size={24} />
            Start Shopping Now
          </a>
        </div>
      </section>

      <PageFooter />
    </div>
  );
}

export default AboutPage;
