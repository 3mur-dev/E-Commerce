import { Mail, RefreshCw, Send, ShoppingCart, User } from "lucide-react";
import { useState } from "react";
import { PageBackground, PageFooter } from "./PageLayout";

function ContactPage() {
  const [form, setForm] = useState({ name: "", email: "", message: "" });
  const [sent, setSent] = useState(false);

  const resetForm = () => {
    setForm({ name: "", email: "", message: "" });
    setSent(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 font-sans antialiased">
      <PageBackground />

      <section className="relative overflow-hidden pb-32 pt-24">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-blue-700/20" />
        <div className="relative mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
          <div className="mx-auto max-w-4xl">
            <h1 className="mb-8 text-5xl font-bold leading-tight md:text-7xl">
              <span className="bg-gradient-to-r from-gray-800 via-gray-600 to-black bg-clip-text text-transparent">
                Get In Touch
              </span>
              <span className="block bg-gradient-to-r from-blue-500 to-blue-600 bg-clip-text text-transparent">
                With Our Team
              </span>
            </h1>
            <p className="mx-auto mb-12 max-w-2xl text-xl leading-relaxed text-gray-600 md:text-2xl">
              If you have any questions, suggestions, or feedback, feel free to reach out. Our team
              responds within 24 hours!
            </p>
          </div>
        </div>
        <div className="absolute bottom-0 left-0 h-24 w-full bg-gradient-to-t from-slate-50 to-transparent" />
      </section>

      <section className="bg-white/50 py-24 backdrop-blur-sm">
        <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
          {sent && (
            <div className="mb-12 rounded-3xl border-2 border-emerald-400/50 bg-emerald-100/80 p-8 text-emerald-800 shadow-2xl backdrop-blur-sm">
              <div className="flex items-center justify-center text-center">
                <Mail className="mr-4" size={38} />
                <div>
                  <h3 className="mb-2 text-2xl font-bold">Message Sent Successfully!</h3>
                  <p>Thank you! We&apos;ll get back to you within 24 hours.</p>
                </div>
              </div>
            </div>
          )}

          <div className="group overflow-hidden rounded-3xl border border-white/50 bg-white/70 shadow-xl transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl backdrop-blur-sm">
            <form
              className="p-12"
              onSubmit={(event) => {
                event.preventDefault();
                setSent(true);
              }}
            >
              <div className="mb-12 grid gap-8 md:grid-cols-2">
                <div>
                  <label className="mb-4 block bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-xl font-bold text-transparent">
                    <User className="mr-2 inline-flex" size={18} />
                    Your Name
                  </label>
                  <input
                    type="text"
                    required
                    value={form.name}
                    onChange={(event) => setForm({ ...form, name: event.target.value })}
                    placeholder="Enter your full name"
                    className="w-full rounded-3xl border-2 border-gray-200 bg-white/50 px-6 py-5 text-lg placeholder-gray-400 shadow-lg transition-all duration-500 hover:border-blue-400 hover:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/30"
                  />
                </div>

                <div>
                  <label className="mb-4 block bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-xl font-bold text-transparent">
                    <Mail className="mr-2 inline-flex" size={18} />
                    Your Email
                  </label>
                  <input
                    type="email"
                    required
                    value={form.email}
                    onChange={(event) => setForm({ ...form, email: event.target.value })}
                    placeholder="your.email@example.com"
                    className="w-full rounded-3xl border-2 border-gray-200 bg-white/50 px-6 py-5 text-lg placeholder-gray-400 shadow-lg transition-all duration-500 hover:border-blue-400 hover:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/30"
                  />
                </div>
              </div>

              <div className="mb-12">
                <label className="mb-4 block bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-xl font-bold text-transparent">
                  <Mail className="mr-2 inline-flex" size={18} />
                  Your Message
                </label>
                <textarea
                  rows={8}
                  required
                  value={form.message}
                  onChange={(event) => setForm({ ...form, message: event.target.value })}
                  placeholder="Tell us more about your inquiry, questions, or feedback..."
                  className="w-full resize-y rounded-3xl border-2 border-gray-200 bg-white/50 px-6 py-5 text-lg font-medium placeholder-gray-400 shadow-lg transition-all duration-500 hover:border-blue-400 hover:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-500/30"
                />
              </div>

              <div className="flex flex-col justify-end gap-6 sm:flex-row">
                <button
                  type="button"
                  onClick={resetForm}
                  className="group flex-1 rounded-3xl border-2 border-gray-200 bg-white/80 px-10 py-5 text-lg font-bold text-gray-800 shadow-xl transition-all duration-500 hover:-translate-y-1 hover:border-gray-300 hover:bg-white hover:shadow-2xl sm:w-auto"
                >
                  <RefreshCw className="mr-3 inline-flex transition-transform group-hover:rotate-180" size={18} />
                  Reset Form
                </button>
                <button
                  type="submit"
                  className="group flex-1 rounded-3xl bg-gradient-to-r from-blue-600 to-blue-700 px-12 py-5 text-lg font-bold text-white shadow-2xl transition-all duration-500 hover:-translate-y-2 hover:from-blue-700 hover:to-blue-800 hover:shadow-3xl sm:w-auto"
                >
                  <span className="flex items-center justify-center">
                    <Send className="mr-3 text-xl transition-transform group-hover:translate-x-1" size={20} />
                    Send Message
                  </span>
                </button>
              </div>
            </form>
          </div>
        </div>
      </section>

      <section className="bg-gradient-to-r from-blue-600 to-blue-700 py-24 text-white">
        <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
          <h2 className="mb-6 bg-gradient-to-r from-white to-blue-100 bg-clip-text text-4xl font-bold text-transparent md:text-5xl">
            Ready to Get Started?
          </h2>
          <p className="mx-auto mb-12 max-w-2xl text-xl opacity-90">
            Join thousands of satisfied customers who trust Omar&apos;s Shop.
          </p>
          <a href="/products" className="inline-flex items-center rounded-3xl bg-white px-12 py-6 text-xl font-bold text-blue-600 shadow-2xl transition-all duration-500 hover:-translate-y-2 hover:bg-gray-50 hover:shadow-3xl">
            <ShoppingCart className="mr-4 text-2xl" size={24} />
            Start Shopping Now
          </a>
        </div>
      </section>

      <PageFooter />
    </div>
  );
}

export default ContactPage;
