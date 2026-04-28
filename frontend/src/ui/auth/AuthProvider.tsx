import React, { createContext, useEffect, useMemo, useState } from "react";
import type { User } from "oidc-client-ts";
import { userManager } from "./oidc";

export type AuthContextValue = {
  user: User | null;
  isLoading: boolean;
  signinRedirect: () => Promise<void>;
  signoutRedirect: () => Promise<void>;
  getAccessToken: () => Promise<string>;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    (async () => {
      const u = await userManager.getUser();
      if (!isMounted) return;
      setUser(u);
      setIsLoading(false);
    })();

    const onUserLoaded = (u: User) => setUser(u);
    const onUserUnloaded = () => setUser(null);
    userManager.events.addUserLoaded(onUserLoaded);
    userManager.events.addUserUnloaded(onUserUnloaded);

    return () => {
      isMounted = false;
      userManager.events.removeUserLoaded(onUserLoaded);
      userManager.events.removeUserUnloaded(onUserUnloaded);
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      signinRedirect: async () => userManager.signinRedirect(),
      signoutRedirect: async () => userManager.signoutRedirect(),
      getAccessToken: async () => {
        const u = await userManager.getUser();
        if (!u || !u.access_token) throw new Error("Not authenticated");
        return u.access_token;
      },
    }),
    [user, isLoading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

