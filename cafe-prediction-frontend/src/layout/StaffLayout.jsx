import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Button, Stack, Typography, useMediaQuery } from "@mui/material";
import { Link } from "react-router-dom";
import ReceiptLongOutlined from "@mui/icons-material/ReceiptLongOutlined";
import PaymentsOutlined from "@mui/icons-material/PaymentsOutlined";
import HistoryRounded from "@mui/icons-material/HistoryRounded";
import Inventory2Outlined from "@mui/icons-material/Inventory2Outlined";
import RefreshRounded from "@mui/icons-material/RefreshRounded";
import { EmptyState, LoadingState, WorkspaceHeading, WorkspaceLink, WorkspaceNavigation, workspaceRailBreakpoint } from "../components/WorkspaceUI";
import { api } from "../api/session";
import StaffInventoryPage from "../features/Inventory/pages/StaffInventoryPages";
import StaffOrderCard from "../features/order/staff/StaffOrderCard";
import StaffOrderDialog from "../features/order/staff/StaffOrderDialog";
import { activePayment, formatMoney } from "../features/order/staff/orderDisplay";

const pollInterval = 4000;
const lastUpdateFormat = new Intl.DateTimeFormat("en-MY", { timeStyle: "medium", timeZone: "Asia/Kuala_Lumpur" });

const staffTabs = [
    { id: "orders", label: "Orders", icon: <ReceiptLongOutlined fontSize="small" /> },
    { id: "payments", label: "Payments", icon: <PaymentsOutlined fontSize="small" /> },
    { id: "history", label: "History", icon: <HistoryRounded fontSize="small" /> },
    { id: "inventory", label: "Inventory", icon: <Inventory2Outlined fontSize="small" /> },
];

function OrderGroup({ title, orders, empty, lane, history, ...actions }) {
    const isPaymentLane = lane === "unpaid" || lane === "paid";
    return (
        <section className={`cp-order-lane${lane ? ` cp-order-lane--${lane}` : ""}`}>
            <div className="cp-lane-header">
                <Typography component={isPaymentLane ? "h3" : "h2"} variant="h6">{title}</Typography>
                <span className="cp-lane-count" aria-label={`${orders.length} orders`}>{orders.length}</span>
            </div>
            {orders.length > 0 ? <div className={history ? "cp-order-history" : "cp-order-tickets"}>
                {orders.map(order => <StaffOrderCard key={order.id} order={order} heading={isPaymentLane ? "h4" : "h3"} {...actions} />)}
            </div> : <div className="cp-lane-empty"><EmptyState icon={ReceiptLongOutlined}>{empty}</EmptyState></div>}
        </section>
    );
}

function PaymentBoard({ orders, actions }) {
    const unpaid = orders.filter(order => !activePayment(order));
    const paid = orders.filter(order => activePayment(order));
    const total = group => formatMoney(group.reduce((sum, order) => sum + Number(order.totalAmount), 0));

    return <>
        <Stack direction="row" flexWrap="wrap" gap={1.25} alignItems="center" justifyContent="space-between" className="cp-section-heading">
            <Typography component="h2" variant="h6">Payments for active orders</Typography>
            <dl className="cp-payment-totals">
                <div><dt>Outstanding</dt><dd>{total(unpaid)}</dd></div>
                <div><dt>Recorded</dt><dd>{total(paid)}</dd></div>
            </dl>
        </Stack>
        <div className="cp-order-grid">
            <OrderGroup lane="unpaid" title="Awaiting payment" orders={unpaid} empty="No orders are awaiting payment." {...actions} />
            <OrderGroup lane="paid" title="Payment recorded" orders={paid} empty="No payments recorded for active orders." {...actions} />
        </div>
    </>;
}

