import { useSearchParams } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import { API_BASE } from "../../api";
import toast from "react-hot-toast";


export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [status, setStatus] = useState<"idle" | "loading" | "success" | "failed">("idle");
  const [message, setMessage] = useState("");
  const [email, setEmail] = useState("");

  const verifyEmail = useCallback(async () => {
    if (!token) {
      setStatus("failed");
      setMessage("Invalid verification link");
      return;
    }

    setStatus("loading");

    try {
      const res = await fetch(
        `${API_BASE}/auth/verify-email?token=${token}`
      );

      const data = await res.json();

if (data.verified) {
  setStatus("success");
  setMessage(data.message);
  try {
    toast.success(data.message || "Email verified successfully.");
  } catch {}
} else {
  setStatus("failed");
  setMessage("Verification failed");
  try {
    toast.error("Verification failed");
  } catch {}
}
    } catch {
      setStatus("failed");
      setMessage("Network error");
    }
  }, [token]);

  useEffect(() => {
    const timerId = window.setTimeout(() => {
      void verifyEmail();
    }, 0);

    return () => window.clearTimeout(timerId);
  }, [verifyEmail]);

  const resendEmail = async () => {
    if (!email.trim()) {
      try {
        toast.error("Please enter your email address");
      } catch {}
      return;
    }

    try {
      const res = await fetch(
        `${API_BASE}/auth/resend-verification?email=${encodeURIComponent(email.trim())}`,
        { method: "POST" }
      );

      const text = await res.text();
      try {
        toast.success(text);
      } catch {
        /* swallow */
      }
    } catch {
      try {
        toast.error("Failed to resend email");
      } catch {}
    }
  };

  return (
    <div className="flex flex-col items-center justify-center h-screen gap-4">
      <h1 className="text-2xl font-bold">Email Verification</h1>

      {(status === "idle" || status === "loading") && <p>Verifying...</p>}

      {message && <p>{message}</p>}

      {status === "failed" && (
        <>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email"
            className="w-full max-w-sm rounded border border-gray-300 px-4 py-2"
          />

          <button
            onClick={resendEmail}
            className="px-4 py-2 bg-green-600 text-white rounded"
          >
            Resend Verification Email
          </button>
        </>
      )}
    </div>
  );
}
