export default function Unauthorized() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950 px-4">
      <div className="text-center bg-slate-900 border border-slate-800 shadow-xl rounded-2xl p-8 max-w-md w-full">
        
        <h1 className="text-6xl font-bold text-red-500">403</h1>
        
        <h2 className="mt-2 text-2xl font-semibold text-white">
          Access Denied
        </h2>

        <p className="mt-4 text-slate-400">
          You don’t have permission to access this page.
        </p>

        <a
          href="/"
          className="inline-block mt-6 px-5 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 transition text-white font-medium"
        >
          Go back home
        </a>
      </div>
    </div>
  );
}