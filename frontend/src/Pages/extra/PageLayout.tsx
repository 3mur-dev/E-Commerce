import type { ReactNode } from "react";

export function PageBackground() {
  return (
    <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
      <div className="absolute -left-16 -top-24 h-80 w-80 rounded-full bg-blue-200/55 blur-3xl" />
      <div className="absolute -right-20 top-1/3 h-[26rem] w-[26rem] rounded-full bg-indigo-100/70 blur-3xl" />
      <div className="absolute bottom-[-8rem] left-1/3 h-96 w-96 rounded-full bg-blue-100/70 blur-3xl" />
    </div>
  );
}

export function PageFooter() {
  return (
    <footer className="mt-10 bg-gradient-to-r from-indigo-500 to-purple-600 py-10 text-white">
      <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <p className="text-sm font-medium text-blue-100 sm:text-base">
          &copy; 2026 Omar&apos;s Shop. All rights reserved.
        </p>
        <div className="mt-4 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm text-blue-100/90">
          <a href="/privacy" className="transition hover:text-white">
            Privacy Policy
          </a>
          <a href="/terms" className="transition hover:text-white">
            Terms of Service
          </a>
          <a href="/about" className="transition hover:text-white">
            About
          </a>
          <a href="/support" className="transition hover:text-white">
            Support
          </a>
          <a href="/contact" className="transition hover:text-white">
            Contact
          </a>
        </div>
      </div>
    </footer>
  );
}

interface PageSectionProps {
  children: ReactNode;
}

export function PageShell({ children }: PageSectionProps) {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 text-slate-900">
      <PageBackground />
      {children}
      <PageFooter />
    </div>
  );
}
