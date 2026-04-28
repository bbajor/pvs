import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiGet } from "../../util/api";

type IvomPlanSummaryDto = {
  id: number;
  patientLastName: string | null;
  patientFirstName: string | null;
  patientBirth: string | null;
  diagnosisName: string | null;
  createdDate: string | null;
  finishedDate: string | null;
};

type SliceResponse<T> = {
  items: T[];
  hasNext: boolean;
};

export function IvomPlansPage() {
  const [q, setQ] = useState("");
  const [data, setData] = useState<SliceResponse<IvomPlanSummaryDto> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = setTimeout(() => {
      (async () => {
        try {
          const url = q ? `/api/v1/ivom-plans?q=${encodeURIComponent(q)}` : "/api/v1/ivom-plans";
          setData(await apiGet<SliceResponse<IvomPlanSummaryDto>>(url));
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
      <h2>IVOM plans</h2>
      <label style={{ display: "block", marginBottom: 8 }}>
        Search: <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Name, ICD, insurer…" />
      </label>
      {error ? <pre style={{ color: "crimson" }}>{error}</pre> : null}
      <ul>
        {(data?.items ?? []).map((p) => (
          <li key={p.id}>
            <Link to={`/ivom/${p.id}`}>
              {p.patientLastName}, {p.patientFirstName} {p.patientBirth ? `(${p.patientBirth})` : ""}{" "}
              {p.diagnosisName ? `– ${p.diagnosisName}` : ""}
            </Link>
            {p.finishedDate ? <span> (finished {p.finishedDate})</span> : null}
          </li>
        ))}
      </ul>
      {data?.hasNext ? <div>More results available…</div> : null}
    </div>
  );
}

