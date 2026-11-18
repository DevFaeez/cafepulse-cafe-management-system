import { useState } from "react";
import { Alert, Button, Stack, TextField, Typography } from "@mui/material";
import ArrowForwardRounded from "@mui/icons-material/ArrowForwardRounded";
import { Brand } from "../../components/WorkspaceUI";
import { useNavigate } from "react-router-dom";
import { signIn } from "../../api/session";
import { workspaces } from "./workspaces";

export default function SignIn() {
    const navigate = useNavigate();
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(event) {
        event.preventDefault();
        const fields = new FormData(event.currentTarget);
        setError("");
        setBusy(true);
        try {
            const role = await signIn(fields.get("email").trim(), fields.get("password"));
            navigate(workspaces[role].path, { replace: true });
        } catch (failure) {
            const status = failure.response?.status;
            setError(status === 401
                ? "Incorrect email or password."
                : status === 400
                    ? "Check your email address and password."
                    : "We couldn't reach the server. Try again.");
        } finally {
            setBusy(false);
        }
    }

    return (
        <main className="cp-signin">
            <div className="cp-signin-photo" aria-hidden="true">
                <img src="/media/cafe-login.webp" alt="" fetchPriority="high" />
            </div>
            <div className="cp-signin-body">
                <Brand large />
                <Stack component="form" className="cp-signin-form" spacing={2.5} onSubmit={handleSubmit}>
                    <div>
                        <Typography component="h1" className="cp-signin-title">Sign in</Typography>
                        <Typography color="text.secondary" sx={{ mt: 1 }}>Access the kiosk, staff dashboard or manager console.</Typography>
                    </div>
                    {error && <Alert severity="error">{error}</Alert>}
                    <TextField name="email" label="Email" type="email" autoComplete="username" required slotProps={{ htmlInput: { maxLength: 100 } }} />
                    <TextField name="password" label="Password" type="password" autoComplete="current-password" required slotProps={{ htmlInput: { maxLength: 72 } }} />
                    <Button type="submit" variant="contained" endIcon={<ArrowForwardRounded />} disabled={busy} sx={{ py: 1.5 }}>
                        {busy ? "Signing in…" : "Sign in"}
                    </Button>
                </Stack>
            </div>
        </main>
    );
}
