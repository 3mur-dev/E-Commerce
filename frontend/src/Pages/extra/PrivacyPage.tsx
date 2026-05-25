import { Bolt, IdCard, ShieldCheck, SlidersHorizontal } from "lucide-react";
import { PageBackground, PageFooter } from "./PageLayout";

const sections = [
  {
    id: "collect",
    title: "Information we collect",
    content: (
      <ul className="list-inside list-disc space-y-2 text-gray-700">
        <li>Account details like name, email, and password.</li>
        <li>Order and payment metadata such as items, totals, and shipping address.</li>
        <li>Device and usage information to keep the site reliable and secure.</li>
      </ul>
    ),
  },
  {
    id: "use",
    title: "How we use information",
    content: (
      <ul className="list-inside list-disc space-y-2 text-gray-700">
        <li>Process and fulfill orders, including shipping updates.</li>
        <li>Provide customer support and respond to inquiries.</li>
        <li>Improve product selection, site performance, and security.</li>
      </ul>
    ),
  },
  {
    id: "cookies",
    title: "Cookies and analytics",
    content:
      "We use cookies and similar technologies to remember your preferences, enable shopping features, and understand how the site is used. You can control cookies through your browser settings.",
  },
  {
    id: "share",
    title: "Sharing and disclosure",
    content:
      "We share data only with trusted service providers needed to process payments, ship orders, and operate the site. We do not sell personal information.",
  },
  {
    id: "security",
    title: "Security and retention",
    content:
      "We use reasonable safeguards to protect your data and retain information only as long as needed for business and legal purposes.",
  },
  {
    id: "choices",
    title: "Your choices",
    content: (
      <ul className="list-inside list-disc space-y-2 text-gray-700">
        <li>Access or update your account information.</li>
        <li>Request deletion of your account where applicable.</li>
        <li>Opt out of marketing emails at any time.</li>
      </ul>
    ),
  },
];

function PrivacyPage() {
  const highlightCards = [
    {
      icon: IdCard,
      title: "What We Collect",
      text: "Basic account, order, and usage data to deliver your shopping experience.",
      gradient: "from-blue-500 to-blue-600",
    },
    {
      icon: Bolt,
      title: "How We Use It",
      text: "To process orders, improve products, and keep your account secure.",
      gradient: "from-emerald-500 to-emerald-600",
    },
    {
      icon: SlidersHorizontal,
      title: "Your Choices",
      text: "Access, update, or delete your information and control marketing preferences.",
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
            <ShieldCheck className="mr-2" size={18} />
            Privacy Policy
          </div>
          <h1 className="mb-6 bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text text-5xl font-bold text-transparent md:text-6xl">
            Your Privacy, Respected
          </h1>
          <p className="mx-auto max-w-3xl text-xl leading-relaxed text-gray-600">
            This policy explains what we collect, why we collect it, and the choices you have when
            using Omar&apos;s Shop.
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
                {sections.map((section) => (
                  <a key={section.id} className="block hover:text-blue-600" href={`#${section.id}`}>
                    {section.title}
                  </a>
                ))}
                <a className="block hover:text-blue-600" href="#contact">Contact us</a>
              </nav>
            </div>
          </aside>
          <div className="space-y-8 lg:col-span-2">
            {sections.map((section) => (
              <section key={section.id} id={section.id} className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
                <h3 className="mb-4 text-2xl font-bold text-gray-800">{section.title}</h3>
                {typeof section.content === "string" ? (
                  <p className="leading-relaxed text-gray-700">{section.content}</p>
                ) : (
                  section.content
                )}
              </section>
            ))}
            <section id="contact" className="rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl">
              <h3 className="mb-4 text-2xl font-bold text-gray-800">Contact us</h3>
              <p className="leading-relaxed text-gray-700">
                Questions about privacy? Reach out through our{" "}
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
            <h2 className="mb-4 text-3xl font-bold md:text-4xl">Need help with an order?</h2>
            <p className="mb-8 text-lg opacity-90">Our support team is ready to assist you quickly and kindly.</p>
            <a href="/contact" className="inline-flex items-center rounded-2xl bg-white px-10 py-4 font-bold text-blue-700 shadow-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl">
              <ShieldCheck className="mr-3" size={18} />
              Contact Support
            </a>
          </div>
        </div>
      </section>

      <PageFooter />
    </div>
  );
}

export default PrivacyPage;
