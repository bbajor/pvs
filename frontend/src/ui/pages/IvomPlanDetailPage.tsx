import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiGet } from "../../util/api";

type IvomTreatmentDto = {
  id: number;
  sideOfEye: string | null;
  date: string | null;
  startTime: string | null;
  endTime: string | null;
  surgicalCenterName: string | null;
  medicationName: string | null;
  frequency: string | null;
  dosage: string | null;
  status: string | null;
};

type IvomPlanDetailDto = {
  id: number;
  createdDate: string | null;
  finishedDate: string | null;
  description: string | null;
  additionalInformation: string | null;
  patient: {
    id: number;
    firstName: string;
    lastName: string;
    birth: string;
    insuranceLabel: string | null;
  } | null;
  diagnosis: { id: number; name: string; icdCode: string | null } | null;
  findings: {
    subretinalFluid: boolean | null;
    intraretinalFluidIncrease: boolean | null;
    serousRpeDetachmentIncrease: boolean | null;
    newRetinalHemorrhage: boolean | null;
    visualAcuityInitialLeft: string | null;
    visualAcuityInitialRight: string | null;
  };
  treatments: IvomTreatmentDto[];
};

export function IvomPlanDetailPage() {
  const { id } = useParams();
  const [data, setData] = useState<IvomPlanDetailDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        if (!id) throw new Error("Missing id");
        setData(await apiGet<IvomPlanDetailDto>(`/api/v1/ivom-plans/${encodeURIComponent(id)}`));
        setError(null);
      } catch (e) {
        setError(e instanceof Error ? e.message : "Unknown error");
      }
    })();
  }, [id]);

  return (
    <div>
      <Link to="/ivom">Back</Link>
      <h2>IVOM plan</h2>
      {error ? <pre style={{ color: "crimson" }}>{error}</pre> : null}
      {!data ? <div>Loading…</div> : null}
      {data ? (
        <>
          <pre style={{ background: "#f5f5f5", padding: 12 }}>{JSON.stringify(data.patient, null, 2)}</pre>
          <div>
            <strong>Diagnosis:</strong> {data.diagnosis?.name ?? "-"} {data.diagnosis?.icdCode ? `(${data.diagnosis.icdCode})` : ""}
          </div>
          <div style={{ marginTop: 12 }}>
            <strong>Treatments</strong>
            <ul>
              {data.treatments.map((t) => (
                <li key={t.id}>
                  {t.date} {t.startTime ? `${t.startTime}-${t.endTime}` : ""} {t.sideOfEye ?? ""}{" "}
                  {t.surgicalCenterName ? `@ ${t.surgicalCenterName}` : ""}{" "}
                  {t.medicationName ? `– ${t.medicationName}` : ""}{" "}
                  {t.status ? `(${t.status})` : ""}
                </li>
              ))}
            </ul>
          </div>
        </>
      ) : null}
    </div>
  );
}

