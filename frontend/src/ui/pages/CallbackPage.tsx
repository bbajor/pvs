import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userManager } from "../auth/oidc";

export function CallbackPage() {
  const nav = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        await userManager.signinRedirectCallback();
        nav("/", { replace: true });
      } catch (e) {
        setError(e instanceof Error ? e.message : "Unknown callback error");
      }
    })();
  }, [nav]);

  return (
    <div>
      <h2>Signing in…</h2>
      {error ? <pre style={{ color: "crimson" }}>{error}</pre> : null}
    </div>
  );
}

