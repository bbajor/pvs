import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "./useAuth";

export function RequireAuth({ element }: { element: React.ReactElement }) {
  const { user, isLoading, signinRedirect } = useAuth();
  const loc = useLocation();

  useEffect(() => {
    if (isLoading) return;
    if (user) return;
    void signinRedirect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading, user]);

  if (isLoading) return <div>Loading…</div>;
  if (!user) return <div>Redirecting to login… ({loc.pathname})</div>;
  return element;
}

