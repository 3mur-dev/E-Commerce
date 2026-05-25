export function logServerError(response: Response, context?: string) {
  try {
    // Only log verbose details in non-production builds
    // Vite exposes import.meta.env.MODE
    // eslint-disable-next-line @typescript-eslint/strict-boolean-expressions
    if (typeof import.meta !== "undefined" && import.meta.env && import.meta.env.MODE !== "production") {
      response.clone().text().then((text) => {
        // eslint-disable-next-line no-console
        console.error("[devLogger] Server error", { status: response.status, context, body: text });
      }).catch(() => {
        // eslint-disable-next-line no-console
        console.error("[devLogger] Server error", { status: response.status, context, body: "<unavailable>" });
      });
    }
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("[devLogger] failed to log server error", err);
  }
}

export default logServerError;
