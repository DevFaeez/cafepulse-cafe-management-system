import { createElement } from "react";
import { Box, Button, LinearProgress, MenuItem, Stack, Tab, Tabs, TextField, Typography } from "@mui/material";
import ArrowOutwardRounded from "@mui/icons-material/ArrowOutwardRounded";
import HourglassEmptyRounded from "@mui/icons-material/HourglassEmptyRounded";

export function Brand({ large = false }) {
    return (
        <span className={`cp-brand${large ? " cp-brand--large" : ""}`}>
            <span className="cp-brand-mark" aria-hidden="true"><i /><i /><i /></span>
            <span>CafePulse</span>
        </span>
    );
}

export function WorkspaceHeading({ title, children }) {
    return (
        <div className="cp-page-heading">
            <Typography component="h1" variant="h1">{title}</Typography>
            {children}
        </div>
    );
}

export const workspaceRailBreakpoint = "(min-width:1001px)";

export function WorkspaceNavigation({ label, prefix, items, current, change, vertical, mobileLabel, disabled }) {
    return (
        <nav className="cp-workspace-rail cp-workspace-rail--operations" aria-label={label}>
            {!vertical && mobileLabel ? (
                <TextField select size="small" label={mobileLabel} value={current} fullWidth
                    onChange={event => change(event.target.value)}>
                    {items.map(({ id, label: itemLabel, icon }) => <MenuItem key={id} value={id}>
                        <Stack component="span" direction="row" gap={1} alignItems="center">{icon}{itemLabel}</Stack>
                    </MenuItem>)}
                </TextField>
            ) : (
                <Tabs value={current} onChange={(_, next) => change(next)} orientation={vertical ? "vertical" : "horizontal"}
                    className="cp-workspace-tabs" aria-label={label}>
                    {items.map(({ id, label: itemLabel, icon, divider }) => (
                        <Tab key={id} label={itemLabel} value={id} icon={icon} iconPosition="start" disabled={disabled}
                            className={divider ? "cp-nav-divider" : undefined}
                            id={`${prefix}-tab-${id}`} aria-controls={`${prefix}-panel`} />
                    ))}
                </Tabs>
            )}
        </nav>
    );
}

export function EmptyState({ children, icon: Icon = HourglassEmptyRounded, action, compact = false }) {
    return (
        <div className={`cp-empty${compact ? " cp-empty--compact" : ""}`}>
            {createElement(Icon, { "aria-hidden": true, fontSize: "small" })}
            <Typography variant="body2" color="text.secondary">{children}</Typography>
            {action}
        </div>
    );
}

export function LoadingState({ children }) {
    return (
        <Stack role="status" spacing={1.25} className="cp-loading">
            <Typography variant="body2" color="text.secondary">{children}</Typography>
            <LinearProgress aria-hidden="true" />
        </Stack>
    );
}

export function WorkspaceLink({ component, to, children }) {
    return <Button component={component} to={to} variant="outlined" endIcon={<ArrowOutwardRounded fontSize="small" />}>
        {children}
    </Button>;
}

export function SkipLink() {
    return <Box component="a" className="cp-skip" href="#main-content">Skip to content</Box>;
}
