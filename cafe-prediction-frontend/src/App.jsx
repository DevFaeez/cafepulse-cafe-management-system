import { lazy, Suspense, useEffect, useState, useSyncExternalStore } from "react";
import { Alert, Box, Button, CssBaseline, Stack, ThemeProvider } from "@mui/material";
import LogoutRounded from "@mui/icons-material/LogoutRounded";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Provider } from "react-redux";
import { Brand, LoadingState, SkipLink } from "./components/WorkspaceUI";
import { lightTheme, workspaceTheme } from "./theme/theme";
import { createStore } from "./index";
import SignIn from "./features/auth/SignIn";
import { workspaces } from "./features/auth/workspaces";
import { currentSession, expireSession, signOut, subscribeToSession, verifySession } from "./api/session";
import "./App.css";
import "./components/workspace.css";
import "./layout/operations.css";
import "./features/order/pages/pos.css";

const CustomerLayout = lazy(() => import("./layout/CustomerLayout"));
const StaffLayout = lazy(() => import("./layout/StaffLayout"));
const ManagerLayout = lazy(() => import("./features/manager/ManagerLayout"));

function Workspace({ role }) {
    const [store] = useState(createStore);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const { pathname } = useLocation();
    const { path, name } = workspaces[role];
    const workspace = pathname === "/staff" ? "Staff" : name;

    async function handleSignOut() {
        setBusy(true);
        setError("");
        try {
            await signOut();
        } catch {
            setError("The server could not confirm sign-out. Try again.");
        } finally {
            setBusy(false);
        }
    }

    return (
        <Provider store={store}>
            <SkipLink />
            <header className="cp-workspace-header">
                <div className="cp-header-inner">
                    <div className="cp-header-identity">
                        <Brand />
                        <span className="cp-header-role">{workspace}</span>
                    </div>
                    <Button color="inherit" startIcon={<LogoutRounded fontSize="small" />} disabled={busy} onClick={handleSignOut}>
                        {busy ? "Signing out…" : "Sign out"}
                    </Button>
                </div>
            </header>
            {error && <div className="cp-shell-feedback"><Alert severity="error">{error}</Alert></div>}
            <Suspense fallback={<div className="cp-page"><LoadingState>Loading workspace…</LoadingState></div>}>
                <Routes>
                    {role === "ROLE_CUSTOMER" && <Route path="/kiosk" element={<CustomerLayout />} />}
                    {role !== "ROLE_CUSTOMER" && <Route path="/staff" element={<StaffLayout canCorrect={role === "ROLE_MANAGER"} />} />}
                    {role === "ROLE_MANAGER" && <Route path="/manager" element={<ManagerLayout />} />}
                    <Route path="*" element={<Navigate to={path} replace />} />
                </Routes>
            </Suspense>
        </Provider>
    );
}

export default function App() {
    const session = useSyncExternalStore(subscribeToSession, currentSession);
    const [checking, setChecking] = useState(() => Boolean(currentSession()));
    const [checkError, setCheckError] = useState(false);
    const [attempt, setAttempt] = useState(0);
    const sessionKey = session?.refreshToken;
    const expiresAt = session?.expiresAt;

    useEffect(() => {
        if (!checking) return;
        let cancelled = false;
        verifySession().then(() => {
            if (!cancelled) setChecking(false);
        }).catch(() => {
            if (cancelled) return;
            if (currentSession()) setCheckError(true);
            else setChecking(false);
        });
        return () => { cancelled = true; };
    }, [checking, attempt, sessionKey]);

    useEffect(() => {
        if (!sessionKey) return;
        const timeout = setTimeout(() => expireSession(sessionKey), Math.max(0, Date.parse(expiresAt) - Date.now()));
        return () => clearTimeout(timeout);
    }, [sessionKey, expiresAt]);

    let content;
    if (!session) {
        content = <SignIn />;
    } else if (checking) {
        content = (
            <Box component="main" className="cp-session-screen">
                <div className="cp-session-panel">
                    <Brand />
                    {checkError ? (
                        <Stack spacing={2}>
                            <Alert severity="error">We couldn't verify your session. Check your connection and try again.</Alert>
                            <Button variant="contained" onClick={() => { setCheckError(false); setAttempt(value => value + 1); }}>Try again</Button>
                        </Stack>
                    ) : <LoadingState>Verifying session…</LoadingState>}
                </div>
            </Box>
        );
    } else {
        content = <Workspace key={sessionKey} role={session.role} />;
    }

    const theme = session && !checking && session.role !== "ROLE_CUSTOMER" ? workspaceTheme : lightTheme;
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            {content}
        </ThemeProvider>
    );
}
