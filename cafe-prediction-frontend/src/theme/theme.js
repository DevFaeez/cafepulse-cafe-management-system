import { alpha, createTheme } from "@mui/material";

const sharedColors = {
    danger: "#972f33",
    amber: "#875216",
    info: "#35586a",
    success: "#386348",
};

const signInColors = {
    canvas: "#f5f2ea", paper: "#fffdf8", field: "#fffefa", ink: "#212720", muted: "#565e50",
    line: "#deded1", action: "#304734", actionDark: "#233628", subtle: "#e7ede0",
    outline: "#aeb3a5", disabled: "#f0efe7", mutedChip: "#edece3",
    tableHead: "#ebece2", tableRow: "#f7f6ef", secondary: "#9c452d",
};

const workspaceColors = {
    canvas: "#ffffff", paper: "#ffffff", field: "#ffffff", ink: "#191d26", muted: "#515967",
    line: "#e2e4e8", action: "#a94412", actionDark: "#87350c", subtle: "#fff1e7",
    outline: "#b4bac3", disabled: "#f1f3f5", mutedChip: "#eef0f3",
    tableHead: "#f1f3f5", tableRow: "#fafbfc", secondary: "#3d4a60",
};

const sans = '"Instrument Sans", "Segoe UI", sans-serif';