export default function StaffLayout({ canCorrect = false }) {
    const desktopNavigation = useMediaQuery(workspaceRailBreakpoint);
    const [tab, setTab] = useState("orders");
    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(null);
    const [offline, setOffline] = useState(false);
    const [busy, setBusy] = useState(false);
    const [action, setAction] = useState(null);
    const [actionError, setActionError] = useState("");
    const request = useRef(null);
    const updating = useRef(false);

    const endpoint = tab === "inventory" ? null
        : tab === "history" ? `/api/order/history?page=${page}` : "/api/order/workbench";

    const refresh = useCallback(async (supersede = false) => {
        if (!endpoint || (request.current && !supersede)) return;
        request.current?.abort();
        const controller = new AbortController();
        request.current = controller;
        try {
            const { data } = await api.get(endpoint, { signal: controller.signal });
            if (controller.signal.aborted) return;
            setSnapshot({ endpoint, data, updatedAt: new Date() });
            setOffline(false);
        } catch {
            if (!controller.signal.aborted) setOffline(true);
        } finally {
            if (request.current === controller) request.current = null;
        }
    }, [endpoint]);

    useEffect(() => {
        if (!endpoint) return;
        refresh();
        const timer = setInterval(refresh, pollInterval);
        return () => {
            clearInterval(timer);
            request.current?.abort();
            request.current = null;
        };
    }, [endpoint, refresh]);

    async function modify(path, method, payload) {
        if (updating.current) return;
        updating.current = true;
        setBusy(true);
        setActionError("");
        try {
            await api[method](path, payload);
            setAction(null);
        } catch (error) {
            const status = error.response?.status;
            if (status === 409 || !status) setAction(null);
            setActionError(error.response?.data?.description ?? "We couldn't confirm the action. Check the record before trying again.");
        } finally {
            await refresh(true);
            updating.current = false;
            setBusy(false);
        }
    }

    function changeTab(next) {
        setTab(next);
        setPage(0);
        setActionError("");
    }

    const data = snapshot?.endpoint === endpoint ? snapshot.data : null;
    const active = Array.isArray(data) ? data : [];
    const actions = {
        busy,
        canCorrect,
        onPayment: order => { setActionError(""); setAction({ order }); },
        onCorrect: (order, payment) => { setActionError(""); setAction({ order, payment }); },
        onCloseOrder: (order, refund) => { setActionError(""); setAction({ order, refund }); },
        onStatus: (order, status) => modify(`/api/order/${order.id}/status`, "put", { revision: order.revision, status }),
    };

    return (
        <div className="cp-workspace-shell cp-workspace-shell--operations">
            <WorkspaceNavigation label="Staff workspace" prefix="staff" items={staffTabs} current={tab}
                change={changeTab} vertical={desktopNavigation} disabled={busy} />
            <main id="main-content" tabIndex={-1} className="cp-workspace-main">
                <WorkspaceHeading title="Staff dashboard">
                    <div className="cp-workspace-tools">
                        {tab !== "inventory" && <>
                            <Typography variant="body2" color="text.secondary" className="cp-sync-label">
                                <span data-connection={offline ? "offline" : data ? "ready" : "waiting"} aria-hidden="true" />
                                {data ? `Last updated ${lastUpdateFormat.format(snapshot.updatedAt)}` : "Waiting for an update"}
                            </Typography>
                            <Button size="small" variant="outlined" startIcon={<RefreshRounded />} onClick={() => refresh(true)} disabled={busy}>Refresh</Button>
                        </>}
                        {canCorrect && <WorkspaceLink component={Link} to="/manager">Manager console</WorkspaceLink>}
                    </div>
                </WorkspaceHeading>
                <Stack spacing={2.25} component="section" id="staff-panel" role="tabpanel" aria-labelledby={`staff-tab-${tab}`}>
                    {tab === "inventory" ? <StaffInventoryPage manager={canCorrect} /> : <>
                        {offline && <Alert severity="warning">Connection lost. The information shown may be out of date. The dashboard will retry automatically.</Alert>}
                        {!action && actionError && <Alert severity="error">{actionError}</Alert>}
                        {!data ? !offline && <LoadingState>Loading orders…</LoadingState> : tab === "orders" ? (
                            <div className="cp-order-grid">
                                <OrderGroup lane="pending" title="Waiting" orders={active.filter(order => order.status === "ORDER_PENDING")}
                                    empty="No orders are waiting." {...actions} />
                                <OrderGroup lane="preparing" title="In preparation" orders={active.filter(order => order.status === "ORDER_PREPARING")}
                                    empty="No orders are being prepared." {...actions} />
                            </div>
                        ) : tab === "payments" ? (
                            <PaymentBoard orders={active} actions={actions} />
                        ) : <>
                            <OrderGroup title="Closed orders" orders={data.orders} empty="No closed orders." history {...actions} />
                            <Stack direction="row" spacing={1} alignItems="center" justifyContent="center" className="cp-pagination">
                                <Button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button>
                                <Typography variant="body2">Page {page + 1}</Typography>
                                <Button disabled={!data.hasNext} onClick={() => setPage(page + 1)}>Next</Button>
                            </Stack>
                        </>}
                    </>}
                </Stack>
                {action && <StaffOrderDialog action={action} busy={busy} error={actionError} onClose={() => { setAction(null); setActionError(""); }}
                    onSubmit={body => modify(`/api/order/${action.order.id}/${action.refund !== undefined ? "close" : action.payment ? "payment/void" : "payment"}`, "post", body)} />}
            </main>
        </div>
    );
}
