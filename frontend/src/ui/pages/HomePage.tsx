import { useEffect, useState } from "react";
import { useAuth } from "../auth/useAuth";
import { apiGet } from "../../util/api";

type MeResponse = {
  userId: string;
  preferredUsername: string;
  fullName: string;
  institutionId: number | null;
};

export function HomePage() {
  const { signoutRedirect } = useAuth();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setMe(await apiGet<MeResponse>("/api/v1/me"));
      } catch (e) {
        setError(e instanceof Error ? e.message : "Unknown error");
      }
    })();
  }, []);

  return (
    <div>
      <h2>Home</h2>
      {error ? <pre style={{ color: "crimson" }}>{error}</pre> : null}
      {me ? (
        <pre style={{ background: "#f5f5f5", padding: 12 }}>{JSON.stringify(me, null, 2)}</pre>
      ) : (
        <div>Loading profile…</div>
      )}
      <button onClick={() => void signoutRedirect()}>Logout</button>
    </div>
  );
}

