import { UserManager, WebStorageStateStore } from "oidc-client-ts";

const authority = import.meta.env.VITE_OIDC_AUTHORITY as string | undefined;
const clientId = import.meta.env.VITE_OIDC_CLIENT_ID as string | undefined;
const redirectUri = import.meta.env.VITE_OIDC_REDIRECT_URI as string | undefined;

if (!authority || !clientId || !redirectUri) {
  // Intentionally throw early: misconfigured auth should fail fast.
  throw new Error("Missing OIDC env vars: VITE_OIDC_AUTHORITY, VITE_OIDC_CLIENT_ID, VITE_OIDC_REDIRECT_URI");
}

export const userManager = new UserManager({
  authority,
  client_id: clientId,
  redirect_uri: redirectUri,
  response_type: "code",
  scope: (import.meta.env.VITE_OIDC_SCOPE as string | undefined) ?? "openid profile",
  post_logout_redirect_uri: (import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI as string | undefined) ?? window.location.origin,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
});