const buildTheme = colors => createTheme({
    cssVariables: true,
    palette: {
        mode: "light",
        primary: { main: colors.action, dark: colors.actionDark, contrastText: "#ffffff" },
        secondary: { main: colors.secondary, contrastText: "#ffffff" },
        background: { default: colors.canvas, paper: colors.paper },
        text: { primary: colors.ink, secondary: colors.muted },
        divider: colors.line,
        success: { main: sharedColors.success, contrastText: "#ffffff" },
        warning: { main: sharedColors.amber, contrastText: "#ffffff" },
        error: { main: sharedColors.danger, contrastText: "#ffffff" },
        info: { main: sharedColors.info, contrastText: "#ffffff" },
    },
    shape: { borderRadius: 8 },
    typography: {
        fontFamily: sans,
        h1: { fontSize: "clamp(2rem, 3.5vw, 3rem)", lineHeight: 1.04, letterSpacing: "-0.045em", fontWeight: 650 },
        h2: { fontSize: "clamp(1.6rem, 2.5vw, 2.15rem)", lineHeight: 1.12, letterSpacing: "-0.036em", fontWeight: 650 },
        h3: { fontSize: "1.4rem", lineHeight: 1.2, letterSpacing: "-0.026em", fontWeight: 650 },
        h4: { fontSize: "clamp(1.85rem, 3vw, 2.55rem)", lineHeight: 1.08, letterSpacing: "-0.04em", fontWeight: 650 },
        h5: { fontSize: "1.55rem", lineHeight: 1.16, letterSpacing: "-0.03em", fontWeight: 650 },
        h6: { fontSize: "1.18rem", lineHeight: 1.26, letterSpacing: "-0.018em", fontWeight: 650 },
        body1: { fontSize: "0.97rem", lineHeight: 1.5 },
        body2: { fontSize: "0.875rem", lineHeight: 1.48 },
        caption: { fontSize: "0.77rem", lineHeight: 1.4 },
        button: { textTransform: "none", fontSize: "0.875rem", fontWeight: 650, lineHeight: 1.3 },
    },
    components: {
        MuiButtonBase: {
            styleOverrides: {
                root: {
                    touchAction: "manipulation",
                    "&.Mui-focusVisible": { outline: `2px solid ${colors.action}`, outlineOffset: 2 },
                },
            },
        },
        MuiButton: {
            defaultProps: { disableElevation: true },
            styleOverrides: {
                root: { borderRadius: 7, minHeight: 42, padding: "8px 15px", flexShrink: 0, textAlign: "center" },
                sizeSmall: { minHeight: 37, padding: "6px 10px" },
                outlined: { borderColor: colors.outline, "&:hover": { borderColor: colors.action, backgroundColor: colors.subtle } },
                text: { "&:hover": { backgroundColor: alpha(colors.action, 0.08) } },
            },
        },
        MuiIconButton: {
            styleOverrides: {
                root: { minHeight: 42, minWidth: 42, borderRadius: 7 },
                sizeSmall: { minHeight: 38, minWidth: 38 },
            },
        },
        MuiPaper: { styleOverrides: { outlined: { borderColor: colors.line } } },
        MuiDialog: {
            styleOverrides: {
                paper: {
                    borderRadius: 12,
                    border: `1px solid ${colors.line}`,
                    boxShadow: "0 20px 72px rgba(23, 31, 21, .24)",
                    margin: 16,
                    maxHeight: "calc(100% - 32px)",
                    "&.MuiDialog-paperFullWidth": { width: "calc(100% - 32px)" },
                    "& > form": { display: "flex", flexDirection: "column", minHeight: 0, overflow: "hidden" },
                },
            },
        },
        MuiBackdrop: { styleOverrides: { root: { backgroundColor: "rgba(24, 32, 24, .62)" } } },
        MuiDialogTitle: {
            styleOverrides: { root: { padding: "21px 24px 18px", fontSize: "1.38rem", fontWeight: 650, letterSpacing: "-.025em" } },
        },
        MuiDialogContent: { styleOverrides: { root: { padding: "20px 24px", borderColor: colors.line } } },
        MuiDialogActions: {
            styleOverrides: { root: { padding: "14px 22px", flexWrap: "wrap", gap: 8, "& > :not(style) ~ :not(style)": { marginLeft: 0 } } },
        },
        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    background: colors.field,
                    "& .MuiOutlinedInput-notchedOutline": { borderColor: colors.outline },
                    "&:hover .MuiOutlinedInput-notchedOutline": { borderColor: colors.action },
                    "&.Mui-disabled": { background: colors.disabled },
                },
            },
        },
        MuiInputLabel: { styleOverrides: { root: { color: colors.muted, fontWeight: 500 } } },
        MuiFormControlLabel: { styleOverrides: { label: { fontSize: "0.95rem" } } },
        MuiChip: {
            styleOverrides: {
                root: { borderRadius: 5, fontWeight: 650 },
                filledDefault: { backgroundColor: colors.mutedChip, color: colors.ink },
                outlined: { borderColor: colors.outline },
                colorSuccess: { backgroundColor: "#dfece0", color: "#2d5136" },
                colorWarning: { backgroundColor: "#f6e7cd", color: "#704816" },
                colorError: { backgroundColor: "#f2dfdc", color: "#842d30" },
                colorInfo: { backgroundColor: "#e1eaf0", color: "#365567" },
            },
        },
        MuiTabs: { styleOverrides: { root: { minHeight: 44 }, indicator: { height: 2, borderRadius: 2 } } },
        MuiTab: {
            styleOverrides: {
                root: {
                    minHeight: 44, minWidth: "auto", padding: "8px 14px", fontSize: "0.875rem",
                    textTransform: "none", fontWeight: 650, color: colors.muted,
                    "& .MuiTab-iconWrapper": { marginBottom: 0 },
                },
            },
        },
        MuiAlert: {
            styleOverrides: {
                root: {
                    alignItems: "center",
                    border: "1px solid currentColor",
                    padding: "5px 12px",
                    "& .MuiAlert-action": { paddingTop: 0, marginRight: 0, alignSelf: "center" },
                    "@media (max-width:480px)": {
                        "& .MuiAlert-message": { flex: 1, minWidth: 0 },
                        "& .MuiAlert-action": { width: "100%", justifyContent: "end" },
                        flexWrap: "wrap",
                    },
                },
            },
        },
        MuiTableContainer: {
            styleOverrides: { root: { borderRadius: 6, border: `1px solid ${colors.line}`, marginTop: 10 } },
        },
        MuiTableCell: {
            styleOverrides: {
                root: { borderColor: colors.line, fontVariantNumeric: "tabular-nums", padding: "11px 14px" },
                head: { backgroundColor: colors.tableHead, fontWeight: 700, color: colors.ink, whiteSpace: "nowrap" },
            },
        },
        MuiTableRow: {
            styleOverrides: { root: { "tbody &:nth-of-type(even)": { backgroundColor: colors.tableRow } } },
        },
    },
});

export const lightTheme = buildTheme(signInColors);
export const workspaceTheme = buildTheme(workspaceColors);
