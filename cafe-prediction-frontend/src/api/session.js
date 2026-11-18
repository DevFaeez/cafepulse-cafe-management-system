import axios from "axios";
import { BASEURL } from "./configApi";

const storageKey = "cafepulse.session";
const authApi = axios.create({ baseURL: BASEURL, timeout: 10000 });
export const api = axios.create({ baseURL: BASEURL, timeout: 10000 });

function readSession() {
    try {
        const saved = JSON.parse(sessionStorage.getItem(storageKey));
        if (saved?.accessToken && saved?.refreshToken && Date.parse(saved.expiresAt) > Date.now()) {
            return saved;
        }
    } catch {
        // A malformed or unavailable browser store is treated as signed out.
    }
    return null;
}

let session = readSession();
const listeners = new Set();
let pendingRefresh;

function saveSession(next) {
    session = next;
    try {
        if (next) sessionStorage.setItem(storageKey, JSON.stringify(next));
        else sessionStorage.removeItem(storageKey);
    } catch {
        // The current tab can still use an in-memory session.
    }
    listeners.forEach(listener => listener());
}

export function subscribeToSession(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
}

export function currentSession() {
    return session;
}

export async function signIn(email, password) {
    const { data } = await authApi.post("/auth/signin", { email, password });
    const { accessToken, refreshToken, expiresAt, role } = data;
    saveSession({ accessToken, refreshToken, expiresAt, role });
    return role;
}

export async function verifySession() {
    const key = session?.refreshToken;
    if (!key) return;
    const { data } = await api.get("/auth/session");
    if (session?.refreshToken === key && session.role !== data.role) {
        saveSession({ ...session, role: data.role });
    }
}

export async function signOut() {
    const key = session?.refreshToken;
    if (!key) return;
    await authApi.post("/auth/logout", { refreshToken: key });
    if (session?.refreshToken === key) saveSession(null);
}

export function expireSession(key) {
    if (session?.refreshToken === key && Date.parse(session.expiresAt) <= Date.now()) {
        saveSession(null);
    }
}

function refreshSession(key) {
    if (pendingRefresh?.key === key) return pendingRefresh.promise;

    const promise = authApi.post("/auth/refresh", { refreshToken: key })
        .then(({ data }) => {
            if (session?.refreshToken === key) {
                saveSession({ ...session, accessToken: data.currentAccessToken });
            }
        })
        .catch(error => {
            if ([400, 401, 403].includes(error.response?.status) && session?.refreshToken === key) {
                saveSession(null);
            }
            throw error;
        })
        .finally(() => {
            if (pendingRefresh?.key === key) pendingRefresh = undefined;
        });
    pendingRefresh = { key, promise };
    return promise;
}

api.interceptors.request.use(config => {
    if (config.sessionKey && config.sessionKey !== session?.refreshToken) {
        throw new axios.CanceledError("Session changed");
    }
    if (session) {
        config.sessionKey = session.refreshToken;
        config.sentAccessToken = session.accessToken;
        config.headers.set("Authorization", `Bearer ${session.accessToken}`);
    }
    return config;
});

api.interceptors.response.use(response => response, async error => {
    const { config } = error;
    const key = config?.sessionKey;
    if (error.response?.status !== 401 || !key || session?.refreshToken !== key) throw error;
    if (config.retried) {
        saveSession(null);
        throw error;
    }

    if (config.sentAccessToken === session.accessToken) await refreshSession(key);
    if (session?.refreshToken !== key) throw error;
    config.retried = true;
    return api(config);
});

function updateSessionField(scope, field, value) {
    if (session?.refreshToken !== scope) return false;
    saveSession({ ...session, [field]: value });
    return true;
}

export function setCheckoutState(scope, checkout) {
    return updateSessionField(scope, "checkout", checkout);
}

export function setStockMovementDraft(scope, draft) {
    return updateSessionField(scope, "stockMovementDraft", draft);
}
