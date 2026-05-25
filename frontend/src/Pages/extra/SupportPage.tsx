import { CreditCard, Headset, LifeBuoy, PackageOpen, RotateCcw, ShieldCheck, Truck } from "lucide-react";
import { useState } from "react";
import { PageBackground, PageFooter } from "./PageLayout";

const faqs = [
  {
    q: "Can I change my order after placing it?",
    a: "We can update orders within 2 hours of purchase. Contact support quickly with your order number.",
  },
  {
    q: "What if my package arrives damaged?",
    a: "Please contact support within 48 hours of delivery with photos of the item and packaging.",
  },
  {
    q: "Do you offer international shipping?",
    a: "Yes, we ship to many regions worldwide. Shipping costs are shown at checkout based on your location.",
  },
  {
    q: "How do I reset my password?",
    a: "Use the login page to reset your password, or contact support if you cannot access your email.",
  },
];

function SupportPage() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);
  const quickActions = [
    {
      icon: PackageOpen,
      title: "Track an Order",
      text: "Check the latest delivery status for your purchases.",
      cta: "Get tracking help",
      href: "/contact",
      gradient: "from-blue-500 to-blue-600",
      textColor: "text-blue-600",
    },
    {
      icon: RotateCcw,
      title: "Returns and Refunds",
      text: "Learn about return eligibility and refund timelines.",
      cta: "View return steps",
      href: "#returns",
      gradient: "from-emerald-500 to-emerald-600",
      textColor: "text-emerald-600",
    },
    {
      icon: Truck,
      title: "Shipping Info",
      text: "Find delivery times, costs, and shipping regions.",
      cta: "See shipping details",
      href: "#shipping",
      gradient: "from-amber-500 to-amber-600",
      textColor: "text-amber-600",
    },
    {
      icon: ShieldCheck,
      title: "Account Help",
      text: "Reset passwords, update profiles, or secure your account.",
      cta: "Contact support",
      href: "/contact",
      gradient: "from-purple-500 to-purple-600",
      textColor: "text-purple-600",
    },
    {
      icon: CreditCard,
      title: "Payments",
      text: "Questions about payment methods or billing issues.",
      cta: "Payment FAQs",
      href: "#payments",
      gradient: "from-sky-500 to-sky-600",
      textColor: "text-sky-600",
    },
    {
      icon: Headset,
      title: "Talk to Us",
      text: "We are ready to help with any order or product questions.",
      cta: "Start a conversation",
      href: "/contact",
      gradient: "from-rose-500 to-rose-600",
      textColor: "text-rose-600",
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 font-sans antialiased">
      <PageBackground />

      <section className="relative overflow-hidden pb-24 pt-24">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-blue-700/20" />
        <div className="relative mx-auto max-w-6xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mb-6 inline-flex items-center rounded-full bg-white/70 px-5 py-2 font-semibold text-blue-700 shadow-lg">
            <LifeBuoy className="mr-2" size={18} />
            Support Center
          </div>
          <h1 className="mb-6 bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text text-5xl font-bold text-transparent md:text-6xl">
            How can we help?
          </h1>
          <p className="mx-auto max-w-3xl text-xl leading-relaxed text-gray-600">
            Find quick answers about orders, shipping, returns, and account support. If you need personal
            help, our team is ready.
          </p>
        </div>
        <div className="absolute bottom-0 left-0 h-20 w-full bg-gradient-to-t from-slate-50 to-transparent" />
      </section>

      <section className="py-16">
        <div className="mx-auto grid max-w-6xl gap-8 px-4 md:grid-cols-2 lg:grid-cols-3 sm:px-6 lg:px-8">
          {quickActions.map(({ icon: Cmp, title, text, cta, href, gradient, textColor }) => {
            return (
              <div key={title} className="rounded-3xl border border-white/60 bg-white/70 p-8 shadow-xl backdrop-blur-sm">
                <div className={`mb-6 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-r ${gradient} text-white shadow-lg`}>
                  <Cmp size={18} />
                </div>
                <h3 className="mb-3 text-xl font-bold text-gray-800">{title}</h3>
                <p className="mb-6 leading-relaxed text-gray-600">{text}</p>
                <a href={href} className={`font-semibold hover:opacity-80 ${textColor}`}>
                  {cta}
                </a>
              </div>
            );
          })}
        </div>
      </section>

      <section className="bg-white/50 py-20 backdrop-blur-sm">
        <div className="mx-auto grid max-w-6xl gap-10 px-4 lg:grid-cols-3 sm:px-6 lg:px-8">
          <aside className="lg:col-span-1">
            <div className="sticky top-28 rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="text-lg font-bold text-gray-800">Help topics</h3>
              <nav className="mt-6 space-y-3 text-gray-600">
                <a className="block hover:text-blue-600" href="#tracking">Order tracking</a>
                <a className="block hover:text-blue-600" href="#returns">Returns and refunds</a>
                <a className="block hover:text-blue-600" href="#shipping">Shipping</a>
                <a className="block hover:text-blue-600" href="#payments">Payments</a>
                <a className="block hover:text-blue-600" href="#faq">FAQs</a>
              </nav>
            </div>
          </aside>
          <div className="space-y-8 lg:col-span-2">
            <section id="tracking" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Order tracking</h3>
              <ol className="list-inside list-decimal space-y-2 text-gray-700">
                <li>Check your order confirmation email for a tracking link.</li>
                <li>If tracking is delayed, allow up to 24 hours for updates.</li>
                <li>Still stuck? Contact support with your order number.</li>
              </ol>
              <div className="mt-6 flex flex-wrap gap-4">
                <a href="/contact" className="inline-flex items-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white shadow-lg transition-colors hover:bg-blue-700">
                  <Headset className="mr-2" size={16} />
                  Contact Support
                </a>
                <a href="/cart" className="inline-flex items-center rounded-2xl border border-blue-100 bg-white px-6 py-3 font-semibold text-blue-600 shadow-lg transition-colors hover:bg-blue-50">
                  <PackageOpen className="mr-2" size={16} />
                  View Orders
                </a>
              </div>
            </section>
            <section id="returns" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Returns and refunds</h3>
              <ul className="list-inside list-disc space-y-2 text-gray-700">
                <li>Returns are accepted within 14 days of delivery.</li>
                <li>Items must be unused and in original packaging.</li>
                <li>Refunds are processed within 5-10 business days.</li>
              </ul>
            </section>
            <section id="shipping" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Shipping</h3>
              <p className="leading-relaxed text-gray-700">
                Standard delivery typically takes 3-7 business days, with express options available at
                checkout. International shipping times vary by region.
              </p>
            </section>
            <section id="payments" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Payments</h3>
              <p className="leading-relaxed text-gray-700">
                We accept major credit cards and secure payment providers. For billing issues or failed
                payments, please contact support.
              </p>
            </section>
          </div>
        </div>
      </section>

      <section id="faq" className="py-20">
        <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
          <div className="mb-12 text-center">
            <h2 className="mb-4 bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-4xl font-bold text-transparent md:text-5xl">
              Frequently Asked Questions
            </h2>
            <p className="text-lg text-gray-600">Quick answers to common questions from customers.</p>
          </div>
          <div className="space-y-4">
            {faqs.map((faq, index) => {
              const open = openIndex === index;
              return (
                <div key={faq.q} className="rounded-2xl border border-white/60 bg-white/80 p-6 shadow-lg">
                  <button
                    type="button"
                    onClick={() => setOpenIndex(open ? null : index)}
                    className="flex w-full items-center justify-between text-left text-lg font-semibold text-gray-800"
                  >
                    <span>{faq.q}</span>
                    <span className={`text-blue-600 transition-transform duration-300 ${open ? "rotate-180" : ""}`}>⌄</span>
                  </button>
                  {open && <div className="mt-4 text-gray-700">{faq.a}</div>}
                </div>
              );
            })}
          </div>
        </div>
      </section>

      <section className="bg-gradient-to-r from-blue-600 to-blue-700 py-20 text-white">
        <div className="mx-auto max-w-5xl px-4 text-center sm:px-6 lg:px-8">
          <h2 className="mb-4 text-3xl font-bold md:text-4xl">Need more details?</h2>
          <p className="mb-8 text-lg opacity-90">Review our policies for full coverage and customer protections.</p>
          <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
            <a href="/terms" className="inline-flex items-center rounded-2xl bg-white px-8 py-4 font-bold text-blue-700 shadow-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl">
              Terms
            </a>
            <a href="/privacy" className="inline-flex items-center rounded-2xl border border-white/40 bg-white/10 px-8 py-4 font-bold text-white shadow-xl transition-all duration-300 hover:bg-white/20">
              Privacy
            </a>
          </div>
        </div>
      </section>

      <PageFooter />
    </div>
  );
}

export default SupportPage;
