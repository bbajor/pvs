import { userManager } from "../ui/auth/oidc";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() ?? "";

async function accessToken(): Promise<string> {
  const user = await userManager.getUser();
  if (!user || !user.access_token) {
    throw new Error("Not authenticated");
  }
  return user.access_token;
}

export async function apiGet<T>(path: string): Promise<T> {
  const token = await accessToken();
  const res = await fetch(toApiUrl(path), {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });
  if (!res.ok) {
    const body = await safeText(res);
    throw new Error(`${res.status} ${res.statusText}${body ? `: ${body}` : ""}`);
  }
  return (await res.json()) as T;
}

function toApiUrl(path: string): string {
  if (!apiBaseUrl) {
    return path;
  }
  const normalizedBase = apiBaseUrl.endsWith("/") ? apiBaseUrl.slice(0, -1) : apiBaseUrl;
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

async function safeText(res: Response) {
  try {
    return await res.text();
  } catch {
    return "";
  }
}

