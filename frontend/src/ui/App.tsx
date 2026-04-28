import { Link, Route, Routes } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import { CallbackPage } from "./pages/CallbackPage";
import { HomePage } from "./pages/HomePage";
import { IvomPlanDetailPage } from "./pages/IvomPlanDetailPage";
import { IvomPlansPage } from "./pages/IvomPlansPage";
import { PatientsPage } from "./pages/PatientsPage";

export function App() {
  return (
    <div style={{ fontFamily: "system-ui, sans-serif", padding: 16 }}>
      <header style={{ display: "flex", gap: 12, alignItems: "center" }}>
        <strong>PVS</strong>
        <nav style={{ display: "flex", gap: 12 }}>
          <Link to="/">Home</Link>
          <Link to="/patients">Patients</Link>
          <Link to="/ivom">IVOM</Link>
        </nav>
      </header>

      <main style={{ marginTop: 16 }}>
        <Routes>
          <Route path="/auth/callback" element={<CallbackPage />} />
          <Route path="/" element={<RequireAuth element={<HomePage />} />} />
          <Route path="/patients" element={<RequireAuth element={<PatientsPage />} />} />
          <Route path="/ivom" element={<RequireAuth element={<IvomPlansPage />} />} />
          <Route path="/ivom/:id" element={<RequireAuth element={<IvomPlanDetailPage />} />} />
        </Routes>
      </main>
    </div>
  );
}

