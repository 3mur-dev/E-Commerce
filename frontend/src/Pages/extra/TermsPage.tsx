import { CheckCircle2, CreditCard, FileText, ShieldCheck, Store } from "lucide-react";
import { PageBackground, PageFooter } from "./PageLayout";

const termsSections = [
  ["acceptance", "Acceptance of terms", "By accessing or using this site, you agree to these terms. If you do not agree, please do not use the site."],
  ["orders", "Orders and payments", [
    "All orders are subject to availability and confirmation.",
    "Prices and promotions may change without notice.",
    "Payment details are processed by secure providers.",
  ]],
  ["shipping", "Shipping and returns", "Shipping timelines are estimates and can vary by location. Return eligibility and timelines will be provided with your order confirmation or on the product page."],
  ["accounts", "Accounts and security", "You are responsible for keeping your account credentials secure. Notify us immediately if you believe your account has been compromised."],
  ["prohibited", "Prohibited use", [
    "Attempting to access data or systems without authorization.",
    "Posting harmful, fraudulent, or misleading content.",
    "Interfering with site performance or security.",
  ]],
  ["ip", "Intellectual property", "All content on this site, including logos and designs, is owned by Omar's Shop or its licensors and is protected by law."],
  ["disclaimer", "Disclaimers", "The site and products are provided as available without warranties of any kind, to the extent permitted by law."],
  ["liability", "Limitation of liability", "Omar's Shop is not liable for indirect or incidental damages arising from your use of the site, to the extent permitted by law."],
  ["changes", "Changes to terms", "We may update these terms from time to time. Continued use of the site means you accept the updated terms."],
] as const;

function TermsPage() {
  const highlightCards = [
    {
      icon: CheckCircle2,
      title: "Fair Use",
      text: "Use the site responsibly and respect the community and our content.",
      gradient: "from-blue-500 to-blue-600",
    },
    {
      icon: CreditCard,
      title: "Orders and Payments",
      text: "Prices, availability, and shipping details are shown at checkout.",
      gradient: "from-emerald-500 to-emerald-600",
    },
    {
      icon: ShieldCheck,
      title: "Account Safety",
      text: "Keep your login secure and let us know if you suspect misuse.",
      gradient: "from-amber-500 to-amber-600",
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 font-sans antialiased">
      <PageBackground />

      <section className="relative overflow-hidden pb-24 pt-24">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-blue-700/20" />
        <div className="relative mx-auto max-w-6xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mb-6 inline-flex items-center rounded-full bg-white/70 px-5 py-2 font-semibold text-blue-700 shadow-lg">
            <FileText className="mr-2" size={18} />
            Terms and Conditions
          </div>
          <h1 className="mb-6 bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text text-5xl font-bold text-transparent md:text-6xl">
            Clear and Simple Rules
          </h1>
          <p className="mx-auto max-w-3xl text-xl leading-relaxed text-gray-600">
            These terms describe your rights and responsibilities when you shop with Omar&apos;s Shop.
          </p>
          <p className="mt-4 text-sm text-gray-500">Last updated: Feb 10, 2026</p>
        </div>
        <div className="absolute bottom-0 left-0 h-20 w-full bg-gradient-to-t from-slate-50 to-transparent" />
      </section>

      <section className="py-16">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 md:grid-cols-3 sm:px-6 lg:px-8">
          {highlightCards.map(({ icon: Cmp, title, text, gradient }) => {
            return (
              <div key={title} className="rounded-3xl border border-white/60 bg-white/70 p-8 shadow-xl backdrop-blur-sm">
                <div className={`mb-6 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-r ${gradient} text-white shadow-lg`}>
                  <Cmp size={18} />
                </div>
                <h3 className="mb-3 text-xl font-bold text-gray-800">{title}</h3>
                <p className="leading-relaxed text-gray-600">{text}</p>
              </div>
            );
          })}
        </div>
      </section>

      <section className="bg-white/50 py-20 backdrop-blur-sm">
        <div className="mx-auto grid max-w-6xl gap-10 px-4 lg:grid-cols-3 sm:px-6 lg:px-8">
          <aside className="lg:col-span-1">
            <div className="sticky top-28 rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="text-lg font-bold text-gray-800">On this page</h3>
              <nav className="mt-6 space-y-3 text-gray-600">
                {termsSections.map(([id, title]) => (
                  <a key={id} className="block hover:text-blue-600" href={`#${id}`}>
                    {title}
                  </a>
                ))}
                <a className="block hover:text-blue-600" href="#contact">Contact us</a>
              </nav>
            </div>
          </aside>
          <div className="space-y-8 lg:col-span-2">
            {termsSections.map(([id, title, content]) => (
              <section key={id} id={id} className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
                <h3 className="mb-4 text-2xl font-bold text-gray-800">{title}</h3>
                {Array.isArray(content) ? (
                  <ul className="list-inside list-disc space-y-2 text-gray-700">
                    {content.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                ) : (
                  <p className="leading-relaxed text-gray-700">{content}</p>
                )}
              </section>
            ))}
            <section id="contact" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Contact us</h3>
              <p className="leading-relaxed text-gray-700">
                Questions about these terms? Contact us through our{" "}
                <a href="/contact" className="font-semibold text-blue-600 hover:text-blue-700">
                  contact page
                </a>
                .
              </p>
            </section>
          </div>
        </div>
      </section>

      <section className="py-20">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <div className="rounded-3xl bg-gradient-to-r from-indigo-500 to-purple-600 p-10 text-center text-white shadow-2xl">
            <h2 className="mb-4 text-3xl font-bold md:text-4xl">Ready to keep shopping?</h2>
            <p className="mb-8 text-lg opacity-90">Explore new arrivals and curated picks built for everyday life.</p>
            <a href="/products" className="inline-flex items-center rounded-2xl bg-white px-10 py-4 font-bold text-blue-700 shadow-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl">
              <Store className="mr-3" size={18} />
              Browse Products
            </a>
          </div>
        </div>
      </section>

      <PageFooter />
    </div>
  );
}

export default TermsPage;
