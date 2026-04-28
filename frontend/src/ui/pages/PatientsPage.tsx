import { useEffect, useState } from "react";
import { apiGet } from "../../util/api";

type PatientSummaryDto = {
  id: number;
  firstName: string;
  lastName: string;
  birth: string;
  insuranceNumber: string | null;
  privateInsurance: boolean;
};

type SliceResponse<T> = {
  items: T[];
  hasNext: boolean;
};

export function PatientsPage() {
  const [q, setQ] = useState("");
  const [data, setData] = useState<SliceResponse<PatientSummaryDto> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => {
      (async () => {
        try {
          const url = q ? `/api/v1/patients?q=${encodeURIComponent(q)}` : "/api/v1/patients";
          setData(await apiGet<SliceResponse<PatientSummaryDto>>(url));
          setError(null);
        } catch (e) {
          setError(e instanceof Error ? e.message : "Unknown error");
        }
      })();
    }, 250);
    return () => clearTimeout(t);
  }, [q]);

  return (
    <div>
      <h2>Patients</h2>
      <label style={{ display: "block", marginBottom: 8 }}>
        Search:{" "}
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Name…" />
      </label>
      {error ? <pre style={{ color: "crimson" }}>{error}</pre> : null}
      <ul>
        {(data?.items ?? []).map((p) => (
          <li key={p.id}>
            {p.lastName}, {p.firstName} ({p.birth})
          </li>
        ))}
      </ul>
      {data?.hasNext ? <div>More results available…</div> : null}
    </div>
  );
}

